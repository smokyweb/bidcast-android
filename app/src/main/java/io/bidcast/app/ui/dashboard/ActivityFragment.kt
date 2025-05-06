package io.bidcast.app.ui.dashboard

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import io.bidcast.app.R
import io.bidcast.app.base.BaseFragment
import io.bidcast.app.controller.ViewPagerAdapter
import io.bidcast.app.databinding.FragmentActivityBinding
import io.bidcast.app.databinding.FragmentExploreBinding

class ActivityFragment : BaseFragment<DashViewModel, FragmentActivityBinding>() {
    override fun getModel(): Class<DashViewModel> = DashViewModel::class.java

    override fun getBind(inflater: LayoutInflater, view: ViewGroup?) =  FragmentActivityBinding.inflate(inflater,view,false)

//    private lateinit var viewPager: ViewPager2
//    private lateinit var tabLayout: TabLayout

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = ViewPagerAdapter(requireActivity())
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

    }
}