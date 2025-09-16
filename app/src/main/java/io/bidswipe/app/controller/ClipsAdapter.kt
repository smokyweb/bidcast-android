package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.ClipsItemBinding
import io.bidswipe.app.utils.setHapticClickListener

class ClipsAdapter(
	mList : List<String> ,
	private val onItemClick : (String , Int) -> Unit = { _ , _ -> } ,
	private val onPlayClick : (String , Int) -> Unit = { _ , _ -> } ,
) : BaseAdapter<String , ClipsItemBinding>(mList) {

	override fun bindView(inflater : LayoutInflater , parent : ViewGroup) =
		ClipsItemBinding.inflate(inflater , parent , false)

	override fun onBind(
		holder : BaseViewHolder<ClipsItemBinding> ,
		position : Int ,
		item : String? ,
	) {
		with(holder) {
			bind.root.setHapticClickListener {
				onItemClick("" , position)
			}
			bind.playButton.setHapticClickListener {
				onPlayClick("" , position)
			}
		}
	}

}