package io.bidswipe.app.utils

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.WindowCompat
import androidx.viewbinding.ViewBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import es.dmoral.toasty.Toasty
import io.bidswipe.app.BuildConfig
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.ui.custom.AppBottomSheet

object Alerts {

    fun log(pkg: String, message: String, isError: Boolean = false, isNetwork: Boolean = false) {
        val logStart = "\n\n<----------------------( LOG START )---------------------->\n\n"
        val logEnd = "\n\n<----------------------( LOG END )---------------------->\n\n"
        val msg = if (isNetwork.not()) {
            logStart + message + logEnd
        } else {
            message
        }

        if (BuildConfig.DEBUG) {
            when {
                isError -> Log.e(pkg, msg)
                isNetwork -> Log.i(pkg, msg)
                else -> Log.d(pkg, msg)
            }
        }
    }

    fun appAlert(mCtx: Context, isCancelable: Boolean = false, view: ViewBinding): AlertDialog {
        return AlertDialog.Builder(mCtx).apply {

            setView(view.root)
            setCancelable(isCancelable)
        }.create().also {
            it.window?.setBackgroundDrawable(
                ContextCompat.getColor(
                    mCtx,
                    clr.transparent
                ).toDrawable()
            )
            it.window?.setDimAmount(0.6f)
        }
    }

    fun appBottomSheet(mCtx: Context, isCancelable: Boolean, view: ViewBinding,isFullScreen:Boolean=false): BottomSheetDialog {
        var dialog= BottomSheetDialog(mCtx, style.BottomSheetDialogStyle).apply {
            setContentView(view.root)
            dismissWithAnimation = true
            setCancelable(isCancelable)

            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.isFitToContents = true
            behavior.skipCollapsed = true
        }.also {
            it.window?.apply {
                setBackgroundDrawable(ContextCompat.getColor(mCtx, clr.transparent).toDrawable())
                setDimAmount(0.6f)
                navigationBarColor = ContextCompat.getColor(mCtx, clr.surface)
                WindowCompat.setDecorFitsSystemWindows(this, false)
            }
        }

        if(isFullScreen){
            dialog.setOnShowListener {
                val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)

                bottomSheet?.layoutParams?.height = ViewGroup.LayoutParams.MATCH_PARENT
                bottomSheet?.requestLayout()

                val behavior = BottomSheetBehavior.from(bottomSheet!!)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.isFitToContents = false
                behavior.skipCollapsed = true
                behavior.peekHeight = mCtx.resources.displayMetrics.heightPixels
            }
        }

        return dialog
    }

    fun showBottomSheet(
        mCtx: Context,
        msg: String,
        title: String = "Error",
        isError: Boolean = false,
        clicks: AlertClicks? = null,
    ) {
        val icon = if (isError) draw.ic_error else draw.ic_success

        AppBottomSheet(
            mCtx, icon, title, msg, "Ok",
            "Ok",
            canCancel = true,
            showSecondary = isError,
            clicks = clicks ?: object : AlertClicks {
                override fun primaryClick(dialog: AppBottomSheet) {
                    dialog.dismiss()
                }

                override fun secondaryClick(dialog: AppBottomSheet) {
                    dialog.dismiss()
                }

            },
        ).show()
    }


    fun error(ctx: Context, message: String) =
        Toasty.error(ctx, message, Toast.LENGTH_SHORT, false).show()

    fun success(ctx: Context, message: String) =
        Toasty.success(ctx, message, Toast.LENGTH_SHORT, false).show()

}