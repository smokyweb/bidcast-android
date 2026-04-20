package io.bidswipe.app.ui.more

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.view.isVisible
import com.wajahatkarim3.easyvalidation.core.view_ktx.validator
import io.bidswipe.app.App
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.databinding.FragmentContactUsBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.hideKeyboard
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.setHapticClickListener
import io.bidswipe.app.utils.showKeyboard
import io.bidswipe.app.utils.value

class ContactUsFragment : BaseFragment<MoreViewModel, FragmentContactUsBinding>() {
	override fun getModel(): Class<MoreViewModel> = MoreViewModel::class.java

	override fun getBind(
		inflater: LayoutInflater,
		view: ViewGroup?,
	) = FragmentContactUsBinding.inflate(inflater, view, false)

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		bind.header.onBackClick {
			finish()
		}

		bind.root.setHapticClickListener {
			hideKeyboard(it)
		}
		setupKeyboardDismiss(bind.root)

		bind.email.setText(App.profileResponse.value?.email.toString())


		bind.sendMessage.setHapticClickListener { it ->
			when {

				bind.firstName.value().isEmpty() -> {
					Alerts.error(mCtx, "Name can not be empty")
					bind.firstName.requestFocus()
					showKeyboard(bind.firstName)
				}

				bind.email.value().isEmpty() -> {
					Alerts.error(mCtx, "Email can not be empty")
					bind.email.requestFocus()
					showKeyboard(bind.email)
				}

				bind.email.value().validator().validEmail().check().not() -> {
					Alerts.error(mCtx, "Please enter valid user email")
					bind.email.requestFocus()
					showKeyboard(bind.email)
				}

				bind.subject.value().isEmpty() -> {
					Alerts.error(mCtx, "Subject can't be empty")
					bind.subject.requestFocus()
					showKeyboard(bind.subject)
				}

				bind.description.value().isEmpty() -> {
					Alerts.error(mCtx, "Enter Message")
					bind.subject.requestFocus()
					showKeyboard(bind.description)
				}

				else -> {

					hideKeyboard(it)
					bind.loader.isVisible = true

					viewModel.contactUs(
						bind.firstName.value().request(),
						bind.email.value().request(),
						bind.subject.value().request(),
						bind.description.value().request()
					)
				}
			}

			viewModel.contactUsRepo.observe(viewLifecycleOwner) {
				when (it) {
					is Resource.Success -> {
						bind.loader.isVisible = false

						it.value.data
						Alerts.success(mCtx, it.value.message.toString())

					}

					is Resource.Error -> {
						bind.loader.isVisible = false
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