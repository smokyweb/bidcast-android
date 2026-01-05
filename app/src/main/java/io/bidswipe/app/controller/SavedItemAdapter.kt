package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.ProductSelectionItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.response.GetProductsByStatusResponse
import io.bidswipe.app.utils.asMoney
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.setHapticClickListener

class SavedItemAdapter(
	mList: MutableList<GetProductsByStatusResponse.Data?>, val mClicks: RecyclerClicks,
) : BaseAdapter<GetProductsByStatusResponse.Data, ProductSelectionItemBinding>(mList) {

	override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
		ProductSelectionItemBinding.inflate(inflater, parent, false)

	override fun onBind(
		holder: BaseViewHolder<ProductSelectionItemBinding>,
		position: Int,
		item: GetProductsByStatusResponse.Data?,
	) {
		with(holder) {

			bind.root.setHapticClickListener {
				mClicks.itemClick(position)
			}

			bind.price.text = item?.product?.pricing.toString().asMoney()

			bind.category.text = item?.product?.category?.name

			/*	bind.productId.text = buildString {
					append(item?.product?.title?.asCapital())
					append(" #")
					append(item?.product?.id.toString())
				}
				bind.sellerUsername.text = buildString {
					append("Seller: ")
					append(item?.product?.seller?.name?.asCapital())
				}

				bind.date.text = buildString {
					append("Date: ")
					append(
						Utils.getFormattedDateTime(
							Const.DD_MM_YYYY_HH_MM_SS ,
							"MM/dd/yyyy" ,
							item?.product?.createdAt.toString()
						)
					)
				}*/

			bind.productName.text = item?.product?.title ?: ""

			bind.img.loadUrl(mCtx, item?.product?.images?.get(0).toString())

			bind.buttonLayout.isVisible = false

		}
	}
}