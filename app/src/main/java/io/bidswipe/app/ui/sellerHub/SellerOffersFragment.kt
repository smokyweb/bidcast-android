package io.bidswipe.app.ui.sellerHub

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.SellerOffersAdapter
import io.bidswipe.app.databinding.FragmentSellerOffersBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetOffersResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.dashboard.ChatActivity
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request

@SuppressLint("NotifyDataSetChanged")
class SellerOffersFragment : BaseFragment<SellerHubViewModel, FragmentSellerOffersBinding>() {
	override fun getModel(): Class<SellerHubViewModel> = SellerHubViewModel::class.java

	override fun getBind(inflater: LayoutInflater, view: ViewGroup?) = FragmentSellerOffersBinding.inflate(inflater, view, false)

	private var offerList = mutableListOf<GetOffersResponse.Data?>()

	private lateinit var adapter: SellerOffersAdapter

	private var page = 1
	private var isLoading = false

	private val mClick = object : RecyclerClicks {
		override fun itemClick(pos: Int, status: String?) {

			val sellerId = offerList[pos]?.user?.id.toString()
			val sellerName = offerList[pos]?.user?.name
			val sellerImage = offerList[pos]?.user?.profileImage

			when (status) {
				"chat" -> {
					val intent = Intent(mCtx, ChatActivity::class.java).apply {
						putExtra("id", sellerId)
						putExtra("name", sellerName)
						putExtra("image", sellerImage)
					}
					startActivity(intent)
				}

				"accept" -> {
					bind.loader.isVisible = true
					viewModel.offerUpdateStatus(offerList[pos]?.id.toString().request(), "accepted".request())
				}

				"reject" -> {
					bind.loader.isVisible = true
					viewModel.offerUpdateStatus(offerList[pos]?.id.toString().request(), "rejected".request())
				}

			}
		}
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		bind.header.onBackClick {
			finish()
		}

		adapter = SellerOffersAdapter(offerList, mClick)

		bind.recycler.adapter = adapter


		bind.recycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
			override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
				super.onScrolled(recyclerView, dx, dy)
				val layoutManager = bind.recycler.layoutManager as LinearLayoutManager
				val lastItemPosition = layoutManager.findLastVisibleItemPosition()
				if (lastItemPosition == (offerList.size - 1)) {
					if (!isLoading) {
						isLoading = true
						page++
						bind.bottomLoader.isVisible = true
						viewModel.offerList(page.toString().request(), "seller".request())
					}
				}
			}
		})

		bind.loader.isVisible = true
		viewModel.offerList(page.toString().request(), "seller".request())

		viewModel.offerListRepo.observe(viewLifecycleOwner) {
			when (it) {
				is Resource.Success -> {
					bind.loader.isVisible = false
					bind.bottomLoader.isVisible = false
					bind.noInternet.isVisible = false

					val mData = it.value.data

					if (page == 1) offerList.clear()

					if (mData?.isNotEmpty() == true) {
						bind.pendingCount.text = (it.value.pending ?: 0).toString()
						bind.acceptedCount.text = (it.value.accepted ?: 0).toString()
						bind.declinedCount.text = (it.value.declined ?: 0).toString()
						mData.filterNotNull().forEach { offer ->
							offerList.add(offer)
						}
						adapter.notifyDataSetChanged()
					}

					if (offerList.isNotEmpty()) {
						bind.recycler.isVisible = true
						bind.noData.isVisible = false
					} else {
						bind.recycler.isVisible = false
						bind.noData.isVisible = true
					}

					isLoading = page >= (it.value.totalPage ?: 0)

				}

				is Resource.Error -> {
					bind.loader.isVisible = false
					bind.bottomLoader.isVisible = false
					bind.noInternet.isVisible = false

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

		viewModel.offerUpdateStatusRepo.observe(viewLifecycleOwner) {

			when (it) {
				is Resource.Success -> {
					bind.loader.isVisible = false
					val index = offerList.indexOfFirst { offer -> offer?.id == it.value.data?.id }
					if (index != -1) {
						val offer = offerList[index]
						offer?.status = it.value.data?.status
						offerList[index] = offer
						adapter.notifyItemChanged(index, offer)
					}
				}

				is Resource.Error -> {
					bind.loader.isVisible = false

					if (it.isNetworkError) {
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