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
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.HomeAdapter
import io.bidswipe.app.controller.SearchResultItem
import io.bidswipe.app.controller.UnifiedSearchResultAdapter
import io.bidswipe.app.databinding.FragmentSearchShowBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.StreamModel
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetMyShowResponse
import io.bidswipe.app.network.response.SearchProduct
import io.bidswipe.app.network.response.SearchUser
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.sellerProfile.SellerProfileActivity
import io.bidswipe.app.ui.product.ProductDetailsActivity
import io.bidswipe.app.ui.upcomingshow.UpcomingShowDetailsActivity
import io.bidswipe.app.ui.watchStream.ViewLiveShowActivity
import io.bidswipe.app.utils.hideKeyboard
import io.bidswipe.app.utils.isTablet
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.setHapticClickListener
import io.bidswipe.app.utils.showKeyboard

// Basecamp #9929090875 (Trey 2026-05-26): unified search — users + products
// now display in a real RecyclerView below the shows grid instead of a
// count-only summary. Phase 1 was: "Also found: N products • M users".
// This commit (Bug 1 fix) replaces that with tappable rows.
//
// NOTE (fix 2026-05-27): this fragment is currently UNREACHABLE — it has a
// destination in dash_nav_graph.xml (goToSearchShowFragment) but no caller
// anywhere navigates to it. The user-facing search bar lives in HomeFragment;
// the unified-search wiring that actually runs is in HomeFragment.kt +
// fragment_home.xml. This file is kept for now because earlier attempted
// fixes targeted it; do not assume edits here affect production search.
@SuppressLint("NotifyDataSetChanged")
class SearchShowFragment : BaseFragment<DashViewModel, FragmentSearchShowBinding>() {

	// Basecamp #9933301500 round 5 (2026-05-28): hold the active filter state
	// across keystrokes + filter sheet opens so the search runs with the user's
	// last-applied filters until they explicitly clear / change them.
	private var browseFilters: BrowseFilters = BrowseFilters()

	// Basecamp #9960348333 (Trey 2026-06-04, round 6): the dedicated Search screen
	// fires TWO independent API calls per query — getLiveShow (shows) and
	// unifiedSearch (users + products) — each with its own observer. Both used to
	// toggle the shared `noData` ("No Shows Found") view independently, so they
	// raced: unifiedSearch (users found) hid noData, then the later getLiveShow
	// (0 shows) re-showed it ON TOP of the visible user list. That's the overlap
	// in Trey's video ("No Shows Found" clipboard over @devtest1 / @test15).
	//
	// Fix: track both result sets and resolve the empty state in ONE place
	// (updateEmptyState). noData only appears when the query is non-blank AND
	// BOTH shows and unified results are empty. It also reads "No results found"
	// rather than "No Shows Found" since this screen searches users+products too.
	private var hasShows = false
	private var hasUnifiedResults = false
	private var showsLoaded = false
	private var unifiedLoaded = false

	private fun updateEmptyState() {
		val query = bind.search.text?.toString()?.trim().orEmpty()
		// Only decide once both calls have reported back for the current query,
		// so we never flash "no results" before unifiedSearch has answered.
		val bothBack = showsLoaded && unifiedLoaded
		val nothing = !hasShows && !hasUnifiedResults
		bind.noData.isVisible = query.isNotBlank() && bothBack && nothing
	}

