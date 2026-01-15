package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.ClipsItemBinding
import io.bidswipe.app.network.response.GetClipsResponse
import io.bidswipe.app.utils.setHapticClickListener

class ClipsAdapter(
	mList : List<GetClipsResponse.Data?>,
	private val onItemClick : (GetClipsResponse.Data? , Int) -> Unit = { _ , _ -> },
	private val onPlayClick : (GetClipsResponse.Data? , Int) -> Unit = { _ , _ -> },
) : BaseAdapter<GetClipsResponse.Data? , ClipsItemBinding>(mList) {

	override fun bindView(inflater : LayoutInflater , parent : ViewGroup) =
		ClipsItemBinding.inflate(inflater , parent , false)

	override fun onBind(
		holder : BaseViewHolder<ClipsItemBinding> ,
		position : Int ,
		item : GetClipsResponse.Data?? ,
	) {
		with(holder) {
            bind.root.setHapticClickListener {
				onItemClick(item , position)
			}
            bind.playButton.setHapticClickListener {
				onPlayClick(item , position)
			}
		}
	}

}