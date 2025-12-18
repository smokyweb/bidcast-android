package io.bidswipe.app.controller

import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.ShareTargetItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.utils.dpToPx
import io.bidswipe.app.utils.draw
import io.bidswipe.app.utils.setHapticClickListener

data class ShareTarget(
    val title: String,
    val icon: Drawable? = null,
    val resolveInfo: ResolveInfo? = null
)

class ShareTargetAdapter(
    mList: MutableList<ShareTarget>,
    val mClicks: RecyclerClicks
) : BaseAdapter<ShareTarget?, ShareTargetItemBinding>(mList) {

    override fun bindView(inflater: LayoutInflater, parent: ViewGroup) = ShareTargetItemBinding.inflate(inflater, parent, false)

    override fun onBind(
        holder: BaseViewHolder<ShareTargetItemBinding>,
        position: Int,
        item: ShareTarget?
    ) {
        with(holder) {

            bind.root.setHapticClickListener {
                mClicks.itemClick(position, "")
            }
            if(position==0){
                bind.shareIcon.setImageResource(draw.ic_link)
                bind.shareTitle.text="Copy"
                val p=mCtx.resources.dpToPx(16)
                bind.shareIcon.setPadding(p,p,p,p)
            }else {
                if (item?.icon == null) {
                    bind.shareIcon.setImageResource(draw.placeholder_square)
                } else {
                    bind.shareIcon.setImageDrawable(item.icon)
                }

                bind.shareTitle.text = item?.title?.replace(" ", "\n")

            }
        }
    }
}
