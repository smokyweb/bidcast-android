package io.bidswipe.app.ui.custom

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import io.bidswipe.app.R
import io.bidswipe.app.databinding.AppAlertViewBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.clr
import io.bidswipe.app.utils.dpToPx
import io.bidswipe.app.utils.layout
import io.bidswipe.app.utils.runSafe
import io.bidswipe.app.utils.setHapticClickListener

enum class AlertType {
    SUCCESS,
    WARNING,
    ERROR,
    INFO
}

class AppBottomSheet(
    private val mCtx: Context,
    var image: Int?,
    title: String,
    message: String?,
    primaryBtnText: String = "Confirm",
    secondaryBtnText: String = "Cancel",
    canCancel: Boolean,
    showSecondary: Boolean,
    iconPadding: Int = 0,
    clicks: AlertClicks,
    alertType: AlertType = AlertType.SUCCESS,
) : BottomSheetDialog(mCtx) {

    @SuppressLint("InflateParams")
    private val bind = AppAlertViewBinding.bind(
        LayoutInflater.from(mCtx).inflate(layout.app_alert_view, null, false)
    )

    init {
        setContentView(bind.root)
        setCancelable(canCancel)
        setCanceledOnTouchOutside(canCancel)

        runSafe {
            window?.navigationBarColor = ContextCompat.getColor(mCtx, clr.onPrimary)
            window?.setDimAmount(0.65f)
        }

        when (alertType) {
            AlertType.ERROR -> {
                bind.primaryBtn.setErrorStyle()
                bind.secondaryBtn.setErrorStyle(true)
                bind.imageCard.setCardBackgroundColor(
                    ContextCompat.getColorStateList(
                        mCtx,
                        clr.error
                    )
                )
            }

            AlertType.SUCCESS -> {
                bind.primaryBtn.setSuccessStyle()
                bind.secondaryBtn.setSuccessStyle(true)
                bind.imageCard.setCardBackgroundColor(
                    ContextCompat.getColorStateList(
                        mCtx,
                        clr.success
                    )
                )
            }

            AlertType.WARNING -> {
                bind.primaryBtn.setWarningStyle()
                bind.secondaryBtn.setWarningStyle(true)
                bind.imageCard.setCardBackgroundColor(
                    ContextCompat.getColorStateList(
                        mCtx,
                        clr.warning
                    )
                )
                bind.imageCard.setPadding(mCtx.resources.dpToPx(iconPadding))
                bind.image.imageTintList =    ContextCompat.getColorStateList(mCtx, clr.onPrimary)
            }

            AlertType.INFO -> {
                bind.primaryBtn.setInfoStyle()
                bind.secondaryBtn.setInfoStyle(true)
                bind.imageCard.setCardBackgroundColor(
                    ContextCompat.getColorStateList(
                        mCtx,
                        clr.onPrimary
                    )
                )
            }

        }

        if (image != null) {
            bind.image.setImageResource(image?: R.drawable.ic_info)
        } else {
            bind.image.isVisible = false
        }

        bind.image.setPadding(iconPadding)

        if (message?.isEmpty()==true) {
            bind.message.isVisible = false
        } else {
            bind.message.text = message?.asCapital()
        }

        bind.title.text = title.asCapital()

        bind.secondaryBtn.isVisible = showSecondary

        bind.primaryBtn.text = primaryBtnText.asCapital()
        bind.secondaryBtn.text = secondaryBtnText.asCapital()

        bind.primaryBtn.setHapticClickListener { clicks.primaryClick(this) }
        bind.secondaryBtn.setHapticClickListener { clicks.secondaryClick(this) }
    }

    private fun MaterialButton.setErrorStyle(isOutline: Boolean = false) {
        if (isOutline) {
            strokeColor = ContextCompat.getColorStateList(mCtx, clr.error)
            rippleColor = ContextCompat.getColorStateList(mCtx, clr.error)
            setTextColor(ContextCompat.getColor(mCtx, clr.error))
        } else {
            setBackgroundColor(ContextCompat.getColor(mCtx, clr.error))
            setTextColor(ContextCompat.getColor(mCtx, clr.onError))
            rippleColor = ContextCompat.getColorStateList(mCtx, clr.onErrorContainer)
        }
    }

    private fun MaterialButton.setWarningStyle(isOutline: Boolean = false) {
        if (isOutline) {
            strokeColor = ContextCompat.getColorStateList(mCtx, clr.warning)
            rippleColor = ContextCompat.getColorStateList(mCtx, clr.warning)
            setTextColor(ContextCompat.getColor(mCtx, clr.warning))
        } else {
            setBackgroundColor(ContextCompat.getColor(mCtx, clr.warning))
            setTextColor(ContextCompat.getColor(mCtx, clr.onError))
            rippleColor = ContextCompat.getColorStateList(mCtx, clr.onErrorContainer)
        }
    }

    private fun MaterialButton.setInfoStyle(isOutline: Boolean = false) {
        if (isOutline) {
            strokeColor = ContextCompat.getColorStateList(mCtx, clr.primary)
            rippleColor = ContextCompat.getColorStateList(mCtx, clr.primaryContainer)
            setTextColor(ContextCompat.getColor(mCtx, clr.primary))
        } else {
            setBackgroundColor(ContextCompat.getColor(mCtx, clr.primary))
            setTextColor(ContextCompat.getColor(mCtx, clr.surface))
            rippleColor = ContextCompat.getColorStateList(mCtx, clr.primaryContainer)
        }
    }

    private fun MaterialButton.setSuccessStyle(isOutline: Boolean = false) {
        if (isOutline) {
            strokeColor = ContextCompat.getColorStateList(mCtx, clr.success)
            rippleColor = ContextCompat.getColorStateList(mCtx, clr.success)
            setTextColor(ContextCompat.getColor(mCtx, clr.success))
        } else {
            setBackgroundColor(ContextCompat.getColor(mCtx, clr.success))
            setTextColor(ContextCompat.getColor(mCtx, clr.onSuccess))
            rippleColor = ContextCompat.getColorStateList(mCtx, clr.successContainer)
        }
    }


}
