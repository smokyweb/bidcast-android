package io.bidswipe.app.ui.custom

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.use
import io.bidswipe.app.databinding.HeaderViewBinding
import io.bidswipe.app.utils.clr
import io.bidswipe.app.utils.draw
import io.bidswipe.app.utils.styleable

class Header @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val bind = HeaderViewBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        context.theme.obtainStyledAttributes(attrs, styleable.Header, 0, 0).use {

            setBackground(it.getColor(styleable.Header_backgroundColor, ContextCompat.getColor(context, clr.onPrimary)))

            setHeaderText(it.getString(styleable.Header_headerTitle) ?: "")

            // Set icons
            setBackIcon(it.getResourceId(styleable.Header_backIconDrawable, draw.ic_back))
            setPrimaryIcon(
                it.getResourceId(
                    styleable.Header_primaryIconDrawable,
                    draw.notification
                )
            )

            // Set visibility of icons
            showPrimaryIcon(it.getBoolean(styleable.Header_showPrimaryIcon, false))
         showBackButton(
                it.getBoolean(
                    styleable.Header_showBackButton,
                    true
                )
            )  // Adjust visibility of back button
        }
    }

    fun setBackground(@ColorRes color: Int) {
        bind.header.setBackgroundColor(color)
    }

    fun setHeaderText(title: String) {
        bind.title.text = title
    }

    fun setBackIcon(@DrawableRes id: Int) {
        bind.backIcon.setImageResource(id)
    }

    fun setPrimaryIcon(@DrawableRes id: Int) {
        bind.primaryIcon.setImageResource(id)
    }

    fun onBackClick(click: OnClickListener) {
        bind.backIcon.setOnClickListener(click)
        bind.back.setOnClickListener(click)
    }

    fun onMorePrimaryClick(click: OnClickListener) {
        bind.primaryIcon.setOnClickListener(click)
        bind.primary.setOnClickListener(click)
    }

    fun showPrimaryIcon(state: Boolean) {
        if (state) {
            bind.primaryIcon.isEnabled = true
            bind.primary.visibility = VISIBLE
            bind.primaryIcon.visibility = VISIBLE
        } else {
            bind.primaryIcon.isEnabled = false
            bind.primary.visibility = INVISIBLE
            bind.primaryIcon.visibility = INVISIBLE
        }
    }

    fun showBackButton(state: Boolean) {
        if (state) {
            bind.back.isEnabled = true
            bind.back.visibility = VISIBLE
            bind.backIcon.visibility = VISIBLE
        } else {
            bind.backIcon.isEnabled = false
            bind.back.visibility = GONE
            bind.backIcon.visibility = INVISIBLE
        }
    }

    fun background(color: Int) {
        bind.header.setBackgroundColor(ContextCompat.getColor(context, color))
    }

    fun setHeaderPadding(left:Int, top:Int, right: Int, bottom:Int){
        bind.root.setPadding(left, top, right, bottom)
    }
}
