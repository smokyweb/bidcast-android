package io.bidswipe.app.ui.dashboard.sellerHub

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.OrdersAdapter
import io.bidswipe.app.controller.SellerOffersAdapter
import io.bidswipe.app.databinding.FragmentSellerOffersBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.utils.finish

class SellerOffersFragment : BaseFragment<SellerHubViewModel, FragmentSellerOffersBinding>() {
	override fun getModel(): Class<SellerHubViewModel> = SellerHubViewModel::class.java
	
	override fun getBind(inflater: LayoutInflater, view: ViewGroup?) = FragmentSellerOffersBinding.inflate(inflater, view, false)
	
	private var itemList = mutableListOf("", "", "", "", "")
	
	private lateinit var adapter: SellerOffersAdapter
	
	private val mClick = object : RecyclerClicks {
		override fun viewClick(pos: Int) {
		}
		
		override fun itemClick(pos: Int, status: String) {
		}
	}
	
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		
		bind.header.onBackClick {
			finish()
		}
		
		adapter = SellerOffersAdapter(itemList,mClick)
		
		bind.recycler.adapter = adapter
		
	}
}