	private fun runCurrentSearchWithFilters() {
		// New query in flight: reset the per-query result/loaded flags so a stale
		// observer from a previous keystroke can't flip the empty state.
		hasShows = false
		hasUnifiedResults = false
		showsLoaded = false
		unifiedLoaded = false
		bind.noData.isVisible = false
		val q = bind.search.text?.toString() ?: ""
		val catIdParts = browseFilters.categoryIds.map { it.toString().request() }
		val subCatIdParts = browseFilters.subCategoryIds.map { it.toString().request() }
		bind.loader.isVisible = true
		viewModel.getLiveShow(
			// Basecamp #9960348333 (Trey 2026-06-03): get-live-show REQUIRES a
			// `type` in {live,upcoming,popular} (backend validation). This call
			// previously omitted it, so the multipart part was dropped and the
			// backend returned 403 "The type field is required." — surfaced as an
			// error popup on the dedicated Search screen (on load AND while typing).
			// Pass a valid default; the real search results come from unifiedSearch.
			type = "popular".request(),
			search = q.request(),
			showFormat = browseFilters.showFormat?.request(),
			tag = browseFilters.tag?.takeIf { it.isNotBlank() }?.request(),
			premierShop = if (browseFilters.premierShop) "1".request() else null,
			shipCountry = browseFilters.shipCountry?.request(),
			shipState = browseFilters.shipState?.takeIf { it.isNotBlank() }?.request(),
			shipping = browseFilters.shipping?.request(),
			categoryIds = catIdParts.ifEmpty { null },
			subCategoryIds = subCatIdParts.ifEmpty { null },
		)
		if (q.isNotBlank()) {
			viewModel.unifiedSearch(
				q,
				categoryIds = browseFilters.categoryIds.ifEmpty { null },
				subCategoryIds = browseFilters.subCategoryIds.ifEmpty { null },
			)
		} else {
			// Blank query: nothing to search. Treat unified as "loaded, empty" so
			// updateEmptyState() isn't blocked waiting on a call we won't make.
			bind.unifiedResultsRecycler.isVisible = false
			bind.unifiedSectionLabel.isVisible = false
			unifiedAdapter.submitList(emptyList())
			hasUnifiedResults = false
			unifiedLoaded = true
			updateEmptyState()
		}
	}
	override fun getModel(): Class<DashViewModel> = DashViewModel::class.java

	override fun getBind(
		inflater: LayoutInflater,
		view: ViewGroup?,
	) = FragmentSearchShowBinding.inflate(inflater, view, false)

	private var showList = mutableListOf<GetMyShowResponse.Data?>()
	private var romIdsList = mutableListOf<StreamModel>()
	private lateinit var homeAdapter: HomeAdapter
	private lateinit var unifiedAdapter: UnifiedSearchResultAdapter

	private val mClick = object : RecyclerClicks {
		override fun itemClick(pos: Int, status: String?) {

			when (status) {

				"user" -> {
					startActivity(
						Intent(mCtx, SellerProfileActivity::class.java).putExtra(
							"userId",
							showList[pos]?.userId.toString()
						)
					)
				}

				"viewShow" -> {

					if (showList[pos]?.isLive == true) {
						startActivity(
							Intent(mCtx, ViewLiveShowActivity::class.java).putExtra("position", pos)
								.putParcelableArrayListExtra("roomIdsList", romIdsList as ArrayList)
						)
					} else {
						// Basecamp #1: tapping an upcoming show in the legacy
						// search grid was a dead no-op. Open the upcoming-show
						// detail screen (parity with Home/Explore).
						startActivity(
							Intent(mCtx, UpcomingShowDetailsActivity::class.java)
								.putExtra("show_id", showList[pos]?.id?.toString())
						)
					}

				}
			}

		}

	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		bind.header.onBackClick {
			findNavController().popBackStack()
		}

		bind.root.setHapticClickListener {
			hideKeyboard(it)
		}

		homeAdapter = HomeAdapter(showList, mClick)
		(bind.recycler.layoutManager as GridLayoutManager).setSpanCount(if (resources.isTablet()) 3 else 2)
		bind.recycler.adapter = homeAdapter

		// Basecamp #9929090875: unified search adapter wiring.
		// User tap → SellerProfileActivity (tab 0 = their shop).
		// Product tap → ProductDetailsActivity with product ID.
		unifiedAdapter = UnifiedSearchResultAdapter(
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
			}
		)
		bind.unifiedResultsRecycler.adapter = unifiedAdapter

		// Basecamp #9960348333 (Trey 2026-06-04): when opened from the Explore search
		// bar, the user may have already typed a query on Explore. Pre-fill it here
		// (before the TextWatcher is attached so it doesn't double-fire) so the
		// initial runCurrentSearchWithFilters() below searches for it immediately.
		arguments?.getString("query")?.takeIf { it.isNotBlank() }?.let { q ->
			bind.search.setText(q)
			bind.search.setSelection(q.length)
		}

		bind.search.requestFocus()

		showKeyboard(bind.search)

		// Basecamp #9933301500 round 5 (2026-05-28): filter button on the search
		// results page. Re-uses the BrowseFiltersSheet already used by Explore +
		// Home for consistency. Apply re-runs both the show-list (getLiveShow)
		// AND the unified search (users + products) with the selected
		// category/subcategory filters. Tag/shipping/showFormat are also passed
		// through to getLiveShow so the show grid respects all filters.
		bind.filterBtn.setHapticClickListener {
			BrowseFiltersSheet(initial = browseFilters) { applied ->
				browseFilters = applied
				runCurrentSearchWithFilters()
			}.show(childFragmentManager, "browse_filters")
		}

