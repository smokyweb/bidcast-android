package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.view.isVisible
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.BidsItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks

class OffersAdapter(mList: MutableList<String>, val mClicks: RecyclerClicks
) : BaseAdapter<String, BidsItemBinding>(mList) {

    override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
        BidsItemBinding.inflate(inflater, parent, false)

    override fun onBind(
        holder: BaseViewHolder<BidsItemBinding>,
        position: Int,
        item: String?
    ) {
        with(holder) {

            bind.buttonLayout.isVisible = true

            bind.root.setOnClickListener {
                mClicks.viewClick(position)
            }

            bind.subTitle.text = buildSpannedString {
                append("Placed an Offer ")
                bold { append(".") }
                append(" 2h ago")
            }

            bind.prodSubTitle.text = buildSpannedString {
                append("Asking price : ")
                append("$60.00")
            }

        }
    }
}