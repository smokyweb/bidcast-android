package io.bidswipe.app.controller

import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseAdapter
import io.bidswipe.app.databinding.DomesticShipmentItemBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.utils.setHapticClickListener

class DomesticShipmentAdapter(
    mList: MutableList<DomesticShipmentModel>, val mClicks: RecyclerClicks,
) : BaseAdapter<DomesticShipmentModel?, DomesticShipmentItemBinding>(mList) {

    override fun bindView(inflater: LayoutInflater, parent: ViewGroup) =
        DomesticShipmentItemBinding.inflate(inflater, parent, false)

    override fun onBind(
        holder: BaseViewHolder<DomesticShipmentItemBinding>,
        position: Int,
        item: DomesticShipmentModel?,
    ) {
        with(holder) {
            if (item?.icon != null) bind.image.setImageResource(item.icon) else bind.image.isVisible =
                false

            bind.title.text = item?.title
            if (!item?.link.isNullOrEmpty()) {
                bind.subTitle.movementMethod = LinkMovementMethod.getInstance()
                val linkText = "Learn More"
                val fullText = "${item.subtitle} Learn More"
                val spannableString = SpannableString(fullText)
                val clickableSpan = object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        mClicks.itemClick(position, "link")
                    }

                    override fun updateDrawState(ds: TextPaint) {
                        super.updateDrawState(ds)
                        ds.isUnderlineText = false
                        ds.color = itemView.context.getColor(R.color.primary)
                        ds.isFakeBoldText = true
                    }
                }
                spannableString.setSpan(
                    clickableSpan,
                    fullText.indexOf(linkText),
                    fullText.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                spannableString.setSpan(
                    StyleSpan(Typeface.BOLD),
                    fullText.indexOf(linkText),
                    fullText.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                bind.subTitle.text = spannableString
            } else {
                if (!item?.subtitle.isNullOrEmpty()) bind.subTitle.text =
                    item.subtitle else bind.subTitle.isVisible = false
            }

            bind.root.setHapticClickListener {
                mClicks.itemClick(position)
            }

            bind.check.setOnCheckedChangeListener { _, _ ->
                mClicks.itemClick(position)
            }

            bind.check.isChecked = item?.isSelected ?: false
        }
    }
}

data class DomesticShipmentModel(
    val icon: Int?,
    val title: String,
    val subtitle: String? = null,
    val link: String? = null,
    var isSelected: Boolean = false
)

