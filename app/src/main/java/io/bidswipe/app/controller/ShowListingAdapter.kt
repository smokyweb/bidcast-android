package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.base.BaseAdapter.BaseViewHolder
import io.bidswipe.app.databinding.ShippingUpdateItemBinding
import io.bidswipe.app.databinding.ShowListingItemBinding

class ShowListingAdapter  (val mList: MutableList<String>
) : BaseAdapter<String, ShowListingItemBinding>(mList) {

    override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
        ShowListingItemBinding.inflate(inflater, parent, false)

    override fun onBind(
        holder: BaseViewHolder<ShowListingItemBinding>,
        position: Int,
        item: String?
    ) {
        with(holder) {



        }
    }
}