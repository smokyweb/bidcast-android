package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.WeightItemBinding

class WeightAdapter(
    val mList: MutableList<String>,
) : BaseAdapter<String, WeightItemBinding>(mList) {

    override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
        WeightItemBinding.inflate(inflater, parent, false)

    override fun onBind(
        holder: BaseViewHolder<WeightItemBinding>,
        position: Int,
        item: String?,
    ) {
        with(holder) {


        }
    }
}