package io.bidcast.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import io.bidcast.app.base.BaseAdapter
import io.bidcast.app.databinding.BidsItemBinding
import io.bidcast.app.databinding.PurchasesItemBinding
import io.bidcast.app.interfaces.RecyclerClicks

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