package io.bidswipe.app.ui.custom

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.core.view.isVisible
import com.github.ybq.android.spinkit.SpinKitView
import io.bidswipe.app.utils.ids
import io.bidswipe.app.utils.layout
import io.bidswipe.app.utils.styleable


@SuppressLint("ClickableViewAccessibility")
class LoaderView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0,
) :
    LinearLayout(context, attrs, defStyleAttr) {

    private var loader: SpinKitView? = null
    private lateinit var click: LinearLayout

    init {
        val attr = context.theme.obtainStyledAttributes(attrs, styleable.LoaderView, 0, 0)
        initView(attr)
    }

    private fun initView(attr: TypedArray) {
        val view = inflate(context, layout.loader_view, this)
        loader = view.findViewById(ids.spinner)
        click = view.findViewById(ids.click)

        //	loader.setColor(attr.getColor(styleable.LoaderView_loaderColor , ContextCompat.getColor(context , clr.primary)))

        click.isVisible = attr.getBoolean(styleable.LoaderView_showBack, true)

        click.setOnClickListener { }

        click.setOnTouchListener { _, _ -> true }

    }

}