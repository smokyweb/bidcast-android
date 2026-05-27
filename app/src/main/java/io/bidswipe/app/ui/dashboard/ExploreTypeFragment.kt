package io.bidswipe.app.ui.dashboard

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Build
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
import io.bidswipe.app.App
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.HomeAdapter
import io.bidswipe.app.controller.SubCategoryAdapter
import io.bidswipe.app.databinding.FragmentExploreTypeBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.StreamModel
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetMyShowResponse
import io.bidswipe.app.network.response.GetSubCategoriesResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.custom.UpcomingShowSheet
import io.bidswipe.app.ui.more.NotificationActivity
import io.bidswipe.app.ui.sellerProfile.SellerProfileActivity
import io.bidswipe.app.ui.watchStream.ViewLiveShowActivity
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.hideKeyboard
import io.bidswipe.app.utils.isTablet
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.runSafe
import io.bidswipe.app.utils.setHapticClickListener
import io.bidswipe.app.utils.value
import androidx.core.view.isVisible

@Suppress("DEPRECATION")
class ExploreTypeFragment : BaseFragment<DashViewModel, FragmentExploreTypeBinding>() {
    override fun getModel(): Class<DashViewModel> = DashViewModel::class.java

    override fun getBind(inflater: LayoutInflater, view: ViewGroup?) =
        FragmentExploreTypeBinding.inflate(inflater, view, false)

    private lateinit var homeAdapter: HomeAdapter
    private var categoriesList = mutableListOf<String>()
    private var showList = mutableListOf<GetMyShowResponse.Data?>()
    private var romIdsList = mutableListOf<String>()
    private var streamList = mutableListOf<StreamModel>()
    private var category = ""
    private var subCategory: String? = null
    private var page = 1
    private var isLoading = false

    private var selectedTabText = "live"

    // Basecamp #9933301500 (2026-05-27): browse filters state. Mutated by
    // BrowseFiltersSheet on Apply; threaded through every getExploreLiveShow
    // call via the loadShows() helper below.
    private var browseFilters = BrowseFilters()

    /**
     * Single entry point for every call to viewModel.getExploreLiveShow().
     * Threads the current browseFilters through so we don't have to update
     * 13 different call sites whenever the filter shape changes.
     */
    private fun loadShows(
        type: String = selectedTabText,
        search: String? = null,
        pageOverride: String? = null,
    ) {
        val f = browseFilters
        viewModel.getExploreLiveShow(
            type = type.request(),
            category = category.request(),
            subCategory = subCategory?.request(),
            search = search?.request(),
            page = pageOverride?.request(),
            showFormat = f.showFormat?.request(),
            tag = f.tag?.request(),
            premierShop = if (f.premierShop) "1".request() else null,
            shipCountry = f.shipCountry?.request(),
            shipState = f.shipState?.request(),
            shipping = f.shipping?.request(),
        )
    }

    private val viewLiveShowLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            page = 1
            loadShows(search = bind.search.value().ifEmpty { null }, pageOverride = page.toString())
        }
    }

    var subCategoryAdapter: SubCategoryAdapter? = null
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
                    // [Basecamp #9930403446] Upcoming-show tap → popup with date/time
                    if (selectedTabText == "upcoming") {
                        val show = showList[pos]
                        UpcomingShowSheet(
                            mCtx = mCtx,
                            profileImageUrl = show?.user?.profileImage,
                            username = show?.user?.username ?: show?.user?.name,
                            showDate = show?.date,
                            showTime = show?.time,
                        ).show()
                        return
                    }

//					if (showList[pos]?.isLive == true) {
                    val roomId = showList[pos]?.roomId.toString()
                    if (App.PIPMode) {
                        Alerts.error(mCtx, "You are already in Live show")
                    } else {
                        viewLiveShowLauncher.launch(
                            Intent(
                                mCtx,
                                ViewLiveShowActivity::class.java
                            ).putExtra("roomId", roomId)
                                .putExtra("userId", showList[pos]?.userId.toString())
                                .putExtra("roomIdsList", romIdsList.joinToString(","))
                                .putParcelableArrayListExtra(
                                    "streamList",
                                    ArrayList(streamList)
                                )
                        )
                    }
