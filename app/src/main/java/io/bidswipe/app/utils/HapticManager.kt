package io.bidswipe.app.utils

import android.view.HapticFeedbackConstants
import android.view.View

object HapticManager {

    private var isEnabled: Boolean = true  // default can be set based on user settings

    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
    }

    fun isEnabled(): Boolean = isEnabled

    fun performHaptic(view: View) {
        if (isEnabled) {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }
}