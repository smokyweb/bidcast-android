package io.bidswipe.app.controller

import android.annotation.SuppressLint
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
import io.bidswipe.app.model.LiveShowModel
import io.bidswipe.app.network.response.GetProductsResponse
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.asMoney
import io.bidswipe.app.utils.dpToPx
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.setHapticClickListener

class FirebaseProductAdapter(
    val from: String = "",
    val mList: MutableList<GetProductsResponse.Data?>, val mClicks: RecyclerClicks,
) : BaseAdapter<GetProductsResponse.Data?, ProductSelectionItemBinding>(mList) {

    override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
        ProductSelectionItemBinding.inflate(inflater, parent, false)

    @SuppressLint("SetTextI18n")
    override fun onBind(
        holder: BaseViewHolder<ProductSelectionItemBinding>,
        position: Int,
        item: GetProductsResponse.Data?,
    ) {
        with(holder) {

            bind.root.setHapticClickListener {
                mClicks.itemClick(position, "select")
            }

            bind.root.alpha = if (item?.status == "sold") 0.5f else 1f

            bind.quantity.text = buildSpannedString {
                append("Status: ")
                if (item?.status == "sold") {
                    bold {
                        color(Color.RED) {
                            append(item.status?.asCapital())
                        }
                    }
                } else {
                    bold { append(item?.status?.asCapital()) }
                }
            }

            if (item?.selected == true) {
                bind.root.strokeWidth = mCtx.resources.dpToPx(4)
                bind.root.strokeColor = ContextCompat.getColor(mCtx, R.color.primary)
                bind.root.setCardBackgroundColor(
                    ContextCompat.getColor(
                        mCtx,
                        R.color.primaryContainer
                    )
                )
            } else {
                bind.root.strokeWidth = 0
                bind.root.strokeColor = ContextCompat.getColor(mCtx, R.color.background)
                bind.root.setCardBackgroundColor(ContextCompat.getColor(mCtx, R.color.background))
            }

            bind.productStatus.isVisible = item?.isCurrent == true

            bind.prodSubTitle.text = buildString {
                append(item?.pricing?.asMoney())
            }

            bind.productName.text = item?.title?.asCapital()

            bind.img.loadUrl(mCtx, item?.images?.get(0) ?: "")

            bind.category.isVisible = item?.category == null
            bind.category.text = buildString {
                append(item?.category?.name?.asCapital())
//                append(" • ")
//                append(item?.condition?.asCapital() ?:"N/A")
            }

            bind.price.text = buildString {
                append(item?.pricing?.asMoney())
            }

            bind.bid.text = "0 bids"
        }
    }
}
