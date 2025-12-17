package io.bidswipe.app.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.ViewPagerAdapter
import io.bidswipe.app.databinding.FragmentActivityBinding
import io.bidswipe.app.ui.more.NotificationActivity

class ActivityFragment : BaseFragment<DashViewModel, FragmentActivityBinding>() {
	override fun getModel(): Class<DashViewModel> = DashViewModel::class.java

	override fun getBind(inflater: LayoutInflater, view: ViewGroup?) = FragmentActivityBinding.inflate(inflater, view, false)

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		val adapter = ViewPagerAdapter(requireActivity(), "Activity")
		bind.pager.adapter = adapter
		bind.pager.isUserInputEnabled = false

		TabLayoutMediator(bind.tabLayout, bind.pager) { tab, position ->
			tab.text = when (position) {
				0 -> "Messages"
				1 -> "Bids"
				2 -> "Offers"
				3 -> "Purchases"
				4 -> "Saved Items"
				else -> ""
			}
		}.attach()

		bind.pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
			override fun onPageSelected(position: Int) {
				super.onPageSelected(position)
				val fragment = adapter.getFragment(position)
				if (fragment.isAdded) {
					when (fragment) {
						is MessagesFragment -> fragment.reloadData()
						is BidsFragment -> fragment.reloadData()
						is OfferFragment -> fragment.reloadData()
						is PurchasesFragment -> {
							fragment.reloadData()
						}

						is SavedItemsFragment -> fragment.reloadData()
					}
				}
			}
		})

		bind.header.onMorePrimaryClick {
			startActivity(Intent(mCtx, NotificationActivity::class.java).putExtra("slug", "notification"))
		}

		// Hide logo in header
		bind.header.hideLogo()

		// Initially hide filter chips
//		bind.chipScroll.visibility = View.GONE

	}

}