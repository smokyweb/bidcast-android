package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.HomeItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks

class HomeAdapter (val mList: MutableList<String>, val mClick: RecyclerClicks
) : BaseAdapter<String, HomeItemBinding>(mList) {

    override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
        HomeItemBinding.inflate(inflater, parent, false)

    override fun onBind(
        holder: BaseViewHolder<HomeItemBinding>,
        position: Int,
        item: String?
    ) {
        with(holder) {

            bind.userInfo.setOnClickListener{

                mClick.itemClick(position,"user")

            }


        }
    }
}