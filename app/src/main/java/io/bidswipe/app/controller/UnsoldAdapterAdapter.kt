package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.UnsoldItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.response.GetSurpriseProductsResponse
import io.bidswipe.app.utils.asMoney
import io.bidswipe.app.utils.draw
import io.bidswipe.app.utils.setHapticClickListener
import io.bidswipe.app.utils.value

class UnsoldAdapterAdapter(
    mList: MutableList<GetSurpriseProductsResponse.Data.Item.Unit?>, val mClicks: RecyclerClicks,
   val callback: (Int,String, String) -> Unit
) : BaseAdapter<GetSurpriseProductsResponse.Data.Item.Unit?, UnsoldItemBinding>(mList) {

    override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
        UnsoldItemBinding.inflate(inflater, parent, false)

    var isEditMode = false

    override fun onBind(
        holder: BaseViewHolder<UnsoldItemBinding>,
        position: Int,
        item: GetSurpriseProductsResponse.Data.Item.Unit?,
    ) {
        with(holder) {

            bind.root.setHapticClickListener {
                mClicks.itemClick(position)
            }

            bind.editCard.setHapticClickListener {
                if (isEditMode) {
                    isEditMode=false
                    bind.edit.setImageResource(draw.ic_edit)
                    bind.editLayout.isVisible = false
                   callback(position,bind.unitPrice.value(),bind.desc.value())
                } else {
                    isEditMode = true
                    bind.edit.setImageResource(draw.ic_check)
                    bind.editLayout.isVisible = true
                }
            }

            bind.pinCard.setHapticClickListener {
                mClicks.itemClick(position, "set_next")
            }

            bind.startAuction.setHapticClickListener {
                mClicks.itemClick(position, "start_auction")
            }

            bind.index.text = "#${item?.id}"
            bind.price.text = (item?.price ?: 0).toString().asMoney()
        }
    }

}