package io.bidswipe.app.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.databinding.FragmentResetPasswordBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.hideKeyboard
import io.bidswipe.app.utils.ids
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.setHapticClickListener
import io.bidswipe.app.utils.showKeyboard
import io.bidswipe.app.utils.string
import io.bidswipe.app.utils.value

class ResetPasswordFragment : BaseFragment<AuthViewModel , FragmentResetPasswordBinding>() {
	override fun getModel() : Class<AuthViewModel> = AuthViewModel::class.java

	override fun getBind(inflater : LayoutInflater , view : ViewGroup?) =
		FragmentResetPasswordBinding.inflate(inflater , view , false)

	private var email = ""

	override fun onViewCreated(view : View , savedInstanceState : Bundle?) {
		super.onViewCreated(view , savedInstanceState)

		email = arguments?.getString("email" , "").toString()

		bind.header.onBackClick {
			findNavController().navigate(ids.goToLoginFragment)
		}

        bind.submit.setHapticClickListener {

			when {
				bind.password.value().isEmpty() -> {
					Alerts.error(mCtx , "please enter new password")
					bind.password.requestFocus()
					showKeyboard(bind.password)
				}

				bind.cPassword.value().isEmpty() -> {
					Alerts.error(mCtx , "please enter password again to confirm")
					bind.cPassword.requestFocus()
					showKeyboard(bind.cPassword)
				}

				bind.password.value() != bind.cPassword.value() -> {
					Alerts.error(mCtx , "confirm password does not matches with password")
					bind.cPassword.requestFocus()
					showKeyboard(bind.cPassword)
				}

				else -> {

					bind.loader.isVisible = true
					hideKeyboard(it)
					viewModel.resetPassword(
						email.request() ,
						bind.password.value().request() ,
						bind.cPassword.value().request()
					)
				}
			}


		}

		viewModel.resetPasswordRepo.observe(viewLifecycleOwner) {
			when (it) {
				is Resource.Success -> {
					viewModel.resetPasswordRepo.value = null
					bind.loader.isVisible = false
					log("RESPONSE ::${it.value}")
					findNavController().navigate(ids.goToLoginFragment)
				}

				is Resource.Error -> {
					viewModel.resetPasswordRepo.value = null
					bind.loader.isVisible = false
					if (it.isNetworkError) {
						errorToast(getString(string.no_internet))
					} else {

						it.parse(mCtx , TAG , mClicks = object : AlertClicks {

							override fun primaryClick(dialog : AppBottomSheet) {
								dialog.dismiss()
							}

							override fun secondaryClick(dialog : AppBottomSheet) {
								dialog.dismiss()
							}

						})

					}
				}

				else -> {}
			}
		}

	}

}