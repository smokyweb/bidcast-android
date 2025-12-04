package io.bidswipe.app.controller

import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.DomesticShipmentItemBinding
import io.bidswipe.app.databinding.ShippingProfileItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.utils.setHapticClickListener

class ShippingProfileAdapter(
    mList: MutableList<String>, val mClicks: RecyclerClicks,
) : BaseAdapter<String?, ShippingProfileItemBinding>(mList) {

    override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
        ShippingProfileItemBinding.inflate(inflater, parent, false)

    override fun onBind(
        holder: BaseViewHolder<ShippingProfileItemBinding>,
        position: Int,
        item: String?,
    ) {
        with(holder) {

        }
    }
}


