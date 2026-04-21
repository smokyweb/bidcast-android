package io.bidswipe.app.ui.more

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.view.isVisible
import io.bidswipe.app.App
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.databinding.FragmentDeleteAccountBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Prefs
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.hideKeyboard
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.setHapticClickListener
import io.bidswipe.app.utils.showKeyboard
import io.bidswipe.app.utils.toAuth
import io.bidswipe.app.utils.value

class DeleteAccountFragment : BaseFragment<MoreViewModel, FragmentDeleteAccountBinding>() {

	override fun getModel(): Class<MoreViewModel> = MoreViewModel::class.java

	override fun getBind(
		inflater: LayoutInflater,
		view: ViewGroup?,
	) = FragmentDeleteAccountBinding.inflate(inflater, view, false)

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		bind.header.onBackClick {
			finish()
		}

		bind.root.setHapticClickListener {
			hideKeyboard(it)
		}
		setupKeyboardDismiss(bind.root)

		bind.submitButton.setHapticClickListener {
			if (bind.reasonInput.value().isEmpty()) {
				Alerts.error(mCtx, "Please enter reason for deletion")
				bind.reasonInput.requestFocus()
				showKeyboard(bind.reasonInput)
				return@setHapticClickListener
			}

			hideKeyboard(it)
			bind.loader.isVisible = true
			viewModel.deleteProfile(bind.reasonInput.value().request())
		}

		viewModel.deleteProfileRepo.observe(viewLifecycleOwner) {
			when (it) {
				is Resource.Success -> {
					bind.loader.isVisible = false
					viewModel.deleteProfileRepo.value = null
					successToast(it.value.message.toString())

					App.profileResponse.value = null
					App.checkKycResponse.value = null
					App.categoryList.clear()
					App.socketManager?.disconnect()
					App.socketManager = null

					Prefs(mCtx).clear()
					startActivity(mCtx.toAuth())
					finish()
				}

				is Resource.Error -> {
					bind.loader.isVisible = false
					viewModel.deleteProfileRepo.value = null
					it.parse(mCtx, TAG, object : AlertClicks {
						override fun primaryClick(dialog: AppBottomSheet) {
							dialog.dismiss()
						}

						override fun secondaryClick(dialog: AppBottomSheet) {
							dialog.dismiss()
						}
					})
				}

				else -> {}
			}
		}
	}

	private fun setupKeyboardDismiss(view: View) {
		if (view !is EditText) {
			view.setOnTouchListener { v, event ->
				if (event.action == MotionEvent.ACTION_DOWN) {
					v.clearFocus()
					hideKeyboard(v)
				}
				false
			}
		}

		if (view is ViewGroup) {
			for (index in 0 until view.childCount) {
				setupKeyboardDismiss(view.getChildAt(index))
			}
		}
	}
}
