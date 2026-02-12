package io.bidswipe.app.ui.sellerProfile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
import io.bidswipe.app.utils.isTablet
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request

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
	private var sellerId:String?=null


	override fun onResume() {
		super.onResume()
		if (Utils.isOnline(mCtx)) {
			bind.noInternet.isVisible = false
			bind.loader.isVisible = false
			bind.recycler.isVisible = true
			bind.noData.isVisible = false
			bind.loader.isVisible=true
			viewModel.getUserClips(sellerId,page.toString())
		} else {
			bind.recycler.isVisible = false
			bind.noData.isVisible = false
			bind.loader.isVisible = false
			bind.noInternet.isVisible = true
		}
	}

	override fun onViewCreated(view : View , savedInstanceState : Bundle?) {
		super.onViewCreated(view , savedInstanceState)

		if(arguments!=null){
			bind.header.isVisible=requireArguments().getBoolean("fromAccount",false)
			bind.header.onBackClick { findNavController().popBackStack() }
		}

		 sellerId =if(requireActivity() is SellerProfileActivity) (requireActivity() as SellerProfileActivity).sellerId else null

		bind.recycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
			override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
				super.onScrolled(recyclerView, dx, dy)
				val layoutManager = bind.recycler.layoutManager as LinearLayoutManager
				val lastItemPosition = layoutManager.findLastVisibleItemPosition()
				if (lastItemPosition == (clipList.size - 1)) {
					if (!isLoading) {
						isLoading = true
						page++
						bind.bottomLoader.isVisible = true
						viewModel.getUserClips(sellerId,page.toString())

					}
				}
			}
		})

		clipsAdapter = ClipsAdapter(mList = clipList){ data,pos->
			startActivity(
				Intent(mCtx, ClipEditActivity::class.java).putExtra("videoUrl", clipList[pos]?.clipUrl?:"").putExtra("isEdit", sellerId==null)
			)
		}

		(bind.recycler.layoutManager as GridLayoutManager).setSpanCount(if (resources.isTablet()) 3 else 2)
		bind.recycler.adapter = clipsAdapter

		bind.loader.isVisible=true
		viewModel.getUserClips(sellerId,page.toString())
		viewModel.getUserClipsRepo.observe(viewLifecycleOwner){
			bind.loader.isVisible = false
			bind.bottomLoader.isVisible = false
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

					isLoading = page >= (it.value.totalPage ?: 0)

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