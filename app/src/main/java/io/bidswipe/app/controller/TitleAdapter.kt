package io.bidswipe.app.controller

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.base.BaseAdapter.BaseViewHolder
import io.bidswipe.app.databinding.SellSheetItemBinding
import io.bidswipe.app.databinding.TipsItemBinding
import io.bidswipe.app.databinding.TitleItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.response.GetAllTipsResponse
import io.bidswipe.app.utils.loadUrl

class TitleAdapter(mList: MutableList<GetAllTipsResponse.Data.Tip?>
) : BaseAdapter<GetAllTipsResponse.Data.Tip?, TitleItemBinding>(mList) {

    override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
        TitleItemBinding.inflate(inflater, parent, false)

    override fun onBind(
        holder: BaseViewHolder<TitleItemBinding>,
        position: Int,
        item: GetAllTipsResponse.Data.Tip?
    ) {
        with(holder) {
            Log.d(TAG, "onBind:${item?.icon} ")

            bind.icon.loadUrl(mCtx, item?.icon.toString())
            bind.title.setHtmlFromString(item?.description ?: "",false)

        }
    }
}