package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.FormatItemBinding
import io.bidswipe.app.databinding.SellSheetItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.FormatModel
import io.bidswipe.app.model.SellModel

class FormatAdapter (mList: MutableList<FormatModel>, val mClicks: RecyclerClicks
) : BaseAdapter<FormatModel, FormatItemBinding>(mList) {

    override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
        FormatItemBinding.inflate(inflater, parent, false)

    override fun onBind(
        holder: BaseViewHolder<FormatItemBinding>,
        position: Int,
        item: FormatModel?
    ) {
        with(holder) {

            bind.root.setOnClickListener {
                mClicks.itemClick(position)
            }

            bind.icon.setImageDrawable(ContextCompat.getDrawable(mCtx,item?.icon?: R.drawable.notification))
            bind.title.text=item?.title

            if (item?.selected == true){
                bind.root.strokeColor = ContextCompat.getColor(mCtx,R.color.secondary)
            }else{
                bind.root.strokeColor = ContextCompat.getColor(mCtx,R.color.outline)
            }

        }
    }
}