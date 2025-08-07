package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.SubCategoryItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.response.GetCategoryResponse
import io.bidswipe.app.utils.loadUrl

class SubCategoryAdapter(
    items: List<GetCategoryResponse.Data?>,
    val mClicks: RecyclerClicks
) : BaseAdapter<GetCategoryResponse.Data, SubCategoryItemBinding>(items) {


    override fun bindView(
        inflater: LayoutInflater,
        parent: ViewGroup) = SubCategoryItemBinding.inflate(inflater, parent, false)

    override fun onBind(
        holder: BaseViewHolder<SubCategoryItemBinding>,
        position: Int,
        item: GetCategoryResponse.Data?,
    ) {
        with(holder.bind){
            root.setOnClickListener {
            mClicks.itemClick(position, null)
        }

            title.text = item?.name
            categoryImage.loadUrl(mCtx,item?.image ?: "")

        }
    }
}