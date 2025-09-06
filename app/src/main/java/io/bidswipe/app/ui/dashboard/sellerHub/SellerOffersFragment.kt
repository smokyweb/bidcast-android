package io.bidswipe.app.ui.dashboard.sellerHub

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.SellerOffersAdapter
import io.bidswipe.app.databinding.FragmentSellerOffersBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetOffersResponse
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request

class SellerOffersFragment : BaseFragment<SellerHubViewModel , FragmentSellerOffersBinding>() {
	override fun getModel() : Class<SellerHubViewModel> = SellerHubViewModel::class.java

	override fun getBind(inflater : LayoutInflater , view : ViewGroup?) = FragmentSellerOffersBinding.inflate(inflater , view , false)

	private var itemList = mutableListOf<GetOffersResponse.Data?>()

	private lateinit var adapter : SellerOffersAdapter

	private var page = 1
	private var isLoading = false

	private val mClick = object : RecyclerClicks {

		override fun itemClick(pos : Int , status : String?) {

			bind.loader.isVisible = true

			if (status == "accept") {
				viewModel.offerUpdateStatus(itemList[pos]?.id.toString().request() , "accepted".request())
			} else if (status == "reject") {
				viewModel.offerUpdateStatus(itemList[pos]?.id.toString().request() , "rejected".request())
			}

		}
	}

	override fun onViewCreated(view : View , savedInstanceState : Bundle?) {
		super.onViewCreated(view , savedInstanceState)

		bind.header.onBackClick {
			finish()
		}

		adapter = SellerOffersAdapter(itemList , mClick)

		bind.recycler.adapter = adapter

		bind.loader.isVisible = true
		viewModel.offerList(1)
		viewModel.offerListRepo.observe(viewLifecycleOwner) {
			bind.loader.isVisible = false
			when (it) {
				is Resource.Success -> {
					if (page == 1) itemList.clear()
					if (it.value.data?.isNotEmpty() == true) {
						bind.recycler.isVisible = true
						bind.noData.isVisible = false

						bind.pendingCount.text = (it.value.pending ?: 0).toString()
						bind.acceptedCount.text = (it.value.accepted ?: 0).toString()
						bind.declinedCount.text = (it.value.declined ?: 0).toString()
						it.value.data.forEach {
							if (it != null) {
								itemList.add(it)
							}
						}
						adapter.notifyDataSetChanged()
					} else {
						bind.recycler.isVisible = false
						bind.noData.isVisible = true
					}
				}

				is Resource.Error -> {
					if (it.isNetworkError) {
						errorToast(getString(R.string.no_internet))
					} else {
						it.parse(mCtx , TAG)
					}
				}

				else -> {}

			}
		}

		viewModel.offerUpdateStatusRepo.observe(viewLifecycleOwner) {

			when (it) {
				is Resource.Success -> {
					bind.loader.isVisible = false
					var index = itemList.indexOfFirst { offer -> offer?.id == it.value.data?.id }
					if (index != - 1) {
						var offer = itemList[index]
						offer?.status = it.value.data?.status
						itemList[index] = offer
						adapter.notifyItemChanged(index , offer)
					}
				}

				is Resource.Error -> {
					bind.loader.isVisible = false
					if (it.isNetworkError) {
						errorToast(getString(R.string.no_internet))
					} else {
						it.parse(mCtx , TAG)
					}
				}

				else -> {}

			}
		}

	}
}