package io.bidswipe.app.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.OffersAdapter
import io.bidswipe.app.databinding.FragmentOfferBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetOffersResponse
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request

class OfferFragment : BaseFragment<DashViewModel, FragmentOfferBinding>() {
	
	override fun getModel(): Class<DashViewModel> = DashViewModel::class.java
	
	override fun getBind(inflater: LayoutInflater, view: ViewGroup?) = FragmentOfferBinding.inflate(inflater, view, false)
	
	private lateinit var offersAdapter: OffersAdapter
	private var mList = mutableListOf<GetOffersResponse.Data?>()
	
	private var mClick = object : RecyclerClicks {
		override fun itemClick(pos: Int, status: String?) {
			bind.loader.isVisible = true
			if (status == "accept") {
				viewModel.offerUpdateStatus(mList[pos]?.id.toString().request(), "accepted".request())
			} else if (status == "reject") {
				viewModel.offerUpdateStatus(mList[pos]?.id.toString().request(), "rejected".request())
			}
		}
	}
	
	private var page = 1
	private var isLoading = false
	
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		
		offersAdapter = OffersAdapter(mList, mClick)
		
		bind.recycler.adapter = offersAdapter
		
		bind.loader.isVisible = true
		viewModel.offerList(page)
		viewModel.offerListRepo.observe(viewLifecycleOwner) {
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
						errorToast(getString(R.string.no_internet))
					} else {
						it.parse(mCtx, TAG)
					}
				}
				
				else -> {}
				
			}
		}
		
		viewModel.offerUpdateStatusRepo.observe(viewLifecycleOwner) {
			bind.loader.isVisible = false
			when (it) {
				is Resource.Success -> {
					var index=mList.indexOfFirst {offer -> offer?.id == it.value.data?.id  }
					if(index!=-1){
						var offer = mList[index]
						offer?.status = it.value.data?.status
						mList[index]=offer
						offersAdapter.notifyItemChanged(index,offer)
					}
				}
				
				is Resource.Error -> {
					if (it.isNetworkError) {
						errorToast(getString(R.string.no_internet))
					} else {
						it.parse(mCtx, TAG)
					}
				}
				
				else -> {}
				
			}
		}
		
		
	}
	
}