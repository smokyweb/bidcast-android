package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.TitleItemBinding
import io.bidswipe.app.network.response.GetAllTipsResponse
import io.bidswipe.app.utils.loadUrl

class TitleAdapter(mList : MutableList<GetAllTipsResponse.Data.Tip?> ,
) : BaseAdapter<GetAllTipsResponse.Data.Tip? , TitleItemBinding>(mList) {

	override fun bindView(inflater : LayoutInflater , parent : ViewGroup) =
		TitleItemBinding.inflate(inflater , parent , false)

	override fun onBind(
		holder : BaseViewHolder<TitleItemBinding> ,
		position : Int ,
		item : GetAllTipsResponse.Data.Tip? ,
	) {
		with(holder) {

			bind.icon.loadUrl(mCtx , item?.icon ?:"")
			bind.icon.isVisible = true
			bind.subTitle.setHtmlFromString("${item?.description ?: ""}" , false)
			bind.title.text = item?.title ?: ""

		}
	}
}