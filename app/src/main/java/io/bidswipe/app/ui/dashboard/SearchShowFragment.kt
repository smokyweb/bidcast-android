package io.bidswipe.app.ui.dashboard

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.tabs.TabLayout
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.UnifiedSearchResultAdapter
import io.bidswipe.app.databinding.FragmentSearchShowBinding
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.SearchPagination
import io.bidswipe.app.network.response.SearchPaginationMeta
import io.bidswipe.app.network.response.SearchProduct
import io.bidswipe.app.network.response.SearchShow
import io.bidswipe.app.network.response.SearchUser
import io.bidswipe.app.ui.sellerProfile.SellerProfileActivity
import io.bidswipe.app.ui.product.ProductDetailsActivity
import io.bidswipe.app.ui.watchStream.ViewLiveShowActivity
import io.bidswipe.app.utils.hideKeyboard
import io.bidswipe.app.utils.isBrowseAuthError
import io.bidswipe.app.utils.isTablet
import io.bidswipe.app.utils.setHapticClickListener
import io.bidswipe.app.utils.showKeyboard

/**
 * Basecamp #9960348333 — dedicated search results page, REBUILT (round 7, 2026-06-04).
 *
 * This screen is the single search-results surface for the whole app: both the
 * Home search bar and the Explore search bar navigate here. It mirrors the PWA
 * search page (resources/views/app/search.blade.php):
 *
 *   - One POST /api/v1/search call returns { shows, products, users }.
 *   - Results are shown in TABS: Shows / Products / Users (counts in labels).
 *   - Default tab = Shows (Larry's call: livestream-shopping app, shows lead).
 *   - A single "No results found" appears only when ALL THREE are empty.
 *   - Clearing the query clears results; input is debounced (350 ms).
 *
 * Prior rounds bounced because they fired get-live-show (which needs a `type`
 * and returned 0 shows for many queries) in parallel with unifiedSearch, then
 * raced two empty-state toggles. We no longer use get-live-show here — the PWA
 * doesn't either. The unified search response already carries the show list.
 */
@SuppressLint("NotifyDataSetChanged")
class SearchShowFragment : BaseFragment<DashViewModel, FragmentSearchShowBinding>() {

	override fun getModel(): Class<DashViewModel> = DashViewModel::class.java

	override fun getBind(
		inflater: LayoutInflater,
		view: ViewGroup?,
	) = FragmentSearchShowBinding.inflate(inflater, view, false)

	// Active filter state, persisted across keystrokes + filter-sheet opens.
	private var browseFilters: BrowseFilters = BrowseFilters()

	private lateinit var resultsAdapter: UnifiedSearchResultAdapter

	// Last unified-search result sets for the current query. Rendered per tab.
	private var shows: List<SearchShow> = emptyList()
	private var products: List<SearchProduct> = emptyList()
	private var users: List<SearchUser> = emptyList()

	private data class SearchPageState(
		var currentPage: Int = 1,
		var lastPage: Int = 1,
		var total: Int = 0,
	)

	private var showsPage = SearchPageState()
	private var productsPage = SearchPageState()
	private var usersPage = SearchPageState()
	private var pendingPagingTab: Int? = null
	private var pendingPage = 1

	private var searchDebounce: android.os.Handler? = null

	// Tab indices (must match the TabItem order in fragment_search_show.xml).
	private companion object {
		const val TAB_SHOWS = 0
		const val TAB_PRODUCTS = 1
		const val TAB_USERS = 2
		const val DEBOUNCE_MS = 350L
		const val SEARCH_PER_PAGE = 20
	}

	private var activeTab = TAB_SHOWS

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		bind.header.onBackClick {
			findNavController().popBackStack()
		}

		bind.root.setHapticClickListener {
			hideKeyboard(it)
		}

		// Results adapter — Shows render as a 2-col grid, Products/Users as
		// full-width rows. The GridLayoutManager + SpanSizeLookup handle that.
		resultsAdapter = UnifiedSearchResultAdapter(
			onUserClick = { user: SearchUser ->
				startActivity(
					Intent(mCtx, SellerProfileActivity::class.java)
						.putExtra("sellerId", (user.id ?: 0).toString())
				)
			},
			onProductClick = { product: SearchProduct ->
				if (product.id != null) {
					startActivity(
						Intent(mCtx, ProductDetailsActivity::class.java)
							.putExtra("productId", product.id.toString())
					)
				}
			},
			onShowClick = { show: SearchShow ->
				if (show.isLive == true) {
					startActivity(
						Intent(mCtx, ViewLiveShowActivity::class.java)
							.putExtra("showId", (show.id ?: 0).toString())
							.putExtra("userId", show.userId?.toString() ?: show.user?.id?.toString() ?: "")
					)
				} else {
					startActivity(
						Intent(mCtx, SellerProfileActivity::class.java)
							.putExtra("sellerId", (show.user?.id ?: show.userId ?: 0).toString())
					)
				}
			},
			onPageClick = { page ->
				goToPage(page)
			},
		)
		val spanCount = if (resources.isTablet()) 3 else 2
		val gridLm = GridLayoutManager(mCtx, spanCount)
		gridLm.spanSizeLookup = resultsAdapter.makeSpanSizeLookup(spanCount)
		bind.resultsRecycler.layoutManager = gridLm
		bind.resultsRecycler.adapter = resultsAdapter

