package io.bidswipe.app.ui.dashboard.sellerProfile

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.ReviewAdapter
import io.bidswipe.app.databinding.FragmentReviewListBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetRatingResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.parse

class ReviewListFragment : BaseFragment<SellerViewModel , FragmentReviewListBinding>() {
	override fun getModel() : Class<SellerViewModel> = SellerViewModel::class.java

	override fun getBind(
		inflater : LayoutInflater ,
		view : ViewGroup? ,
	) = FragmentReviewListBinding.inflate(inflater , view , false)
	private lateinit var adapter : ReviewAdapter
	private var list = mutableListOf<GetRatingResponse.Data.Rating?>()
	private var sellerId = ""
	private var mClick = object : RecyclerClicks {
		override fun itemClick(pos : Int , status : String?) {

		}

	}
	override fun onResume() {
		super.onResume()
		if (Utils.isOnline(mCtx)) {
			bind.noInternet.isVisible = false
			bind.loader.isVisible = true
			viewModel.getSellerRating(sellerId)
		} else {
			bind.recycler.isVisible = false
			bind.noData.isVisible = false
			bind.loader.isVisible = false
			bind.noInternet.isVisible = true
		}
	}


	@SuppressLint("NotifyDataSetChanged")
	override fun onViewCreated(view : View , savedInstanceState : Bundle?) {
		super.onViewCreated(view , savedInstanceState)

		sellerId = activity?.intent?.getStringExtra("userId") ?: ""
		adapter = ReviewAdapter(list , mClick)
		bind.recycler.adapter = adapter

		bind.loader.isVisible = true
		bind.noInternet.onClick {
			bind.loader.isVisible = true
			bind.noInternet.isVisible = false
			viewModel.getSellerRating(sellerId)
		}

		viewModel.getSellerRating(sellerId)
		viewModel.getSellerRatingRepo.observe(viewLifecycleOwner) {
			when (it) {
				is Resource.Success -> {
					bind.loader.isVisible = false
					bind.noInternet.isVisible = false

					val mData = it.value.data
					list.clear()
					if (mData?.ratings != null) {
						list.addAll(mData.ratings)
					}
					if (list.isEmpty()) {
						bind.noData.isVisible = true
						bind.recycler.isVisible = false
					} else {
						bind.noData.isVisible = false
						bind.recycler.isVisible = true

					}
					adapter.notifyDataSetChanged()

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