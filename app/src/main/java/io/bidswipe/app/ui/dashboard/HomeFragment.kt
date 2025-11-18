package io.bidswipe.app.ui.dashboard

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import io.bidswipe.app.App
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.HomeAdapter
import io.bidswipe.app.controller.HomeCategoryAdapter
import io.bidswipe.app.databinding.FragmentHomeBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.StreamModel
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetMyShowResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.more.NotificationActivity
import io.bidswipe.app.ui.sellerProfile.SellerProfileActivity
import io.bidswipe.app.ui.watchStream.ViewLiveShowActivity
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.hideKeyboard
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.setHapticClickListener
import io.bidswipe.app.utils.value

@SuppressLint("NotifyDataSetChanged")
class HomeFragment : BaseFragment<DashViewModel, FragmentHomeBinding>() {

    override fun getModel(): Class<DashViewModel> = DashViewModel::class.java

    override fun getBind(inflater: LayoutInflater, view: ViewGroup?) =
        FragmentHomeBinding.inflate(inflater, view, false)

    private lateinit var homeAdapter: HomeAdapter
    private lateinit var categoryAdapter: HomeCategoryAdapter
    private var showList = mutableListOf<GetMyShowResponse.Data?>()
    private val categoryTiles = mutableListOf<HomeCategoryAdapter.CategoryTile>()
    private var romIdsList = mutableListOf<String>()
    private var streamList = mutableListOf<StreamModel>()
    private var page = 1
    private var isLoading = false

    private var selectedCategory = "for_you"
    private var selectedCategoryTileId = "for_you"
    private var hasInitializedCategories = false

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
                        val roomId = showList[pos]?.roomId.toString()

                        if (App.PIPMode) {
                            Alerts.error(mCtx, "You are already in Live show")
                        } else {
                            startActivity(
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
                    }
                }
            }

        }
    }

    private var selectedTabText = "live"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bind.header.setHapticClickListener {
            hideKeyboard(it)
        }

        bind.searchLayout.setEndIconOnClickListener {
            bind.search.setText("")
            hideKeyboard(it)
        }

        bind.main.setHapticClickListener {
            hideKeyboard(it)
        }

        bind.recycler.setHapticClickListener {
            hideKeyboard(it)
        }

        homeAdapter = HomeAdapter(showList, mClick)

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

        bind.recycler.setOnScrollChangeListener { _, _, _, _, _ ->
            val layoutManager = bind.recycler.layoutManager as GridLayoutManager
            val lastItemPosition = layoutManager.findLastVisibleItemPosition()

            val listSize = showList.size

            if (lastItemPosition == listSize - 1 && !isLoading) {
                isLoading = true
                page++
                viewModel.getLiveShow(
                    selectedTabText.request(),
                    selectedCategory.request(),
                    bind.search.value().ifEmpty { null }?.request(),
                    page.toString().request()
                )
            }
        }

        bind.searchLayout.isEndIconVisible = false

        bind.search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim() ?: ""
                bind.searchLayout.isEndIconVisible = query.isNotEmpty()

                if (!s.isNullOrEmpty()) {
                    bind.loader.isVisible = true
                    page = 1
                    viewModel.getLiveShow(
                        selectedTabText.request(),
                        selectedCategory.request(),
                        s.toString().request(),
                        page.toString().request()
                    )
                }
            }
        })

        bind.searchLayout.setEndIconOnClickListener {
            bind.search.setText("")
            bind.searchLayout.isEndIconVisible = false
            hideKeyboard(it)
        }

        bind.swipeRefreshLayout.setOnRefreshListener {
            page = 1
            viewModel.getLiveShow(
                selectedTabText.request(),
                selectedCategory.request(),
                page = page.toString().request()
            )
            viewModel.getCategory()
        }

        bind.noInternet.onClick {
            bind.loader.isVisible = false
            bind.noInternet.isVisible = false
            viewModel.getLiveShow(
                selectedTabText.request(),
                selectedCategory.request(),
                page = page.toString().request()
            )
            viewModel.getCategory()
        }

        selectTab(bind.live, true)

        bind.live.setHapticClickListener { selectTab(it as TextView, false) }

        bind.popular.setHapticClickListener { selectTab(it as TextView, false) }

        bind.comingSoon.setHapticClickListener { selectTab(it as TextView, false) }

        bind.loader.isVisible = false

        viewModel.getCategory()
        viewModel.getCategoryRepo.observe(viewLifecycleOwner) { it ->
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false
                    bind.noInternet.isVisible = false
                    bind.noData.isVisible = false
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
                        mData?.filter { data -> data?.isSelected == true }?.mapNotNull { category ->
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
                        bind.noData.isVisible = false
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

        viewModel.getLiveShowRepo.observe(viewLifecycleOwner) { it ->
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false
                    bind.swipeRefreshLayout.isRefreshing = false
                    bind.noInternet.isVisible = false
                    bind.noData.isVisible = false

                    val mData = it.value.data

//					romIdsList.clear()
                    streamList.clear()

                    mData?.forEach {
                        streamList.add(StreamModel(it?.roomId.toString(), it?.rtcToken ?: ""))
                        romIdsList.add(it?.roomId.toString())
                    }

                    if (page == 1) {
                        showList.clear()
                    }

                    mData?.forEach {
                        if (it?.user != null) {
                            showList.add(it)
                        }
                    }

                    if (showList.isEmpty()) {
                        bind.noData.isVisible = true
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
                selectedCategory.request(),
                page = page.toString().request()
            )
        }
    }

    fun selectTab(selectedTab: TextView, isFirst: Boolean) {
        val tabs = listOf(bind.live, bind.popular, bind.comingSoon)
        tabs.forEach {
            it.setTextAppearance(R.style.TitleMedium)
            it.setTextColor(ContextCompat.getColor(mCtx, R.color.outlineVariant))
            it.typeface = android.graphics.Typeface.create(
                android.graphics.Typeface.DEFAULT,
                android.graphics.Typeface.NORMAL
            )
        }
        selectedTab.setTextColor(ContextCompat.getColor(mCtx, R.color.scrim))
        selectedTab.setTextAppearance(R.style.TitleMedium)
        selectedTab.typeface = android.graphics.Typeface.create(
            android.graphics.Typeface.DEFAULT,
            android.graphics.Typeface.BOLD
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
                        selectedCategory.request(),
                        page = page.toString().request()
                    )
                }
            }

            bind.popular -> {
                selectedTabText = "popular"
                viewModel.getLiveShow(
                    "popular".request(),
                    selectedCategory.request(),
                    page = page.toString().request()
                )
            }

            bind.comingSoon -> {
                selectedTabText = "upcoming"
                viewModel.getLiveShow(
                    "upcoming".request(),
                    selectedCategory.request(),
                    page = page.toString().request()
                )
            }
        }
    }

}