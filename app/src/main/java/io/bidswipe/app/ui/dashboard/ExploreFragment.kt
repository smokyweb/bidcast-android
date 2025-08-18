package io.bidswipe.app.ui.dashboard

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.ExploreAdapter
import io.bidswipe.app.databinding.FragmentExploreBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetCategoryResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.dashboard.more.NotificationActivity
import io.bidswipe.app.utils.ids
import io.bidswipe.app.utils.parse

class ExploreFragment : BaseFragment<DashViewModel, FragmentExploreBinding>() {
    override fun getModel(): Class<DashViewModel> = DashViewModel::class.java

    override fun getBind(inflater: LayoutInflater, view: ViewGroup?) =
        FragmentExploreBinding.inflate(inflater, view, false)

    private lateinit var exploreAdapter: ExploreAdapter
    private var exploreList = mutableListOf<GetCategoryResponse.Data?>()
    private var currentSelectedTab: TextView? = null

    private val mClick = object : RecyclerClicks {
        override fun itemClick(pos: Int, status: String?) {

            val category = exploreList[pos]?.name

            findNavController().navigate(
                ids.goTopExploreType,
                bundleOf("category" to category)
            )

        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        exploreAdapter = ExploreAdapter(exploreList, mClick)
        bind.recycler.adapter = exploreAdapter

        bind.header.onMoreSecondaryClick {
            bind.searchExpandLayout.toggle()

            if (bind.searchExpandLayout.isExpanded) {
                bind.email.requestFocus()
            }
        }

        bind.header.onMorePrimaryClick {
            startActivity(
                Intent(mCtx, NotificationActivity::class.java).putExtra(
                    "slug",
                    "notification"
                )
            )
        }

        bind.swipeRefreshLayout.setOnRefreshListener {
            when(currentSelectedTab?.text) {
                "Recommended" -> viewModel.getCategory(type = "recommended")
                "Popular" -> viewModel.getCategory(type = "popular")
                else-> viewModel.getCategory()
            }
        }

        bind.noInternet.onClick {
            bind.loader.isVisible = true
            bind.noInternet.isVisible = false

            when {
                bind.searchExpandLayout.isExpanded -> viewModel.getCategory()
                bind.recommended.isSelected -> viewModel.getCategory(type = "recommended")
                bind.popular.isSelected -> viewModel.getCategory(type = "popular")
                bind.all.isSelected -> viewModel.getCategory()
            }
        }

        bind.recommended.setOnClickListener { selectTab(it as TextView) }
        bind.popular.setOnClickListener { selectTab(it as TextView) }
        bind.all.setOnClickListener { selectTab(it as TextView) }

        selectTab(bind.recommended)

//        bind.loader.isVisible = true
//        viewModel.getCategory()
        viewModel.getCategoryRepo.observe(viewLifecycleOwner) {
            bind.loader.isVisible = false
            bind.swipeRefreshLayout.isRefreshing = false
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false
                    bind.swipeRefreshLayout.isRefreshing = false
                    bind.noInternet.isVisible = false

                    if (it.value.data?.isNotEmpty() == true) {
                        exploreList.clear()
                        exploreList.addAll(it.value.data)
                        exploreAdapter.notifyDataSetChanged()
                    }
                }

                is Resource.Error -> {
                    bind.noInternet.isVisible = false
                    bind.swipeRefreshLayout.isRefreshing = false
                    bind.loader.isVisible = false

                    if (it.isNetworkError) {
                        bind.noInternet.isVisible = true
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

    private fun selectTab(selectedTab: TextView) {
        currentSelectedTab = selectedTab

        bind.loader.isVisible = true
        when(currentSelectedTab?.text) {
            "Recommended" -> viewModel.getCategory(type = "recommended")
            "Popular" -> viewModel.getCategory(type = "popular")
            else-> viewModel.getCategory()
        }

        listOf(bind.recommended, bind.popular, bind.all).forEach { tab ->
            tab.setTextAppearance(R.style.TitleMedium)
            tab.setTextColor(ContextCompat.getColor(mCtx, R.color.outlineVariant))
            tab.isSelected = (tab == selectedTab)
        }

        selectedTab.setTextColor(ContextCompat.getColor(mCtx, R.color.scrim))
        selectedTab.setTextAppearance(R.style.TitleLarge)

    }
}