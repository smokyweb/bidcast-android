package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.ShareTargetItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.ChatModel
import io.bidswipe.app.utils.Prefs
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.dpToPx
import io.bidswipe.app.utils.draw
import io.bidswipe.app.utils.loadUrl

class ShareChatAdapter(
    mList: MutableList<ChatModel>,
    val mClicks: RecyclerClicks
) : BaseAdapter<ChatModel?, ShareTargetItemBinding>(mList) {

    override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
        ShareTargetItemBinding.inflate(inflater, parent, false)

    override fun onBind(
        holder: BaseViewHolder<ShareTargetItemBinding>,
        position: Int,
        item: ChatModel?
    ) {
        with(holder) {

            bind.root.setOnClickListener {
                mClicks.itemClick(position, "")
            }

            if (position == 0) {
                bind.shareIcon.setImageResource(draw.search)
                bind.shareTitle.text = "Search"
                val p = mCtx.resources.dpToPx(16)
                bind.shareIcon.setPadding(p, p, p, p)
            } else {
                if (item?.users?.senderId == Prefs(mCtx).getUserData()?.id.toString()) {
                    bind.shareTitle.text = item.users?.receiverName?.asCapital()
                    if (item.users?.receiverImage?.isEmpty() == false) bind.shareIcon.loadUrl(
                        mCtx,
                        item.users?.receiverImage ?: "",
                        draw.placeholder_user,
                        item.users?.receiverName
                    )
                } else {
                    bind.shareTitle.text = item?.users?.senderName?.asCapital()
                    if (item?.users?.receiverImage?.isEmpty() == false) bind.shareIcon.loadUrl(
                        mCtx,
                        item.users?.senderImage ?: "",
                        draw.placeholder_user,
                        item?.users?.senderName
                    )
                }
            }
        }
    }
}
