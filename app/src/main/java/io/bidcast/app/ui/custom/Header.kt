package io.bidcast.app.ui.custom

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.content.res.use
import androidx.core.view.isVisible
import io.bidcast.app.databinding.HeaderViewBinding
import io.bidcast.app.utils.clr
import io.bidcast.app.utils.draw
import io.bidcast.app.utils.styleable

class Header @JvmOverloads constructor(
	context : Context ,
	attrs : AttributeSet? = null ,
	defStyleAttr : Int = 0
) : LinearLayout(context , attrs , defStyleAttr) {

	private val bind = HeaderViewBinding.inflate(LayoutInflater.from(context) , this , true)

	init {

		context.theme.obtainStyledAttributes(attrs , styleable.Header , 0 , 0).use {
			bind.backIcon.isVisible = it.getBoolean(styleable.Header_back_icon , true)

			setBackIcon(it.getResourceId(styleable.Header_back_icon , draw.ic_back))
			setMoreIcon(it.getResourceId(styleable.Header_more_icon , draw.ic_delete))
			setHeaderText(it.getString(styleable.Header_header_title) ?: "")
			showMore(it.getBoolean(styleable.Header_show_more , false))
			showBack(it.getBoolean(styleable.Header_show_back , true))


			if (it.getBoolean(styleable.Header_is_transparent, false)) {
				bind.root.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, clr.transparent))
			}
		}
	}

	fun setHeaderText(title : String) { bind.text.text = title
	}

	fun onBackClick(click : OnClickListener) {
		bind.backIcon.setOnClickListener(click)
	}

	fun onMoreClick(click : OnClickListener) {
		bind.moreIcon.setOnClickListener(click)
	}

	fun setMoreIcon(@DrawableRes id : Int) = bind.moreIcon.setImageResource(id)

	fun setBackIcon(@DrawableRes id : Int) = bind.backIcon.setImageResource(id)

	fun showMore(state : Boolean) {
		if (state) {
			bind.moreIcon.isEnabled = true
			bind.moreSection.visibility = VISIBLE
			bind.moreIcon.visibility = VISIBLE
		} else {
			bind.moreIcon.isEnabled = false
			bind.moreSection.visibility = INVISIBLE
			bind.moreIcon.visibility = INVISIBLE
		}
	}

	fun showBack(state : Boolean) {
		if (state) {
			bind.back.isEnabled = true
			bind.back.visibility = VISIBLE
			bind.back.visibility = VISIBLE
		} else {
			bind.backIcon.isEnabled = false
			bind.back.visibility = INVISIBLE
			bind.backIcon.visibility = INVISIBLE
		}
	}


//	fun setMoreIcon(@DrawableRes id: Int) = bind.moreIcon.setImageResource(id)

	fun background(color:Int){
		bind.header.setBackgroundColor(resources.getColor(clr.primary))
	}

}