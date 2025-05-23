package io.bidswipe.app.utils

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
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

    fun appAlert(mCtx: Context, isCancelable: Boolean = false, view: ViewBinding) =
        AlertDialog.Builder(mCtx).apply {
            setView(view.root)
            setCancelable(isCancelable)
        }.create().also {
            it.window?.setBackgroundDrawable(
                ColorDrawable(
                    ContextCompat.getColor(
                        mCtx,
                        clr.transparent
                    )
                )
            )
            it.window?.setDimAmount(0.6f)
        }

    fun appBottomSheet(mCtx: Context, isCancelable: Boolean, view: ViewBinding) =
        BottomSheetDialog(mCtx, style.BottomSheetDialogStyle).apply {
            setContentView(view.root)
            dismissWithAnimation = true
            setCancelable(isCancelable)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

    fun showBottomSheet(
        mCtx: Context,
        msg: String,
        title: String = "Error",
        isError: Boolean = false,
        clicks: AlertClicks? = null
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