package io.bidswipe.app.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.content.res.TypedArray
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.text.method.HideReturnsTransformationMethod
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import androidx.annotation.IdRes
import androidx.annotation.NavigationRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.rubensousa.decorator.LinearDividerDecoration
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import io.bidswipe.app.R
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.model.LiveShowModelOld
import io.bidswipe.app.network.Resource
import io.bidswipe.app.ui.custom.AlertType
import io.bidswipe.app.ui.custom.AppBottomSheet
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.Locale
import kotlin.math.ceil

fun runSafe(callback : () -> Unit) {
	try {
		callback.invoke()
	} catch (e : Exception) {
		Alerts.log("EXCEPTION" , "=> " + e.localizedMessage?.toString())
	}
}

inline fun <reified T : Enum<T>> TypedArray.getEnum(index : Int , default : T) =
	getInt(index , - 1).let {
		if (it >= 0) enumValues<T>()[it] else default
	}

inline fun <T : ViewBinding> AppCompatActivity.bind(crossinline inflater : (LayoutInflater) -> T) =
	lazy(LazyThreadSafetyMode.NONE) {
		inflater(layoutInflater)
	}

private var density = 1f

fun Resources.checkDisplaySize() = runSafe { density = displayMetrics.density }

fun Resources.dpToPx(dp : Int) =
	TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP , dp.toFloat() , displayMetrics).toInt()

fun Resources.dp(value : Float) : Int {
	if (density == 1f) {
		checkDisplaySize()
	}

	return if (value == 0f) {
		0
	} else ceil((density * value).toDouble()).toInt()
}


fun ImageView.loadUrl(mCtx : Context , url : String , placeHolder : Int? = null) {
	runSafe {
		Glide.with(mCtx)
			.load(url)
			.placeholder(placeHolder ?: R.drawable.avatar)
			.error(placeHolder)
			.into(this)
	}
}


fun EditText.value() = this.text.toString().trim()

fun EditText.setNumberInput() {
	val preTypeface = typeface
	inputType = InputType.TYPE_NUMBER_VARIATION_PASSWORD or InputType.TYPE_CLASS_NUMBER
	typeface = preTypeface
	transformationMethod = HideReturnsTransformationMethod.getInstance()
}

fun String.asMoney() = "$" + "%.2f".format(this.toDouble())


fun String.request() = this.trim().toRequestBody("text/plain".toMediaTypeOrNull())

fun String.asHtml() = HtmlCompat.fromHtml(this , HtmlCompat.FROM_HTML_MODE_COMPACT).toString()

fun String.asCapital() =
	this.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

fun Int.toHex() = "#" + Integer.toHexString(this).substring(2)

fun NavController.setNewStart(@IdRes id : Int , @NavigationRes graphId : Int) {
	val navGraph = this.navInflater.inflate(graphId)
	navGraph.setStartDestination(id)
	this.graph = navGraph
}

fun AppCompatActivity.hideKeyboard() {
	val imm = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
	imm.hideSoftInputFromWindow((currentFocus ?: View(this)).windowToken , 0)
}

fun AppCompatActivity.showKeyboard(view : View? = null) {
	val focus = view ?: (currentFocus ?: View(this))

	val imm = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
	imm.showSoftInput(focus , InputMethodManager.SHOW_IMPLICIT)
}

fun Fragment.finish() {
	try {
		activity?.finishAfterTransition()
	} catch (e : Exception) {
		e.printStackTrace()
		requireActivity().finishAfterTransition()
	}
}

fun Fragment.hideKeyboard(view : View) {
	val imm = context?.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
	imm.hideSoftInputFromWindow(view.windowToken , 0)
}

fun Fragment.showKeyboard(view : View) {
	val imm = context?.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
	imm.showSoftInput(view , InputMethodManager.SHOW_IMPLICIT)
}

fun Fragment.intent() : Intent {
	return try {
		activity?.intent !!
	} catch (e : Exception) {
		e.printStackTrace()
		requireActivity().intent
	}
}

fun Resource.Error.parse(
	mCtx : Context ,
	tag : String ,
	mClicks : AlertClicks? = null ,
	showSecondary : Boolean = false ,
	title : String = "Error" ,
	showAlert : Boolean = true ,
) {
	val message = try {
		if (this.isNetworkError)
			mCtx.getString(string.no_internet)
		else if (this.errorCode?.toInt() == 413)
			"Image is too large"
		else
			this.errorResponse?.message?.asHtml()?.asCapital() ?: "No Data Found"
	} catch (e : Exception) {
		e.printStackTrace()
		e.localizedMessage?.asCapital() ?: "No Data Found"
	}

	if (this.isNetworkError) Alerts.log(tag , "ERROR : \n${this.errorCode}")

	val clicks = if (this.errorResponse?.errorType == "UNAUTHORIZED" || this.errorResponse?.errorType == "invalid_token") {
		object : AlertClicks {
			override fun primaryClick(dialog : AppBottomSheet) {
				dialog.dismiss()
//				mCtx.cancelNotification()
				Prefs(mCtx).clear()

				mCtx.startActivity(mCtx.toAuth().apply {
					addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
				})
			}

			override fun secondaryClick(dialog : AppBottomSheet) {
				dialog.dismiss()
			}
		}
	} else {
		mClicks ?: object : AlertClicks {
			override fun primaryClick(dialog : AppBottomSheet) {
				dialog.dismiss()
			}

			override fun secondaryClick(dialog : AppBottomSheet) {
				dialog.dismiss()

			}
		}
	}

	if (showAlert) {
		AppBottomSheet(
			mCtx = mCtx ,
			image = draw.ic_error ,
			title = title ,
			message = message ,
			primaryBtnText = "Ok" ,
			secondaryBtnText = "Cancel" ,
			canCancel = false ,
			showSecondary = showSecondary ,
			clicks = clicks ,
			alertType = AlertType.ERROR
		).show()
	}
}

