package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.PurchasesItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.response.GetProductsByStatusResponse
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.asMoney
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.setHapticClickListener

class SavedItemAdapter(
	mList : MutableList<GetProductsByStatusResponse.Data?> , val mClicks : RecyclerClicks ,
) : BaseAdapter<GetProductsByStatusResponse.Data , PurchasesItemBinding>(mList) {

	override fun bindView(inflater : LayoutInflater , parent : ViewGroup) =
		PurchasesItemBinding.inflate(inflater , parent , false)

	override fun onBind(
		holder : BaseViewHolder<PurchasesItemBinding> ,
		position : Int ,
		item : GetProductsByStatusResponse.Data? ,
	) {
		with(holder) {

            bind.root.setHapticClickListener {
				mClicks.itemClick(position)
			}

			bind.price.text = item?.product?.pricing.toString().asMoney()

			bind.productId.text = buildString {
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
			}

			bind.productImage.loadUrl(mCtx , item?.product?.images?.get(0).toString())

		}
	}
}