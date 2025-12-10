package io.bidswipe.app.ui.sellerHub

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.tabs.TabLayoutMediator
import io.bidswipe.app.App
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.ViewPagerAdapter
import io.bidswipe.app.databinding.FragmentAnalyticsBinding
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.loadUrl

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
				else -> "Over All"
			}
		}.attach()

		bind.pager.isUserInputEnabled = false

		val userData = App.profileResponse.value

		bind.userName.text = userData?.name?.asCapital()

		bind.bio.text = userData?.bio

		bind.profileImage.loadUrl(mCtx,userData?.profileImage ?:"")

	}

}