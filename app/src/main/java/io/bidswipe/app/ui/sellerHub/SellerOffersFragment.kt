package io.bidswipe.app.ui.sellerHub

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.SellerOffersAdapter
import io.bidswipe.app.databinding.FragmentSellerOffersBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetOffersResponse
import io.bidswipe.app.ui.dashboard.ChatActivity
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request

@SuppressLint("NotifyDataSetChanged")
class SellerOffersFragment : BaseFragment<SellerHubViewModel, FragmentSellerOffersBinding>() {
	override fun getModel(): Class<SellerHubViewModel> = SellerHubViewModel::class.java

	override fun getBind(inflater: LayoutInflater, view: ViewGroup?) = FragmentSellerOffersBinding.inflate(inflater, view, false)

	private var itemList = mutableListOf<GetOffersResponse.Data?>()

	private lateinit var adapter: SellerOffersAdapter

	private var page = 1

	private val mClick = object : RecyclerClicks {

		override fun itemClick(pos: Int, status: String?) {

			val sellerId = itemList[pos]?.user?.id.toString()
			val sellerName = itemList[pos]?.user?.name
			val sellerImage = itemList[pos]?.user?.profileImage

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
					viewModel.offerUpdateStatus(itemList[pos]?.id.toString().request(), "accepted".request())
				}

				"reject" -> {
					bind.loader.isVisible = true
					viewModel.offerUpdateStatus(itemList[pos]?.id.toString().request(), "rejected".request())
				}

			}
		}
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		bind.header.onBackClick {
			finish()
		}

		adapter = SellerOffersAdapter(itemList, mClick)

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
						it.value.data.forEach { offer ->
							if (offer != null) {
								itemList.add(offer)
							}
						}
						adapter.notifyDataSetChanged()
					} else {
						bind.recycler.isVisible = false
						bind.noData.isVisible = true
					}
				}

				is Resource.Error -> {
					it.parse(mCtx, TAG)
				}

				else -> {}

			}
		}

		viewModel.offerUpdateStatusRepo.observe(viewLifecycleOwner) {

			when (it) {
				is Resource.Success -> {
					bind.loader.isVisible = false
					val index = itemList.indexOfFirst { offer -> offer?.id == it.value.data?.id }
					if (index != -1) {
						val offer = itemList[index]
						offer?.status = it.value.data?.status
						itemList[index] = offer
						adapter.notifyItemChanged(index, offer)
					}
				}

				is Resource.Error -> {
					bind.loader.isVisible = false
					it.parse(mCtx, TAG)
				}

				else -> {}

			}
		}

	}
}