package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.ItemPromoteMetricBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.PromoteMetricModel
import io.bidswipe.app.utils.setHapticClickListener

class PromoteMetricAdapter(
    mList: MutableList<PromoteMetricModel>,
    val mClicks: RecyclerClicks,
) : BaseAdapter<PromoteMetricModel, ItemPromoteMetricBinding>(mList) {

    override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
        ItemPromoteMetricBinding.inflate(inflater, parent, false)

    override fun onBind(
        holder: BaseViewHolder<ItemPromoteMetricBinding>,
        position: Int,
        item: PromoteMetricModel?,
    ) {
        with(holder) {
            bind.title.text = item?.title ?: ""
            bind.value.text = item?.value ?: "N/A"
            bind.description.text = item?.description ?: ""

            bind.root.setHapticClickListener {
                mClicks.itemClick(position)
            }
        }
    }
}
