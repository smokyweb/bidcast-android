package io.bidswipe.app.ui.dashboard.sellerProfile

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.tabs.TabLayoutMediator
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.ViewPagerAdapter
import io.bidswipe.app.databinding.FragmentSellerProfileBinding

class SellerProfileFragment : BaseFragment<SellerViewModel,FragmentSellerProfileBinding>() {
    override fun getModel(): Class<SellerViewModel> = SellerViewModel::class.java

    override fun getBind(inflater: LayoutInflater, view: ViewGroup?) = FragmentSellerProfileBinding.inflate(inflater,view,false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = ViewPagerAdapter(requireActivity(),"Shop")
        bind.pager.adapter = adapter

        TabLayoutMediator(bind.tabLayout, bind.pager) { tab, position ->
            tab.text = when (position) {
                0 -> "Shop"
                1 -> "Shows"
                2 -> "Reviews"
                3 -> "Clips"
                else -> ""
            }
        }.attach()

    }

}