package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.ShareTargetItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.utils.draw
import io.bidswipe.app.utils.setHapticClickListener

data class ShareTarget(
    val iconResId: Int,
    val title: String,
    val type: String
)

class ShareTargetAdapter(
    mList: MutableList<ShareTarget>,
    val mClicks: RecyclerClicks
) : BaseAdapter<ShareTarget?, ShareTargetItemBinding>(mList) {

    override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
        ShareTargetItemBinding.inflate(inflater, parent, false)

    override fun onBind(
        holder: BaseViewHolder<ShareTargetItemBinding>,
        position: Int,
        item: ShareTarget?
    ) {
        with(holder) {
            bind.shareIcon.setImageResource(item?.iconResId?: draw.placeholder_square)
            bind.shareTitle.text = item?.title

            bind.root.setHapticClickListener {
                mClicks.itemClick(position, item?.type)
            }
        }
    }
}
