package io.bidswipe.app.ui.dashboard

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.OffersAdapter
import io.bidswipe.app.databinding.FragmentOfferBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetOffersResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request

class OfferFragment : BaseFragment<DashViewModel, FragmentOfferBinding>() {
	
	override fun getModel(): Class<DashViewModel> = DashViewModel::class.java
	
	override fun getBind(inflater: LayoutInflater, view: ViewGroup?) = FragmentOfferBinding.inflate(inflater, view, false)
	
	private lateinit var offersAdapter: OffersAdapter
	private var mList = mutableListOf<GetOffersResponse.Data?>()
	
	private var mClick = object : RecyclerClicks {
		override fun itemClick(pos: Int, status: String?) {
			Log.d(TAG, "itemClick: ${mList[pos]}")
			bind.loader.isVisible = true
			if (status == "accept") {
				viewModel.offerUpdateStatus(mList[pos]?.id.toString().request(), "accepted".request())
			} else if (status == "reject") {
				viewModel.offerUpdateStatus(mList[pos]?.id.toString().request(), "rejected".request())
			}
		}
	}
	
	private var page = 1

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		
		offersAdapter = OffersAdapter(mList, mClick)
		
		bind.recycler.adapter = offersAdapter

		bind.noInternet.onClick {
			bind.loader.isVisible = true
			bind.noInternet.isVisible = false
			page = 1
			viewModel.offerList(page)
		}

		bind.swipeRefreshLayout.setOnRefreshListener {
			page = 1
			viewModel.offerList(page)
		}
		
		bind.loader.isVisible = true

		viewModel.offerList(page)
		viewModel.offerListRepo.observe(viewLifecycleOwner) {
			bind.swipeRefreshLayout.isRefreshing = false
			bind.noInternet.isVisible = false
			bind.loader.isVisible = false

			when (it) {
				is Resource.Success -> {
					if (page == 1) mList.clear()
					if (it.value.data?.isNotEmpty() == true) {
						bind.recycler.isVisible = true
						bind.noData.isVisible = false
						it.value.data.forEach {
							if (it != null) {
								mList.add(it)
							}
						}
						offersAdapter.notifyDataSetChanged()
					} else {
						bind.recycler.isVisible = false
						bind.noData.isVisible = true
					}
				}
				
				is Resource.Error -> {
					if (it.isNetworkError) {
						bind.noInternet.isVisible = true
						bind.recycler.isVisible = false
						bind.noData.isVisible = false

					} else {
						it.parse(mCtx, TAG)
					}
				}
				
				else -> {}
				
			}
		}

		viewModel.offerUpdateStatusRepo.observe(viewLifecycleOwner) {

			when (it) {
				is Resource.Success -> {
					bind.loader.isVisible = false
					val index=mList.indexOfFirst { offer -> offer?.id == it.value.data?.id  }
					if(index!=-1){
						val updatedItem = mList[index]?.copy(status = it.value.data?.status)
						mList[index] = updatedItem
						offersAdapter.notifyItemChanged(index, updatedItem)
					}
				}
				
				is Resource.Error -> {
					bind.loader.isVisible = false
					bind.swipeRefreshLayout.isRefreshing =false

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