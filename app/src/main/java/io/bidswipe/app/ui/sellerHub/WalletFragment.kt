package io.bidswipe.app.ui.sellerHub

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.google.android.material.tabs.TabLayoutMediator
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.ViewPagerAdapter
import io.bidswipe.app.databinding.FragmentWalletBinding
import io.bidswipe.app.utils.finish

class WalletFragment : BaseFragment<SellerHubViewModel, FragmentWalletBinding>() {

	override fun getModel(): Class<SellerHubViewModel> = SellerHubViewModel::class.java

	override fun getBind(inflater: LayoutInflater, view: ViewGroup?) = FragmentWalletBinding.inflate(inflater, view, false)

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		bind.header.onBackClick {
			finish()
		}

		bind.swipeRefreshLayout.setOnRefreshListener {
			bind.swipeRefreshLayout.isRefreshing = false
		}

		bind.noInternet.onClick {
			bind.noInternet.isVisible = false
		}

		val adapter = ViewPagerAdapter(requireActivity(), "wallet")
		bind.pager.adapter = adapter

		bind.pager.isUserInputEnabled = false

		TabLayoutMediator(bind.tabs, bind.pager) { tab, position ->
			tab.text = when (position) {
				0 -> "Wallet"
				1 -> "Transactions"
				else -> ""
			}
		}.attach()

	}
}
