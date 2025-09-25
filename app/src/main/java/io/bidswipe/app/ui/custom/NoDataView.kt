package io.bidswipe.app.ui.custom

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.use
import androidx.core.view.isVisible
import io.bidswipe.app.databinding.NoDataViewBinding
import io.bidswipe.app.utils.draw
import io.bidswipe.app.utils.setHapticClickListener
import io.bidswipe.app.utils.styleable

@SuppressLint("ClickableViewAccessibility")
class NoDataView @JvmOverloads constructor(
	val mCtx : Context ,
	attrs : AttributeSet? = null ,
	defStyleAttr : Int = 0 ,
) :
	LinearLayout(mCtx , attrs , defStyleAttr) {
	private val bind = NoDataViewBinding.inflate(LayoutInflater.from(mCtx) , this , true)
	var title : TextView = bind.title

	init {
		mCtx.theme.obtainStyledAttributes(attrs , styleable.NoDataView , 0 , 0).use {
			bind.icon.setImageResource(it.getResourceId(styleable.NoDataView_icon , draw.empty))
			title.text = it.getString(styleable.NoDataView_title_text)
			bind.title.text = it.getString(styleable.NoDataView_title_text)
			bind.desc.text = it.getString(styleable.NoDataView_desc_text)
			bind.btn.text = it.getString(styleable.NoDataView_btn_title)
			bind.icon.isVisible = it.getBoolean(styleable.NoDataView_iconVisible , true)
			bind.btn.isVisible = it.getBoolean(styleable.NoDataView_show_button , false)

//            bind.root.setHapticClickListener { }
//            bind.root.setOnTouchListener { _, _ -> true }
		}
	}

	fun onClick(click : OnClickListener) {
		bind.btn.setOnClickListener { click.onClick(this) }
	}

}