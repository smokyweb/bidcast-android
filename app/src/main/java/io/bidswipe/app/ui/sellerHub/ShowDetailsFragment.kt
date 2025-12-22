package io.bidswipe.app.ui.sellerHub

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.databinding.FragmentShowDetailsBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.asMoney
import io.bidswipe.app.utils.ids
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.toSellerShow

class ShowDetailsFragment : BaseFragment<SellerHubViewModel, FragmentShowDetailsBinding>() {
	override fun getModel(): Class<SellerHubViewModel> = SellerHubViewModel::class.java

	override fun getBind(
		inflater: LayoutInflater,
		view: ViewGroup?,
	) = FragmentShowDetailsBinding.inflate(inflater, view, false)

	private var showId = ""
	private var videoUrl = ""

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		showId = arguments?.getString("showId") ?: ""

		bind.header.onBackClick {
			findNavController().popBackStack()
		}

		bind.header.setHeaderText(viewModel.selectedShow?.showDetail ?: "Show Details")

		val menu = PopupMenu(mCtx, bind.header.findViewById<AppCompatImageView>(R.id.primaryIcon))
		menu.menuInflater.inflate(R.menu.show_menu, menu.menu)

		menu.setOnMenuItemClickListener {
			when (it.itemId) {
				ids.restart_show -> {
					startActivity(mCtx.toSellerShow(viewModel.showTime, viewModel.selectedShow))
				}
			}
			return@setOnMenuItemClickListener true
		}

		bind.header.onMorePrimaryClick {
			menu.show()
		}

		bind.watchVideo.setOnClickListener {

			if (videoUrl.isNotEmpty()) {
				findNavController().navigate(R.id.showDetailsVideoReceiptPlayerFragment2, bundleOf("videoUrl" to videoUrl))
			} else {
				successToast("Video is not available")
			}

		}

		bind.loader.isVisible = true
		viewModel.getShowOverview(showId)

		viewModel.getShowDetailRepo.observe(viewLifecycleOwner) {
			when (it) {
				is Resource.Success -> {
					bind.loader.isVisible = false

					val mData = it.value.data

					log("data: $mData")

					videoUrl = mData?.fileUrl ?: ""

					bind.duration.text = buildString {
						append("Show Duration: ")
						append(mData?.videoDuration)
					}

					bind.sales.text = mData?.totalSales?.asMoney()
					bind.orders.text = mData?.orderCount.toString()
					bind.durationShow.text = mData?.videoDuration
					bind.shares.text = mData?.shareCount.toString()
					bind.viewers.text = mData?.viewerCount.toString()
					bind.newFollowers.text = mData?.newFollowers.toString()
					bind.contributions.text = mData?.contributionsCount.toString().asMoney()
					bind.bids.text = mData?.totalBids.toString()

				}

				is Resource.Error -> {
					bind.loader.isVisible = false

					it.parse(mCtx, TAG, object : AlertClicks {
						override fun primaryClick(dialog: AppBottomSheet) {
							dialog.dismiss()
						}

						override fun secondaryClick(dialog: AppBottomSheet) {
							dialog.dismiss()
						}
					})

				}

				else -> {}

			}
		}

	}

}