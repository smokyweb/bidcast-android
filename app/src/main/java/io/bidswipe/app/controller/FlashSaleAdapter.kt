package io.bidswipe.app.controller

// Basecamp #9933973683 return (2026-05-29): Flash Sales explore-section adapter.
// Renders a horizontal row of active flash-sale product cards on HomeFragment.
// Endpoint: POST api/v1/get-product with sale_type=flash_sale, status=active.
// Each card shows: product image, ⚡ badge, title, original price (struck-through
// when flash_sale_price is available from the response), and the sale price.
// Tap → ProductDetailsActivity for the product detail page.

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.ItemFlashSaleCardBinding
import io.bidswipe.app.network.response.Product
import io.bidswipe.app.utils.asMoney
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.setHapticClickListener

class FlashSaleAdapter(
    private val items: MutableList<Product?>,
    private val onItemClick: (Product) -> Unit,
) : BaseAdapter<Product?, ItemFlashSaleCardBinding>(items) {

    override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
        ItemFlashSaleCardBinding.inflate(inflater, parent, false)

    override fun onBind(
        holder: BaseViewHolder<ItemFlashSaleCardBinding>,
        position: Int,
        item: Product?,
    ) {
        if (item == null) return
        with(holder.bind) {
            root.setHapticClickListener { onItemClick(item) }

            productImage.loadUrl(mCtx, item.images?.firstOrNull() ?: "")
            productTitle.text = item.title?.asCapital() ?: ""

            val salePrice = item.flashSalePrice
            if (salePrice != null && salePrice > 0.0) {
                // Show original price (struck through) + flash sale price below
                originalPrice.isVisible = true
                originalPrice.paintFlags = originalPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                originalPrice.text = item.pricing?.toDoubleOrNull()?.let { "$%.2f".format(it) }
                    ?: item.pricing.orEmpty()
                flashPrice.text = "$%.2f".format(salePrice)
            } else {
                // No separate flash price returned — show pricing as the offer price
                originalPrice.isVisible = false
                flashPrice.text = item.pricing?.toDoubleOrNull()?.let { "$%.2f".format(it) }
                    ?: item.pricing?.asMoney() ?: "$0.00"
            }
        }
    }
}
