package io.bidswipe.app.controller

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.ContextThemeWrapper
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
			val wrapper = ContextThemeWrapper(mCtx, R.style.popupMenuStyle)
			val menu = PopupMenu(
				wrapper,
				bind.moreMenu
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

			bind.productName.text = item?.title?.asCapital()

			bind.prodSubTitle.text = item?.category?.name
			val stock = item?.quantity?.toIntOrNull() ?: 1
			bind.quantity.text = buildString {
				append("Quantity: ")
				append(item?.quantity)
			}

			// Basecamp #9991372302: show the stream-qty stepper only when stock > 1.
			// For single-unit products the quantity to sell is always 1 — no stepper needed.
			if (stock > 1) {
				bind.streamQtyLayout.visibility = View.VISIBLE
				// Initialise streamQuantity to full stock on first display (0 = unset).
				if ((item?.streamQuantity ?: 0) < 1) {
					item?.streamQuantity = stock
				}
				val current = item?.streamQuantity ?: stock
				bind.streamQtyValue.text = current.toString()
				bind.streamQtyMax.text = "of $stock"

				bind.streamQtyMinus.setHapticClickListener {
					val cur = item?.streamQuantity ?: stock
					if (cur > 1) {
						item?.streamQuantity = cur - 1
						bind.streamQtyValue.text = (cur - 1).toString()
					}
				}
				bind.streamQtyPlus.setHapticClickListener {
					val cur = item?.streamQuantity ?: stock
					if (cur < stock) {
						item?.streamQuantity = cur + 1
						bind.streamQtyValue.text = (cur + 1).toString()
					}
				}
			} else {
				bind.streamQtyLayout.visibility = View.GONE
				// Ensure streamQuantity is 1 for single-unit products.
				if (item != null) item.streamQuantity = 1
			}

			bind.img.loadUrl(mCtx, item?.images?.first() ?: "")

		}
	}

	override fun getItemCount(): Int {
		return mList.size
	}
}