package io.bidswipe.app.ui.custom

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.content.res.use
import androidx.core.view.isVisible
import io.bidswipe.app.databinding.HeaderViewBinding
import io.bidswipe.app.utils.draw
import io.bidswipe.app.utils.setHapticClickListener
import io.bidswipe.app.utils.styleable

class Header @JvmOverloads constructor(
	context : Context ,
	attrs : AttributeSet? = null ,
	defStyleAttr : Int = 0 ,
) : LinearLayout(context , attrs , defStyleAttr) {

	private val bind = HeaderViewBinding.inflate(LayoutInflater.from(context) , this , true)

	init {
		context.theme.obtainStyledAttributes(attrs , styleable.Header , 0 , 0).use {

			setHeaderText(it.getString(styleable.Header_headerTitle) ?: "")

			// Configure the header mode (back button, app text, icons, etc.)
			configureHeaderMode(it.getInt(styleable.Header_headerMode , 0))

			// Set icons
			setBackIcon(it.getResourceId(styleable.Header_backIconDrawable , draw.ic_back))
			setPrimaryIcon(it.getResourceId(styleable.Header_primaryIconDrawable , draw.ic_delete))
			setSecondaryIcon(
				it.getResourceId(
					styleable.Header_secondaryIconDrawable ,
					draw.ic_delete
				)
			)

			// Set visibility of icons
			showPrimaryIcon(it.getBoolean(styleable.Header_showPrimaryIcon , false))
			showSecondaryIcon(it.getBoolean(styleable.Header_showSecondaryIcon , false))
			showBackButton(
				it.getBoolean(
					styleable.Header_showBackButton ,
					true
				)
			)  // Adjust visibility of back button
		}
	}

	private fun configureHeaderMode(headerMode : Int) {
		when (headerMode) {
			0 -> { // BACK_BUTTON_WITH_TITLE_AND_ACTION_BUTTONS
				bind.back.isVisible = true
				bind.title.isVisible = true
				bind.appText.isVisible = false
				bind.secondaryIcon.isVisible = true
				bind.secondary.isVisible = true
				bind.primaryIcon.isVisible = true // Include primary icon
				bind.primary.isVisible = true   // Include primary button
			}

			1 -> { // APP_TEXT_WITH_ACTION_BUTTONS
				bind.back.isVisible = false
				bind.title.isVisible = true
				bind.appText.isVisible = true
				bind.secondaryIcon.isVisible = true
				bind.secondary.isVisible = true
				bind.primaryIcon.isVisible = true // Include primary icon
				bind.primary.isVisible = true   // Include primary button
			}

			2 -> { // APP_TEXT_WITH_BACK_AND_ACTION_BUTTONS
				bind.back.isVisible = true
				bind.appText.isVisible = true
				bind.secondaryIcon.isVisible = true
				bind.secondary.isVisible = true
				bind.primaryIcon.isVisible = true // Include primary icon
				bind.primary.isVisible = true   // Include primary button
			}
		}
	}

	fun setHeaderText(title : String) {
		bind.title.text = title
	}

	fun setBackIcon(@DrawableRes id : Int) {
		bind.backIcon.setImageResource(id)
	}

	fun setPrimaryIcon(@DrawableRes id : Int) {
		bind.primaryIcon.setImageResource(id)
	}

	fun setSecondaryIcon(@DrawableRes id : Int) {
		bind.secondaryIcon.setImageResource(id)
	}

	fun onBackClick(click : OnClickListener) {
		bind.backIcon.setOnClickListener(click)
		bind.back.setOnClickListener(click)
	}

	fun onMorePrimaryClick(click : OnClickListener) {
		bind.primaryIcon.setOnClickListener(click)
	}

	fun onMoreSecondaryClick(click : OnClickListener) {
		bind.secondaryIcon.setOnClickListener(click)
	}

	fun showSecondaryIcon(state : Boolean) {
		if (state) {
			bind.secondaryIcon.isEnabled = true
			bind.secondary.visibility = VISIBLE
			bind.secondaryIcon.visibility = VISIBLE
		} else {
			bind.secondaryIcon.isEnabled = false
			bind.secondary.visibility = GONE
			bind.secondaryIcon.visibility = GONE
		}
	}

	fun showPrimaryIcon(state : Boolean) {
		if (state) {
			bind.primaryIcon.isEnabled = true
			bind.primary.visibility = VISIBLE
			bind.primaryIcon.visibility = VISIBLE
		} else {
			bind.primaryIcon.isEnabled = false
			bind.primary.visibility = GONE
			bind.primaryIcon.visibility = GONE
		}
	}

	fun showBackButton(state : Boolean) {
		if (state) {
			bind.back.isEnabled = true
			bind.back.visibility = VISIBLE
			bind.backIcon.visibility = VISIBLE
		} else {
			bind.backIcon.isEnabled = false
			bind.back.visibility = GONE
			bind.backIcon.visibility = GONE
			bind.appText.setPadding(20 , 0 , 0 , 0)
		}
	}

	fun background(color : Int) {
		bind.header.setBackgroundColor(ContextCompat.getColor(context , color))
	}
}
