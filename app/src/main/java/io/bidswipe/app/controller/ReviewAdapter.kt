package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.ReviewItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks

class ReviewAdapter (mList: MutableList<String>, val mClicks: RecyclerClicks
) : BaseAdapter<String?, ReviewItemBinding>(mList) {

    override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
        ReviewItemBinding.inflate(inflater, parent, false)

    override fun onBind(
        holder: BaseViewHolder<ReviewItemBinding>,
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