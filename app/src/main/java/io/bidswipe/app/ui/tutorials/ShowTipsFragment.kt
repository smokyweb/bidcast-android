package io.bidswipe.app.ui.tutorials

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.ProductTipsPagerAdapter
import io.bidswipe.app.databinding.FragmentShowTipsBinding
import io.bidswipe.app.model.LiveShowModel
import io.bidswipe.app.network.response.toLiveShowProduct
import io.bidswipe.app.ui.dashboard.DashViewModel
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.ids
import io.bidswipe.app.utils.setHapticClickListener
import io.bidswipe.app.utils.string
import io.bidswipe.app.utils.toScheduleShow
import io.bidswipe.app.utils.toSellerShow

class ShowTipsFragment : BaseFragment<DashViewModel, FragmentShowTipsBinding>() {

	override fun getModel(): Class<DashViewModel> = DashViewModel::class.java

	override fun getBind(inflater: LayoutInflater, view: ViewGroup?) =
		FragmentShowTipsBinding.inflate(inflater, view, false)

	private var productTipList = mutableListOf("")
	private lateinit var pagerAdapter: ProductTipsPagerAdapter

	private var type = ""
	private var showId = ""

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		type = arguments?.getString("type", "").toString()
		showId = arguments?.getString("showId", "").toString()
		log("ShowId : $showId")

		when (type) {
			"liveTips" -> bind.header.setHeaderText("Going Live Tips")
			"bringInBuyers" -> bind.header.setHeaderText("Bring In Buyers")
			"goLive" -> bind.header.setHeaderText("Live Stream Tips")
		}

		bind.header.onBackClick {
			findNavController().popBackStack()
		}

		bind.stepProgress.max = productTipList.size

		pagerAdapter = ProductTipsPagerAdapter(productTipList, type)
		bind.pager.adapter = pagerAdapter

		bind.pager.isUserInputEnabled = false

		bind.pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
			override fun onPageSelected(position: Int) {
				super.onPageSelected(position)

				bind.stepProgress.progress = position + 1

				bind.step.text = buildString {
					append("Step ")
					append(position + 1)
					append(" of ${productTipList.size}")
				}

				if (position == productTipList.lastIndex) {
					bind.continueBtn.text = resources.getString(string._continue)
				} else {
					bind.continueBtn.text = resources.getString(string.continue_to_next_step)
				}

			}
		})

		bind.continueBtn.setHapticClickListener {
			if (bind.pager.currentItem == productTipList.size - 1) {
				when (type) {
					"showTips" -> {
						val a = activity as TutorialsActivity

						a.scheduleShowLauncher.launch(mCtx.toScheduleShow("showTutorial"))

						findNavController().popBackStack()
					}

					"liveTips" -> {
						findNavController().navigate(ids.goToLiveRehearsalFragment)
					}

					"goLive" -> {
						val data = viewModel.currentShowData
						log("SHOW DATA Before Start Shoe: $data")

						val products = viewModel.currentShowData?.products?.map { product -> product?.toLiveShowProduct() }
						products?.first()?.isCurrent = true

						val show = LiveShowModel(
							seller = LiveShowModel.Seller(
								id = userId,
								image = userImage,
								name = userName,
								rating = ""
							),
							products = products ?: mutableListOf(),
							roomId = "live_room_${userId}_${data?.id.toString()}",
							showDetail = "Test Details",
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
						)

						startActivity(mCtx.toSellerShow(data?.time, show))
						finish()
					}

					else -> {
						findNavController().navigate(ids.goToReferFriendFragment)
					}
				}

			} else {
				bind.pager.currentItem += 1
			}

		}

	}

}