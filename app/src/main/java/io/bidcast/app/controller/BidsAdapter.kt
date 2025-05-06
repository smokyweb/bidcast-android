package io.bidcast.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import io.bidcast.app.base.BaseAdapter
import io.bidcast.app.databinding.BidsItemBinding
import io.bidcast.app.databinding.MessagesItemsBinding
import io.bidcast.app.interfaces.RecyclerClicks

class BidsAdapter(mList: MutableList<String>, val mClicks: RecyclerClicks
) : BaseAdapter<String, BidsItemBinding>(mList) {

    override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
        BidsItemBinding.inflate(inflater, parent, false)

    override fun onBind(
        holder: BaseViewHolder<BidsItemBinding>,
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