fun View.setMargins(left : Int = 0 , top : Int , right : Int = 0 , bottom : Int = 0) {
	val params = this.layoutParams as ViewGroup.MarginLayoutParams
	params.setMargins(left , top , right , bottom)
	this.layoutParams = params
}

fun NavController.animatedNav(@IdRes id : Int , bundle : Bundle? = null) {
	val opt = NavOptions.Builder()
		.setEnterAnim(anim.slide_in_right)
		.setExitAnim(anim.slide_out_left)
		.setPopEnterAnim(anim.slide_in_left)
		.setPopExitAnim(anim.slide_out_right)
		.build()

	this.navigate(id , bundle , opt)
}


fun Context.getRecyclerDivider(left : Int = 0 , top : Int = 0 , right : Int = 0 , bottom : Int = 0) =
	LinearDividerDecoration.create(
		color = ContextCompat.getColor(this , clr.surfaceVariant) ,
		size = resources.dpToPx(2) ,
		leftMargin = resources.dpToPx(left) ,
		topMargin = resources.dpToPx(top) ,
		rightMargin = resources.dpToPx(right) ,
		bottomMargin = resources.dpToPx(bottom) ,
		addAfterLastPosition = false ,
		orientation = RecyclerView.VERTICAL ,
		addBeforeFirstPosition = false ,
	)

fun Context.getBitmapFromUrl(url : String , callBack : (Bitmap?) -> Unit) {
	Handler(Looper.getMainLooper()).post {
		Picasso.get().load(url).placeholder(draw.app_icon).error(draw.app_icon).into(object : Target {
			override fun onBitmapLoaded(bitmap : Bitmap? , from : Picasso.LoadedFrom?) {
				callBack(
					bitmap ?: ContextCompat.getDrawable(this@getBitmapFromUrl , draw.app_icon)?.toBitmap()
				)
			}

			override fun onBitmapFailed(e : java.lang.Exception? , errorDrawable : Drawable?) {
				callBack(errorDrawable?.toBitmap())
			}

			override fun onPrepareLoad(placeHolderDrawable : Drawable?) {
//					callBack(placeHolderDrawable?.toBitmap())
			}
		})
	}
}

fun SwipeRefreshLayout.setDefaults() {
	this.setColorSchemeResources(clr.primary , clr.onPrimaryContainer)
	this.setProgressBackgroundColorSchemeResource(clr.primaryContainer)
}

fun EditText.addDecimalLimiter(maxLimit : Int = 2) {
	this.addTextChangedListener(object : TextWatcher {

		override fun afterTextChanged(s : Editable?) {
			val str = this@addDecimalLimiter.text !!.toString()
			if (str.isEmpty()) return
			val str2 = decimalLimiter(str , maxLimit)

			if (str2 != str) {
				this@addDecimalLimiter.setText(str2)
				val pos = this@addDecimalLimiter.text !!.length
				this@addDecimalLimiter.setSelection(pos)
			}
		}

		override fun beforeTextChanged(s : CharSequence? , start : Int , count : Int , after : Int) {

		}

		override fun onTextChanged(s : CharSequence? , start : Int , before : Int , count : Int) {

		}

	})
}

fun decimalLimiter(string : String , maxDecimal : Int) : String {

	var str = string
	if (str[0] == '.') str = "0$str"
	val max = str.length

	var rFinal = ""
	var after = false
	var i = 0
	var up = 0
	var decimal = 0
	var t : Char

	val decimalCount = str.count { ".".contains(it) }

	if (decimalCount > 1)
		return str.dropLast(1)

	while (i < max) {
		t = str[i]
		if (t != '.' && ! after) {
			up ++
		} else if (t == '.') {
			after = true
		} else {
			decimal ++
			if (decimal > maxDecimal)
				return rFinal
		}
		rFinal += t
		i ++
	}
	return rFinal
}

/**
 * Extension function to get the current product from a nullable list of LiveShowModel.Product.
 * Returns the first product where isCurrent == true, or null if none.
 */

fun List<LiveShowModelOld.Product?>?.getCurrentProduct() : LiveShowModelOld.Product? {
	return this?.firstOrNull { it?.isCurrent == true }
}

fun View.setHapticClickListener(onClick: (View) -> Unit) {
	setOnClickListener {
		HapticManager.performHaptic(this)
		onClick(it)
	}
}