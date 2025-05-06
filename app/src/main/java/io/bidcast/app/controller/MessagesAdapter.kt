package io.bidcast.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import io.bidcast.app.R
import io.bidcast.app.base.BaseAdapter
import io.bidcast.app.databinding.MessagesItemsBinding
import io.bidcast.app.databinding.SellSheetItemBinding
import io.bidcast.app.interfaces.RecyclerClicks
import io.bidcast.app.model.SellModel

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