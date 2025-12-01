package io.bidswipe.app.ui.tutorials

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.SellAdapter
import io.bidswipe.app.databinding.FragmentGetStartedBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.SellModel
import io.bidswipe.app.ui.dashboard.DashViewModel
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.ids
import io.bidswipe.app.utils.setHapticClickListener

class GetStartedFragment : BaseFragment<DashViewModel, FragmentGetStartedBinding>() {
	override fun getModel(): Class<DashViewModel> = DashViewModel::class.java

	override fun getBind(inflater: LayoutInflater, view: ViewGroup?) = FragmentGetStartedBinding.inflate(inflater, view, false)

	private var exploreList = mutableListOf<SellModel>()

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		bind.header.onBackClick {
			finish()
		}

		exploreList.clear()
		exploreList.addAll(
			listOf(
				SellModel(R.drawable.ic_hand_shake, R.color.secondaryContainer, "Honor Purchases & Freebies", "Fulfill all orders promptly and honor your commitments"),
				SellModel(R.drawable.ic_block, R.color.tertiaryContainer, "Do Not Sell Counterfeits", "Only sell authentic and legitimate products"),
				SellModel(R.drawable.ic_checked_tag, R.color.successContainer, "Do Not Lie About Items", "Provide accurate descriptions and images"),
				SellModel(R.drawable.ic_vehicle, R.color.successContainer, "Ship Quickly & Safely", "Use appropriate packaging and ship within 3 days")
			)
		)

		val adapter = SellAdapter(mList = exploreList, "getStarted", object : RecyclerClicks {

			override fun itemClick(pos: Int, status: String?) {

			}

		})

		bind.recycler.adapter = adapter

		bind.continueBtn.setHapticClickListener {
			if (bind.checkBox.isChecked.not()) {

				Alerts.error(mCtx, "Please agree with guidelines")
			} else {
				findNavController().navigate(ids.goToPlayerFragment)
			}
		}


	}

}