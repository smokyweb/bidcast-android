package io.bidswipe.app.ui.dashboard

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import io.bidswipe.app.App
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.FlashSaleAdapter
import io.bidswipe.app.controller.HomeAdapter
import io.bidswipe.app.controller.HomeCategoryAdapter
import io.bidswipe.app.controller.SearchResultItem
import io.bidswipe.app.controller.UnifiedSearchResultAdapter
import io.bidswipe.app.databinding.FragmentHomeBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.StreamModel
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetMyShowResponse
import io.bidswipe.app.network.response.Product
import io.bidswipe.app.network.response.SearchProduct
import io.bidswipe.app.network.response.SearchUser
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.custom.UpcomingShowSheet
import io.bidswipe.app.ui.more.NotificationActivity
import io.bidswipe.app.ui.product.ProductDetailsActivity
import io.bidswipe.app.ui.product.ProductSetDetailsActivity
import io.bidswipe.app.ui.sellerProfile.SellerProfileActivity
import io.bidswipe.app.ui.watchStream.ViewLiveShowActivity
import io.bidswipe.app.utils.Alerts
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import io.bidswipe.app.utils.hideKeyboard
import io.bidswipe.app.utils.isBrowseAuthError
import io.bidswipe.app.utils.isTablet
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.runSafe
import io.bidswipe.app.utils.setHapticClickListener
import io.bidswipe.app.utils.value

@SuppressLint("NotifyDataSetChanged")
class HomeFragment : BaseFragment<DashViewModel, FragmentHomeBinding>() {
    override fun getModel(): Class<DashViewModel> = DashViewModel::class.java
    override fun getBind(inflater: LayoutInflater, view: ViewGroup?) =
        FragmentHomeBinding.inflate(inflater, view, false)

    private lateinit var homeAdapter: HomeAdapter
    private lateinit var categoryAdapter: HomeCategoryAdapter
    // Basecamp #9933973683 return (2026-05-29): Flash Sales section adapter.
    private lateinit var flashSaleAdapter: FlashSaleAdapter
    private val flashSaleItems = mutableListOf<Product?>()

    // Basecamp #9929090875 (Trey 2026-05-26 / fix 2026-05-27): unified search
    // (users + products) results render in a dedicated RecyclerView on the home
    // screen, below the shows grid. Previous 3 fix attempts wired this up in
    // SearchShowFragment which is never navigated to from anywhere — the
    // user-facing search bar lives here in HomeFragment.
    private lateinit var unifiedAdapter: UnifiedSearchResultAdapter
    private var searchDebounce: android.os.Handler? = null
    // Basecamp #9933301500 round 5 (2026-05-28): in-memory filter state for
    // the Home search results page. Persisted across keystrokes inside this
    // session so a user can apply filters, type to refine, and the filters
    // stick. Cleared when the filter sheet's Clear button runs (BrowseFilters
    // default has no fields set).
    private var homeSearchFilters: BrowseFilters = BrowseFilters()
    private var showList = mutableListOf<GetMyShowResponse.Data?>()
    private val categoryTiles = mutableListOf<HomeCategoryAdapter.CategoryTile>()
    private var romIdsList = mutableListOf<String>()
    private var streamList = mutableListOf<StreamModel>()
    private var page = 1
    private var isLoading = false
    private var selectedCategory = "for_you"
    private var selectedCategoryTileId = "for_you"
    private var hasInitializedCategories = false

