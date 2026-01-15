package io.bidswipe.app.ui.sellerProfile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.ClipsAdapter
import io.bidswipe.app.databinding.FragmentClipsBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetClipsResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.parse

class ClipsFragment : BaseFragment<SellerViewModel , FragmentClipsBinding>() {
	override fun getModel() = SellerViewModel::class.java

	override fun getBind(
		inflater : LayoutInflater ,
		view : ViewGroup? ,
	) = FragmentClipsBinding.inflate(inflater , view , false)

	private lateinit var clipsAdapter : ClipsAdapter

	private  var clipList =mutableListOf<GetClipsResponse.Data?>()
	private var page=1
	private var isLoading=false


	override fun onResume() {
		super.onResume()
		if (Utils.isOnline(mCtx)) {
			bind.noInternet.isVisible = false
			bind.loader.isVisible = false
			bind.recycler.isVisible = true
			bind.noData.isVisible = false
		} else {
			bind.recycler.isVisible = false
			bind.noData.isVisible = false
			bind.loader.isVisible = false
			bind.noInternet.isVisible = true
		}
	}

	override fun onViewCreated(view : View , savedInstanceState : Bundle?) {
		super.onViewCreated(view , savedInstanceState)

		val sellerId = (requireActivity() as SellerProfileActivity).sellerId

		clipsAdapter = ClipsAdapter(mList = clipList)
		bind.recycler.adapter = clipsAdapter

		bind.loader.isVisible=true
		viewModel.getUserClips(sellerId)
		viewModel.getUserClipsRepo.observe(viewLifecycleOwner){
			bind.loader.isVisible = false
			when (it) {
				is Resource.Success -> {
					bind.noInternet.isVisible = false

					val mData = it.value.data
					if (page == 1) {
						clipList.clear()
					}
					if (mData != null) {
						clipList.addAll(mData)
					}

					if (clipList.isEmpty()) {
						bind.noData.isVisible = true
						bind.recycler.isVisible = false
					} else {
						bind.noData.isVisible = false
						bind.recycler.isVisible = true
					}

//					isLoading = page >= (it.value.totalPage ?: 0)

					clipsAdapter.notifyDataSetChanged()
				}

				is Resource.Error -> {
					bind.noData.isVisible = false
					bind.loader.isVisible = false

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
}