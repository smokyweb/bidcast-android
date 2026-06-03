package io.bidswipe.app.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
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
import androidx.compose.ui.text.toUpperCase
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
import io.bidswipe.app.R
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.ui.custom.AlertType
import io.bidswipe.app.ui.custom.AppBottomSheet
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.Locale
import java.util.Locale.getDefault
import kotlin.math.ceil

fun runSafe(callback : () -> Unit) {
	try {
		callback.invoke()
	} catch (e : Exception) {
		Alerts.log("EXCEPTION" , "=> " + e.localizedMessage?.toString())
	}
}

inline fun <reified T : Enum<T>> TypedArray.getEnum(index : Int , default : T) = getInt(index , - 1).let {
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

fun Resources.isTablet(): Boolean {
	return (this.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK == Configuration.SCREENLAYOUT_SIZE_LARGE) ||
			(this.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK == Configuration.SCREENLAYOUT_SIZE_XLARGE)
}

fun ImageView.loadUrl(mCtx : Context , url : String , placeHolder : Int? = null,userName:String?=null) {
	runSafe {
		if (url.isEmpty() && !userName.isNullOrEmpty()) {
			val firstLetter = userName.firstOrNull()?.toString() ?: ""
			val drawable =Utils.generateTextDrawable(mCtx, firstLetter.uppercase(getDefault()))
			this.setImageDrawable(drawable)
		}else {
			Glide.with(mCtx)
				.load(url)
				.placeholder(placeHolder ?: R.drawable.placeholder_square)
				.error(placeHolder)
				.into(this)
		}
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
	this.replaceFirstChar { if (it.isLowerCase()) it.titlecase(getDefault()) else it.toString() }

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
	tag : String ?= null ,
	mClicks : AlertClicks? = null ,
	showSecondary : Boolean = false ,
	title : String = "Error" ,
	showAlert : Boolean = true ,
) {
	// MC (2026-05-28): Blank Seller Hub popup fix. Track whether the backend
	// actually gave us a real (non-blank) message. When it didn't (empty string
	// or whitespace) AND this isn't a network/known error, we suppress the
	// dialog entirely below - a benign empty state should not pop a blank alert.
	val rawBackendMessage = this.errorResponse?.message?.asHtml()?.asCapital()
	val hasRealMessage = !rawBackendMessage.isNullOrBlank()
	val message = try {
		if (this.isNetworkError)
			mCtx.getString(string.no_internet)
		// Basecamp #9928367737 (2026-05-27 round 2): errorCode here can be a
		// non-numeric string like 'UNKNOWN_ERROR' / 'NETWORK_ERROR' / 'JSON_ERROR'.
		// Previously this used .toInt() which threw NumberFormatException and
		// surfaced 'For input string: "UNKNOWN_ERROR"' as the error message to
		// the user, masking the real backend error. Use toIntOrNull instead so
		// non-numeric codes fall through to the real errorResponse.message.
		else if (this.errorCode?.toIntOrNull() == 413)
			"Image is too large"
		else
		// MC (2026-05-28): takeIf { it.isNotBlank() } so an empty-string backend
		// message yields the fallback rather than rendering a blank dialog.
			this.errorResponse?.message?.asHtml()?.asCapital()?.takeIf { it.isNotBlank() } ?: "No Data Found"
	} catch (e : Exception) {
		e.printStackTrace()
		e.localizedMessage?.asCapital()?.takeIf { it.isNotBlank() } ?: "No Data Found"
	}

	if (this.isNetworkError) Alerts.log(tag?: mCtx.javaClass.simpleName.toString() , "ERROR : \n${this.errorCode}")

	val clicks = if (this.errorResponse?.errorType == "UNAUTHORIZED" || this.errorResponse?.errorType == "invalid_token"||this.errorResponse?.message=="Token not found") {
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

	// MC (2026-05-28): Blank Seller Hub popup fix. Suppress the dialog when the
	// backend returned a blank/empty message AND this isn't a network error or a
	// special (413 / UNAUTHORIZED / invalid_token) case. A benign empty state
	// should never render a popup at all (blank or fallback). Real errors
	// (network, known codes, or any non-blank backend message) still show.
	val isAuthError = this.errorResponse?.errorType == "UNAUTHORIZED" ||
		this.errorResponse?.errorType == "invalid_token" ||
		this.errorResponse?.message == "Token not found"

	// Basecamp #9958788158 (2026-06-03): "token is invalid" popup on first app
	// open after install. When an auth error (UNAUTHORIZED / invalid_token /
	// Token not found) arrives while the user has NO stored token — i.e. they
	// are not logged in (fresh install, or a leftover/expired token that has
	// already been cleared) — there is no session to "expire", so popping a
	// "token is invalid" dialog is pure noise that confuses a brand-new user.
	// In that case suppress the dialog and silently route to the auth screen
	// instead of alarming the user. Real session-expiry (auth error WHILE a
	// token exists) still shows the dialog + logout flow as before.
	val notLoggedIn = try { Prefs(mCtx).token().isEmpty() } catch (e: Throwable) { false }
	if (isAuthError && notLoggedIn) {
		try {
			Prefs(mCtx).clear()
			mCtx.startActivity(mCtx.toAuth().apply {
				addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
			})
		} catch (e: Throwable) {
			Alerts.log(tag ?: mCtx.javaClass.simpleName, "silent auth-route failed: ${e.message}")
		}
		return
	}

	val isSpecialCase = this.isNetworkError ||
		this.errorCode?.toIntOrNull() == 413 ||
		isAuthError
	val shouldShow = showAlert && (hasRealMessage || isSpecialCase)
	if (shouldShow) {
		AppBottomSheet(
			mCtx = mCtx ,
			image = draw.ic_error ,
			title = title ,
			message = message ,
			primaryBtnText = "Okay" ,
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

//fun List<LiveShowModelOld.Product?>?.getCurrentProduct() = this?.firstOrNull { it?.isCurrent == true }

fun View.setHapticClickListener(onClick : (View) -> Unit) {
	setOnClickListener {
		HapticManager.performHaptic(this)
		onClick(it)
	}
}

/** #42: Ensure image URL is absolute. Prepend backend base if path starts with '/' or is blank. */
fun String?.toAbsoluteUrl(): String {
    if (this.isNullOrBlank()) return ""
    return if (this.startsWith("http")) this else "${Const.BASE_URL}$this"
}

fun Long.toEpochMillis(): Long {
	return when (this.toString().length) {
		10 -> this * 1000L              // seconds → millis
		13 -> this                      // already millis
		else -> this.toString()
			.take(13)
			.toLong()                   // trim extra digits
	}
}
