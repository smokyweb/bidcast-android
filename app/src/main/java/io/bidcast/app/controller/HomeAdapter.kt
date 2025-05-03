package io.bidcast.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import io.bidcast.app.base.BaseAdapter
import io.bidcast.app.databinding.HomeItemBinding

class HomeAdapter (
    val mList: MutableList<String>,
) : BaseAdapter<String, HomeItemBinding>(mList) {

    override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
        HomeItemBinding.inflate(inflater, parent, false)

    override fun onBind(
        holder: BaseViewHolder<HomeItemBinding>,
        position: Int,
        item: String?
    ) {
        with(holder) {


        }
    }
}