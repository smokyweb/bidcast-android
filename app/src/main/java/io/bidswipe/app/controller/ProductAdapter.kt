package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.ExploreItemBinding
import io.bidswipe.app.databinding.ProductListItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks

class ProductAdapter (  val mList: MutableList<String>,val mClicks: RecyclerClicks
) : BaseAdapter<String, ProductListItemBinding>(mList) {

    override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
        ProductListItemBinding.inflate(inflater, parent, false)

    override fun onBind(
        holder: BaseViewHolder<ProductListItemBinding>,
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