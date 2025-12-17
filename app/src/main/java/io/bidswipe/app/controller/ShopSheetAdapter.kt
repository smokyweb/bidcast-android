package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.text.buildSpannedString
import androidx.core.view.isVisible
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.ShopSheetItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.response.GetMyInventoryResponse
import io.bidswipe.app.network.response.GetProductsResponse
import io.bidswipe.app.network.response.Product
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.setHapticClickListener

class ShopSheetAdapter(
	mList : MutableList<Product?>, type : String
	, val mClicks : RecyclerClicks
) : BaseAdapter<Product? , ShopSheetItemBinding>(mList) {

	override fun bindView(inflater : LayoutInflater , parent : ViewGroup) =
		ShopSheetItemBinding.inflate(inflater , parent , false)

	override fun onBind(
		holder : BaseViewHolder<ShopSheetItemBinding> ,
		position : Int ,
		item : Product? ,
	) {
		with(holder) {

			bind.primary.isVisible = false
			bind.secondary.isVisible = false

			bind.img.loadUrl(mCtx , item?.images?.get(0).toString())

			bind.productName.text = item?.title?.asCapital()

			bind.price.text = buildSpannedString {
//				append(item?.description)
			}

			if (item?.selected == true) {
				bind.root.strokeWidth = 2
			} else {
				bind.root.strokeWidth = 0
			}

            bind.root.setHapticClickListener {
				mClicks.itemClick(position)
			}

//            bind.price.text = item?.pricing.toString().asMoney()

		}
	}
}