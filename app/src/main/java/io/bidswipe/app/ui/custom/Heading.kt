package io.bidswipe.app.ui.custom

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.use
import io.bidswipe.app.utils.clr
import io.bidswipe.app.utils.dpToPx
import io.bidswipe.app.utils.style
import io.bidswipe.app.utils.styleable


class Heading @JvmOverloads constructor(
	context : Context ,
	attrs : AttributeSet? = null ,
	defStyleAttr : Int = 0 ,
) : LinearLayout(context , attrs , defStyleAttr) {

	init {
		context.theme.obtainStyledAttributes(attrs , styleable.Heading , 0 , 0).use {
			orientation = VERTICAL
			val title = TextView(context).apply {
				text = it.getString(styleable.Heading_title)
				setTextAppearance(style.TitleLarge)
				isAllCaps = false
				setTextColor(ContextCompat.getColor(context , clr.scrim))
				setPadding(0 , 0 , 0 , context.resources.dpToPx(4))
				layoutParams =
					ViewGroup.LayoutParams(LayoutParams.WRAP_CONTENT , LayoutParams.WRAP_CONTENT)
			}


			addView(title , 0)
			//addView(line, 1)
		}
	}

}