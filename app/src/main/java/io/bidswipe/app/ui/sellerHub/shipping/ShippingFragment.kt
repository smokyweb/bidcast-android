package io.bidswipe.app.ui.sellerHub.shipping

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.SellAdapter
import io.bidswipe.app.databinding.FragmentShippingBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.SellModel
import io.bidswipe.app.network.Resource
import io.bidswipe.app.ui.sellerHub.SellerHubViewModel
import io.bidswipe.app.utils.animatedNav
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.parse

class ShippingFragment : BaseFragment<SellerHubViewModel, FragmentShippingBinding>() {
	override fun getModel(): Class<SellerHubViewModel> = SellerHubViewModel::class.java

	override fun getBind(inflater: LayoutInflater, view: ViewGroup?) =
		FragmentShippingBinding.inflate(inflater, view, false)

	private var itemList = mutableListOf<SellModel>()

	private lateinit var adapter: SellAdapter

	var shippingStatus : Boolean? = false

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		bind.header.onBackClick {
			finish()
		}
		itemList.clear()
		itemList.addAll(
			listOf(
				SellModel(
					R.drawable.ic_shop,
					R.color.outline,
					"Free Pickup",
					"Local pickup settings"
				),
				SellModel(
					R.drawable.ic_shipping,
					R.color.outline,
					"Domestic Shipments",
					"National delivery options"
				),
				SellModel(
					R.drawable.ic_dollar,
					R.color.outline,
					"Shipping Costs",
					"Manage shipping rates"
				),
				SellModel(
					R.drawable.ic_setting,
					R.color.outline,
					"Shipping Profiles",
					"Custom shipping profiles"
				)
			)
		)

		adapter = SellAdapter(itemList, "shipping", object : RecyclerClicks {
			override fun itemClick(pos: Int, status: String?) {
				log("CLICK $pos")
				when (pos) {
					0 -> findNavController().animatedNav(R.id.toFreePickup, bundleOf("status" to shippingStatus))
					1 -> findNavController().animatedNav(R.id.toDomesticShipments)
					2 -> findNavController().animatedNav(R.id.toShippingCost)
					3 -> findNavController().animatedNav(R.id.toShippingProfiles)
					else -> findNavController().animatedNav(R.id.toFreePickup)
				}
			}
		})

		bind.recycler.adapter = adapter

		bind.loader.isVisible = true
		viewModel.settingsList()
		viewModel.settingsListRepo.observe(viewLifecycleOwner) {
			when (it) {
				is Resource.Success -> {
					bind.loader.isVisible = false
					viewModel.settingsListRepo.value = null

					val mData = it.value.data

					shippingStatus = mData?.freeShipping

					itemList[0].status = if (mData?.freeShipping == true) "ON" else "OFF"

					adapter.notifyItemChanged(0)

				}

				is Resource.Error -> {
					bind.loader.isVisible = false
					viewModel.settingsListRepo.value = null
					it.parse(mCtx, TAG)
				}

				else -> {}

			}
		}


	}
}