package io.bidswipe.app.ui.more

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.CouponsAdapter
import io.bidswipe.app.databinding.FragmentCouponsBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetCouponsResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.parse

class CouponsFragment : BaseFragment<MoreViewModel, FragmentCouponsBinding>() {
    override fun getModel() = MoreViewModel::class.java

    override fun getBind(
        inflater: LayoutInflater,
        view: ViewGroup?
    ) = FragmentCouponsBinding.inflate(inflater, view, false)

    private var couponList = mutableListOf<GetCouponsResponse.Data?>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bind.header.onBackClick {
            finish()
        }

        bind.couponRecycler.adapter = CouponsAdapter(couponList, object : RecyclerClicks {
            override fun itemClick(pos: Int, status: String?) {

            }
        })

        bind.loader.isVisible = true
        viewModel.getCoupon()
        viewModel.getCouponRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false

                    val mData = it.value.data

                    couponList.clear()

                    if (mData?.isNotEmpty() == true) {
                        bind.noData.isVisible = false
                        bind.couponRecycler.isVisible = true
                        couponList.addAll(mData)
                    } else {
                        bind.noData.isVisible = true
                    }

                    bind.couponRecycler.adapter?.notifyDataSetChanged()

                }

                is Resource.Error -> {
                    bind.loader.isVisible = false

                    it.parse(mCtx, TAG, object : AlertClicks {
                        override fun primaryClick(dialog: AppBottomSheet) {
                            dialog.dismiss()

                        }

                        override fun secondaryClick(dialog: AppBottomSheet) {
                            dialog.dismiss()

                        }
                    })
                }

                else -> {}

            }
        }

    }

}
