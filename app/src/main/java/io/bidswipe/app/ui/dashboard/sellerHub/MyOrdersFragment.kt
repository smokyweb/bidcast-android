package io.bidswipe.app.ui.dashboard.sellerHub

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.InventoryAdapter
import io.bidswipe.app.controller.OrdersAdapter
import io.bidswipe.app.databinding.FragmentInventoryBinding
import io.bidswipe.app.databinding.FragmentMyOrdersBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.utils.finish

class MyOrdersFragment : BaseFragment<SellerHubViewModel, FragmentMyOrdersBinding>() {
	override fun getModel(): Class<SellerHubViewModel> = SellerHubViewModel::class.java
	
	override fun getBind(inflater: LayoutInflater, view: ViewGroup?) = FragmentMyOrdersBinding.inflate(inflater, view, false)
	
	private var itemList = mutableListOf("", "", "", "", "")
	
	private lateinit var adapter: OrdersAdapter
	
	private val mClick = object : RecyclerClicks {
		
		override fun itemClick(pos: Int, status: String?) {
		}
	}
	
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		
		bind.header.onBackClick {
			finish()
		}
		
		adapter = OrdersAdapter(itemList,mClick)
		
		bind.recycler.adapter = adapter
		
	}
}