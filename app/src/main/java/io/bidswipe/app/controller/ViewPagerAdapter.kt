package io.bidswipe.app.controller

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import io.bidswipe.app.ui.dashboard.BidsFragment
import io.bidswipe.app.ui.dashboard.MessagesFragment
import io.bidswipe.app.ui.dashboard.OfferFragment
import io.bidswipe.app.ui.dashboard.PurchasesFragment
import io.bidswipe.app.ui.dashboard.SavedItemsFragment
import io.bidswipe.app.ui.dashboard.sellerHub.OverAllFragment
import io.bidswipe.app.ui.dashboard.sellerHub.TransactionsFragment
import io.bidswipe.app.ui.dashboard.sellerHub.WalletViewFragment
import io.bidswipe.app.ui.dashboard.sellerProfile.ReviewListFragment
import io.bidswipe.app.ui.dashboard.sellerProfile.SellerShowFragment
import io.bidswipe.app.ui.dashboard.sellerProfile.ShopFragment

class ViewPagerAdapter(fragmentActivity: FragmentActivity, type: String) :
    FragmentStateAdapter(fragmentActivity) {
    private val fragments = if (type == "Activity") {
        listOf(
            MessagesFragment(),
            BidsFragment(),
            OfferFragment(),
            PurchasesFragment(),
            SavedItemsFragment()
        )
    } else if (type == "Analytics") {
        listOf(
            OverAllFragment(),
            BidsFragment(),
            OfferFragment(),
            PurchasesFragment()
        )
    } else if (type == "wallet") {

        listOf(
            WalletViewFragment(),
            TransactionsFragment()
        )
    } else {
        listOf(
            ShopFragment(),
            SellerShowFragment(),
            ReviewListFragment(),
            PurchasesFragment()
        )
    }

    override fun getItemCount(): Int = fragments.size


    override fun createFragment(position: Int): Fragment = fragments[position]


}