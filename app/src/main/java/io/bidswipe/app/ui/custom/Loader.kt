package io.riseshine.app.ui.custom

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.view.Window
import android.view.WindowManager
import io.bidswipe.app.ui.custom.LoaderView
import io.bidswipe.app.utils.runSafe

object Loader {
	private var dialog: Dialog? = null

	fun show(context: Context) {
		if (dialog == null || dialog?.isShowing != true) {
			dialog = Dialog(context).apply {
				requestWindowFeature(Window.FEATURE_NO_TITLE)
				setContentView(LoaderView(context))
				setCancelable(false) // Prevent dismissing by back button
				window?.setLayout(
					WindowManager.LayoutParams.MATCH_PARENT,
					WindowManager.LayoutParams.MATCH_PARENT
				)
				window?.setBackgroundDrawableResource(android.R.color.transparent)
			}

			runSafe {
				if (context is Activity && !context.isFinishing) {
					dialog?.show()
				}
			}
		}
	}

	fun dismiss() {
		dialog?.dismiss()
		dialog = null
	}
}




