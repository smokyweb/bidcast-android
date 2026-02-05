package io.bidswipe.app.ui.custom

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.use
import androidx.core.view.isVisible
import io.bidswipe.app.utils.clr
import io.bidswipe.app.utils.dpToPx
import io.bidswipe.app.utils.draw
import io.bidswipe.app.utils.style
import io.bidswipe.app.utils.styleable

class Heading @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

	lateinit var title: TextView
	lateinit var endIcon: ImageButton

	init {
		orientation = VERTICAL

		context.theme.obtainStyledAttributes(attrs, styleable.Heading, 0, 0).use {

			// Row: title + close
			val headerRow = LinearLayout(context).apply {
				orientation = HORIZONTAL
				layoutParams = LayoutParams(
					LayoutParams.MATCH_PARENT,
					LayoutParams.WRAP_CONTENT
				)
				gravity = Gravity.CENTER_VERTICAL
			}

			title = TextView(context).apply {
				text = it.getString(styleable.Heading_title)
				setTextAppearance(
					it.getResourceId(
						styleable.Heading_style,
						style.TitleLarge
					)
				)
				setTextColor(ContextCompat.getColor(context, clr.onSurface))
				layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
			}

			endIcon = ImageButton(context).apply {
				setImageResource(draw.ic_arrow_down_2)
				setBackgroundResource(android.R.color.transparent)
			}
			endIcon.isVisible = it.getBoolean(styleable.Heading_show_close, false)

			headerRow.addView(title)
			headerRow.addView(endIcon)

			addView(headerRow)
		}
	}

	fun onCloseCLick(click : OnClickListener) {
		endIcon.setOnClickListener(click)
	}

}
