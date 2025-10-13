package io.bidswipe.app.ui.sellerHub

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.OrdersAdapter
import io.bidswipe.app.databinding.FragmentMyOrdersBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetOrdersResponse
import io.bidswipe.app.ui.custom.AlertType
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request

class MyOrdersFragment : BaseFragment<SellerHubViewModel, FragmentMyOrdersBinding>() {
	override fun getModel(): Class<SellerHubViewModel> = SellerHubViewModel::class.java
	
	override fun getBind(inflater: LayoutInflater, view: ViewGroup?) =
		FragmentMyOrdersBinding.inflate(inflater, view, false)
	
	private var orderList = mutableListOf<GetOrdersResponse.Data?>()
	private lateinit var adapter: OrdersAdapter
	private var page = 1
	private var isLoading = false
	
	private val mClick = object : RecyclerClicks {
		override fun itemClick(pos: Int, status: String?) {
			findNavController().navigate(R.id.toOrderStatus, bundleOf("orderId" to orderList[pos]?.id.toString(), "from" to "my_orders"))
		}
	}
	
	@SuppressLint("NotifyDataSetChanged")
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		
		bind.header.onBackClick {
			finish()
		}
		bind.header.onMoreSecondaryClick {
			showDeleteConfirmationDialog()
		}
		
		adapter = OrdersAdapter(orderList, mClick)
		
		bind.recycler.adapter = adapter
		
		bind.loader.isVisible = true
		
		bind.swipeRefreshLayout.setOnRefreshListener {
			page = 1
			viewModel.getOrderListing(page = page.toString().request(),"".request())
		}

		bind.recycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
			override fun onScrolled(recyclerView : RecyclerView , dx : Int , dy : Int) {
				super.onScrolled(recyclerView , dx , dy)
				val layoutManager = bind.recycler.layoutManager as LinearLayoutManager
				val lastItemPosition = layoutManager.findLastVisibleItemPosition()
				if (lastItemPosition == (orderList.size - 1)) {
					if (! isLoading) {
						isLoading = true
						page ++
						bind.bottomLoader.isVisible = true
						viewModel.getOrderListing(page = page.toString().request(),"".request())
					}
				}
			}
		})

		bind.search.addTextChangedListener(object : TextWatcher {
			override fun beforeTextChanged(s : CharSequence? , start : Int , count : Int , after : Int) {}
			override fun onTextChanged(s : CharSequence? , start : Int , before : Int , count : Int) {}
			override fun afterTextChanged(s : Editable?) {
				if (! s.isNullOrEmpty()) {
					page = 1
					bind.loader.isVisible = true
					viewModel.getOrderListing(page.toString().request(),"".request(), s.toString().request())
				}
			}
		})
		
		bind.noInternet.onClick {
			bind.loader.isVisible = true
			bind.noInternet.isVisible = false
			viewModel.getOrderListing(page = page.toString().request(),"".request())
		}
		
		viewModel.getOrderListing(page = page.toString().request(),"".request())
		viewModel.getOrderListingRepo.observe(viewLifecycleOwner) {
			when (it) {
				is Resource.Success -> {
					bind.swipeRefreshLayout.isRefreshing = false
					bind.noInternet.isVisible = false
					bind.bottomLoader.isVisible = false
					bind.loader.isVisible = false
					
					val mData = it.value.data

					if (page == 1) {
						orderList.clear()
					}

					if (mData != null) {
						orderList.addAll(mData)
					}

					bind.newOrderCount.text = it.value.newOrderCount.toString()
					bind.processingOrderCount.text = it.value.processingOrderCount.toString()
					bind.completedOrderCount.text = it.value.completeOrderCount.toString()

					if (mData?.isNotEmpty() == true) {
						orderList.addAll(mData)
					}
					
					if (mData?.isEmpty() == true) {
						
						bind.noData.isVisible = true
						bind.recycler.isVisible = false
						
					} else {
						bind.noData.isVisible = false
						bind.recycler.isVisible = true
					}

					isLoading = page >= (it.value.totalPage ?: 0)
					
					adapter.notifyDataSetChanged()
					
				}
				
				is Resource.Error -> {
					bind.swipeRefreshLayout.isRefreshing = false
					bind.loader.isVisible = false
					bind.bottomLoader.isVisible = false


					if (it.isNetworkError) {
						bind.noInternet.isVisible = true
						bind.recycler.isVisible = false
						
					} else {
						it.parse(mCtx, TAG, object : AlertClicks {
							override fun primaryClick(dialog: AppBottomSheet) {
								dialog.dismiss()
							}
							
							override fun secondaryClick(dialog: AppBottomSheet) {
								dialog.dismiss()
								
							}
						})
					}
				}
				
				else -> {}
				
			}
		}
		
	}
	
	private fun showDeleteConfirmationDialog() {
		AppBottomSheet(
			mCtx,
			R.drawable.ic_delete,
			"Delete Order",
			"Are you sure you want to delete all orders?",
			primaryBtnText = "Delete",
			secondaryBtnText = "Cancel",
			canCancel = true,
			showSecondary = true,
			alertType = AlertType.ERROR,
			clicks = object : AlertClicks {
				override fun primaryClick(dialog: AppBottomSheet) {
					dialog.dismiss()
					bind.loader.isVisible = false
				}
				
				override fun secondaryClick(dialog: AppBottomSheet) {
					dialog.dismiss()
				}
				
			},
			
			).show()
	}
}