//					}
                }
            }

        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        category = arguments?.getString("category") ?: ""
        subCategory = arguments?.getString("subcategory")

        val headerText = if (subCategory != null) subCategory?.asCapital() ?: "" else category.asCapital()
        bind.header.setHeaderText(headerText)

        val subCategories = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelableArrayList(
                "sub_list",
                GetSubCategoriesResponse.Data.Subcategory::class.java
            )
        } else {
            @Suppress("DEPRECATION")
            arguments?.getParcelableArrayList("sub_list")
        }

        if (subCategories?.isNotEmpty() == true) {
            subCategoryAdapter = SubCategoryAdapter(subCategories, object : RecyclerClicks {
                override fun itemClick(pos: Int, status: String?) {
                    subCategories.forEachIndexed { index, sub ->
                        sub.isSelected = pos == index
                        subCategoryAdapter?.notifyDataSetChanged()
                    }

                    subCategory = subCategories[pos].name
                    page = 1
                    bind.loader.isVisible = true
                    loadShows()
                }
            }, "explore")

            bind.categoryRecycler.adapter = subCategoryAdapter
            bind.categoryRecycler.isVisible = true

        } else {
            bind.categoryRecycler.isVisible = false
        }

        bind.root.setHapticClickListener {
            hideKeyboard(it)
        }

        bind.main.setHapticClickListener {
            hideKeyboard(it)
        }

        bind.header.onBackClick {
            findNavController().popBackStack()
        }

        App.socketManager?.onRoomCreated { showData ->
            log("START GOT EXPLORE  FRAGMENT ${showData.categoryId}--${showData.subCategoryId}")
            activity?.runOnUiThread {
                if (selectedTabText == "live") {
                    loadShows(search = bind.search.value().ifEmpty { null }, pageOverride = "1")
                }
            }
        }

        App.socketManager?.onRoomEnded { json ->
            log("END GOT EXPLORE  FRAGMENT $json")
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

        bind.swipeRefreshLayout.setOnRefreshListener {
            bind.search.setText("")
            bind.searchLayout.isEndIconVisible = false
            loadShows()
        }

        bind.searchLayout.isEndIconVisible = false
        bind.search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                bind.searchLayout.isEndIconVisible = !s.isNullOrEmpty()

                bind.loader.isVisible = true
                bind.recycler.isVisible = false
                bind.noData.isVisible = false

                if (!s.isNullOrEmpty()) {
                    loadShows(search = s.toString())
                } else {
                    loadShows()
                }
            }
        })

        bind.searchLayout.setEndIconOnClickListener {
            bind.search.setText("")
            loadShows()
            hideKeyboard(it)
        }

        bind.noInternet.setHapticClickListener {
            bind.loader.isVisible = true
            bind.noInternet.isVisible = false
            loadShows()
        }

        bind.header.onMorePrimaryClick {
            startActivity(
                Intent(mCtx, NotificationActivity::class.java).putExtra(
                    "slug",
                    "notification"
                )
            )
        }

        homeAdapter = HomeAdapter(showList, mClick)
        (bind.recycler.layoutManager as GridLayoutManager).setSpanCount(if (resources.isTablet()) 3 else 2)
        bind.recycler.adapter = homeAdapter

        bind.recycler.setOnScrollChangeListener { _, _, _, _, _ ->
            val layoutManager = bind.recycler.layoutManager as GridLayoutManager
            val lastItemPosition = layoutManager.findLastVisibleItemPosition()

            val listSize = showList.size

            if (lastItemPosition == listSize - 1 && !isLoading) {
                isLoading = true
                page++
                loadShows(search = bind.search.value().ifEmpty { null }, pageOverride = page.toString())
            }
        }


        selectTab(bind.live)

        bind.live.setHapticClickListener { selectTab(it as TextView) }
        bind.popular.setHapticClickListener { selectTab(it as TextView) }
        bind.comingSoon.setHapticClickListener { selectTab(it as TextView) }

        // Basecamp #9933301500 (2026-05-27): browse filter button
        bind.btnFilters.setHapticClickListener {
            BrowseFiltersSheet(initial = browseFilters) { applied ->
                browseFilters = applied
                bind.filterActiveDot.isVisible = applied.isActive
                page = 1
                bind.loader.isVisible = true
                loadShows()
            }.show(childFragmentManager, "browse_filters")
        }

        categoriesList = mutableListOf(category)
        loadShows()
        viewModel.getExploreLiveShowRepo.observe(viewLifecycleOwner) { it ->
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false
                    bind.swipeRefreshLayout.isRefreshing = false
                    bind.noInternet.isVisible = false

                    val mData = it.value.data

                    /*mData?.forEach {
                        romIdsList.add(StreamModel(it?.roomId.toString() , ""))
                    }*/

                    if (page == 1) {
                        romIdsList.clear()
                        showList.clear()
                        streamList.clear()
                    }

                    mData?.forEach {
                        showList.add(it)
                        streamList.add(
                            StreamModel(
                                it?.roomId.toString(),
                                it?.rtcToken ?: "",
                                thumbnail = it?.thumbnail?.get(0)
                            )
                        )
                        romIdsList.add(it?.roomId.toString())
                    }

                    if (showList.isEmpty()) {
                        bind.noData.isVisible = true
                        bind.recycler.isVisible = false
                    } else {
                        bind.noData.isVisible = false
                        bind.recycler.isVisible = true
                    }

                    homeAdapter.notifyDataSetChanged()
                    isLoading = page >= (it.value.totalPage ?: 0)
                }

                is Resource.Error -> {
                    bind.swipeRefreshLayout.isRefreshing = false
                    bind.loader.isVisible = false

                    if (it.isNetworkError) {
                        bind.noInternet.isVisible = true
                        bind.noData.isVisible = false
                        bind.recycler.isVisible = false
                    } else {
                        bind.noInternet.isVisible = false
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

    override fun onPause() {
        super.onPause()
        bind.search.setText("")
    }

    fun selectTab(selectedTab: TextView) {

        bind.search.setText("")

        listOf(bind.live, bind.popular, bind.comingSoon).forEach { tab ->
            tab.setTextAppearance(R.style.TitleMedium)
            tab.setTextColor(ContextCompat.getColor(mCtx, R.color.outlineVariant))
            tab.isSelected = (tab == selectedTab)
        }

        selectedTab.setTextColor(ContextCompat.getColor(mCtx, R.color.scrim))
        selectedTab.setTextAppearance(R.style.TitleLarge)

        bind.loader.isVisible = true

        when (selectedTab) {
            bind.live -> {
                selectedTabText = "live"
                loadShows(type = "live")
            }

            bind.popular -> {
                selectedTabText = "popular"
                loadShows(type = "popular")
            }

            bind.comingSoon -> {
                selectedTabText = "upcoming"
                loadShows(type = "upcoming")
            }

        }
    }

}