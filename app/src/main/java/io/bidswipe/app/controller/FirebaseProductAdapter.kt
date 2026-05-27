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
import io.bidswipe.app.network.response.Product
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.asMoney
import io.bidswipe.app.utils.dpToPx
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.setHapticClickListener

class FirebaseProductAdapter(
    val from: String = "",
    val mList: MutableList<Product?>, val mClicks: RecyclerClicks,
) : BaseAdapter<Product?, ProductSelectionItemBinding>(mList) {

    override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
        ProductSelectionItemBinding.inflate(inflater, parent, false)

    @SuppressLint("SetTextI18n")
    override fun onBind(
        holder: BaseViewHolder<ProductSelectionItemBinding>,
        position: Int,
        item: Product?,
    ) {
        with(holder) {

            bind.root.setHapticClickListener {
                when (from) {
                    "freebie" -> mClicks.itemClick(position, "freebie")
                    // Basecamp #9929871140 (2026-05-27): randomizer-slot picker
                    // shares the inventory row layout with the live-show flow,
                    // which includes prominent Start Auction + Pin/Set-next
                    // buttons. Trey reported users couldn’t figure out how to
                    // ADD a product to a slot — they saw the action buttons and
                    // assumed those were the only way to interact. Route any tap
                    // anywhere on the row to the “select” action when picking
                    // for a randomizer slot.
                    "randomizer_slot" -> mClicks.itemClick(position, "select")
                    else -> mClicks.itemClick(position, "select")
                }
            }

            bind.startAuction.setHapticClickListener {
                // Basecamp #9929871140 (2026-05-27): when picking for a
                // randomizer slot, the Start Auction button should NOT start
                // an auction — it should select the product for the slot. Avoid
                // confusing UI by routing the click to the select action.
                if (from == "randomizer_slot") mClicks.itemClick(position, "select")
                else mClicks.itemClick(position, "start_auction")
            }

            bind.setForNext.setHapticClickListener {
                // Same as above — redirect the Pin/Set-next button to select
                // when in the randomizer-slot picker.
                if (from == "randomizer_slot") mClicks.itemClick(position, "select")
                else mClicks.itemClick(position, "set_next")
            }

//            bind.root.alpha = if (item?.status == "sold") 0.5f else 1f

            bind.quantity.isVisible = item?.status == "inactive"
            // Basecamp #9929871140 (2026-05-27): hide the live-show action
            // buttons (Start Auction / Pin) when the row is rendered inside
            // the randomizer-slot picker. Users only need to select; the
            // explicit “Select” footer button on the picker is the canonical
            // confirm CTA.
            bind.buttonLayout.isVisible =
                item?.status != "inactive" && from != "freebie" && from != "randomizer_slot"

            bind.quantity.text = buildSpannedString {
                append("Status: ")
                if (item?.status == "inactive") {
                    bold {
                        color(Color.RED) {
                            append("Sold")
                        }
                    }
                } else {
                    bold { append("Sold") }
                }
            }

            if (from == "freebie" || from == "randomizer_slot") {
                // Basecamp #9929871140 (2026-05-27): mirror the freebie
                // selection-stroke pattern for randomizer-slot picking so
                // the user gets immediate visual feedback that their tap
                // registered. Pure stroke-on-card; no other styling change.
                bind.root.strokeColor = ContextCompat.getColor(mCtx, R.color.primary)
                bind.root.strokeWidth = if (item?.selected == true) mCtx.resources.dpToPx(2) else 0
            } else {
            if (item?.selected == true) {
                bind.pinCard.setCardBackgroundColor(ContextCompat.getColor(mCtx, R.color.primary))
                bind.setForNext.imageTintList = (ColorStateList.valueOf(ContextCompat.getColor(mCtx, R.color.surface)))
            } else {
                bind.pinCard.setCardBackgroundColor(ContextCompat.getColor(mCtx, R.color.outline))
                bind.setForNext.imageTintList = (ColorStateList.valueOf(ContextCompat.getColor(mCtx, R.color.onSurface)))
            }
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
