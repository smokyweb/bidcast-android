package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.TipsItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks

class TipsAdapter(
    mList: MutableList<String>, val mClicks: RecyclerClicks,
) : BaseAdapter<String?, TipsItemBinding>(mList) {

    override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
        TipsItemBinding.inflate(inflater, parent, false)

    override fun onBind(
        holder: BaseViewHolder<TipsItemBinding>,
        position: Int,
        item: String?,
    ) {
        with(holder) {

            bind.root.setOnClickListener {
                mClicks.itemClick(position)
            }


        }
    }
}