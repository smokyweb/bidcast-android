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
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.asMoney
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

			bind.root.setHapticClickListener {
				mClicks.itemClick(position)
			}

			// Price formatting - bold, below title
			bind.price.text =
				buildSpannedString {
					append("Price: ")
					color(ContextCompat.getColor(mCtx, io.bidswipe.app.R.color.scrim)) {
						bold { append(item?.product?.pricing?.asMoney()) }
					}
				}

			// Product title - bold
			bind.productId.text = buildString {
				append(item?.product?.title?.asCapital())
				if (item?.product?.id != null) {
					append(" #")
					append(item.product.id.toString())
				}
			}

			// Date formatting
			bind.date.text = buildString {
				append("Purchased: ")
				append(
					Utils.getFormattedDateTime(
						"dd-MM-yyyy HH:mm:ss",
						"MM/dd/yy",
						item?.product?.createdAt.toString()
					)
				)
			}

			// Seller username with "From:" label
			bind.sellerUsername.text = (item?.product?.seller?.name ?: "").asCapital()
			bind.sellerUsername.setHapticClickListener {
				mClicks.itemClick(position, "seller")
			}

			// Status chip
			val status = item?.status?.asCapital() ?: "Completed"
			bind.status.text = status
			bind.status.isVisible = status.isNotEmpty()

			// Load product image
			bind.productImage.loadUrl(mCtx, item?.product?.images?.get(0).toString())

		}
	}
}