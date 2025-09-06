package io.bidswipe.app.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import com.wajahatkarim3.easyvalidation.core.view_ktx.validator
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.databinding.FragmentCreateAccountBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.hideKeyboard
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.showKeyboard
import io.bidswipe.app.utils.value

class CreateAccountFragment : BaseFragment<AuthViewModel , FragmentCreateAccountBinding>() {
	override fun getModel() : Class<AuthViewModel> = AuthViewModel::class.java

	override fun getBind(inflater : LayoutInflater , view : ViewGroup?) =
		FragmentCreateAccountBinding.inflate(inflater , view , false)

	override fun onViewCreated(view : View , savedInstanceState : Bundle?) {
		super.onViewCreated(view , savedInstanceState)

		bind.header.onBackClick {
			findNavController().popBackStack()
		}


		bind.layout.setOnClickListener {
			hideKeyboard(it)
		}

		bind.createAccountBtn.setOnClickListener {
			when {

				bind.firstName.value().isEmpty() -> {
					Alerts.error(mCtx , "Name can not be empty")
					bind.firstName.requestFocus()
					showKeyboard(bind.firstName)
				}

				bind.lastName.value().isEmpty() -> {
					Alerts.error(mCtx , "Name can not be empty")
					bind.lastName.requestFocus()
					showKeyboard(bind.lastName)
				}

				bind.email.value().isEmpty() -> {
					Alerts.error(mCtx , "Email can not be empty")
					bind.email.requestFocus()
					showKeyboard(bind.email)
				}

				bind.email.value().validator().validEmail().check().not() -> {
					Alerts.error(mCtx , "Please enter valid user email")
					bind.email.requestFocus()
					showKeyboard(bind.email)
				}

				bind.password.value().isEmpty() -> {
					Alerts.error(mCtx , "Password can not be empty")
					bind.password.requestFocus()
					showKeyboard(bind.password)
				}

				bind.password.value().validator().minLength(6).check().not() -> {
					Alerts.error(mCtx , "Enter at least 6 digit password")
					bind.password.requestFocus()
					showKeyboard(bind.password)
				}

				bind.cPassword.value().isEmpty() -> {
					Alerts.error(mCtx , "Confirm password can not be empty")
					bind.cPassword.requestFocus()
					showKeyboard(bind.cPassword)
				}

				bind.cPassword.value() != bind.password.value() -> {
					Alerts.error(mCtx , "Confirm password not matched with password")
					bind.cPassword.requestFocus()
					showKeyboard(bind.cPassword)
				}

				else -> {
					hideKeyboard(it)
					bind.loader.isVisible = true

					viewModel.signUp(
						bind.firstName.text?.trim().toString().request() ,
						bind.lastName.text?.trim().toString().request() ,
						bind.email.text?.trim().toString().request() ,
						bind.password.text?.trim().toString().request() ,
						bind.cPassword.text?.trim().toString().request() ,
						bind.referralCode.value().ifEmpty { null }?.request()
					)
				}
			}
		}

		viewModel.signUpRepo.observe(viewLifecycleOwner) {
			when (it) {
				is Resource.Success -> {
					viewModel.signUpRepo.value = null
					bind.loader.isVisible = false
					successToast(it.value.message.toString())
//                  Prefs(mCtx).putString(Prefs.USER, Gson().toJson(it.value.data).toString())
					findNavController().popBackStack()
				}

				is Resource.Error -> {
					bind.loader.isVisible = false
					viewModel.signUpRepo.value = null
					if (it.isNetworkError) {
						errorToast(getString(R.string.no_internet))
					} else {
						it.parse(mCtx , TAG , object : AlertClicks {
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