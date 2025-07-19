package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.OfferPriceItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.OfferModel
import io.bidswipe.app.utils.asMoney

class MakeOfferAdapter(
	mList: MutableList<OfferModel>, val mClicks: RecyclerClicks,
) : BaseAdapter<OfferModel, OfferPriceItemBinding>(mList) {

    override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
        OfferPriceItemBinding.inflate(inflater, parent, false)

    override fun onBind(
		holder: BaseViewHolder<OfferPriceItemBinding>,
		position: Int,
		item: OfferModel?,
	) {
        with(holder) {

            bind.root.setOnClickListener {
                mClicks.itemClick(position)
            }

            if (item?.selected == true) {
                bind.root.strokeWidth = 2
                bind.root.strokeColor = ContextCompat.getColor(mCtx, R.color.primary)
            } else {
                bind.root.strokeWidth = 0
            }

            bind.amount.text = item?.amount?.asMoney()
            bind.discount.text = item?.percent
        }
    }
}