    // Basecamp #9933301500 round 5 (2026-05-28): one runner for getLiveShow +
    // unifiedSearch with the current filters baked in. Called from the search
    // TextWatcher AND from BrowseFiltersSheet Apply callback.
    private fun runSearchWithFilters(query: String) {
        val catIdParts = homeSearchFilters.categoryIds.map { it.toString().request() }
        val subCatIdParts = homeSearchFilters.subCategoryIds.map { it.toString().request() }
        viewModel.getLiveShow(
            selectedTabText.request(),
            selectedCategoryRequest(),
            search = query.request(),
            page = page.toString().request(),
            showFormat = homeSearchFilters.showFormat?.request(),
            tag = homeSearchFilters.tag?.takeIf { it.isNotBlank() }?.request(),
            premierShop = if (homeSearchFilters.premierShop) "1".request() else null,
            shipCountry = homeSearchFilters.shipCountry?.request(),
            shipState = homeSearchFilters.shipState?.takeIf { it.isNotBlank() }?.request(),
            shipping = homeSearchFilters.shipping?.request(),
            categoryIds = catIdParts.ifEmpty { null },
            subCategoryIds = subCatIdParts.ifEmpty { null },
        )
        // Basecamp #9938023997 round 5 (2026-05-28): pass ALL active filters
        // to unifiedSearch, not just category/subcategory. Previously show_format,
        // tag, premier_shop, shipping were only passed to getLiveShow (the shows
        // tab) but not to unifiedSearch (the multi-section results recycler).
        searchDebounce?.removeCallbacksAndMessages(null)
        searchDebounce = android.os.Handler(android.os.Looper.getMainLooper())
        searchDebounce?.postDelayed({
            viewModel.unifiedSearch(
                query,
                categoryIds = homeSearchFilters.categoryIds.ifEmpty { null },
                subCategoryIds = homeSearchFilters.subCategoryIds.ifEmpty { null },
                showFormat = homeSearchFilters.showFormat,
                tag = homeSearchFilters.tag?.takeIf { it.isNotBlank() },
                premierShop = if (homeSearchFilters.premierShop) true else null,
                shipping = homeSearchFilters.shipping,
            )
        }, 350)
    }

