package io.bidswipe.app.ui.dashboard.sellerHub

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.PromoteFeatureAdapter
import io.bidswipe.app.databinding.FragmentPromoteToolsBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetPromoteToolsResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.setHapticClickListener
import io.bidswipe.app.utils.toTutorials

class PromoteToolsFragment : BaseFragment<SellerHubViewModel , FragmentPromoteToolsBinding>() {
	override fun getModel() : Class<SellerHubViewModel> = SellerHubViewModel::class.java

	override fun getBind(
        inflater : LayoutInflater ,
        view : ViewGroup? ,
    ) = FragmentPromoteToolsBinding.inflate(inflater , view , false)

	private var gridList = mutableListOf<GetPromoteToolsResponse.Data.Feature?>()
	private lateinit var gridAdapter : PromoteFeatureAdapter

	private val mClick = object : RecyclerClicks {
		override fun itemClick(pos : Int , status : String?) {
		}

	}

	override fun onViewCreated(view : View , savedInstanceState : Bundle?) {
		super.onViewCreated(view , savedInstanceState)

		bind.header.onBackClick {
			finish()
		}

		gridAdapter = PromoteFeatureAdapter(gridList , mClick)
		bind.gridRecycler.adapter = gridAdapter

        bind.startLearning.setHapticClickListener {
			startActivity(mCtx.toTutorials().putExtra("type" , "promoteTools"))
		}

		bind.loader.isVisible = true
		viewModel.getPromoteTools()

		viewModel.getPromoteToolsRepo.observe(viewLifecycleOwner) {
			when (it) {

				is Resource.Success -> {
					bind.loader.isVisible = false
					val mData = it.value.data
					gridList.clear()
					bind.shows.text = (mData?.showOptions?.shows ?: 0).toString()
					bind.views.text = (mData?.showOptions?.views ?: 0).toString()
					bind.followers.text = (mData?.showOptions?.followers ?: 0).toString()
					gridList.addAll(mData?.features ?: emptyList())
					gridAdapter.notifyDataSetChanged()
				}

				is Resource.Error -> {

					bind.loader.isVisible = false

					it.parse(mCtx , TAG , object : AlertClicks {
						override fun primaryClick(dialog : AppBottomSheet) {
							dialog.dismiss()

						}

						override fun secondaryClick(dialog : AppBottomSheet) {
							dialog.dismiss()

						}
					})

				}

			}

		}

	}

}