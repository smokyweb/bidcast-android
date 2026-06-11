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
import io.bidswipe.app.ui.dashboard.DashViewModel
import io.bidswipe.app.utils.ids
import io.bidswipe.app.utils.setHapticClickListener
import io.bidswipe.app.utils.string
import io.bidswipe.app.utils.toScheduleShow

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

					// Basecamp #9986425399: "goLive" is no longer navigated from
					// PrepareYourShowFragment — step 4 now goes to ShowDetailsActivity
					// so the seller can start the show explicitly from the overview.
					// Guard this branch: if somehow reached, just pop back rather than
					// launching AgoraPublisherActivity unexpectedly.
					"goLive" -> {
						findNavController().popBackStack()
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