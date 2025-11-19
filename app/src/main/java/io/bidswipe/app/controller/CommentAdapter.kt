package io.bidswipe.app.controller

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.LiveCommentItemBinding
import io.bidswipe.app.model.LiveChatModel
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.loadUrl

class CommentAdapter(
	mList : MutableList<LiveChatModel?> ,
	private var sellerId:String?
) : BaseAdapter<LiveChatModel , LiveCommentItemBinding>(mList) {

	override fun bindView(inflater : LayoutInflater , parent : ViewGroup) =
		LiveCommentItemBinding.inflate(inflater , parent , false)

	override fun onBind(
		holder : BaseViewHolder<LiveCommentItemBinding> ,
		position : Int ,
		item : LiveChatModel? ,
	) {
		with(holder) {
			bind.userName.text = item?.userName?.asCapital()
			bind.message.text = item?.message
			bind.userImage.loadUrl(mCtx , item?.userImage.toString())
			Log.d(TAG, "onBind: $sellerId")
			bind.hostView.isVisible = sellerId.toString()==item?.userId
		}
	}
}