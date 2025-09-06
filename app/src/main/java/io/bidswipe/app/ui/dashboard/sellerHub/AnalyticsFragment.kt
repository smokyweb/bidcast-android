package io.bidswipe.app.ui.dashboard.sellerHub

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.tabs.TabLayoutMediator
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.ViewPagerAdapter
import io.bidswipe.app.databinding.FragmentAnalyticsBinding
import io.bidswipe.app.utils.finish

class AnalyticsFragment : BaseFragment<SellerHubViewModel , FragmentAnalyticsBinding>() {
	override fun getModel() : Class<SellerHubViewModel> = SellerHubViewModel::class.java

	override fun getBind(
        inflater : LayoutInflater ,
        view : ViewGroup? ,
    ) = FragmentAnalyticsBinding.inflate(inflater , view , false)

	override fun onViewCreated(view : View , savedInstanceState : Bundle?) {
		super.onViewCreated(view , savedInstanceState)

		bind.header.onBackClick {
			finish()
		}

		val adapter = ViewPagerAdapter(requireActivity() , "Analytics")
		bind.pager.adapter = adapter

		TabLayoutMediator(bind.tabs , bind.pager) { tab , position ->
			tab.text = when (position) {
				0 -> "Over All"
				1 -> "Livestream"
				2 -> "Promote"
				3 -> "Trust"
				else -> ""
			}
		}.attach()

	}

}