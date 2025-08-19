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
    private var searchQuery: String = ""

    companion object{
        private var recommendedList = mutableListOf<GetCategoryResponse.Data?>()
        private var popularList = mutableListOf<GetCategoryResponse.Data?>()
        private var allList = mutableListOf<GetCategoryResponse.Data?>()
        private var isDataLoaded = false
    }


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
                bind.search.requestFocus()
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

        bind.search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchQuery = s.toString().trim()
                filterCurrentTabData(searchQuery)
            }
        })

        bind.swipeRefreshLayout.setOnRefreshListener {
            when (currentSelectedTab?.text) {
                "Recommended" -> viewModel.getCategory(type = "recommended")
                "Popular" -> viewModel.getCategory(type = "popular")
                else -> viewModel.getCategory()
            }
        }

        bind.noInternet.onClick {
            bind.loader.isVisible = true
            bind.noInternet.isVisible = false
            when {
                bind.recommended.isSelected -> viewModel.getCategory(type = "recommended")
                bind.popular.isSelected -> viewModel.getCategory(type = "popular")
                bind.all.isSelected -> viewModel.getCategory()
            }
        }

        bind.recommended.setOnClickListener { selectTab(it as TextView) }
        bind.popular.setOnClickListener { selectTab(it as TextView) }
        bind.all.setOnClickListener { selectTab(it as TextView) }

        if (isDataLoaded) {
            selectTab(bind.recommended)
            bind.loader.isVisible = false
        } else {
            selectTab(bind.recommended)

            viewModel.getCategoryRepo.observe(viewLifecycleOwner) { it ->
                handleCategoryResponse(it)
            }
        }
    }

    private fun filterCurrentTabData(query: String) {
        val currentList = when (currentSelectedTab?.text) {
            "Recommended" -> recommendedList
            "Popular" -> popularList
            else -> allList
        }
        exploreList.clear()
        if (query.isEmpty()) {
            exploreList.addAll(currentList)
        } else {
            exploreList.addAll(currentList.filter {
                it?.name?.contains(
                    query,
                    ignoreCase = true
                ) == true
            })
        }
        exploreAdapter.notifyDataSetChanged()

        bind.recycler.isVisible = exploreList.isNotEmpty()
        bind.noData.isVisible = exploreList.isEmpty() && query.isNotEmpty()
        bind.noInternet.isVisible = exploreList.isEmpty() && query.isEmpty()

    }

    private fun selectTab(selectedTab: TextView) {
        currentSelectedTab = selectedTab

        bind.search.text?.clear()
        searchQuery = ""

        val showLoader = when (currentSelectedTab?.text) {
            "Recommended" -> recommendedList.isEmpty()
            "Popular" -> popularList.isEmpty()
            else -> allList.isEmpty()
        }
        bind.loader.isVisible = showLoader
        when (currentSelectedTab?.text) {
            "Recommended" -> {
                if (recommendedList.isEmpty()) {
                    viewModel.getCategory(type = "recommended")
                } else {
                    exploreList.clear()
                    exploreList.addAll(recommendedList)
                    exploreAdapter.notifyDataSetChanged()
                    bind.loader.isVisible = false
                }
            }

            "Popular" -> {
                if (popularList.isEmpty()) {
                    viewModel.getCategory(type = "popular")
                } else {
                    exploreList.clear()
                    exploreList.addAll(popularList)
                    exploreAdapter.notifyDataSetChanged()
                    bind.loader.isVisible = false
                }
            }

            else -> {
                if (allList.isEmpty()) {
                    viewModel.getCategory()
                } else {
                    exploreList.clear()
                    exploreList.addAll(allList)
                    exploreAdapter.notifyDataSetChanged()
                    bind.loader.isVisible = false
                }
            }
        }

        listOf(bind.recommended, bind.popular, bind.all).forEach { tab ->
            tab.setTextAppearance(R.style.TitleMedium)
            tab.setTextColor(ContextCompat.getColor(mCtx, R.color.outlineVariant))
            tab.isSelected = (tab == selectedTab)
        }
        selectedTab.setTextColor(ContextCompat.getColor(mCtx, R.color.scrim))
        selectedTab.setTextAppearance(R.style.TitleLarge)
    }

    private fun handleCategoryResponse(it: Resource<GetCategoryResponse>) {
        bind.loader.isVisible = false
        bind.swipeRefreshLayout.isRefreshing = false
        when (it) {
            is Resource.Success -> {
                bind.loader.isVisible = false
                bind.swipeRefreshLayout.isRefreshing = false
                bind.noInternet.isVisible = false

                if (it.value.data.isNullOrEmpty()) {
                    bind.recycler.isVisible = false
                    bind.noData.isVisible = true
                    bind.noInternet.isVisible = false
                } else {
                    when (currentSelectedTab?.text) {
                        "Recommended" -> {
                            recommendedList.clear()
                            recommendedList.addAll(it.value.data)
                            exploreList.clear()
                            exploreList.addAll(recommendedList)
                        }

                        "Popular" -> {
                            popularList.clear()
                            popularList.addAll(it.value.data)
                            exploreList.clear()
                            exploreList.addAll(popularList)
                        }

                        else -> {
                            allList.clear()
                            allList.addAll(it.value.data)
                            exploreList.clear()
                            exploreList.addAll(allList)
                        }
                    }
                    exploreAdapter.notifyDataSetChanged()
                    isDataLoaded = true
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