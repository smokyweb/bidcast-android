package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.net.toUri
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.base.BaseAdapter.BaseViewHolder
import io.bidswipe.app.databinding.PaymentCardItemBinding
import io.bidswipe.app.databinding.UploadImageItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks

class PaymentCardAdapter(mList: MutableList<String>, val mClicks: RecyclerClicks
) : BaseAdapter<String?, PaymentCardItemBinding>(mList) {

    override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
        PaymentCardItemBinding.inflate(inflater, parent, false)

    override fun onBind(
        holder: BaseViewHolder<PaymentCardItemBinding>,
        position: Int,
        item: String?
    ) {
        with(holder) {

            bind.root.setOnClickListener {
                mClicks.itemClick(position)
            }



        }
    }
}