package io.bidswipe.app.controller

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.SellSheetItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.response.GetAllTipsResponse
import io.bidswipe.app.utils.loadUrl

class ThumbnailTipsAdapter(
    mList: MutableList<GetAllTipsResponse.Data.Tip?>, val type: String, val mClicks: RecyclerClicks,
) : BaseAdapter<GetAllTipsResponse.Data.Tip?, SellSheetItemBinding>(mList) {

    override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
        SellSheetItemBinding.inflate(inflater, parent, false)

    override fun onBind(
        holder: BaseViewHolder<SellSheetItemBinding>,
        position: Int,
        item: GetAllTipsResponse.Data.Tip?,
    ) {
        with(holder) {

            bind.root.setOnClickListener {
                mClicks.itemClick(position)
            }

            if (type == "getStarted" || type == "tips") {
                bind.next.isVisible = false
            }

            if (type == "shipping") {
                bind.root.background.setTint(ContextCompat.getColor(mCtx, R.color.background))
            }

            Log.d(TAG, "onBind:${item?.icon} ")

            bind.icon.loadUrl(mCtx, item?.icon.toString())
            bind.iconCard.setCardBackgroundColor(Color.parseColor(item?.color))
            bind.subTitle.text = item?.description
            bind.title.text = item?.title

        }
    }
}