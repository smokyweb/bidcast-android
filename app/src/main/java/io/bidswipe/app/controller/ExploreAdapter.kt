package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.base.BaseAdapter.BaseViewHolder
import io.bidswipe.app.databinding.ExploreItemBinding
import io.bidswipe.app.databinding.HomeItemBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks

class ExploreAdapter(  val mList: MutableList<String>,val mClicks: RecyclerClicks
) : BaseAdapter<String, ExploreItemBinding>(mList) {

    override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
        ExploreItemBinding.inflate(inflater, parent, false)

    override fun onBind(
        holder: BaseViewHolder<ExploreItemBinding>,
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