package io.bidswipe.app.controller

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.text.buildSpannedString
import androidx.core.view.isVisible
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.InventoryItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.response.Product
import io.bidswipe.app.network.response.isLiveAuctionFormat
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.asMoney
import io.bidswipe.app.utils.ids
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.setHapticClickListener
import io.bidswipe.app.utils.setMargins

class InventoryAdapter(
	val mList: MutableList<Product?>,
	private val isSelectionMode: Boolean,
	val mClicks: RecyclerClicks,
	val from: String = ""
) : BaseAdapter<Product?, InventoryItemBinding>(mList) {

	override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
		InventoryItemBinding.inflate(inflater, parent, false)

	override fun onBind(
		holder: BaseViewHolder<InventoryItemBinding>,
		position: Int,
		item: Product?,
	) {
		with(holder) {
			if (from == "show_details") {
				bind.moreMenu.isVisible = false
				bind.root.cardElevation = 0F
				bind.root.setMargins(0, 0, 0, 0)
				bind.divider.isVisible = position != mList.lastIndex
			} else {
				bind.divider.isVisible = false
				bind.moreMenu.isVisible = true
			}

			bind.statusCard.isVisible = item?.status == "inactive"
			bind.stockCount.isVisible = item?.status != "inactive"

			bind.productName.text = item?.title?.asCapital()
			Log.d(TAG, "onBind: ${item?.productCondition}")
			bind.prodSubTitle.text = buildSpannedString {
				if (item?.productCondition != null) {
					append((item.productCondition.replace("_", " ")))
					append(" ")
					append(Const.BULLET)
					append(" ")
				}
				append(item?.category?.name ?: "")
			}

			if (item?.images?.isNotEmpty() == true) {
				bind.productImage.loadUrl(
					mCtx,
					item.images[0] ?: "",
					placeHolder = R.drawable.placeholder_rect
				)
			}

			// Basecamp #9954326658: per-row pricing-format badge (PWA parity).
			// Red "Live Auction" for auction products, blue "Buy Now" otherwise.
			if (item != null) {
				val isAuction = item.isLiveAuctionFormat()
				bind.formatBadgeText.text = if (isAuction) "Live Auction" else "Buy Now"
				if (isAuction) {
					bind.formatBadge.setCardBackgroundColor(
						ContextCompat.getColor(mCtx, R.color.errorContainer)
					)
					bind.formatBadgeText.setTextColor(
						ContextCompat.getColor(mCtx, R.color.error)
					)
				} else {
					bind.formatBadge.setCardBackgroundColor(
						ContextCompat.getColor(mCtx, R.color.primaryContainer)
					)
					bind.formatBadgeText.setTextColor(
						ContextCompat.getColor(mCtx, R.color.onPrimaryContainer)
					)
				}
				bind.formatBadge.isVisible = true
			} else {
				bind.formatBadge.isVisible = false
			}

			bind.price.text = buildSpannedString {
				append((item?.pricing ?: 0.0).toString().asMoney())
			}

			bind.stockCount.text = buildString {
				append("Stock: ")
				append(item?.quantity ?: 0)
			}

			if (isSelectionMode) {
				if (item?.selected == true) {
					bind.root.strokeWidth = 2
					bind.root.strokeColor = ContextCompat.getColor(mCtx, R.color.primary)
				} else {
					bind.root.strokeWidth = 0
				}

				bind.click.setHapticClickListener {
					mClicks.itemClick(position, "toggle")
				}
			} else {
				bind.root.strokeWidth = 0
				bind.click.setHapticClickListener {
					mClicks.itemClick(position)
				}
			}

			val wrapper = ContextThemeWrapper(mCtx, R.style.popupMenuStyle)
			val menu = PopupMenu(
				wrapper,
				bind.moreMenu
			)

			when (item?.status) {
				"inactive" -> menu.menu.add(0, ids.active, 0, "Activate")
				"active" -> menu.menu.add(0, ids.deActive, 0, "Deactivate")
			}

			menu.menuInflater.inflate(R.menu.inventory_menu, menu.menu)

			menu.setOnMenuItemClickListener {
				when (it.itemId) {
					ids.active -> {
						mClicks.itemClick(position, "active")
					}

					ids.deActive -> {
						mClicks.itemClick(position, "inactive")
					}

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

}