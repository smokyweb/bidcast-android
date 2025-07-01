package io.bidswipe.app.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.BidsItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.response.FetchBidResponse
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.asMoney
import io.bidswipe.app.utils.loadUrl

class BidsAdapter(mList: MutableList<FetchBidResponse.Data?>, val mClicks: RecyclerClicks
) : BaseAdapter<FetchBidResponse.Data?, BidsItemBinding>(mList) {

    override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
        BidsItemBinding.inflate(inflater, parent, false)

    override fun onBind(
        holder: BaseViewHolder<BidsItemBinding>,
        position: Int,
        item: FetchBidResponse.Data?
    ) {
        with(holder) {

            bind.userName.text = item?.user?.username
            bind.userImage.loadUrl(mCtx,item?.user?.profileImage.toString())
            bind.offerPrice.text = item?.product?.pricing.toString().asMoney()
            bind.productNmae.text = item?.product?.title
            bind.productImage.loadUrl(mCtx,item?.product?.images?.get(0).toString())
            bind.prodSubTitle.text = buildString {
                append("Current Bid: ")
                append(item?.bidPrice.toString().asMoney())
            }

            bind.root.setOnClickListener {
                mClicks.itemClick(position)
            }

            try {
                bind.subTitle.text = buildSpannedString {
                    append("Placed a Bid ")
                    bold { append("•") }
                    append(Utils.getTimeAgo(item?.createdAt ?: ""))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
    }
}