package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.MessagesItemsBinding
import io.bidswipe.app.databinding.ShopItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks

class ShopAdapter(mList: MutableList<String>, val mClicks: RecyclerClicks
) : BaseAdapter<String, ShopItemBinding>(mList) {

    override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
        ShopItemBinding.inflate(inflater, parent, false)

    override fun onBind(
        holder: BaseViewHolder<ShopItemBinding>,
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