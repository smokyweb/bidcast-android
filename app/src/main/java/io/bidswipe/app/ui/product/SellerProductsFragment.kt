package io.bidswipe.app.ui.product

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.get
import androidx.core.view.isVisible
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.ShopSheetAdapter
import io.bidswipe.app.databinding.FragmentSellerProductsBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetMyInventoryResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.runSafe
import io.bidswipe.app.utils.setHapticClickListener

class SellerProductsFragment : BaseFragment< ProductViewModel, FragmentSellerProductsBinding>() {
	override fun getModel(): Class<ProductViewModel> = ProductViewModel::class.java
	override fun getBind(
		inflater: LayoutInflater,
		view: ViewGroup?
	): FragmentSellerProductsBinding  = FragmentSellerProductsBinding.inflate(inflater, view, false)

	private lateinit var shopSheetAdapter : ShopSheetAdapter

	private var productList = mutableListOf<GetMyInventoryResponse.Data?>()

	private var sellerId = ""

	private val mClick = object : RecyclerClicks{
		override fun itemClick(pos: Int, status: String?) {
		}
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		sellerId = requireActivity().intent.getStringExtra("sellerId") ?:""

		setUpChips()

		shopSheetAdapter = ShopSheetAdapter(mList = productList, type = "shop", mClick)

		bind.recycler.adapter = shopSheetAdapter

		bind.close.setHapticClickListener {
			finish()
		}

		bind.loader.isVisible = true

		viewModel.getUserProducts(sellerId.request())

		viewModel.getUserProductsRepo.observe(viewLifecycleOwner){ it ->

			when(it){
				is Resource.Success -> {
					bind.loader.isVisible = false
					val mData = it.value.data

					if (mData!=null){

						productList.clear()
						productList.addAll(mData)

						shopSheetAdapter.notifyDataSetChanged()

					}

				}

				is Resource.Error -> {
					bind.loader.isVisible = false

					it.parse(mCtx, TAG, object : AlertClicks {
						override fun primaryClick(dialog: AppBottomSheet) {
							dialog.dismiss()

						}

						override fun secondaryClick(dialog: AppBottomSheet) {
							dialog.dismiss()
						}
					})

				}

				else ->{}
			}

		}

	}

	private fun setUpChips() {
		bind.search.setText("")
		bind.chipGroup.removeAllViews()

		bind.chipGroup.addView(
			Utils.makeAChip(
				mCtx = mCtx,
				text = "",
				selected = false,
				closeIconVisible = false,
				chipPadding = 12,
				iconRes = R.drawable.ic_product_filter
			)
		)

		bind.chipGroup.addView(
			Utils.makeAChip(
				mCtx = mCtx,
				text = "Sort",
				selected = false,
				closeIconVisible = false,
				chipPadding = 12)
		)

		listOf("Auction", "Buy Now", "Giveaway", "Sold").forEach {
			bind.chipGroup.addView(
				Utils.makeAChip(
					mCtx = mCtx,
					text = it,
					selected = false,
					closeIconVisible = false,
					chipPadding = 12,
				)
			)
		}

		bind.chipGroup.setOnCheckedStateChangeListener { chipGroup, _ ->
			runSafe {
				val chipId = chipGroup.checkedChipId
				val index = chipGroup.indexOfChild(chipGroup.findViewById(chipId))
				/*bind.loader.isVisible = true

				when (index) {
					0 -> {
						selectedTabText = "recommended"
						viewModel.getCategory(type = "recommended", getCount = "true")
					}

					1 -> {
						selectedTabText = "popular"
						viewModel.getCategory(type = "popular", getCount = "true")
					}

					2 -> {
						selectedTabText = "all"
						viewModel.getCategory(type = "all", getCount = "true")
					}

				}*/
			}
		}

	}



}