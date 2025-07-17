package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.MessagesItemsBinding
import io.bidswipe.app.interfaces.RecyclerClicks

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
                mClicks.itemClick(position)
            }


        }
    }
}