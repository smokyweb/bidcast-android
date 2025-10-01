package io.bidswipe.app.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.databinding.FragmentForgotPasswordBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.hideKeyboard
import io.bidswipe.app.utils.ids
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.setHapticClickListener
import io.bidswipe.app.utils.showKeyboard
import io.bidswipe.app.utils.value

class ForgotPasswordFragment : BaseFragment<AuthViewModel, FragmentForgotPasswordBinding>() {
	override fun getModel(): Class<AuthViewModel> = AuthViewModel::class.java

	override fun getBind(
		inflater: LayoutInflater,
		view: ViewGroup?,
	) = FragmentForgotPasswordBinding.inflate(inflater, view, false)

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		bind.header.onBackClick {
			findNavController().popBackStack()
		}

		bind.layout.setHapticClickListener {
			hideKeyboard(it)
		}

		bind.submit.setHapticClickListener {

			when {

				bind.email.value().isEmpty() -> {
					Alerts.error(mCtx, "Please enter email address")
					bind.email.requestFocus()
					showKeyboard(bind.email)
				}

				Utils.validateEmail(bind.email.value()).not() -> {
					Alerts.error(mCtx, "please enter correct email address")
					bind.email.requestFocus()
					showKeyboard(bind.email)
				}

				else -> {

					hideKeyboard(it)
					bind.loader.isVisible = true
					viewModel.forgotPassword(bind.email.value().request())

				}
			}

		}

		viewModel.forgotPasswordRepo.observe(viewLifecycleOwner) {
			when (it) {
				is Resource.Success -> {
					viewModel.forgotPasswordRepo.value = null
					bind.loader.isVisible = false
					log("RESPONSE ::${it.value}")
					findNavController().navigate(
						ids.goToOTPFragment,
						bundleOf("email" to bind.email.value())
					)
				}

				is Resource.Error -> {
					viewModel.forgotPasswordRepo.value = null
					bind.loader.isVisible = false


					it.parse(mCtx, TAG, mClicks = object : AlertClicks {

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