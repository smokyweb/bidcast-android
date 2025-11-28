package io.bidswipe.app.ui.dashboard

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isNotEmpty
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.PurchasesAdapter
import io.bidswipe.app.databinding.FragmentPurchasesBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetProductsByStatusResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.product.ProductDetailsActivity
import io.bidswipe.app.ui.sellerProfile.SellerProfileActivity
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request

@SuppressLint("NotifyDataSetChanged")
class PurchasesFragment : BaseFragment<DashViewModel, FragmentPurchasesBinding>() {

    override fun getModel(): Class<DashViewModel> = DashViewModel::class.java

    override fun getBind(inflater: LayoutInflater, view: ViewGroup?) = FragmentPurchasesBinding.inflate(inflater, view, false)

    private lateinit var purchasesAdapter: PurchasesAdapter
    private var mList = mutableListOf<GetProductsByStatusResponse.Data?>()
    private var page = 1
    private var isLoading = false
    private var currentFilter = ""
    private var type = "purchased"

    private var mClick = object : RecyclerClicks {
        override fun itemClick(pos: Int, status: String?) {

            when(status){

                "product" -> {
                    if (mList[pos]?.orderId?.isNotEmpty() == true){
                        startActivity(
                            Intent(mCtx, ProductDetailsActivity::class.java).putExtra(
                                "productId",
                                mList[pos]?.product?.id.toString()).putExtra(
                                "type",
                                "orderDetail"
                            ).putExtra("orderId", mList[pos]?.orderId.toString())
                        )
                    }
                }

                "profile" -> {
                    if (mList[pos]?.product?.seller != null) {
                        startActivity(
                            Intent(mCtx, SellerProfileActivity::class.java).putExtra(
                                "sellerId",
                                mList[pos]?.product?.seller?.id.toString()
                            )
                        )
                    }
                }

            }


        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupFilterChips()
        purchasesAdapter = PurchasesAdapter(mList, mClick)

        bind.recycler.adapter = purchasesAdapter

        bind.recycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = bind.recycler.layoutManager as LinearLayoutManager
                val lastItemPosition = layoutManager.findLastVisibleItemPosition()
                if (lastItemPosition == (mList.size - 1)) {
                    if (!isLoading) {
                        isLoading = true
                        page++
                        bind.bottomLoader.isVisible = true
                        loadData()
                    }
                }
            }
        })


        bind.swipeRefreshLayout.setOnRefreshListener {
            page = 1
            loadData()
        }

        bind.noInternet.onClick {
            bind.loader.isVisible = true
            bind.noInternet.isVisible = false
            page = 1
            loadData()
        }

        bind.loader.isVisible = true
        loadData()
        viewModel.getPurchasedProductsByStatusRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false
                    bind.bottomLoader.isVisible = false
                    bind.noInternet.isVisible = false
                    bind.swipeRefreshLayout.isRefreshing = false

                    val mData = it.value.data
                    if (page == 1) {
                        mList.clear()

                    }
                    if (mData != null) {
                        mList.addAll(mData)
                    }

                    if (mList.isEmpty()) {
                        bind.noData.isVisible = true
                        bind.recycler.isVisible = false
                    } else {
                        bind.noData.isVisible = false
                        bind.recycler.isVisible = true
                    }

                    isLoading = page >= (it.value.totalPage ?: 0)

                    purchasesAdapter.notifyDataSetChanged()

                }

                is Resource.Error -> {
                    bind.noData.isVisible = false
                    bind.loader.isVisible = false
                    bind.bottomLoader.isVisible = false
                    bind.swipeRefreshLayout.isRefreshing = false

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

    fun reloadData() {
        if (Utils.isOnline(mCtx)) {
            bind.loader.isVisible = true
            bind.noInternet.isVisible = false
            page = 1
            loadData()
        } else {
            bind.loader.isVisible = false
            bind.noInternet.isVisible = true
            bind.recycler.isVisible = false
            bind.noData.isVisible = false
        }
    }

    fun filterByStatus(filter: String) {
        currentFilter = when (filter) {
            "in_progress" -> "in_progress"
            "completed" -> "completed"
            else -> ""
        }
        page = 1
        loadData()
    }

    private fun loadData() {
        viewModel.getPurchasedProductsByStatus(type.request(), page.toString().request(),currentFilter.ifEmpty { null }?.request())
    }

    private fun setupFilterChips() {
        if (bind.filterChipGroup.childCount > 0) {
            return // Already set up
        }

        val filters = listOf( "All", "In Progress", "Completed")
        filters.forEachIndexed { index, filter ->
            val chip = Utils.makeAChip(
                mCtx = mCtx,
                text = filter,
                selected = index == 0,
                closeIconVisible = false,
                chipPadding = 12,
            )
            chip.setOnClickListener {
                filterByStatus(filter.lowercase().replace(" ", "_"))
                // Update chip selection
                bind.filterChipGroup.check(chip.id)
            }
            bind.filterChipGroup.addView(chip)
        }

        // Select first chip (All)
        if (bind.filterChipGroup.isNotEmpty()) {
            bind.filterChipGroup.check(bind.filterChipGroup.getChildAt(0).id)
        }

    }
}