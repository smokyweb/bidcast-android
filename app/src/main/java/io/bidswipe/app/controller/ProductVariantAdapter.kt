package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.VariantItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks

class ProductVariantAdapter(
	val mList: MutableList<String>,
	val mClicks: RecyclerClicks,
) : BaseAdapter<String, VariantItemBinding>(mList) {

	override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
		VariantItemBinding.inflate(inflater, parent, false)

	override fun onBind(
		holder: BaseViewHolder<VariantItemBinding>,
		position: Int,
		item: String?,
	) {
		with(holder) {

			bind.title.text = item

			bind.root.setOnClickListener {

				mClicks.itemClick(position)

			}

		}
	}
}