package io.bidswipe.app.base

import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import com.permissionx.guolindev.PermissionX
import com.zeugmasolutions.localehelper.LocaleAwareCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import io.bidswipe.app.App
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Prefs

@Suppress("PropertyName")
@AndroidEntryPoint
abstract class BaseActivity : LocaleAwareCompatActivity() {

    protected var TAG = javaClass.simpleName.toString()
    protected var token = ""
    protected var userId = ""
    protected var userName = ""
    protected var userImage = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        userId = Prefs(this).getUserData()?.id.toString()
        userName = Prefs(this).getUserData()?.name.toString()
        userImage = Prefs(this).getUserData()?.profileImage ?: ""

        if (App.profileResponse.value?.preferences?.hapticFeedback == true) {
            applyHapticToAllClickableViews(window.decorView)
        }


        /*token = Prefs(this).token()
        userId = Prefs(this).getUserData()?.id.toString()
        userName = Prefs(this).getUserData()?.firstname + " " + Prefs(this).getUserData()?.lastname*/
    }

    private fun applyHapticToAllClickableViews(view: View) {
        if (view.isClickable) {
            view.setOnTouchListener { v, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                }
                v.performClick()
                false
            }
        }

        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                applyHapticToAllClickableViews(view.getChildAt(i))
            }
        }
    }

    protected fun log(msg: String) = Alerts.log(TAG, msg)

    protected fun errorToast(msg: String) = Alerts.error(this, msg)

    protected fun successToast(msg: String) = Alerts.success(this, msg)

    protected fun requestPerms(perms: Array<String>, result: (status: Boolean) -> Unit) {
        PermissionX.init(this)
            .permissions(*perms)
            .explainReasonBeforeRequest()
            .onExplainRequestReason { scope, deniedList ->
                scope.showRequestReasonDialog(deniedList, "Grant Permission", "OK", "Cancel")
            }
            .onForwardToSettings { scope, deniedList ->
                scope.showForwardToSettingsDialog(
                    deniedList,
                    "You need to allow necessary permissions in Settings manually",
                    "OK",
                    "Cancel"
                )
            }
            .request { allGranted, grantedList, deniedList ->
                if (allGranted) {
                    Alerts.log(TAG, "GRANTED : $grantedList")
                    result(true)
                } else {
                    Alerts.log(TAG, "DENIED : $deniedList")
                    result(false)
                }
            }
    }

    protected fun onBackPress(callback: () -> Unit) {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finishAfterTransition()
                callback.invoke()
                Alerts.log(TAG, "BACK PRESS CLICKED")
            }
        })
    }
}