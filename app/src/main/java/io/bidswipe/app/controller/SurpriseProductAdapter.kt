package io.bidswipe.app.controller

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.core.view.isVisible
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.ProductSelectionItemBinding
import io.bidswipe.app.databinding.SurpriseSetItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.response.GetSurpriseProductsResponse
import io.bidswipe.app.network.response.Product
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.asMoney
import io.bidswipe.app.utils.dpToPx
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.setHapticClickListener

class SurpriseProductAdapter(
    val from: String = "",
    val mList: MutableList<GetSurpriseProductsResponse.Data?>, val mClicks: RecyclerClicks,
) : BaseAdapter<GetSurpriseProductsResponse.Data?, SurpriseSetItemBinding>(mList) {

    override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
        SurpriseSetItemBinding.inflate(inflater, parent, false)

    @SuppressLint("SetTextI18n")
    override fun onBind(
        holder: BaseViewHolder<SurpriseSetItemBinding>,
        position: Int,
        item: GetSurpriseProductsResponse.Data?,
    ) {
        with(holder) {

            bind.root.setHapticClickListener {
                if (from == "freebie") {
                    mClicks.itemClick(position, "freebie")
                } else {
                    mClicks.itemClick(position, "select")
                }
            }

            bind.startAuction.setHapticClickListener {
                mClicks.itemClick(position, "start_auction")
            }

            bind.setForNext.setHapticClickListener {
                mClicks.itemClick(position, "set_next")
            }

            bind.buttonLayout.isVisible =  from != "freebie"

            bind.prodSubTitle.text = buildString {
                append(item?.description)
            }

            bind.productName.text = item?.name?.asCapital()

            bind.category.isVisible =false

            bind.price.text = buildString {
                append(item?.price?.toString()?.asMoney())
            }
        }
    }
}
