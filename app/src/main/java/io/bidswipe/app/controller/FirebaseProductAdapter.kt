package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.ProductListItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.LiveShowModel
import io.bidswipe.app.utils.asMoney
import io.bidswipe.app.utils.loadUrl

class FirebaseProductAdapter(
	val mList: MutableList<LiveShowModel.Product?>, val mClicks: RecyclerClicks,
) : BaseAdapter<LiveShowModel.Product?, ProductListItemBinding>(mList) {

	override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
		ProductListItemBinding.inflate(inflater, parent, false)

	override fun onBind(
		holder: BaseViewHolder<ProductListItemBinding>,
		position: Int,
		item: LiveShowModel.Product?,
	) {
		with(holder) {

			bind.root.setOnClickListener {
				mClicks.itemClick(position, "select")
			}

			bind.edit.setOnClickListener {
				mClicks.itemClick(position, "edit")
			}

			bind.trash.setOnClickListener {
				mClicks.itemClick(position, "delete")
			}

			if (item?.selected == true) {
				bind.root.strokeWidth = 2
				bind.root.strokeColor = ContextCompat.getColor(mCtx, R.color.primary)
			} else {
				bind.root.strokeWidth = 0
			}

			bind.prodSubTitle.text = buildString {
				append("Price: ")
				append(item?.price?.asMoney())
			}

			bind.productName.text = item?.name
			bind.quantity.isVisible = false

			bind.img.loadUrl(mCtx, item?.image ?:"")

		}
	}
}