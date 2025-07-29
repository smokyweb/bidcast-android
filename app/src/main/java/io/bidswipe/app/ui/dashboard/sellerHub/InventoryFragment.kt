package io.bidswipe.app.ui.dashboard.sellerHub

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.InventoryAdapter
import io.bidswipe.app.databinding.FragmentInventoryBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetMyInventoryResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.toListProduct

class InventoryFragment : BaseFragment<SellerHubViewModel, FragmentInventoryBinding>() {
    override fun getModel(): Class<SellerHubViewModel> = SellerHubViewModel::class.java

    override fun getBind(inflater: LayoutInflater, view: ViewGroup?) =
        FragmentInventoryBinding.inflate(inflater, view, false)

    private var itemList = mutableListOf<GetMyInventoryResponse.Data?>()
    private lateinit var adapter: InventoryAdapter
    private var isLoading = false
    private var page = 1
    private var selectedTab = "active"

    private val mClick = object : RecyclerClicks {

        override fun itemClick(pos: Int, status: String?) {

            startActivity(mCtx.toListProduct().putExtra("product" , itemList[pos]))

        }

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bind.header.onBackClick {
            finish()
        }

        adapter = InventoryAdapter(itemList, mClick)

        bind.recycler.adapter = adapter

        bind.tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                selectedTab = tab?.text.toString().lowercase()
                page = 1
                bind.loader.isVisible = true
                viewModel.getMyInventory(selectedTab.request(), page.toString().request())
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {

            }

            override fun onTabReselected(tab: TabLayout.Tab?) {

            }
        })

        bind.addNewProduct.setOnClickListener {
            startActivity(mCtx.toListProduct())
        }

        bind.recycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = bind.recycler.layoutManager as LinearLayoutManager
                val lastItemPosition = layoutManager.findLastVisibleItemPosition()
                if (lastItemPosition == (itemList.size - 1)) {
                    if (!isLoading) {
                        isLoading = true
                        page++
                        bind.bottomLoader.isVisible = true
                        viewModel.getMyInventory(selectedTab.request(), page.toString().request())
                    }
                }
            }
        })

        bind.swipeRefreshLayout.setOnRefreshListener {
            page = 1
            isLoading = false
            itemList.clear()
            bind.recycler.isVisible = false
            bind.noData.isVisible = false
            viewModel.getMyInventory(selectedTab.request(), page.toString().request())
        }

        bind.noInternet.onClick {
            bind.noInternet.isVisible = false
            bind.bottomLoader.isVisible = false
            bind.loader.isVisible = true

            bind.tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    selectedTab = tab?.text.toString().lowercase()
                    page = 1
                    isLoading = false
                    itemList.clear()
                    bind.recycler.isVisible = false
                    bind.noData.isVisible = false
                    viewModel.getMyInventory(selectedTab.request(), page.toString().request())
                }

                override fun onTabUnselected(tab: TabLayout.Tab?) {}
                override fun onTabReselected(tab: TabLayout.Tab?) {}
            })
        }

        bind.loader.isVisible = true

        viewModel.getMyInventory("active".request(), "1".request())
        viewModel.getMyInventoryRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false
                    bind.bottomLoader.isVisible = false
                    bind.noInternet.isVisible = false
                    bind.swipeRefreshLayout.isRefreshing = false

                    val mData = it.value.data

                    if (page == 1) {
                        itemList.clear()
                    }

                    if (mData != null) {
                        itemList.addAll(mData)
                    }

                    if (itemList.isNotEmpty()) {
                        bind.recycler.isVisible = true
                        bind.noData.isVisible = false
                    } else {
                        bind.recycler.isVisible = false
                        bind.noData.isVisible = true
                    }

                    isLoading = page >= (it.value.totalPage ?: 0)

                    adapter.notifyDataSetChanged()

                }

                is Resource.Error -> {
                    bind.loader.isVisible = false
                    bind.bottomLoader.isVisible = false
                    bind.swipeRefreshLayout.isRefreshing = false


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

}