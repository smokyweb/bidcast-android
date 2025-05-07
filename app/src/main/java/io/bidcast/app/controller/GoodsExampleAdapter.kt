package io.bidcast.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import io.bidcast.app.base.BaseAdapter
import io.bidcast.app.databinding.BidsItemBinding
import io.bidcast.app.databinding.GoodsItemBinding
import io.bidcast.app.interfaces.RecyclerClicks

class GoodsExampleAdapter(mList: MutableList<String>
) : BaseAdapter<String, GoodsItemBinding>(mList) {

    override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
        GoodsItemBinding.inflate(inflater, parent, false)

    override fun onBind(
        holder: BaseViewHolder<GoodsItemBinding>,
        position: Int,
        item: String?
    ) {
        with(holder) {

            bind.root.setOnClickListener {

            }


        }
    }
}