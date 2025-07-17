package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.LiveCommentItemBinding
import io.bidswipe.app.databinding.TitleItemBinding
import io.bidswipe.app.model.CommentModel
import io.bidswipe.app.utils.loadUrl

class CommentAdapter(mList: MutableList<CommentModel?>
) : BaseAdapter<CommentModel, LiveCommentItemBinding>(mList) {

    override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
        LiveCommentItemBinding.inflate(inflater, parent, false)

    override fun onBind(
        holder: BaseViewHolder<LiveCommentItemBinding>,
        position: Int,
        item: CommentModel?
    ) {
        with(holder) {
            bind.userName.text = item?.userName
            bind.message.text = item?.message
            bind.userImage.loadUrl(mCtx,item?.userImage.toString())
        }
    }
}