package io.bidswipe.app.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.databinding.FragmentOTPBinding
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

class OTPFragment : BaseFragment<AuthViewModel , FragmentOTPBinding>() {
	override fun getModel() : Class<AuthViewModel> = AuthViewModel::class.java

	override fun getBind(inflater : LayoutInflater , view : ViewGroup?) =
		FragmentOTPBinding.inflate(inflater , view , false)

	private var email = ""

	override fun onViewCreated(view : View , savedInstanceState : Bundle?) {
		super.onViewCreated(view , savedInstanceState)

		email = arguments?.getString("email" , "").toString()

		bind.header.onBackClick {
			findNavController().popBackStack()
		}

		bind.layout.setHapticClickListener {
			hideKeyboard(it)
		}


		bind.submit.setHapticClickListener {

			when {
				bind.otp.value().isEmpty() -> {
					Alerts.error(mCtx , "please enter the otp sent to your email")
					bind.otp.requestFocus()
					showKeyboard(bind.otp)
				}

				else -> {
					hideKeyboard(it)
					bind.loader.isVisible = true
					viewModel.verifyOtp(email.request() , bind.otp.value().request())
				}

			}


		}

		viewModel.verifyOtpRepo.observe(viewLifecycleOwner) {
			when (it) {
				is Resource.Success -> {
					viewModel.verifyOtpRepo.value = null
					bind.loader.isVisible = false
					log("RESPONSE ::${it.value}")
					findNavController().navigate(ids.goToResetPassword , bundleOf("email" to email))
				}

				is Resource.Error -> {
					viewModel.verifyOtpRepo.value = null
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