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

	private fun runCurrentSearchWithFilters() {
		val q = bind.search.text?.toString() ?: ""
		val catIdParts = browseFilters.categoryIds.map { it.toString().request() }
		val subCatIdParts = browseFilters.subCategoryIds.map { it.toString().request() }
		bind.loader.isVisible = true
		viewModel.getLiveShow(
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
			bind.unifiedResultsRecycler.isVisible = false
			bind.unifiedSectionLabel.isVisible = false
			unifiedAdapter.submitList(emptyList())
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
						.putExtra("sellerId", user.id.toString())
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

					if (showList.isEmpty()) {
						bind.noData.isVisible = true
						bind.recycler.isVisible = false
					} else {
						bind.noData.isVisible = false
						bind.recycler.isVisible = true
					}


					homeAdapter.notifyDataSetChanged()

				}

				is Resource.Error -> {
					bind.loader.isVisible = false

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

					if (resultItems.isNotEmpty()) {
						unifiedAdapter.submitList(resultItems)
						bind.unifiedResultsRecycler.isVisible = true
						bind.unifiedSectionLabel.isVisible = true
						// FIX (deep-diag 2026-05-26): noData spans heading→parent bottom and
						// has higher z-order than unifiedResultsRecycler in the XML, so it
						// overlays and hides unified results when shows=empty.
						// Hide noData whenever we have user/product results to show.
						bind.noData.isVisible = false
					} else {
						bind.unifiedResultsRecycler.isVisible = false
						bind.unifiedSectionLabel.isVisible = false
					}
				}
				else -> {
					bind.unifiedResultsRecycler.isVisible = false
					bind.unifiedSectionLabel.isVisible = false
				}
			}
		}
	}

}
