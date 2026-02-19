package io.bidswipe.app.ui.custom

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import io.bidswipe.app.R
import io.bidswipe.app.databinding.InfoCardViewBinding

class InfoCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding = InfoCardViewBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        context.theme.obtainStyledAttributes(attrs, R.styleable.InfoCard, 0, 0).use {

            // Icon
            val iconRes = it.getResourceId(R.styleable.InfoCard_infoIcon, R.drawable.ic_info)
            binding.icon.setImageResource(iconRes)

            // Text
            binding.tipText.text = it.getString(R.styleable.InfoCard_infoText) ?: ""

            // Background color
            val bgColor = it.getColor(
                    R.styleable.InfoCard_infoBackgroundColor,
                    ContextCompat.getColor(context, R.color.primaryContainer)
                )
            binding.rootLayout.setCardBackgroundColor( bgColor)

            // Text color
            val textColor =
                it.getColor(
                    R.styleable.InfoCard_infoTextColor,
                    ContextCompat.getColor(context, R.color.onSurface)
                )
            binding.tipText.setTextColor(textColor)

            // Icon tint
            val iconTint =
                it.getColor(
                    R.styleable.InfoCard_infoIconTint,
                    ContextCompat.getColor(context, R.color.primary)
                )
            binding.icon.setColorFilter(iconTint)
        }
    }


    fun setText(text: String) {
        binding.tipText.text = text
    }

    fun setIcon(resId: Int) {
        binding.icon.setImageResource(resId)
    }

    fun setTextColor(color: Int) {
        binding.tipText.setTextColor(color)
    }

    fun setIconTint(color: Int) {
        binding.icon.setColorFilter(color)
    }

    fun setCardBackground(color: Int) {
        binding.rootLayout.setBackgroundColor(color)
    }
}