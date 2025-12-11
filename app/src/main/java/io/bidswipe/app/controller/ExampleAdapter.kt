package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.TitleItemBinding
import io.bidswipe.app.utils.Const

class ExampleAdapter(
	mList : MutableList<String?> ,
) : BaseAdapter<String , TitleItemBinding>(mList) {

	override fun bindView(inflater : LayoutInflater , parent : ViewGroup) =
		TitleItemBinding.inflate(inflater , parent , false)

	override fun onBind(
		holder : BaseViewHolder<TitleItemBinding> ,
		position : Int ,
		item : String? ,
	) {
		with(holder) {

			bind.icon.isVisible = false
			bind.title.setHtmlFromString("${Const.BULLET} $item" , false)

		}
	}
}