		bind.loader.isVisible = true
		runCurrentSearchWithFilters()

		bind.search.addTextChangedListener(object : TextWatcher {
			private var debounce: android.os.Handler? = null

			override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
			override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

			override fun afterTextChanged(p0: Editable?) {
				// Basecamp #9922137198 / #9929090875: fire both show search + unified search.
				// Debounce 350 ms to avoid hammering the API on every keystroke.
				debounce?.removeCallbacksAndMessages(null)
				debounce = android.os.Handler(android.os.Looper.getMainLooper())
				debounce?.postDelayed({
					runCurrentSearchWithFilters()
				}, 350)
			}
		})

		viewModel.getLiveShowRepo.observe(viewLifecycleOwner) { resource ->
			when (resource) {
				is Resource.Success -> {
					bind.loader.isVisible = false

					val mData = resource.value.data

					mData?.forEach {
						romIdsList.add(StreamModel(it?.roomId.toString(), ""))
					}

					showList.clear()

					mData?.forEach {
						showList.add(it)
					}

					// Basecamp #9960348333 round 6: don't toggle the shared noData here
					// directly (it raced unifiedSearch). Record whether shows exist and
					// let updateEmptyState() make the combined decision. recycler
					// visibility still follows shows-only since it only holds shows.
					hasShows = showList.isNotEmpty()
					showsLoaded = true
					bind.recycler.isVisible = hasShows
					updateEmptyState()

					homeAdapter.notifyDataSetChanged()

				}

				is Resource.Error -> {
					bind.loader.isVisible = false
					// A failed show fetch shouldn't strand the empty-state resolver.
					hasShows = false
					showsLoaded = true
					bind.recycler.isVisible = false
					updateEmptyState()

					resource.parse(mCtx, TAG, object : AlertClicks {
						override fun primaryClick(dialog: AppBottomSheet) {
							dialog.dismiss()

						}

						override fun secondaryClick(dialog: AppBottomSheet) {
							dialog.dismiss()

						}
					})
				}

				else -> {}

			}
		}

		// Basecamp #9929090875 (Trey 2026-05-26): unified search observer — Phase 2.
		// Replace the old count-only summary with actual tappable rows for users + products.
		viewModel.unifiedSearchRepo.observe(viewLifecycleOwner) { resource ->
			when (resource) {
				is Resource.Success -> {
					val data = resource.value.data
					val resultItems = mutableListOf<SearchResultItem>()

					// Users first, then products
					data?.users?.forEach { resultItems.add(SearchResultItem.UserItem(it)) }
					data?.products?.forEach { resultItems.add(SearchResultItem.ProductItem(it)) }

					hasUnifiedResults = resultItems.isNotEmpty()
					unifiedLoaded = true
					if (resultItems.isNotEmpty()) {
						unifiedAdapter.submitList(resultItems)
						bind.unifiedResultsRecycler.isVisible = true
						bind.unifiedSectionLabel.isVisible = true
					} else {
						unifiedAdapter.submitList(emptyList())
						bind.unifiedResultsRecycler.isVisible = false
						bind.unifiedSectionLabel.isVisible = false
					}
					// Basecamp #9960348333 round 6: single combined empty-state decision
					// (see updateEmptyState). Never independently force noData here, or
					// it races the shows observer and "No Shows Found" overlays results.
					updateEmptyState()
				}
				is Resource.Error -> {
					// Basecamp #9942607925 round 2 (2026-06-03): explicit error branch so
					// search failures are logged and not silently blank. The blank-query
					// guard in DashViewModel.unifiedSearch() already prevents 422 from an
					// empty query; this handles any other unexpected failures.
					hasUnifiedResults = false
					unifiedLoaded = true
					bind.unifiedResultsRecycler.isVisible = false
					bind.unifiedSectionLabel.isVisible = false
					updateEmptyState()
					android.util.Log.w("SearchShowFragment", "unifiedSearch error: ${resource.errorResponse?.message}")
				}
				else -> {
					bind.unifiedResultsRecycler.isVisible = false
					bind.unifiedSectionLabel.isVisible = false
				}
			}
		}
	}

}
