package io.bidcast.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import io.bidcast.app.R
import io.bidcast.app.base.BaseAdapter
import io.bidcast.app.base.BaseAdapter.BaseViewHolder
import io.bidcast.app.databinding.ExploreItemBinding
import io.bidcast.app.databinding.HomeItemBinding
import io.bidcast.app.databinding.SellSheetItemBinding
import io.bidcast.app.interfaces.AlertClicks
import io.bidcast.app.interfaces.RecyclerClicks
import io.bidcast.app.model.SellModel

class SellAdapter(mList: MutableList<SellModel>, val mClicks: RecyclerClicks
) : BaseAdapter<SellModel, SellSheetItemBinding>(mList) {

    override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
        SellSheetItemBinding.inflate(inflater, parent, false)

    override fun onBind(
        holder: BaseViewHolder<SellSheetItemBinding>,
        position: Int,
        item: SellModel?
    ) {
        with(holder) {

            bind.root.setOnClickListener {
                mClicks.viewClick(position)
            }

            bind.icon.setImageDrawable(ContextCompat.getDrawable(mCtx,item?.icon?: R.drawable.notification))
            bind.iconCard.setCardBackgroundColor(ContextCompat.getColor(mCtx,item?.color?:R.color.primaryContainer))
            bind.subTitle.text=item?.subtitle
            bind.title.text=item?.title

        }
    }
}