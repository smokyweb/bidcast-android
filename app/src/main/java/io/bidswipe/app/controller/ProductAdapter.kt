package io.bidswipe.app.controller

import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.PopupMenu
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.ProductListItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.response.Product
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.ids
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.setHapticClickListener

class ProductAdapter(
	val mList: MutableList<Product>, val mClicks: RecyclerClicks
) : BaseAdapter<Product?, ProductListItemBinding>(mList) {

	override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
		ProductListItemBinding.inflate(inflater, parent, false)

	override fun onBind(
		holder: BaseViewHolder<ProductListItemBinding>,
		position: Int,
		item: Product?,
	) {
		with(holder) {

			bind.productName.text = item?.title?.asCapital()

			bind.prodSubTitle.text = item?.category?.name
			bind.quantity.text = buildString {
				append("Quantity: ")
				append(item?.quantity)
			}
			bind.img.loadUrl(mCtx, item?.images?.get(0) ?: "")

			val menu = PopupMenu(
				mCtx,
				bind.root.findViewById<AppCompatImageView>(R.id.moreMenu),
				Gravity.START
			)

			menu.menuInflater.inflate(R.menu.inventory_menu, menu.menu)

			menu.setOnMenuItemClickListener {
				when (it.itemId) {

					ids.delete -> {
						mClicks.itemClick(position, "delete")
					}

					else -> {

						mClicks.itemClick(position, "edit")

					}

				}
				return@setOnMenuItemClickListener true
			}

			bind.moreMenu.setHapticClickListener {
				menu.show()
			}

		}
	}

	override fun getItemCount(): Int {
		return mList.size
	}
}