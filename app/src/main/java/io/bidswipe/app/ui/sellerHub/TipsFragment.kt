package io.bidswipe.app.ui.sellerHub

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.TipsAdapter
import io.bidswipe.app.databinding.FragmentTipsBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetTipAmountResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.asMoney
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.parse

class TipsFragment : BaseFragment<SellerHubViewModel, FragmentTipsBinding>() {
	override fun getModel(): Class<SellerHubViewModel> = SellerHubViewModel::class.java

	override fun getBind(
		inflater: LayoutInflater,
		view: ViewGroup?,
	) = FragmentTipsBinding.inflate(inflater, view, false)

	private var tipsList = mutableListOf<GetTipAmountResponse.Data.Tip?>()

	private lateinit var tipsAdapter: TipsAdapter

	private val mClick = object : RecyclerClicks {
		override fun itemClick(pos: Int, status: String?) {
		}
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		onBackPressed { finish() }

		bind.header.onBackClick {
			finish()
		}

		tipsAdapter = TipsAdapter(tipsList, mClick)

		bind.recycler.adapter = tipsAdapter

		bind.loader.isVisible = true

		viewModel.getTipAmount()

		viewModel.getTipAmountRepo.observe(viewLifecycleOwner) {
			when (it) {
				is Resource.Success -> {
					bind.loader.isVisible = false
					val mData = it.value.data

					bind.toDayTip.text = mData?.summary?.todayTips?.toString()?.asMoney()
					bind.totalTip.text = mData?.summary?.totalTips?.asMoney()

					tipsList.clear()
					if (mData?.tips != null){
						tipsList.addAll(mData.tips)
					}

					if (tipsList.isEmpty()){
						bind.recycler.isVisible = false
						bind.noData.isVisible = true
					}else{
						bind.recycler.isVisible = true
						bind.noData.isVisible = false
					}

					tipsAdapter.notifyDataSetChanged()

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