package io.bidswipe.app.ui.dashboard

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.OffersAdapter
import io.bidswipe.app.databinding.FragmentOfferBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetOffersResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.product.ProductDetailsActivity
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request

@SuppressLint("NotifyDataSetChanged")
class OfferFragment : BaseFragment<DashViewModel, FragmentOfferBinding>() {

	override fun getModel(): Class<DashViewModel> = DashViewModel::class.java

	override fun getBind(inflater: LayoutInflater, view: ViewGroup?) = FragmentOfferBinding.inflate(inflater, view, false)

	private lateinit var offersAdapter: OffersAdapter
	private var mList = mutableListOf<GetOffersResponse.Data?>()

	private var page = 1
	private var isLoading = false

	private var mClick = object : RecyclerClicks {
		override fun itemClick(pos: Int, status: String?) {
			startActivity(
				Intent(mCtx, ProductDetailsActivity::class.java).putExtra(
					"productId",
					mList[pos]?.productId.toString()
				)
			)
		}
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		offersAdapter = OffersAdapter(mList, mClick)

		bind.recycler.adapter = offersAdapter

		bind.noInternet.onClick {
			bind.loader.isVisible = true
			bind.noInternet.isVisible = false
			page = 1
			viewModel.offerList(page.toString().request(), "user".request())
		}

		bind.recycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
			override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
				super.onScrolled(recyclerView, dx, dy)
				val layoutManager = bind.recycler.layoutManager as LinearLayoutManager
				val lastItemPosition = layoutManager.findLastVisibleItemPosition()
				if (lastItemPosition == (mList.size - 1)) {
					if (!isLoading) {
						isLoading = true
						page++
						bind.bottomLoader.isVisible = true
						viewModel.offerList(page.toString().request(), "user".request())
					}
				}
			}
		})

		bind.loader.isVisible = true

		viewModel.offerList(page.toString().request(), "user".request())
		viewModel.offerListRepo.observe(viewLifecycleOwner) { it ->
			viewModel.isViewPagerDataLoaded.value=true
			when (it) {
				is Resource.Success -> {
					val mData = it.value.data
					bind.noInternet.isVisible = false
					bind.bottomLoader.isVisible = false
					bind.loader.isVisible = false

					if (page == 1) mList.clear()

					if (mData?.isNotEmpty() == true) {
						it.value.data.forEach {
							if (it != null) {
								mList.add(it)
							}
						}
					}

					if (mList.isNotEmpty()) {
						bind.recycler.isVisible = true
						bind.noData.isVisible = false
					} else {
						bind.recycler.isVisible = false
						bind.noData.isVisible = true
					}

					offersAdapter.notifyDataSetChanged()

					isLoading = page >= (it.value.totalPage ?: 0)

				}

				is Resource.Error -> {
					bind.noInternet.isVisible = false
					bind.loader.isVisible = false
					bind.bottomLoader.isVisible = false

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

	fun reloadData() {
		if (Utils.isOnline(mCtx)) {
			bind.loader.isVisible = true
			bind.noInternet.isVisible = false
			page = 1
			viewModel.offerList(page.toString().request(), "user".request())
		} else {
			bind.loader.isVisible = false
			bind.noInternet.isVisible = true
			bind.recycler.isVisible = false
			bind.noData.isVisible = false
			viewModel.isViewPagerDataLoaded.value=true
		}
	}

}