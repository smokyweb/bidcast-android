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
) : BaseAdapter<GetSurpriseProductsResponse.Data?, ProductSelectionItemBinding>(mList) {

    override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
        ProductSelectionItemBinding.inflate(inflater, parent, false)

    @SuppressLint("SetTextI18n")
    override fun onBind(
        holder: BaseViewHolder<ProductSelectionItemBinding>,
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

//            bind.root.alpha = if (item?.status == "sold") 0.5f else 1f

//            bind.quantity.isVisible = item?.status == "inactive"
//            bind.buttonLayout.isVisible = item?.status != "inactive" && from != "freebie"

            bind.quantity.text = "${item?.items?.sumOf { it?.quantity?:0 } }Items"

//            if (from == "freebie") {
//                bind.root.strokeColor = ContextCompat.getColor(mCtx, R.color.primary)
//                bind.root.strokeWidth = if (item?.selected == true) mCtx.resources.dpToPx(2) else 0
//            } else {
//            if (item?.selected == true) {
//                bind.pinCard.setCardBackgroundColor(ContextCompat.getColor(mCtx, R.color.primary))
//                bind.setForNext.imageTintList = (ColorStateList.valueOf(ContextCompat.getColor(mCtx, R.color.surface)))
//            } else {
//                bind.pinCard.setCardBackgroundColor(ContextCompat.getColor(mCtx, R.color.outline))
//                bind.setForNext.imageTintList = (ColorStateList.valueOf(ContextCompat.getColor(mCtx, R.color.onSurface)))
//            }
//        }

//            bind.productStatus.isVisible = item?.isCurrent == true

            bind.prodSubTitle.text = buildString {
                append(item?.description)
            }

            bind.productName.text = item?.name?.asCapital()

            bind.img.isVisible=false

            bind.category.isVisible =false

            bind.price.text = buildString {
                append(item?.price?.toString()?.asMoney())
            }

            bind.bid.isVisible=false
        }
    }
}
