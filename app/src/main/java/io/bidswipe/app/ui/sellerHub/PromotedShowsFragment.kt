package io.bidswipe.app.ui.sellerHub

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import io.bidswipe.app.App
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.ShowListingAdapter
import io.bidswipe.app.databinding.FragmentPromotedShowsBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.LiveShowModel
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetMyShowResponse
import io.bidswipe.app.network.response.toLiveShowProduct
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.animatedNav
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request

class PromotedShowsFragment : BaseFragment<SellerHubViewModel, FragmentPromotedShowsBinding>() {
	override fun getModel(): Class<SellerHubViewModel> = SellerHubViewModel::class.java


	override fun getBind(inflater: LayoutInflater, view: ViewGroup?): FragmentPromotedShowsBinding = FragmentPromotedShowsBinding.inflate(inflater, view, false)

	private lateinit var showAdapter: ShowListingAdapter

	private var showList = mutableListOf<GetMyShowResponse.Data?>()

	private val mClicks = object : RecyclerClicks {
		override fun itemClick(pos: Int, status: String?) {

			val profile = App.profileResponse.value

			if (profile?.sellerIdentityStatus != "verified") {
				startActivity(Intent(mCtx, SellerVerificationActivity::class.java))
				return
			}

			val data = showList[pos]
			val user = data?.user
			val products = data?.products?.map { product -> product?.toLiveShowProduct() }

			if (products?.isEmpty() == true) {
				Alerts.error(mCtx, "No products found for this Show")
				return
			} else {
				products?.first()?.isCurrent = true
			}

			val showData = LiveShowModel(
				seller = LiveShowModel.Seller(
					id = user?.id.toString(),
					image = user?.profileImage,
					name = user?.name,
					rating = user?.rating ?: ""
				),
				products = emptyList<LiveShowModel.Product>(),
				roomId = "live_room_${userId}_${data?.id.toString()}",
				showDetail = data?.title ?: "",
				thumbnail = data?.thumbnail?.getOrNull(0) ?: "",
				viewerCount = "1",
				highestBid = LiveShowModel.HighestBid(
					bidAmount = "",
					userName = "",
					userImage = "",
					userId = "",
					productId = ""
				),
				isLive = true,
				time = Utils.timestamp().toString(),
				showId = data?.id.toString(),
				allowBidForAll = true,
				bidCountDown = "",
				showTimer = "",
				categoryId = data?.category?.id.toString()
			)


			viewModel.selectedShow = showData
			viewModel.showTime = showList[pos]?.time

			findNavController().navigate(R.id.action_promoteToolsFragment_to_showDetailsFragment, bundleOf("showId" to showList[pos]?.id.toString()))

		}

	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		showAdapter = ShowListingAdapter(showList, mClicks)

		bind.recycler.adapter = showAdapter

		bind.swipeRefreshLayout.setOnRefreshListener {

			viewModel.getMyScheduledShow("promoted".request())
		}

		bind.noInternet.onClick {
			viewModel.getMyScheduledShow("promoted".request())
		}

		viewModel.getMyScheduledShow("promoted".request())

		viewModel.getMyScheduledShowRepo.observe(viewLifecycleOwner) {
			when (it) {
				is Resource.Success -> {
					bind.noInternet.isVisible = false
					bind.swipeRefreshLayout.isRefreshing = false

					showList.clear()
					it.value.data?.let { data ->
						showList.addAll(data.distinctBy { show -> show?.id })
					}
					showAdapter.notifyDataSetChanged()

					if (showList.isEmpty()) {
						bind.noData.isVisible = true
						bind.recycler.isVisible = false
					} else {
						bind.noData.isVisible = false
						bind.recycler.isVisible = true
					}

				}

				is Resource.Error -> {
					bind.swipeRefreshLayout.isRefreshing = false
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

	}

}