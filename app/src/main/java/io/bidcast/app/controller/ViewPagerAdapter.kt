package io.bidcast.app.controller

import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager2.adapter.FragmentStateAdapter
import io.bidcast.app.ui.auth.LoginFragment
import io.bidcast.app.ui.dashboard.BidsFragment
import io.bidcast.app.ui.dashboard.MessagesFragment
import io.bidcast.app.ui.dashboard.OfferFragment
import io.bidcast.app.ui.dashboard.PurchasesFragment
import io.bidcast.app.ui.dashboard.SavedItemsFragment

class ViewPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
    private val fragments = listOf(
        MessagesFragment(),
        BidsFragment(),
        OfferFragment(),
        PurchasesFragment(),
        SavedItemsFragment()
    )

    override fun getItemCount(): Int = fragments.size


    override fun createFragment(position: Int): Fragment = fragments[position]


}