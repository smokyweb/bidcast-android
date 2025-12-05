package io.bidswipe.app.ui.sellerHub

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.bidswipe.app.App
import io.bidswipe.app.R
import io.bidswipe.app.controller.InventoryFilterAdapter
import io.bidswipe.app.controller.InventoryFilterModel
import io.bidswipe.app.databinding.FragmentInventoryFilterSheetBinding
import io.bidswipe.app.interfaces.RecyclerClicks

class InventoryFilterSheetFragment : BottomSheetDialogFragment() {

	private lateinit var bind : FragmentInventoryFilterSheetBinding

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		bind = FragmentInventoryFilterSheetBinding.inflate(inflater, container, false)
		return bind.root
	}


	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		Log.d("TAG", "onViewCreated:   bottom sheet")

		val conditionList = mutableListOf(
			InventoryFilterModel.InnerModel(null, "New"),
			InventoryFilterModel.InnerModel(null, "Like New"),
			InventoryFilterModel.InnerModel(null, "Gently Loved"),
			InventoryFilterModel.InnerModel(null, "Well Loved"),
			InventoryFilterModel.InnerModel(null, "Other"),
			InventoryFilterModel.InnerModel(null, "Trending"),
		)

		val sortList = mutableListOf(
			InventoryFilterModel.InnerModel(null, "Newest First"),
			InventoryFilterModel.InnerModel(null, "Oldest First"),
			InventoryFilterModel.InnerModel(null, "Price: Low to High"),
			InventoryFilterModel.InnerModel(null, "Price: High to Low"),
		)

		val filterList = mutableListOf(
			InventoryFilterModel(
				R.drawable.ic_more,
				"Category",
				"category",
				App.categoryList.map { InventoryFilterModel.InnerModel(it?.id, it?.name) }
					.toMutableList()
			),
			InventoryFilterModel(R.drawable.ic_more, "Condition", "condition", conditionList),
			InventoryFilterModel(R.drawable.ic_product_filter, "Sort By", "sort", sortList),
		)

		bind.recycler.adapter =
			InventoryFilterAdapter(filterList, object : RecyclerClicks {
				override fun itemClick(pos: Int, status: String?) {
					when (status) {
						"open" -> {
							if (filterList[pos].isOpened) {
								filterList[pos].isOpened = false
								bind.recycler.adapter?.notifyItemChanged(
									pos
								)
							} else {
								filterList.forEachIndexed { index, model ->
									model.isOpened = pos == index
									bind.recycler.adapter?.notifyItemChanged(
										index,
										model
									)
								}
							}
						}
					}
				}
			})

		bind.priceMoreIcon.setOnClickListener {
			bind.priceLayout.isExpanded = !bind.priceLayout.isExpanded
		}



	}

	override fun onStart() {
		super.onStart()

		val dialog = dialog as? BottomSheetDialog
		dialog?.let {
			// Access the BottomSheetBehavior from the dialog's behavior
			val behavior = it.behavior

			// Set the state to EXPANDED
			behavior.state = BottomSheetBehavior.STATE_EXPANDED

		}

	}



}