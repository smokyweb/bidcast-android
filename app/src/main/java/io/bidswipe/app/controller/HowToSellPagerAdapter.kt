package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.HowToSellItemBinding
import io.bidswipe.app.network.response.GetHowToSellResponse
import io.bidswipe.app.utils.loadUrl

class HowToSellPagerAdapter(mList : MutableList<GetHowToSellResponse.Data?>) :
	BaseAdapter<GetHowToSellResponse.Data? , HowToSellItemBinding>(mList) {

	override fun bindView(inflater : LayoutInflater , parent : ViewGroup) =
		HowToSellItemBinding.inflate(inflater , parent , false)

	override fun onBind(
		holder : BaseViewHolder<HowToSellItemBinding> ,
		position : Int ,
		item : GetHowToSellResponse.Data? ,
	) {
		with(holder) {

			bind.title.text = item?.title.toString()
			bind.description.setHtmlFromString(item?.description ?: "" , false)

			bind.img.loadUrl(mCtx , item?.image.toString())


		}
	}
}