package io.bidswipe.app.ui.dashboard

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.BidsAdapter
import io.bidswipe.app.databinding.FragmentBidsBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.StreamModel
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.FetchBidResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.parse

class BidsFragment : BaseFragment<DashViewModel,FragmentBidsBinding>() {

    override fun getModel(): Class<DashViewModel> = DashViewModel::class.java

    override fun getBind(inflater: LayoutInflater, view: ViewGroup?) = FragmentBidsBinding.inflate(inflater,view, false)

    private lateinit var bidsAdapter : BidsAdapter
    private var mList = mutableListOf<FetchBidResponse.Data?>()

    private var mClick = object : RecyclerClicks {
        override fun itemClick(pos: Int, status: String?) {
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bidsAdapter = BidsAdapter(mList,mClick)

        bind.recycler.adapter = bidsAdapter


        bind.loader.isVisible = true

        viewModel.fetchBids()
        viewModel.fetchBidsRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {

                    bind.loader.isVisible = false
                    val mData = it.value.data

                    mList.clear()

                    mData?.forEach { _ ->
                        mList.addAll(mData)
                    }

                    if (mList.isEmpty()){
                        bind.noData.isVisible = true
                        bind.recycler.isVisible = false
                    }else{
                        bind.noData.isVisible = false
                        bind.recycler.isVisible = true
                    }

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