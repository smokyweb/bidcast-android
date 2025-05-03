package io.bidcast.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import io.bidcast.app.base.BaseAdapter
import io.bidcast.app.base.BaseAdapter.BaseViewHolder
import io.bidcast.app.databinding.ExploreItemBinding
import io.bidcast.app.databinding.HomeItemBinding
import io.bidcast.app.interfaces.AlertClicks
import io.bidcast.app.interfaces.RecyclerClicks

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