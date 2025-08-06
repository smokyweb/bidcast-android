package io.bidswipe.app.ui.dashboard

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import io.bidswipe.app.App
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.HomeAdapter
import io.bidswipe.app.databinding.FragmentHomeBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.StreamModel
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetMyShowResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.dashboard.more.NotificationActivity
import io.bidswipe.app.ui.dashboard.sellerProfile.SellerProfileActivity
import io.bidswipe.app.ui.dashboard.watchStream.ViewLiveShowActivity
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Prefs
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.ids
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.runSafe

class HomeFragment : BaseFragment<DashViewModel, FragmentHomeBinding>() {

    override fun getModel(): Class<DashViewModel> = DashViewModel::class.java

    override fun getBind(inflater: LayoutInflater, view: ViewGroup?) =
        FragmentHomeBinding.inflate(inflater, view, false)

    private lateinit var homeAdapter: HomeAdapter
    private var showList = mutableListOf<GetMyShowResponse.Data?>()
    private var categoriesList = mutableListOf<String>()
    private var romIdsList = mutableListOf<StreamModel>()

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

                        if (App.PIPMode){
                            Alerts.error(mCtx,"You are already in Live show")

                        }else{
                            startActivity(
                                Intent(
                                    mCtx,
                                    ViewLiveShowActivity::class.java
                                ).putExtra("position", pos)
                                    .putParcelableArrayListExtra("roomIdsList", romIdsList as ArrayList)
                            )
                        }



                    }

                }

            }

        }
    }
    private var selectedTabText = "live"

    @SuppressLint("NotifyDataSetChanged", "SuspiciousIndentation")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        homeAdapter = HomeAdapter(showList, mClick)

        bind.recycler.adapter = homeAdapter

        bind.header.onMorePrimaryClick {
            startActivity(
                Intent(mCtx, NotificationActivity::class.java).putExtra(
                    "slug",
                    "notification"
                )
            )
        }

        Log.d(TAG, "onViewCreated: ${Prefs(mCtx).getString(Prefs.PUSH_TOKEN)}")

        bind.header.onMoreSecondaryClick {
            findNavController().navigate(ids.goToSearchShowFragment)
        }

        bind.swipeRefreshLayout.setOnRefreshListener {
            viewModel.getLiveShow(selectedTabText.request())
        }

        bind.noInternet.onClick {
            bind.loader.isVisible = true
            bind.noInternet.isVisible = false
            viewModel.getLiveShow(selectedTabText.request())
        }

        selectTab(bind.live)

        bind.live.setOnClickListener { selectTab(it as TextView) }
        bind.popular.setOnClickListener { selectTab(it as TextView) }
        bind.comingSoon.setOnClickListener { selectTab(it as TextView) }

        categoriesList = mutableListOf("For You", "Collectibles", "Trading Cards")
        categoriesList.forEach {
            bind.chipGroup.addView(
                Utils.makeAChip(
                    mCtx = mCtx,
                    text = it,
                    selected = false
                )
            )
        }

        bind.chipGroup.setOnCheckedStateChangeListener { chipGroup, _ ->
            runSafe {
                val chipId = chipGroup.checkedChipId
                chipGroup.indexOfChild(chipGroup.findViewById(chipId))
            }
        }

        bind.loader.isVisible = true

        viewModel.getLiveShow(selectedTabText.request())
        viewModel.getLiveShowRepo.observe(viewLifecycleOwner) { it ->
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false
                    bind.swipeRefreshLayout.isRefreshing = false
                    bind.noInternet.isVisible = false

                    val mData = it.value.data

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
                    bind.swipeRefreshLayout.isRefreshing = false
                    bind.loader.isVisible = false

                    if (it.isNetworkError) {
                        bind.noInternet.isVisible = true
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

    fun selectTab(selectedTab: TextView) {
        val tabs = listOf(bind.live, bind.popular, bind.comingSoon)
        tabs.forEach {
            it.setTextAppearance(R.style.TitleMedium)
            it.setTextColor(ContextCompat.getColor(mCtx, R.color.outlineVariant))
        }
        selectedTab.setTextColor(ContextCompat.getColor(mCtx, R.color.scrim))
        selectedTab.setTextAppearance(R.style.TitleLarge)

        when (selectedTab) {
            bind.live -> {
                bind.loader.isVisible = true
                selectedTabText = "live"
                viewModel.getLiveShow("live".request())
            }

            bind.popular -> {
                bind.loader.isVisible = true
                selectedTabText = "popular"
                viewModel.getLiveShow("popular".request())
            }

            bind.comingSoon -> {
                bind.loader.isVisible = true
                selectedTabText = "upcoming"
                viewModel.getLiveShow("upcoming".request())
            }

        }
    }

}