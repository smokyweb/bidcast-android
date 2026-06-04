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

	private var searchDebounce: android.os.Handler? = null

	// Tab indices (must match the TabItem order in fragment_search_show.xml).
	private companion object {
		const val TAB_SHOWS = 0
		const val TAB_PRODUCTS = 1
		const val TAB_USERS = 2
		const val DEBOUNCE_MS = 350L
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
		)
		val gridLm = GridLayoutManager(mCtx, if (resources.isTablet()) 3 else 2)
		gridLm.spanSizeLookup = resultsAdapter.makeSpanSizeLookup()
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
				runSearch()
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
				searchDebounce?.postDelayed({ runSearch() }, DEBOUNCE_MS)
			}
		})

		observeSearch()

		// Initial query (if pre-filled) → search now.
		if (bind.search.text?.toString()?.trim().orEmpty().isNotEmpty()) {
			runSearch()
		}
	}

	private fun runSearch() {
		val q = bind.search.text?.toString()?.trim().orEmpty()
		if (q.isEmpty()) {
			clearResults()
			return
		}
		bind.loader.isVisible = true
		viewModel.unifiedSearch(
			q,
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
					shows = data?.shows.orEmpty()
					products = data?.products.orEmpty()
					users = data?.users.orEmpty()
					updateTabCounts()
					renderActiveTab()
				}

				is Resource.Error -> {
					bind.loader.isVisible = false
					// Don't pop a scary dialog — just clear and log. Auth errors
					// are browse-token errors that shouldn't block guest search.
					shows = emptyList()
					products = emptyList()
					users = emptyList()
					updateTabCounts()
					renderActiveTab()
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
		resultsAdapter.submitList(emptyList())
		bind.searchTabBar.isVisible = false
		bind.noData.isVisible = false
		bind.loader.isVisible = false
	}

	private fun updateTabCounts() {
		bind.searchTabBar.getTabAt(TAB_SHOWS)?.text = "Shows (${shows.size})"
		bind.searchTabBar.getTabAt(TAB_PRODUCTS)?.text = "Products (${products.size})"
		bind.searchTabBar.getTabAt(TAB_USERS)?.text = "Users (${users.size})"
	}

	/** Render the currently-selected tab; resolve the single empty state. */
	private fun renderActiveTab() {
		val query = bind.search.text?.toString()?.trim().orEmpty()
		val allEmpty = shows.isEmpty() && products.isEmpty() && users.isEmpty()

		// Tabs visible whenever there's a non-blank query AND at least one result.
		bind.searchTabBar.isVisible = query.isNotBlank() && !allEmpty
		bind.noData.isVisible = query.isNotBlank() && allEmpty

		if (query.isBlank() || allEmpty) {
			resultsAdapter.submitList(emptyList())
			return
		}

		when (activeTab) {
			TAB_PRODUCTS -> resultsAdapter.submitProductsOnly(products)
			TAB_USERS -> resultsAdapter.submitUsersOnly(users)
			else -> resultsAdapter.submitShowsOnly(shows)
		}
		bind.resultsRecycler.isVisible = true
	}
}
