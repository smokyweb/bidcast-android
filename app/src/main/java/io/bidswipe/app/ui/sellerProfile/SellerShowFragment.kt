package io.bidswipe.app.ui.sellerProfile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.HomeAdapter
import io.bidswipe.app.databinding.FragmentSellerShowBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetMyShowResponse
import io.bidswipe.app.ui.agoraStream.AgoraPublisherActivity
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request

class SellerShowFragment : BaseFragment<SellerViewModel , FragmentSellerShowBinding>() {
	override fun getModel() : Class<SellerViewModel> = SellerViewModel::class.java

	override fun getBind(
        inflater : LayoutInflater ,
        view : ViewGroup? ,
    ) = FragmentSellerShowBinding.inflate(inflater , view , false)

	private lateinit var showAdapter : HomeAdapter
	private var showList = mutableListOf<GetMyShowResponse.Data?>()
	private var page = 1
	private var isLoading = false
	private val mClicks = object : RecyclerClicks {
		override fun itemClick(pos : Int , status : String?) {

			/*startActivity(
				Intent(mCtx , AgoraPublisherActivity::class.java).putExtra(
					"showId" ,
					showList[pos]?.id.toString()
				)
			)*/

		}

	}

	override fun onResume() {
		super.onResume()
		if (Utils.isOnline(mCtx)) {
			bind.noInternet.isVisible = false
			bind.loader.isVisible = true
			page = 1
			viewModel.getMyScheduledShow("upcoming".request())
		} else {
			bind.recycler.isVisible = false
			bind.noData.isVisible = false
			bind.loader.isVisible = false
			bind.noInternet.isVisible = true
		}
	}


	override fun onViewCreated(view : View , savedInstanceState : Bundle?) {
		super.onViewCreated(view , savedInstanceState)

		showAdapter = HomeAdapter(showList , mClicks)

		bind.recycler.adapter = showAdapter
		bind.noInternet.onClick {
			bind.loader.isVisible = true
			bind.noInternet.isVisible = false
			page = 1
			viewModel.getMyScheduledShow("upcoming".request())
		}

		bind.loader.isVisible = true
		viewModel.getMyScheduledShow("upcoming".request())

		viewModel.getMyScheduledShowRepo.observe(viewLifecycleOwner) {
			when (it) {
				is Resource.Success -> {
					bind.loader.isVisible = false
					bind.noInternet.isVisible = false

					val mData = it.value.data
					if (page == 1) {
						showList.clear()
					}
					if (mData != null) {
						showList.addAll(mData)
					}

					if (showList.isEmpty()) {
						bind.noData.isVisible = true
						bind.recycler.isVisible = false
					} else {
						bind.noData.isVisible = false
						bind.recycler.isVisible = true
					}
					isLoading = page >= (it.value.totalPage ?: 0)
					showAdapter.notifyDataSetChanged()
				}

				is Resource.Error -> {
					bind.noData.isVisible = false
					bind.loader.isVisible = false

					if (it.isNetworkError) {
						bind.noInternet.isVisible = true
						bind.recycler.isVisible = false
						bind.noData.isVisible = false
//                        errorToast(getString(R.string.no_internet))
					} else {
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

				else -> {}

			}
		}


	}

}