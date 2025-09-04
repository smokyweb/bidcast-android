package io.bidswipe.app.ui.dashboard.sellerProfile

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.ShopAdapter
import io.bidswipe.app.databinding.FragmentShopBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetMyInventoryResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.dashboard.product.ProductDetailsActivity
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.runSafe

class ShopFragment : BaseFragment<SellerViewModel , FragmentShopBinding>() {
	override fun getModel() : Class<SellerViewModel> = SellerViewModel::class.java

	override fun getBind(inflater : LayoutInflater , view : ViewGroup?) = FragmentShopBinding.inflate(inflater , view , false)
	private var productList = mutableListOf<GetMyInventoryResponse.Data?>()
	private lateinit var shopAdapter : ShopAdapter
	private var sellerId = ""
	private var page = 1
	private var isLoading = false

	private val mClick = object : RecyclerClicks {
		override fun itemClick(pos : Int , status : String?) {
			startActivity(Intent(mCtx , ProductDetailsActivity::class.java).putExtra("productId" , productList[pos]?.id.toString()))

		}
	}

	override fun onResume() {
		super.onResume()
		if (Utils.isOnline(mCtx)) {
			bind.noInternet.isVisible = false
			bind.loader.isVisible = true
			page = 1
			viewModel.getUserProducts(sellerId.request())
		} else {
			bind.recycler.isVisible = false
			bind.noData.isVisible = false
			bind.loader.isVisible = false
			bind.noInternet.isVisible = true
		}
	}

	@SuppressLint("NotifyDataSetChanged")
	override fun onViewCreated(view : View , savedInstanceState : Bundle?) {
		super.onViewCreated(view , savedInstanceState)

		sellerId = activity?.intent?.getStringExtra("userId") ?: ""
		repeat(5) {
			bind.chipGroup.addView(
				Utils.makeAChip(
					mCtx = mCtx ,
					text = "For You" ,
					selected = false
				)
			)
		}

		bind.chipGroup.setOnCheckedStateChangeListener { chipGroup , _ ->
			runSafe {
				val chipId = chipGroup.checkedChipId
				chipGroup.indexOfChild(chipGroup.findViewById(chipId))
			}
		}
		bind.noInternet.onClick {
			bind.loader.isVisible = true
			bind.noInternet.isVisible = false
			page = 1
			viewModel.getUserProducts(sellerId.request())

		}

		shopAdapter = ShopAdapter(productList , mClick)
		bind.recycler.adapter = shopAdapter
		viewModel.getUserProducts(sellerId.request())
		viewModel.getUserProductsRepo.observe(viewLifecycleOwner) {
			when (it) {
				is Resource.Success -> {
					bind.loader.isVisible = false
					bind.noInternet.isVisible = false
					bind.noData.isVisible = false

					val mData = it.value.data
					if (page == 1) {
						productList.clear()
					}
					if (mData != null) {
						productList.addAll(mData)
					}
					if (productList.isEmpty()) {
						bind.noData.isVisible = true
						bind.recycler.isVisible = false
					} else {
						bind.noData.isVisible = false
						bind.recycler.isVisible = true
					}
					/* productList.clear()
					 mData?.forEach {
						 productList.add(it)
					 }*/
					isLoading = page >= (it.value.totalPage ?: 0)
					shopAdapter.notifyDataSetChanged()

				}

				is Resource.Error -> {
					bind.loader.isVisible = false
					bind.noData.isVisible = false

					if (it.isNetworkError) {
						bind.noInternet.isVisible = true
						bind.recycler.isVisible = false
						bind.noData.isVisible = false
//                        errorToast(getString(R.string.no_internet))
					} else {
						it.parse(mCtx , TAG , object : AlertClicks {
							override fun primaryClick(dialog : AppBottomSheet) {
								dialog.dismiss()

							}

							override fun secondaryClick(dialog : AppBottomSheet) {
								dialog.dismiss()

							}
						})
					}
				}

				else -> {}

			}
		}

	}

}