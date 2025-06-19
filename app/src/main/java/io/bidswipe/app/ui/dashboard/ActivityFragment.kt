package io.bidswipe.app.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.tabs.TabLayoutMediator
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.ViewPagerAdapter
import io.bidswipe.app.databinding.FragmentActivityBinding
import io.bidswipe.app.ui.dashboard.more.NotificationActivity

class ActivityFragment : BaseFragment<DashViewModel, FragmentActivityBinding>() {
    override fun getModel(): Class<DashViewModel> = DashViewModel::class.java

    override fun getBind(inflater: LayoutInflater, view: ViewGroup?) =  FragmentActivityBinding.inflate(inflater,view,false)

//    private lateinit var viewPager: ViewPager2
//    private lateinit var tabLayout: TabLayout

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = ViewPagerAdapter(requireActivity(),"Activity")
        bind.pager.adapter = adapter

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

        bind.header.onMorePrimaryClick {
            startActivity(Intent(mCtx , NotificationActivity::class.java).putExtra("slug","notification"))
        }

    }
}