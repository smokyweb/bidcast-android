package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.BenifitsItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.response.AboutUsResponse
import io.bidswipe.app.utils.loadUrl

class FeaturedAdapter(
    mList: MutableList<AboutUsResponse.Data.Feature?>, val mClicks: RecyclerClicks,
) : BaseAdapter<AboutUsResponse.Data.Feature?, BenifitsItemBinding>(mList) {

    override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
        BenifitsItemBinding.inflate(inflater, parent, false)

    override fun onBind(
        holder: BaseViewHolder<BenifitsItemBinding>,
        position: Int,
        item: AboutUsResponse.Data.Feature?,
    ) {
        with(holder) {

            bind.title.text = item?.title ?: ""
            bind.description.text = item?.description ?: ""
            bind.image.loadUrl(mCtx, item?.icon.toString())

            bind.root.setOnClickListener {
                mClicks.itemClick(position)
            }

        }
    }
}