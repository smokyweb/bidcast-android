package io.bidswipe.app.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.BidsAdapter
import io.bidswipe.app.databinding.FragmentBidsBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.FetchBidResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request

class BidsFragment : BaseFragment<DashViewModel, FragmentBidsBinding>() {

    override fun getModel(): Class<DashViewModel> = DashViewModel::class.java

    override fun getBind(inflater: LayoutInflater, view: ViewGroup?) =
        FragmentBidsBinding.inflate(inflater, view, false)

    private lateinit var bidsAdapter: BidsAdapter
    private var mList = mutableListOf<FetchBidResponse.Data?>()
    private var page = 1
    private var isLoading = false

    private var mClick = object : RecyclerClicks {
        override fun itemClick(pos: Int, status: String?) {
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bidsAdapter = BidsAdapter(mList, mClick)

        bind.recycler.adapter = bidsAdapter

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
                        viewModel.fetchBids(page.toString())
                    }
                }
            }
        })

        bind.loader.isVisible = true

        viewModel.fetchBids("1")
        viewModel.fetchBidsRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {

                    bind.loader.isVisible = false
                    val mData = it.value.data

                    mList.clear()

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

                    log("SIZE : ${mList.size} ")

                    bidsAdapter.notifyDataSetChanged()

                }

                is Resource.Error -> {
                    bind.loader.isVisible = false
                    if (it.isNetworkError) {
                        errorToast(getString(R.string.no_internet))
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