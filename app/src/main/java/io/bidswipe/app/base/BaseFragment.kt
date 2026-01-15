@file:Suppress("PropertyName")

package io.bidswipe.app.base

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import androidx.viewbinding.ViewBinding
import com.permissionx.guolindev.PermissionX
import io.bidswipe.app.App
import io.bidswipe.app.R
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Prefs
import io.bidswipe.app.utils.clr

abstract class BaseFragment<VM : ViewModel, BIND : ViewBinding> : Fragment() {

    protected lateinit var viewModel: VM
    protected lateinit var mCtx: Context
    protected lateinit var bind: BIND
    protected lateinit var userId: String
    protected lateinit var userName: String
    protected lateinit var userUserName: String
    protected lateinit var userImage: String
    protected lateinit var TAG: String
    protected lateinit var dropdownBg: Drawable

    override fun onCreateView(
		inflater: LayoutInflater,
		view: ViewGroup?,
		savedInstanceState: Bundle?,
	): View? {
        bind = getBind(inflater, view)
        mCtx = inflater.context

        // Set fragment background to surface color
        bind.root.setBackgroundColor(ContextCompat.getColor(mCtx, clr.surface))

        if (App.profileResponse.value?.preferences?.hapticFeedback == true) {
            applyHapticToAllClickableViews(bind.root)
        }


        TAG = try {
            findNavController().currentDestination?.label.toString().uppercase()
        } catch (_: Exception) {
            "FRAGMENT_$tag"
        }

        userId = Prefs(mCtx).getUserData()?.id.toString()
        userName = Prefs(mCtx).getUserData()?.name.toString()
        userUserName = Prefs(mCtx).getUserData()?.username.toString()
        userImage = Prefs(mCtx).getUserData()?.profileImage.toString()
//		authUserData = Prefs(mCtx).getUserData()
        viewModel = ViewModelProvider(requireActivity())[getModel()]

        dropdownBg = ContextCompat.getDrawable(mCtx, R.drawable.card_8)!!

        return bind.root
    }

    protected fun log(msg: String) {
        Alerts.log(TAG, msg)
    }

    protected fun errorToast(msg: String) {
        Alerts.error(mCtx, msg)
    }

    protected fun successToast(msg: String) {
        Alerts.success(mCtx, msg)
    }

    protected fun requestPerms(perms: Array<String>, result: (status: Boolean) -> Unit) {
        PermissionX.init(this)
            .permissions(*perms)
            .explainReasonBeforeRequest()
            .onExplainRequestReason { scope, deniedList ->
                scope.showRequestReasonDialog(deniedList, "Grant Permission", "OK", "Cancel")
            }.onForwardToSettings { scope, deniedList ->
                scope.showForwardToSettingsDialog(
                    deniedList,
                    "You need to allow necessary permissions in Settings manually",
                    "OK",
                    "Cancel"
                )
            }.request { allGranted, grantedList, deniedList ->
                if (allGranted) {
                    Alerts.log(TAG, "GRANTED : $grantedList")
                    result(true)
                } else {
                    Alerts.log(TAG, "DENIED : $deniedList")
                    result(false)
                }
            }
    }

    protected fun onBackPressed(callBack: () -> Unit) {
        activity?.onBackPressedDispatcher?.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    callBack.invoke()
                }
            })
    }

    private fun applyHapticToAllClickableViews(view: View) {
        if (view.isClickable) {
            view.setOnTouchListener { v, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                }
                false
            }
        }

        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                applyHapticToAllClickableViews(view.getChildAt(i))
            }
        }
    }

    abstract fun getModel(): Class<VM>

    abstract fun getBind(inflater: LayoutInflater, view: ViewGroup?): BIND

}