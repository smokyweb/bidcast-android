package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.core.view.isVisible
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.PurchasesItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.response.GetProductsByStatusResponse
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.asMoney
import io.bidswipe.app.utils.clr
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.setHapticClickListener

class PurchasesAdapter(
	mList: MutableList<GetProductsByStatusResponse.Data?>, val mClicks: RecyclerClicks,
) : BaseAdapter<GetProductsByStatusResponse.Data, PurchasesItemBinding>(mList) {

	override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
		PurchasesItemBinding.inflate(inflater, parent, false)

	override fun onBind(
		holder: BaseViewHolder<PurchasesItemBinding>,
		position: Int,
		item: GetProductsByStatusResponse.Data?,
	) {
		with(holder) {

			bind.sellerLayout.setHapticClickListener {
				mClicks.itemClick(position, "profile")
			}

			bind.view.setHapticClickListener {
				mClicks.itemClick(position, "product")
			}

			bind.productImage.setHapticClickListener {
				mClicks.itemClick(position, "product")
			}

			val price = if (item?.productId != null) {
				(item.product?.pricing ?: "0")
			} else {
				(item?.productSetItemUnit?.price ?: 0.0).toString()
			}

			bind.price.text =
				buildSpannedString {
					append("Price: ")
					color(ContextCompat.getColor(mCtx, io.bidswipe.app.R.color.scrim)) {
						bold { append(price.asMoney()) }
					}
				}

			val productId = if (item?.productId != null) {
				(item.productId)
			} else {
				item?.productSetItemUnit?.id
			}

			val title = if (item?.productId != null) {
				item.product?.title
			} else {
				item?.productSet?.name
			}

			// Product title - bold
			bind.productId.text = buildString {
				append(title?.asCapital())
					append(" #")
					append(productId.toString())
			}

			// Date formatting
			bind.date.text = buildString {
				append("Purchased: ")
				append(
					Utils.getFormattedDateTime(
						Const.YYYY_MM_DD_HH_MM_SS,
						"MM/dd/yy",
						item?.createdAt.toString()
					)
				)
			}

			bind.sellerUsername.text =if(item?.productId!=null) (item.product?.user?.name ?: "").asCapital() else (item?.productSet?.seller?.name?:"").asCapital()
			bind.sellerUsername.setHapticClickListener {
				mClicks.itemClick(position, "seller")
			}

			// Status chip
			val status = item?.status?.asCapital() ?: "Completed"
			bind.status.text = status
			bind.status.isVisible = status.isNotEmpty()

			when (item?.status?.lowercase()) {
				"cancelled", "rejected" -> {
					bind.statusCard.setCardBackgroundColor(ContextCompat.getColor(mCtx, clr.errorContainer))
					bind.statusCard.strokeColor = ContextCompat.getColor(mCtx, clr.error)
					bind.status.setTextColor(ContextCompat.getColor(mCtx, clr.error))
				}

				"delivered" -> {
					bind.statusCard.setCardBackgroundColor(ContextCompat.getColor(mCtx, clr.successContainer))
					bind.statusCard.strokeColor = ContextCompat.getColor(mCtx, clr.success)
					bind.status.setTextColor(ContextCompat.getColor(mCtx, clr.success))
				}

				else -> {
					bind.statusCard.setCardBackgroundColor(ContextCompat.getColor(mCtx, clr.warningContainer))
					bind.statusCard.strokeColor = ContextCompat.getColor(mCtx, clr.warning)
					bind.status.setTextColor(ContextCompat.getColor(mCtx, clr.warning))
				}
			}

			if (item?.productId != null) {
				bind.imageCard.isVisible = true
				bind.productImage.loadUrl(mCtx, item.product?.images?.get(0) ?: "")
			} else {
				bind.imageCard.isVisible = false
			}

		}
	}

}