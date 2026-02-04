package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.AvailableItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.request.SurpriseProductModel
import io.bidswipe.app.utils.setHapticClickListener

class AvailableItemAdapter(
    mList: MutableList<SurpriseProductModel>, val mClicks: RecyclerClicks,
) : BaseAdapter<SurpriseProductModel, AvailableItemBinding>(mList) {

    override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
        AvailableItemBinding.inflate(inflater, parent, false)

    override fun onBind(
        holder: BaseViewHolder<AvailableItemBinding>,
        position: Int,
        item: SurpriseProductModel?,
    ) {
        with(holder) {

            bind.root.setHapticClickListener {
                mClicks.itemClick(position)
            }

            bind.productName.text = item?.name
            bind.quantity.text = "Qty: "+(item?.quantity ?: 0).toString()

        }
    }
}