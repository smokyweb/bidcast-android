package io.bidswipe.app.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper.*
import androidx.recyclerview.widget.RecyclerView
import io.bidswipe.app.controller.ChatAdapter
import kotlin.math.abs
import kotlin.math.min

class MessageSwiper(
	private val context : Context ,
	private val swipeControllerActions : SwipeControllerActions ,
) : Callback() {

	private lateinit var imageDrawable : Drawable
	private lateinit var shareRound : Drawable

	private var currentItemViewHolder : RecyclerView.ViewHolder? = null
	private lateinit var mView : View

	private var lastReplyButtonAnimationTime = 0L
	private var replyButtonProgress = 0F
	private var dX = 0F

	private var startTracking = false
	private var swipeBack = false
	private var isVibrate = false

	override fun getMovementFlags(recyclerView : RecyclerView , viewHolder : RecyclerView.ViewHolder) : Int {
		mView = viewHolder.itemView
		imageDrawable = ContextCompat.getDrawable(context , draw.ic_reply) !!
		shareRound = ContextCompat.getDrawable(context , draw.small_circle) !!
		return makeMovementFlags(ACTION_STATE_IDLE , RIGHT)
	}

	override fun onMove(recyclerView : RecyclerView , viewHolder : RecyclerView.ViewHolder , target : RecyclerView.ViewHolder) = false

	override fun onSwiped(viewHolder : RecyclerView.ViewHolder , direction : Int) {}

	override fun convertToAbsoluteDirection(flags : Int , layoutDirection : Int) = if (swipeBack) {
		swipeBack = false
		0
	}
	else {
		super.convertToAbsoluteDirection(flags , layoutDirection)
	}

	override fun onChildDraw(c : Canvas , v : RecyclerView , h : RecyclerView.ViewHolder , dX : Float , dY : Float , state : Int , isActive : Boolean) {
		if (h is ChatAdapter.DateViewHolder) {
			Alerts.log(javaClass.simpleName , "DATE ITEM")
		}
		else {
			if (state == ACTION_STATE_SWIPE) {
				setTouchListener(v , h)
			}

			if (mView.translationX < convertToDp(130) || dX < this.dX) {
				super.onChildDraw(c , v , h , dX , dY , state , isActive)
				this.dX = dX
				startTracking = true
			}
			currentItemViewHolder = h
			drawReplyButton(c)
		}
	}

	@SuppressLint("ClickableViewAccessibility")
	fun setTouchListener(recyclerView : RecyclerView , viewHolder : RecyclerView.ViewHolder) {
		recyclerView.setOnTouchListener { _ , event ->
			swipeBack = event.action == MotionEvent.ACTION_CANCEL || event.action == MotionEvent.ACTION_UP
			if (swipeBack) {
				if (abs(mView.translationX) >= this@MessageSwiper.convertToDp(100)) {
					swipeControllerActions.showReplyUI(viewHolder.absoluteAdapterPosition)
				}
			}
			false
		}
	}

	private fun drawReplyButton(canvas : Canvas) {
		if ((currentItemViewHolder == null)) {
			return
		}

		val translationX = mView.translationX
		val newTime = System.currentTimeMillis()

		val dt = min(17 , newTime - lastReplyButtonAnimationTime)
		lastReplyButtonAnimationTime = newTime
		val showing = translationX >= convertToDp(30)

		if (showing) {
			if (replyButtonProgress < 1.0f) {
				replyButtonProgress += dt / 180.0f
				if (replyButtonProgress > 1.0f) {
					replyButtonProgress = 1.0f
				}
				else {
					mView.invalidate()
				}
			}
		}
		else if (translationX <= 0.0f) {
			replyButtonProgress = 0f
			startTracking = false
			isVibrate = false
		}
		else {
			if (replyButtonProgress > 0.0f) {
				replyButtonProgress -= dt / 180.0f
				if (replyButtonProgress < 0.1f) {
					replyButtonProgress = 0f
				}
				else {
					mView.invalidate()
				}
			}
		}

		val alpha : Int
		val scale : Float

		if (showing)  {
			scale = if (replyButtonProgress <= 0.8f) {
				1.2f * (replyButtonProgress / 0.8f)
			}
			else {
				1.2f - 0.2f * ((replyButtonProgress - 0.8f) / 0.2f)
			}

			alpha = min(255f , 255 * (replyButtonProgress / 0.8f)).toInt()
		}
		else {
			scale = replyButtonProgress
			alpha = min(255f , 255 * replyButtonProgress).toInt()
		}

		shareRound.alpha = alpha

		imageDrawable.alpha = alpha
		if (startTracking) {
			if (! isVibrate && mView.translationX >= convertToDp(100)) {
				mView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
				isVibrate = true
			}
		}

		val x = if (mView.translationX > convertToDp(130)) {
			convertToDp(130) / 2
		}
		else {
			(mView.translationX / 2).toInt()
		}

		val y = (mView.top + mView.measuredHeight / 2).toFloat()
//		shareRound.colorFilter = PorterDuffColorFilter(ContextCompat.getColor(context , clr.error) , PorterDuff.Mode.MULTIPLY)

		shareRound.setBounds(
			(x - convertToDp(18) * scale).toInt() ,
			(y - convertToDp(18) * scale).toInt() ,
			(x + convertToDp(18) * scale).toInt() ,
			(y + convertToDp(18) * scale).toInt()
		)
		shareRound.draw(canvas)

		imageDrawable.setBounds(
			(x - convertToDp(12) * scale).toInt() ,
			(y - convertToDp(11) * scale).toInt() ,
			(x + convertToDp(12) * scale).toInt() ,
			(y + convertToDp(10) * scale).toInt()
		)
		imageDrawable.draw(canvas)
		shareRound.alpha = 255
		imageDrawable.alpha = 255
	}

	private fun convertToDp(pixel : Int) = context.resources.dp(pixel.toFloat())

	interface SwipeControllerActions {
		fun showReplyUI(position : Int)
	}

}