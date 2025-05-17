package io.bidswipe.app.controller

import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager2.adapter.FragmentStateAdapter
import io.bidswipe.app.ui.auth.LoginFragment
import io.bidswipe.app.ui.dashboard.BidsFragment
import io.bidswipe.app.ui.dashboard.MessagesFragment
import io.bidswipe.app.ui.dashboard.OfferFragment
import io.bidswipe.app.ui.dashboard.PurchasesFragment
import io.bidswipe.app.ui.dashboard.SavedItemsFragment
import io.bidswipe.app.ui.dashboard.sellerProfile.ShopFragment

class ViewPagerAdapter(fragmentActivity: FragmentActivity , type : String) : FragmentStateAdapter(fragmentActivity) {
    private val fragments = if (type == "Activity"){
        listOf(
            MessagesFragment(),
            BidsFragment(),
            OfferFragment(),
            PurchasesFragment(),
            SavedItemsFragment()
        )
    } else {
        listOf(
           ShopFragment(),
            BidsFragment(),
            OfferFragment(),
            PurchasesFragment()
        )
    }

    override fun getItemCount(): Int = fragments.size


    override fun createFragment(position: Int): Fragment = fragments[position]


}