		bind.searchTabBar.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
			override fun onTabSelected(tab: TabLayout.Tab) {
				activeTab = tab.position
				renderActiveTab()
			}

			override fun onTabUnselected(tab: TabLayout.Tab) {}
			override fun onTabReselected(tab: TabLayout.Tab) {}
		})

		bind.filterBtn.setHapticClickListener {
			BrowseFiltersSheet(initial = browseFilters) { applied ->
				browseFilters = applied
				runSearch(resetPages = true)
			}.show(childFragmentManager, "browse_filters")
		}

		// Pre-fill a query passed in from Home/Explore (set BEFORE the watcher is
		// attached so it doesn't double-fire).
		arguments?.getString("query")?.takeIf { it.isNotBlank() }?.let { q ->
			bind.search.setText(q)
			bind.search.setSelection(q.length)
		}

		bind.searchLayout.setEndIconOnClickListener {
			bind.search.setText("")
			hideKeyboard(it)
		}

		bind.search.requestFocus()
		showKeyboard(bind.search)

		bind.search.addTextChangedListener(object : TextWatcher {
			override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
			override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
			override fun afterTextChanged(p0: Editable?) {
				searchDebounce?.removeCallbacksAndMessages(null)
				val q = p0?.toString()?.trim().orEmpty()
				if (q.isEmpty()) {
					// Query cleared → clear results immediately, no stale rows.
					clearResults()
					return
				}
				searchDebounce = android.os.Handler(android.os.Looper.getMainLooper())
				searchDebounce?.postDelayed({ runSearch(resetPages = true) }, DEBOUNCE_MS)
			}
		})

		observeSearch()

		// Initial query (if pre-filled) → search now.
		if (bind.search.text?.toString()?.trim().orEmpty().isNotEmpty()) {
			runSearch(resetPages = true)
		}
	}

	private fun runSearch(
		page: Int = 1,
		pagingTab: Int? = null,
		resetPages: Boolean = false,
	) {
		val q = bind.search.text?.toString()?.trim().orEmpty()
		if (q.isEmpty()) {
			clearResults()
			return
		}
		if (resetPages || pagingTab == null) {
			resetPageState()
		}
		pendingPagingTab = pagingTab
		pendingPage = page.coerceAtLeast(1)
		bind.loader.isVisible = true
		viewModel.unifiedSearch(
			q,
			page = pendingPage,
			perPage = SEARCH_PER_PAGE,
			categoryIds = browseFilters.categoryIds.ifEmpty { null },
			subCategoryIds = browseFilters.subCategoryIds.ifEmpty { null },
			showFormat = browseFilters.showFormat,
			tag = browseFilters.tag?.takeIf { it.isNotBlank() },
			premierShop = if (browseFilters.premierShop) true else null,
			shipping = browseFilters.shipping,
		)
	}

	private fun observeSearch() {
		viewModel.unifiedSearchRepo.observe(viewLifecycleOwner) { resource ->
			when (resource) {
				is Resource.Success -> {
					bind.loader.isVisible = false
					val data = resource.value.data
					val requestedTab = pendingPagingTab
					val requestedPage = data?.pagination?.page ?: pendingPage
					if (requestedTab == null) {
						shows = data?.shows.orEmpty()
						products = data?.products.orEmpty()
						users = data?.users.orEmpty()
						updateAllPageState(data?.pagination)
					} else {
						updatePagedTab(requestedTab, data, requestedPage)
					}
					updateTabCounts()
					renderActiveTab()
					if (requestedTab != null) {
						bind.resultsRecycler.scrollToPosition(0)
					}
					pendingPagingTab = null
					pendingPage = 1
				}

				is Resource.Error -> {
					bind.loader.isVisible = false
					// Don't pop a scary dialog — just clear and log. Auth errors
					// are browse-token errors that shouldn't block guest search.
					shows = emptyList()
					products = emptyList()
					users = emptyList()
					resetPageState()
					updateTabCounts()
					renderActiveTab()
					pendingPagingTab = null
					pendingPage = 1
					if (!resource.isBrowseAuthError()) {
						android.util.Log.w(
							"SearchShowFragment",
							"unifiedSearch error: ${resource.errorResponse?.message}"
						)
					}
				}

				else -> {}
			}
		}
	}

	private fun clearResults() {
		searchDebounce?.removeCallbacksAndMessages(null)
		shows = emptyList()
		products = emptyList()
		users = emptyList()
		resetPageState()
		resultsAdapter.submitList(emptyList())
		bind.searchTabBar.isVisible = false
		bind.noData.isVisible = false
		bind.loader.isVisible = false
		pendingPagingTab = null
		pendingPage = 1
	}

	private fun updateTabCounts() {
		bind.searchTabBar.getTabAt(TAB_SHOWS)?.text = "Shows (${tabTotal(TAB_SHOWS, shows.size)})"
		bind.searchTabBar.getTabAt(TAB_PRODUCTS)?.text = "Products (${tabTotal(TAB_PRODUCTS, products.size)})"
		bind.searchTabBar.getTabAt(TAB_USERS)?.text = "Users (${tabTotal(TAB_USERS, users.size)})"
	}

	/** Render the currently-selected tab; resolve the single empty state. */
	private fun renderActiveTab() {
		val query = bind.search.text?.toString()?.trim().orEmpty()
		val allEmpty = totalResultCount() == 0

		// Tabs visible whenever there's a non-blank query AND at least one result.
		bind.searchTabBar.isVisible = query.isNotBlank() && !allEmpty
		bind.noData.isVisible = query.isNotBlank() && allEmpty

		if (query.isBlank() || allEmpty) {
			resultsAdapter.submitList(emptyList())
			return
		}

		when (activeTab) {
			TAB_PRODUCTS -> resultsAdapter.submitProductsOnly(products, productsPage.currentPage, productsPage.lastPage)
			TAB_USERS -> resultsAdapter.submitUsersOnly(users, usersPage.currentPage, usersPage.lastPage)
			else -> resultsAdapter.submitShowsOnly(shows, showsPage.currentPage, showsPage.lastPage)
		}
		bind.resultsRecycler.isVisible = true
	}

	private fun goToPage(page: Int) {
		val state = pageStateFor(activeTab)
		val target = page.coerceIn(1, state.lastPage.coerceAtLeast(1))
		if (target == state.currentPage) return
		runSearch(page = target, pagingTab = activeTab)
	}

	private fun updatePagedTab(
		tab: Int,
		data: io.bidswipe.app.network.response.SearchData?,
		requestedPage: Int,
	) {
		when (tab) {
			TAB_PRODUCTS -> {
				products = data?.products.orEmpty()
				updatePageState(productsPage, data?.pagination?.products, requestedPage, products.size)
			}
			TAB_USERS -> {
				users = data?.users.orEmpty()
				updatePageState(usersPage, data?.pagination?.users, requestedPage, users.size)
			}
			else -> {
				shows = data?.shows.orEmpty()
				updatePageState(showsPage, data?.pagination?.shows, requestedPage, shows.size)
			}
		}
	}

	private fun updateAllPageState(pagination: SearchPagination?) {
		val page = pagination?.page ?: 1
		updatePageState(showsPage, pagination?.shows, page, shows.size)
		updatePageState(productsPage, pagination?.products, page, products.size)
		updatePageState(usersPage, pagination?.users, page, users.size)
	}

	private fun updatePageState(
		state: SearchPageState,
		meta: SearchPaginationMeta?,
		page: Int,
		fallbackCount: Int,
	) {
		state.total = meta?.total ?: fallbackCount
		state.lastPage = (meta?.lastPage ?: 1).coerceAtLeast(1)
		state.currentPage = if (state.total > 0) {
			page.coerceAtLeast(1).coerceAtMost(state.lastPage)
		} else {
			1
		}
	}

	private fun resetPageState() {
		showsPage = SearchPageState()
		productsPage = SearchPageState()
		usersPage = SearchPageState()
	}

	private fun pageStateFor(tab: Int): SearchPageState = when (tab) {
		TAB_PRODUCTS -> productsPage
		TAB_USERS -> usersPage
		else -> showsPage
	}

	private fun tabTotal(tab: Int, fallbackCount: Int): Int {
		val total = pageStateFor(tab).total
		return if (total > 0) total else fallbackCount
	}

	private fun totalResultCount(): Int =
		tabTotal(TAB_SHOWS, shows.size) + tabTotal(TAB_PRODUCTS, products.size) + tabTotal(TAB_USERS, users.size)
}
