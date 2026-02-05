package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.BenifitsItemBinding
import io.bidswipe.app.databinding.UnsoldItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.request.SurpriseProductModel
import io.bidswipe.app.network.response.GetPremierShopResponse
import io.bidswipe.app.network.response.GetSurpriseProductsResponse
import io.bidswipe.app.utils.asMoney
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.setHapticClickListener

class UnsoldAdapterAdapter(
	mList : MutableList<GetSurpriseProductsResponse.Data.Item.Unit?>, val mClicks : RecyclerClicks,
) : BaseAdapter<GetSurpriseProductsResponse.Data.Item.Unit? , UnsoldItemBinding>(mList) {

	override fun bindView(inflater : LayoutInflater , parent : ViewGroup) =
		UnsoldItemBinding.inflate(inflater , parent , false)

	override fun onBind(
		holder : BaseViewHolder<UnsoldItemBinding> ,
		position : Int ,
		item :GetSurpriseProductsResponse.Data.Item.Unit? ,
	) {
		with(holder) {

            bind.root.setHapticClickListener {
				mClicks.itemClick(position)
			}

			bind.index.text="#${item?.id}"
			bind.price.text=(item?.price?:0).toString().asMoney()
		}
	}

}