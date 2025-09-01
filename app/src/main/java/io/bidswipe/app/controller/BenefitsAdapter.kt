package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.BenifitsItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.SellModel
import io.bidswipe.app.network.response.GetPremierShopResponse
import io.bidswipe.app.utils.loadUrl

class BenefitsAdapter(
    mList: MutableList<GetPremierShopResponse.Data.Feature?>, val mClicks: RecyclerClicks,
) : BaseAdapter<GetPremierShopResponse.Data.Feature?, BenifitsItemBinding>(mList) {

    override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
        BenifitsItemBinding.inflate(inflater, parent, false)

    override fun onBind(
        holder: BaseViewHolder<BenifitsItemBinding>,
        position: Int,
        item: GetPremierShopResponse.Data.Feature?,
    ) {
        with(holder) {

            bind.title.text = item?.title ?: ""
            bind.description.text = item?.description ?: ""

            bind.image.loadUrl(mCtx,item?.icon?:"")

            bind.root.setOnClickListener {
                mClicks.itemClick(position)
            }

        }
    }
}