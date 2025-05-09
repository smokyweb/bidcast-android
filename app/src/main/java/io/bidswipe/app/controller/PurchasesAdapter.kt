package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.BidsItemBinding
import io.bidswipe.app.databinding.PurchasesItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks

class PurchasesAdapter(mList: MutableList<String>, val mClicks: RecyclerClicks, val type : String
) : BaseAdapter<String, PurchasesItemBinding>(mList) {

    override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
        PurchasesItemBinding.inflate(inflater, parent, false)

    override fun onBind(
        holder: BaseViewHolder<PurchasesItemBinding>,
        position: Int,
        item: String?
    ) {
        with(holder) {

            bind.root.setOnClickListener {
                mClicks.viewClick(position)
            }

            if (type == "saved"){

                bind.prodSubTitle.text = "Seller: Jhon Smith"

            }


        }
    }
}