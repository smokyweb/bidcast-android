package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.MessagesItemsBinding
import io.bidswipe.app.databinding.SellSheetItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.SellModel

class MessagesAdapter(mList: MutableList<String>, val mClicks: RecyclerClicks
) : BaseAdapter<String, MessagesItemsBinding>(mList) {

    override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
        MessagesItemsBinding.inflate(inflater, parent, false)

    override fun onBind(
        holder: BaseViewHolder<MessagesItemsBinding>,
        position: Int,
        item: String?
    ) {
        with(holder) {

            bind.root.setOnClickListener {
                mClicks.viewClick(position)
            }


        }
    }
}