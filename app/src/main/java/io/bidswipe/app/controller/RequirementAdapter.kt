package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.base.BaseAdapter.BaseViewHolder
import io.bidswipe.app.databinding.RequirementItemBinding
import io.bidswipe.app.databinding.StatusItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks

class RequirementAdapter (mList: MutableList<String>, val mClicks: RecyclerClicks
) : BaseAdapter<String, RequirementItemBinding>(mList) {

    override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
        RequirementItemBinding.inflate(inflater, parent, false)

    override fun onBind(
        holder: BaseViewHolder<RequirementItemBinding>,
        position: Int,
        item: String?
    ) {
        with(holder) {


        }
    }
}