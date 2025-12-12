package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.text.buildSpannedString
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.ShopItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.response.GetProductsResponse
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.asMoney
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.setHapticClickListener

class ShopAdapter(
	mList : MutableList<GetProductsResponse.Data?>, val mClicks : RecyclerClicks,
) : BaseAdapter<GetProductsResponse.Data? , ShopItemBinding>(mList) {

	override fun bindView(inflater : LayoutInflater , parent : ViewGroup) =
		ShopItemBinding.inflate(inflater , parent , false)

	override fun onBind(
		holder : BaseViewHolder<ShopItemBinding> ,
		position : Int ,
		item : GetProductsResponse.Data? ,
	) {
		with(holder) {
            bind.root.setHapticClickListener {
				mClicks.itemClick(position)
			}

			bind.productImage.loadUrl(mCtx , item?.image?.get(0).toString())

			bind.productName.text = item?.title.toString().asCapital()

			if (item?.condition != null) {
				bind.category.text = buildSpannedString {
					append(item.category.toString().asCapital() + " ")
					append(Const.BULLET)
					append( " "+item.condition)
				}
			} else {
				bind.category.text = item?.category.toString().asCapital()
			}

			bind.price.text = item?.price.toString().asMoney()

		}
	}
}