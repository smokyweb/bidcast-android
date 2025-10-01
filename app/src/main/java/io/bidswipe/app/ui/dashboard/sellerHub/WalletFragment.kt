package io.bidswipe.app.ui.dashboard.sellerHub

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.google.android.material.tabs.TabLayoutMediator
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.PayoutAdapter
import io.bidswipe.app.controller.ViewPagerAdapter
import io.bidswipe.app.databinding.FragmentWalletBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.utils.finish

class WalletFragment : BaseFragment<SellerHubViewModel, FragmentWalletBinding>() {

	override fun getModel(): Class<SellerHubViewModel> = SellerHubViewModel::class.java

	override fun getBind(inflater: LayoutInflater, view: ViewGroup?) = FragmentWalletBinding.inflate(inflater, view, false)
	private var itemList = mutableListOf("", "", "", "", "")

	private lateinit var adapter: PayoutAdapter

	private var kycStatus = false

	private val mClick = object : RecyclerClicks {

		override fun itemClick(pos: Int, status: String?) {
		}
	}

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

		TabLayoutMediator(bind.tabs, bind.pager) { tab, position ->
			tab.text = when (position) {
				0 -> "Wallet"
				1 -> "Transactions"
				else -> ""
			}
		}.attach()

	}
}
