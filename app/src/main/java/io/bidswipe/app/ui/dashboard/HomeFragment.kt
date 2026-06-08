package io.bidswipe.app.ui.dashboard

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
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
import io.bidswipe.app.databinding.FragmentHomeBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.StreamModel
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetMyShowResponse
import io.bidswipe.app.network.response.Product
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.custom.UpcomingShowSheet
import io.bidswipe.app.ui.more.NotificationActivity
import io.bidswipe.app.ui.product.ProductDetailsActivity
import io.bidswipe.app.ui.product.ProductSetDetailsActivity
import io.bidswipe.app.ui.sellerProfile.SellerProfileActivity
import io.bidswipe.app.ui.watchStream.ViewLiveShowActivity
import io.bidswipe.app.utils.Alerts
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

    // Basecamp #9960348333 (round 7 rebuild, 2026-06-04): Home no longer renders
    // search results inline. The search bar NAVIGATES to the dedicated tabbed
    // results page (SearchShowFragment) carrying the typed query. This restores
    // the original (commit 9f5d56dd) behavior that the UI overhaul stripped, and
    // ends the half-migrated mess where Home both navigated AND rendered inline.
    private var showList = mutableListOf<GetMyShowResponse.Data?>()
    private val categoryTiles = mutableListOf<HomeCategoryAdapter.CategoryTile>()
    private var romIdsList = mutableListOf<String>()
    private var streamList = mutableListOf<StreamModel>()
    private var page = 1
    private var isLoading = false
    private var selectedCategory = "for_you"
    private var selectedCategoryTileId = "for_you"
    private var hasInitializedCategories = false

    // Basecamp #9960348333 (round 7 rebuild): navigate to the dedicated search
    // results page, optionally carrying the query already typed on Home.
    private fun goToSearchResults(query: String?) {
        val args = query?.takeIf { it.isNotBlank() }?.let {
            androidx.core.os.bundleOf("query" to it)
        }
        findNavController().navigate(R.id.goToSearchShowFragment, args)
    }

    private val viewLiveShowLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            page = 1
            requestHomeLiveShows(
                search = bind.search.value().ifEmpty { null }?.request(),
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
    private fun selectedCategoryKey(type: String = selectedTabText) = if (type == "live") null else selectedCategory

    private fun isCurrentHomeLiveShowResult(result: HomeLiveShowResult): Boolean {
        if (result.type != selectedTabText) return false

        val activeCategory = selectedCategoryKey(result.type)
        val isForYouFallback = selectedCategory == "for_you" && result.category == "all"
        return result.category == activeCategory || isForYouFallback
    }

    private fun requestHomeLiveShows(
        type: String = selectedTabText,
        category: String? = selectedCategoryKey(type),
        pageToLoad: Int = page,
        search: okhttp3.RequestBody? = bind.search.value().ifEmpty { null }?.request(),
    ) {
        viewModel.getHomeLiveShow(
            requestType = type,
            requestCategory = category,
            requestPage = pageToLoad,
            type = type.request(),
            category = category?.request(),
            search = search,
            page = pageToLoad.toString().request(),
        )
    }

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

        // Basecamp #9960348333 (round 7 rebuild): Home is browse-only again.
        // The save-search bell + filter button belong on the dedicated search
        // results page now (which has its own filter), so they're hidden here.
        bind.saveBellBtn.isVisible = false
        bind.searchFilterBtn.isVisible = false

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
                    // Basecamp #9960348333 (round 7 rebuild): Home no longer hosts
                    // inline search, so flash sales just track their own data.
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
                requestHomeLiveShows(
                    search = bind.search.value().ifEmpty { null }?.request(),
                )
            }
        }

        bind.searchLayout.isEndIconVisible = false

        // Basecamp #9960348333 (round 7 rebuild): the Home search bar NAVIGATES to
        // the dedicated tabbed search results page (SearchShowFragment). Home keeps
        // its normal content (shows grid + flash sales) and never renders search
        // results inline; the results page owns the live search input + the
        // Shows/Products/Users tabs.
        //
        // The Home field is made non-focusable so it behaves like a BUTTON: a tap
        // can't steal focus + open the keyboard inline (which is what made the old
        // setOnClickListener unreliable — the framework consumed the first tap for
        // focus and the click never fired, so nothing navigated). Now every tap
        // (field, search icon) just opens the dedicated page.
        bind.search.isFocusable = false
        bind.search.isFocusableInTouchMode = false
        bind.search.isCursorVisible = false
        bind.search.setOnClickListener {
            hideKeyboard(it)
            goToSearchResults(null)
        }
        bind.searchLayout.setStartIconOnClickListener {
            goToSearchResults(null)
        }

        bind.swipeRefreshLayout.setOnRefreshListener {
            page = 1
            requestHomeLiveShows()
            viewModel.getCategory()
        }

        bind.noInternet.onClick {
            bind.loader.isVisible = false
            bind.noInternet.isVisible = false
            requestHomeLiveShows()
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
                        requestHomeLiveShows(
                            // MC cmpaj2fex0000w5hgq64jp9k4 merge (2026-05-24): kept GitLab's
                            // selectedCategoryKey() helper which sends null for live tab
                            // (avoids server-side filter on "for_you" sentinel value).
                            search = bind.search.value().ifEmpty { null }?.request(),
                            pageToLoad = 1,
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

        viewModel.homeLiveShowRepo.observe(viewLifecycleOwner) { result ->
            if (!isCurrentHomeLiveShowResult(result)) return@observe

            when (val resource = result.resource) {
                is Resource.Success -> {
                    bind.loader.isVisible = false
                    bind.swipeRefreshLayout.isRefreshing = false
                    bind.noInternet.isVisible = false

                    val mData = resource.value.data


                    if (result.page == 1) {
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
                    if (showList.isEmpty() && result.page == 1 && selectedCategory == "for_you" && result.category != "all") {
                        requestHomeLiveShows(category = "all", pageToLoad = 1)
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

                    isLoading = result.page >= (resource.value.totalPage ?: 0)

                }

                is Resource.Error -> {
                    bind.swipeRefreshLayout.isRefreshing = false
                    bind.loader.isVisible = false

                    if (resource.isNetworkError) {
                        bind.noInternet.isVisible = true
                        bind.recycler.isVisible = false
                        bind.noData.isVisible = false
                    } else if (resource.isBrowseAuthError()) {
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
                        resource.parse(mCtx, TAG, object : AlertClicks {
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
            requestHomeLiveShows()
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
                    requestHomeLiveShows(type = "live")
                }
            }

            bind.popular -> {
                selectedTabText = "popular"
                requestHomeLiveShows(type = "popular")
            }

            bind.comingSoon -> {
                selectedTabText = "upcoming"
                requestHomeLiveShows(type = "upcoming")
            }
        }
    }


}
