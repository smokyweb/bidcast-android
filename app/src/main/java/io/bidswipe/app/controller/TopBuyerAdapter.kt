package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.ItemTopBuyerBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.TopBuyerModel
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.setHapticClickListener

class TopBuyerAdapter(
    mList: MutableList<TopBuyerModel>,
    val mClicks: RecyclerClicks,
) : BaseAdapter<TopBuyerModel, ItemTopBuyerBinding>(mList) {

    override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
        ItemTopBuyerBinding.inflate(inflater, parent, false)

    override fun onBind(
        holder: BaseViewHolder<ItemTopBuyerBinding>,
        position: Int,
        item: TopBuyerModel?,
    ) {
        with(holder) {
            bind.rank.text = item?.rank?.toString() ?: ""
            bind.buyerName.text = item?.buyerName ?: ""
            bind.value.text = item?.value ?: ""

            item?.profileImage?.let {
                bind.profileImage.loadUrl(mCtx, it)
            }

            bind.root.setHapticClickListener {
                mClicks.itemClick(position)
            }
        }
    }
}