    private val viewLiveShowLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            page = 1
            viewModel.getLiveShow(
                selectedTabText.request(),
                selectedCategoryRequest(),
                search = bind.search.value().ifEmpty { null }?.request(),
                page = page.toString().request()
            )
        }
    }

    /*	private val streamingResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->

            log( "result: $result")

            if (result.resultCode == Activity.RESULT_OK) {

                val data = result.data
                val sellerId = data?.getStringExtra("sellerId")
                val type = data?.getStringExtra("type")

                startActivity(Intent(mCtx, ProductDetailsActivity::class.java).putExtra("type", "shop").putExtra("sellerId", sellerId))

            }

        }*/

    private val mClick = object : RecyclerClicks {
        override fun itemClick(pos: Int, status: String?) {

            when (status) {

                "user" -> {
                    startActivity(
                        Intent(mCtx, SellerProfileActivity::class.java).putExtra(
                            "sellerId",
                            showList[pos]?.userId.toString()
                        )
                    )
                }

                "viewShow" -> {
                    // [Basecamp #9930403446] Upcoming-show tap → popup with date/time.
                    // [Basecamp #9933847997] "View Show" button opens upcoming show details.
                    if (selectedTabText == "upcoming") {
                        val show = showList[pos]
                        UpcomingShowSheet(
                            mCtx = mCtx,
                            profileImageUrl = show?.user?.profileImage,
                            username = show?.user?.username ?: show?.user?.name,
                            showDate = show?.date,
                            showTime = show?.time,
                            showId = show?.id?.toString(),
                            onViewShow = { id ->
                                startActivity(
                                    android.content.Intent(
                                        mCtx,
                                        io.bidswipe.app.ui.upcomingshow.UpcomingShowDetailsActivity::class.java
                                    ).putExtra("show_id", id)
                                )
                            },
                        ).show()
                        return
                    }

//                    if (showList[pos]?.isLive == true) {
                    val roomId = showList[pos]?.roomId.toString()

                    if (App.PIPMode) {
                        Alerts.error(mCtx, "You are already in Live show")
                    } else {
                        viewLiveShowLauncher.launch(
                            Intent(mCtx, ViewLiveShowActivity::class.java)
                                .putExtra("roomId", roomId)
                                .putExtra("userId", showList[pos]?.userId.toString())
                                .putExtra("roomIdsList", romIdsList.joinToString(","))
                                .putParcelableArrayListExtra(
                                    "streamList",
                                    ArrayList(streamList)
                                )
                        )
                    }
//                    }
                }
            }
        }
    }

    private var selectedTabText = "live"
    private fun selectedCategoryRequest() = if (selectedTabText == "live") null else selectedCategory.request()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        App.isWatchStreamInPIP.observe(viewLifecycleOwner) {
            if (it) {
                App.isWatchStreamInPIP.value = false
                startActivity(
                    Intent(mCtx, ProductDetailsActivity::class.java).putExtra(
                        "type",
                        "shop"
                    ).putExtra("sellerId", App.currentSellerId)
                )
            }
        }

        bind.header.setHapticClickListener {
            hideKeyboard(it)
        }

        bind.searchLayout.setEndIconOnClickListener {
            bind.search.setText("")
            hideKeyboard(it)
        }

        // Basecamp #9933801536 (2026-05-27): save-search bell
        // Basecamp #9933801536 (2026-05-27): save-search bell. Posts current
        // search text to /api/saved-searches and flips bell to filled state.
        // Network runs on Dispatchers.IO; UI mutations marshalled back to main.
        // Basecamp #9933301500 round 5 (2026-05-28): filter button on Home search results.
        // Opens BrowseFiltersSheet (already used by Explore / Browse / SearchShowFragment).
        // Apply re-runs both getLiveShow and unifiedSearch with the new filter state.
        // Basecamp #9933301500 (2026-05-29): filter button is always visible on Home —
        // in browse mode (no search query) it filters the live-show grid; in search mode
        // it narrows both getLiveShow and unifiedSearch as before.
        bind.searchFilterBtn.isVisible = true
        bind.searchFilterBtn.setHapticClickListener {
            BrowseFiltersSheet(initial = homeSearchFilters) { applied ->
                homeSearchFilters = applied
                // Flip icon tint when filters are active so user sees the badge.
                bind.searchFilterBtn.setColorFilter(
                    if (applied.isActive) android.graphics.Color.parseColor("#FFA500") else android.graphics.Color.BLACK
                )
                val q = bind.search.value()
                page = 1
                if (q.isNotEmpty()) {
                    runSearchWithFilters(q)
                } else {
                    // Browse mode: apply filters to the live-show grid directly.
                    val catIdParts = applied.categoryIds.map { it.toString().request() }
                    val subCatIdParts = applied.subCategoryIds.map { it.toString().request() }
                    bind.loader.isVisible = true
                    viewModel.getLiveShow(
                        selectedTabText.request(),
                        selectedCategoryRequest(),
                        page = "1".request(),
                        showFormat = applied.showFormat?.request(),
                        tag = applied.tag?.takeIf { it.isNotBlank() }?.request(),
                        premierShop = if (applied.premierShop) "1".request() else null,
                        shipCountry = applied.shipCountry?.request(),
                        shipState = applied.shipState?.takeIf { it.isNotBlank() }?.request(),
                        shipping = applied.shipping?.request(),
                        categoryIds = catIdParts.ifEmpty { null },
                        subCategoryIds = subCatIdParts.ifEmpty { null },
                    )
                }
            }.show(childFragmentManager, "home_search_browse_filters")
        }

        bind.saveBellBtn.setHapticClickListener {
            val q = bind.search.value().trim()
            if (q.isEmpty()) {
                Alerts.error(mCtx, "Type a search term before saving")
                return@setHapticClickListener
            }
            viewLifecycleOwner.lifecycleScope.launch {
                val code: Int = withContext(Dispatchers.IO) {
                    try {
                        val token = io.bidswipe.app.utils.Prefs(mCtx).token()
                        val body = org.json.JSONObject().apply { put("query", q) }.toString()
                        val url = java.net.URL("${io.bidswipe.app.utils.Const.BASE_URL}/api/saved-searches")
                        val conn = url.openConnection() as java.net.HttpURLConnection
                        conn.requestMethod = "POST"
                        conn.setRequestProperty("Content-Type", "application/json")
                        conn.setRequestProperty("Accept", "application/json")
                        conn.setRequestProperty("Authorization", "Bearer $token")
                        conn.doOutput = true
                        conn.outputStream.use { it.write(body.toByteArray()) }
                        val rc = conn.responseCode
                        conn.disconnect()
                        rc
                    } catch (e: Exception) {
                        e.printStackTrace()
                        -1
                    }
                }
                if (code == 200 || code == 201) {
                    bind.saveBellBtn.setImageResource(io.bidswipe.app.R.drawable.ic_bell_filled)
                    Alerts.success(mCtx, "Search saved — you'll get notified when something matches")
                } else if (code == -1) {
                    Alerts.error(mCtx, "Network error saving search")
                } else {
                    Alerts.error(mCtx, "Could not save search (code $code)")
                }
            }
        }

        bind.main.setHapticClickListener {
            hideKeyboard(it)
        }

        bind.recycler.setHapticClickListener {
            hideKeyboard(it)
        }

        homeAdapter = HomeAdapter(showList, mClick)

        (bind.recycler.layoutManager as GridLayoutManager).setSpanCount(if (resources.isTablet()) 3 else 2)
        bind.recycler.adapter = homeAdapter

        categoryAdapter = HomeCategoryAdapter(categoryTiles, object : RecyclerClicks {
            override fun itemClick(pos: Int, status: String?) {
                val tile = categoryTiles[pos]

                categoryTiles.forEachIndexed { index, tile ->
                    tile.isSelected = index == pos
                    categoryAdapter.notifyItemChanged(index)
                }

                when (tile.tileType) {
                    HomeCategoryAdapter.TileType.SEE_ALL -> {
                        findNavController().navigate(R.id.goToExploreFragment)
                    }

                    else -> handleCategorySelection(tile, true)
                }
            }
        })

        bind.categoryRecycler.layoutManager =
            LinearLayoutManager(mCtx, LinearLayoutManager.HORIZONTAL, false)
        bind.categoryRecycler.adapter = categoryAdapter

        bind.notification.setHapticClickListener {
            startActivity(
                Intent(mCtx, NotificationActivity::class.java).putExtra(
                    "slug",
                    "notification"
                )
            )
        }

        // Basecamp #9933973683 return (2026-05-29): Flash Sales section.
        // Horizontal RecyclerView above the Live/Popular/Upcoming tabs.
        flashSaleAdapter = FlashSaleAdapter(flashSaleItems) { product ->
            startActivity(
                android.content.Intent(mCtx, io.bidswipe.app.ui.product.ProductDetailsActivity::class.java)
                    .putExtra("productId", product.id?.toString())
            )
        }
        bind.flashSalesRecycler.layoutManager =
            androidx.recyclerview.widget.LinearLayoutManager(
                mCtx, androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false
            )
        bind.flashSalesRecycler.adapter = flashSaleAdapter

        viewModel.getFlashSaleProducts()

        viewModel.flashSaleProductsRepo.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Success -> {
                    val products = resource.value.products.orEmpty()
                    flashSaleItems.clear()
                    flashSaleItems.addAll(products)
                    flashSaleAdapter.notifyDataSetChanged()
                    bind.flashSalesSection.isVisible = flashSaleItems.isNotEmpty()
                }
                else -> {
                    // On error or empty: keep section hidden. Don’t surface an
                    // error to the user — flash sales are supplementary content.
                }
            }
        }

        bind.recycler.setOnScrollChangeListener { _, _, _, _, _ ->
            val layoutManager = bind.recycler.layoutManager as GridLayoutManager
            val lastItemPosition = layoutManager.findLastVisibleItemPosition()

            if (resources.isTablet()) showList.lastIndex - 3 else showList.lastIndex - 2

            if (lastItemPosition == showList.lastIndex && !isLoading) {
                isLoading = true
                page++
                viewModel.getLiveShow(
                    selectedTabText.request(),
                    selectedCategoryRequest(),
                    search = bind.search.value().ifEmpty { null }?.request(),
                    page = page.toString().request()
                )
            }
        }

        bind.searchLayout.isEndIconVisible = false

        // Basecamp #9929090875 (Trey 2026-05-26 / fix 2026-05-27): unified search
        // adapter wiring. User tap → SellerProfileActivity. Product tap →
        // ProductDetailsActivity. Adapter is bound here even when empty so that
        // submitList() updates the live RecyclerView once results arrive.
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
            },
            // Basecamp #9929090875 round 3 (2026-05-27): show results now
            // live in the unified RecyclerView too, with the same card UX as
            // Live/Upcoming. Tap routes to ViewLiveShowActivity for live ones,
            // or the seller profile for upcoming.
            onShowClick = { show: io.bidswipe.app.network.response.SearchShow ->
                if (show.isLive == true) {
                    startActivity(
                        Intent(mCtx, io.bidswipe.app.ui.watchStream.ViewLiveShowActivity::class.java)
                            .putExtra("showId", (show.id ?: 0).toString())
                            .putExtra("userId", show.userId?.toString() ?: show.user?.id?.toString() ?: "")
                    )
                } else {
                    startActivity(
                        Intent(mCtx, SellerProfileActivity::class.java)
                            .putExtra("sellerId", (show.user?.id ?: show.userId ?: 0).toString())
                    )
                }
            }
        )
        // Basecamp #9929090875 redesign (2026-05-28): configure GridLayoutManager with
        // spanCount=2 so Show cards render 2-per-row; attach the adapter's SpanSizeLookup
        // so headers/products/users stay full-width.
        val searchGridLm = androidx.recyclerview.widget.GridLayoutManager(mCtx, 2)
        bind.unifiedResultsRecycler.layoutManager = searchGridLm
        bind.unifiedResultsRecycler.adapter = unifiedAdapter
        searchGridLm.spanSizeLookup = unifiedAdapter.makeSpanSizeLookup()

        // Basecamp #9942607925 (2026-05-29 Trey QA): tap the search bar →
        // navigate to SearchShowFragment (the dedicated search results page).
        // The inline TextWatcher below is kept as a safety net but the
        // primary UX is the full-screen search page.
        bind.search.setOnClickListener {
            hideKeyboard(it)
            findNavController().navigate(R.id.goToSearchShowFragment)
        }
        bind.searchLayout.setStartIconOnClickListener {
            findNavController().navigate(R.id.goToSearchShowFragment)
        }

        bind.search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim() ?: ""
                bind.searchLayout.isEndIconVisible = query.isNotEmpty()
                // Basecamp #9933801536 (2026-05-28 round 2): save-search bell
                // should only appear once the user has typed something.
                bind.saveBellBtn.isVisible = query.isNotEmpty()
                // Basecamp #9933301500 / #9942607925: filter button is always visible
                // (set once above); do NOT hide it here. Bell stays search-only.

                if (!s.isNullOrEmpty()) {
                    // Basecamp #9929090875 redesign (2026-05-28): hide normal home content
                    // while a search is active; results appear INLINE in its place.
                    bind.heading.isVisible = false
                    bind.swipeRefreshLayout.isVisible = false
                    // #9933973683 return: also hide flash section during search
                    bind.flashSalesSection.isVisible = false

                    page = 1
                    runSearchWithFilters(s.toString())
                } else {
                    // Query cleared → restore normal home content, hide search results.
                    searchDebounce?.removeCallbacksAndMessages(null)
                    bind.unifiedResultsRecycler.isVisible = false
                    bind.heading.isVisible = true
                    bind.swipeRefreshLayout.isVisible = true
                    // Restore flash section if it has items
                    bind.flashSalesSection.isVisible = flashSaleItems.isNotEmpty()
                    unifiedAdapter.submitList(emptyList())
                }
            }
        })

        // Basecamp #9929090875 redesign (2026-05-28): observe unified search results.
        // Shows / Products / Users are rendered in separate sections with section headers.
        // Results replace the normal home content (heading + swipeRefreshLayout are
        // hidden while a query is active; see search text watcher above).
        viewModel.unifiedSearchRepo.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Success -> {
                    val data = resource.value.data
                    val shows    = data?.shows.orEmpty()
                    val products = data?.products.orEmpty()
                    val users    = data?.users.orEmpty()

                    if (shows.isNotEmpty() || products.isNotEmpty() || users.isNotEmpty()) {
                        unifiedAdapter.submitResults(shows, products, users)
                        bind.unifiedResultsRecycler.isVisible = true
                        // Basecamp #9942607925: hide noData when unified results are present
                        // (noData overlays unifiedResultsRecycler in ConstraintLayout z-order).
                        bind.noData.isVisible = false
                    } else {
                        bind.unifiedResultsRecycler.isVisible = false
                    }
                }
                is Resource.Error -> {
                    // Basecamp #9942607925 round 2 (2026-06-03): surface API errors so a
                    // broken search isn't silently blank. Don't pop an alert on auth errors
                    // (those are handled by the getLiveShow observer), but log the failure.
                    bind.unifiedResultsRecycler.isVisible = false
                    if (!resource.isBrowseAuthError()) {
                        android.util.Log.w(TAG, "unifiedSearch error: ${resource.errorResponse?.message}")
                    }
                }
                else -> {
                    bind.unifiedResultsRecycler.isVisible = false
                }
            }
        }

        bind.searchLayout.setEndIconOnClickListener {
            bind.search.setText("")
            bind.searchLayout.isEndIconVisible = false
            hideKeyboard(it)
        }

        bind.swipeRefreshLayout.setOnRefreshListener {
            page = 1
            viewModel.getLiveShow(
                selectedTabText.request(),
                selectedCategoryRequest(),
                page = page.toString().request()
            )
            viewModel.getCategory()
        }

        bind.noInternet.onClick {
            bind.loader.isVisible = false
            bind.noInternet.isVisible = false
            viewModel.getLiveShow(
                selectedTabText.request(),
                selectedCategoryRequest(),
                page = page.toString().request()
            )
            viewModel.getCategory()
        }

        val selectedTabView = when (selectedTabText) {
            "popular" -> bind.popular
            "upcoming" -> bind.comingSoon
            else -> bind.live
        }
        selectTab(selectedTabView, true)

        bind.live.setHapticClickListener { selectTab(it as TextView, false) }

        bind.popular.setHapticClickListener { selectTab(it as TextView, false) }

        bind.comingSoon.setHapticClickListener { selectTab(it as TextView, false) }

        if (App.categoryList.isNotEmpty()) {

            bind.loader.isVisible = false
            val mData = App.categoryList

            categoryTiles.clear()

            categoryTiles.add(
                HomeCategoryAdapter.CategoryTile(
                    id = "for_you",
                    title = getString(R.string.for_you_tile),
                    iconRes = R.drawable.profile_circle,
                    tileType = HomeCategoryAdapter.TileType.FOR_YOU,
                    isSelected = true
                )
            )

            categoryTiles.addAll(
                mData.filter { data -> data?.isSelected == true }.distinct().mapNotNull { category ->
                    val name = category?.name ?: return@mapNotNull null
                    HomeCategoryAdapter.CategoryTile(
                        id = name,
                        title = name,
                        imageUrl = category.image,
                        tileType = HomeCategoryAdapter.TileType.CATEGORY,
                    )
                }
            )

            categoryTiles.add(
                HomeCategoryAdapter.CategoryTile(
                    id = "see_all",
                    title = getString(R.string.see_all_categories_tile),
                    iconRes = R.drawable.ic_tile_grid,
                    tileType = HomeCategoryAdapter.TileType.SEE_ALL,
                )
            )

            categoryAdapter.notifyDataSetChanged()

            val tileToSelect =
                categoryTiles.firstOrNull { it.id == selectedCategoryTileId && it.tileType != HomeCategoryAdapter.TileType.SEE_ALL }
                    ?: categoryTiles.firstOrNull { it.tileType != HomeCategoryAdapter.TileType.SEE_ALL }

            tileToSelect?.let { tile ->
                val shouldFetch =
                    !hasInitializedCategories || tile.id != selectedCategoryTileId
                handleCategorySelection(tile, shouldFetch)
                hasInitializedCategories = true
            }

            App.socketManager?.onRoomCreated { showData ->
                log("ON CREATED ${showData.categoryId}-- ${selectedCategory}-- ${selectedCategoryTileId}\n-- ${selectedTabText}")
                activity?.runOnUiThread {
                    if (selectedTabText == "live") {
                        viewModel.getLiveShow(
                            selectedTabText.request(),
                            // MC cmpaj2fex0000w5hgq64jp9k4 merge (2026-05-24): kept GitLab's
                            // selectedCategoryRequest() helper which sends null for live tab
                            // (avoids server-side filter on "for_you" sentinel value).
                            selectedCategoryRequest(),
                            search = bind.search.value().ifEmpty { null }?.request(),
                            page = 1.toString().request()
                        )
                    }
                }
            }

            App.socketManager?.onRoomEnded { json ->
                log("END GOT HOME FRAGMENT $json")
                runSafe {
                    requireActivity().runOnUiThread {
                        val roomID = json.optString("room_end")
                        if (roomID.isNotEmpty()) {
                            streamList.removeIf { it.roomId == roomID }
                            romIdsList.remove(roomID)
                            if (showList.isNotEmpty()) {
                                val position = showList.indexOfFirst { it?.roomId == roomID }
                                if (position >= 0) {
                                    showList.removeAt(position)
                                }
                                if (showList.isEmpty()) {
                                    bind.noData.isVisible = true
                                    bind.recycler.isVisible = false
                                    bind.noInternet.isVisible = false

                                } else {
                                    if (position >= 0) {
                                        homeAdapter.notifyItemRemoved(position)
                                        homeAdapter.notifyItemRangeChanged(position, showList.size)
                                    }

                                }
                            }

                        }

                    }
                }
            }

        } else {
            bind.loader.isVisible = false
            viewModel.getCategory()
            viewModel.getCategoryRepo.observe(viewLifecycleOwner) { it ->
                when (it) {
                    is Resource.Success -> {
                        bind.loader.isVisible = false
                        bind.noInternet.isVisible = false
                        viewModel.getCategoryRepo.value = null

                        val mData = it.value.data

                        categoryTiles.clear()

                        categoryTiles.add(
                            HomeCategoryAdapter.CategoryTile(
                                id = "for_you",
                                title = getString(R.string.for_you_tile),
                                iconRes = R.drawable.profile_circle,
                                tileType = HomeCategoryAdapter.TileType.FOR_YOU,
                                isSelected = true
                            )
                        )

                        categoryTiles.addAll(
                            mData?.filter { data -> data?.isSelected == true }
                                ?.distinct()?.mapNotNull { category ->
                                    val name = category?.name ?: return@mapNotNull null
                                    HomeCategoryAdapter.CategoryTile(
                                        id = name,
                                        title = name,
                                        imageUrl = category.image,
                                        tileType = HomeCategoryAdapter.TileType.CATEGORY,
                                    )
                                } ?: emptyList()
                        )

                        categoryTiles.add(
                            HomeCategoryAdapter.CategoryTile(
                                id = "see_all",
                                title = getString(R.string.see_all_categories_tile),
                                iconRes = R.drawable.ic_tile_grid,
                                tileType = HomeCategoryAdapter.TileType.SEE_ALL,
                            )
                        )

                        categoryAdapter.notifyDataSetChanged()

                        val tileToSelect =
                            categoryTiles.firstOrNull { it.id == selectedCategoryTileId && it.tileType != HomeCategoryAdapter.TileType.SEE_ALL }
                                ?: categoryTiles.firstOrNull { it.tileType != HomeCategoryAdapter.TileType.SEE_ALL }

                        tileToSelect?.let { tile ->
                            val shouldFetch =
                                !hasInitializedCategories || tile.id != selectedCategoryTileId
                            handleCategorySelection(tile, shouldFetch)
                            hasInitializedCategories = true
                        }

                    }

                    is Resource.Error -> {
                        if (it.isNetworkError) {
                            bind.noInternet.isVisible = true
                            bind.recycler.isVisible = false

                        } else if (it.isBrowseAuthError()) {
                            // Basecamp #9958788158 (2026-06-03 round 2): get-category is a
                            // public browse endpoint, but the backend rejects a stale/expired
                            // token with {error_type:invalid_token, message:"Token is invalid"}.
                            // On the Live Now dashboard (first open after install with a
                            // leftover token) this used to pop a scary "Token is invalid"
                            // dialog. Browsing must work for guests, so swallow the auth error
                            // here and just show the empty state instead of alarming the user.
                            bind.recycler.isVisible = false
                        } else {
                            it.parse(mCtx, TAG, object : AlertClicks {
                                override fun primaryClick(dialog: AppBottomSheet) {
                                    dialog.dismiss()

                                }

                                override fun secondaryClick(dialog: AppBottomSheet) {
                                    dialog.dismiss()

                                }
                            })
                        }
                    }

                    else -> {}
                }
            }
        }

        viewModel.getLiveShowRepo.observe(viewLifecycleOwner) { it ->
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false
                    bind.swipeRefreshLayout.isRefreshing = false
                    bind.noInternet.isVisible = false

                    val mData = it.value.data


                    if (page == 1) {
                        romIdsList.clear()
                        showList.clear()
                        streamList.clear()
                    }

                    mData?.forEach { it1 ->
                        if (streamList.find { it.roomId == it1?.roomId.toString() } == null) {
                            streamList.add(
                                StreamModel(
                                    it1?.roomId.toString(),
                                    it1?.rtcToken ?: "",
                                    thumbnail = it1?.thumbnail?.get(0)
                                )
                            )
                        }
                        if (!romIdsList.contains(it1?.roomId.toString())) {
                            romIdsList.add(it1?.roomId.toString())
                        }

                    }

                    mData?.forEach { it1 ->
                        if (it1?.user != null) {
                            if (showList.find { it?.roomId == it1.roomId.toString() } == null) {
                                showList.add(it1)
                            }
                        }
                    }

                    // QA-FIX (MC task cmo8utxn700fd3u1hjk2a1yny):
                    // Previously (MC task cmo7iafya00cofi15op1wnifq) this fallback fell back
                    // to fetching ALL live shows whenever ANY category returned an empty list.
                    // That caused tapping a specific category (e.g. "Gaming") with zero live
                    // shows in it to silently show every other category's live shows, which
                    // reads to users as "explore still shows my favorites instead of the
                    // category I picked".
                    //
                    // Narrow the fallback to ONLY the special For-You tab, where it is the
                    // expected UX. For any other specific category, respect the empty result
                    // and show the real "no shows" state instead.
                    if (showList.isEmpty() && page == 1 && selectedCategory == "for_you") {
                        viewModel.getLiveShow(
                            selectedTabText.request(),
                            "all".request(),
                            page = "1".request()
                        )
                        return@observe
                    }

                    if (showList.isEmpty()) {
                        // Basecamp #9942607925: suppress noData when search is active —
                        // unified search results (users/products) may populate the overlay
                        // RecyclerView even if the shows grid returns empty.
                        if (bind.search.text.isNullOrBlank()) {
                            bind.noData.isVisible = true
                        }
                        bind.recycler.isVisible = false
                        bind.noInternet.isVisible = false

                    } else {
                        bind.noData.isVisible = false
                        bind.recycler.isVisible = true
                        bind.noInternet.isVisible = false
                    }

                    homeAdapter.notifyDataSetChanged()

                    isLoading = page >= (it.value.totalPage ?: 0)

                }

                is Resource.Error -> {
                    bind.swipeRefreshLayout.isRefreshing = false
                    bind.loader.isVisible = false

                    if (it.isNetworkError) {
                        bind.noInternet.isVisible = true
                        bind.recycler.isVisible = false
                        bind.noData.isVisible = false
                    } else if (it.isBrowseAuthError()) {
                        // Basecamp #9958788158 (2026-06-03 round 2): get-live-show returns
                        // {error_type:invalid_token, message:"Token is invalid"} for a
                        // stale/expired token. This is THE call that produced the "Token is
                        // invalid" popup on the Live Now tab on first open after install
                        // (see screenshot). Live Now must be browsable for guests, so swallow
                        // the auth error and show the empty/no-data state instead of popping
                        // a dialog + logout prompt.
                        if (bind.search.text.isNullOrBlank()) {
                            bind.noData.isVisible = true
                        }
                        bind.recycler.isVisible = false
                        bind.noInternet.isVisible = false
                    } else {
                        it.parse(mCtx, TAG, object : AlertClicks {
                            override fun primaryClick(dialog: AppBottomSheet) {
                                dialog.dismiss()
                            }

                            override fun secondaryClick(dialog: AppBottomSheet) {
                                dialog.dismiss()

                            }
                        })
                    }
                }

                else -> {}

            }
        }

    }

    private fun handleCategorySelection(
        tile: HomeCategoryAdapter.CategoryTile,
        shouldFetch: Boolean
    ) {
        if (tile.tileType == HomeCategoryAdapter.TileType.SEE_ALL) return

        // Update selection state for all tiles - ensure only one is selected
        categoryTiles.forEachIndexed { index, categoryTile ->
            val shouldSelect = categoryTile.id == tile.id
            if (categoryTile.isSelected != shouldSelect) {
                categoryTile.isSelected = shouldSelect
                categoryAdapter.notifyItemChanged(index)
            }
        }

        selectedCategoryTileId = tile.id
        selectedCategory = if (tile.tileType == HomeCategoryAdapter.TileType.FOR_YOU) {
            "for_you"
        } else {
            tile.id
        }

        if (shouldFetch) {
            page = 1
            bind.search.setText("")
            bind.loader.isVisible = true
            viewModel.getLiveShow(
                selectedTabText.request(),
                selectedCategoryRequest(),
                page = page.toString().request()
            )
        }
    }

    fun selectTab(selectedTab: TextView, isFirst: Boolean) {
        val tabs = listOf(bind.live, bind.popular, bind.comingSoon)
        tabs.forEach {
            it.setTextAppearance(R.style.TitleMedium)
            it.setTextColor(ContextCompat.getColor(mCtx, R.color.outlineVariant))
            it.typeface = Typeface.create(
                Typeface.DEFAULT,
                Typeface.NORMAL
            )
        }
        selectedTab.setTextColor(ContextCompat.getColor(mCtx, R.color.scrim))
        selectedTab.setTextAppearance(R.style.TitleMedium)
        selectedTab.typeface = Typeface.create(
            Typeface.DEFAULT,
            Typeface.BOLD
        )

        bind.search.setText("")
        bind.loader.isVisible = true

        page = 1

        when (selectedTab) {
            bind.live -> {
                selectedTabText = "live"
                if (!isFirst) {
                    viewModel.getLiveShow(
                        "live".request(),
                        selectedCategoryRequest(),
                        page = page.toString().request()
                    )
                }
            }

            bind.popular -> {
                selectedTabText = "popular"
                viewModel.getLiveShow(
                    "popular".request(),
                    selectedCategoryRequest(),
                    page = page.toString().request()
                )
            }

            bind.comingSoon -> {
                selectedTabText = "upcoming"
                viewModel.getLiveShow(
                    "upcoming".request(),
                    selectedCategoryRequest(),
                    page = page.toString().request()
                )
            }
        }
    }


}
