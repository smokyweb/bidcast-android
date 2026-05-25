package io.bidswipe.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import com.gyf.immersionbar.ktx.immersionBar
import com.wajahatkarim3.easyvalidation.core.view_ktx.validator
import io.bidswipe.app.App
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.databinding.FragmentCreateAccountBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.interest.ChooseInterestActivity
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Prefs
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.hideKeyboard
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.setHapticClickListener
import io.bidswipe.app.utils.showKeyboard
import io.bidswipe.app.utils.toDash
import io.bidswipe.app.utils.value

class CreateAccountFragment : BaseFragment<AuthViewModel , FragmentCreateAccountBinding>() {
	override fun getModel() : Class<AuthViewModel> = AuthViewModel::class.java

	override fun getBind(inflater : LayoutInflater , view : ViewGroup?) = FragmentCreateAccountBinding.inflate(inflater , view , false)

	var referralCode = ""

	override fun onViewCreated(view : View , savedInstanceState : Bundle?) {
		super.onViewCreated(view , savedInstanceState)

		referralCode = arguments?.getString("referralCode") ?: ""

		if (referralCode.isNotEmpty()) {
			bind.referralCode.setText(referralCode)
		}

		log("REFERRAL CODE : $referralCode")

		bind.header.onBackClick {
			findNavController().popBackStack()
		}

		bind.login.setOnClickListener {
			findNavController().popBackStack()
		}

		bind.layout.setHapticClickListener {
			hideKeyboard(it)
		}


		bind.createAccountBtn.setHapticClickListener {
			when {

				bind.firstName.value().isEmpty() -> {
					Alerts.error(mCtx , "First name can not be empty")
					bind.firstName.requestFocus()
					showKeyboard(bind.firstName)
				}

				bind.lastName.value().isEmpty() -> {
					Alerts.error(mCtx , "Last name can not be empty")
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

				// QA-FIX: Copy said "6 digit password" which is both misleading (digits ≠ characters)
				// and too weak; tightened to 8 characters minimum for baseline security. If product
				// wants more (numbers/symbols/etc.) they can layer it on, but 8 chars is a safer floor.
				bind.password.value().validator().minLength(8).check().not() -> {
					Alerts.error(mCtx , "Password must be at least 8 characters")
					bind.password.requestFocus()
					showKeyboard(bind.password)
				}

				bind.cPassword.value().isEmpty() -> {
					Alerts.error(mCtx , "Confirm password can not be empty")
					bind.cPassword.requestFocus()
					showKeyboard(bind.cPassword)
				}

				// QA-FIX: Grammar pass on user-facing copy.
				bind.cPassword.value() != bind.password.value() -> {
					Alerts.error(mCtx , "Passwords do not match")
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
					// MC cmpaj2fex0000w5hgq64jp9k4 merge (2026-05-24): both sides do
					// the same auto-login-after-signup flow; kept GitLab's cleaner
					// implementation (explicit Alerts.error for missing token +
					// early return) over GitHub's slightly older nested-if form.
					successToast(it.value.message.toString())

					val token = it.value.data?.token?.trim().orEmpty()
					if (token.isEmpty()) {
						Alerts.error(mCtx, "Account created but no session token was returned. Please log in.")
						findNavController().popBackStack()
						return@observe
					}

					Prefs(mCtx).putString(Prefs.TOKEN, "Bearer $token")
					App.getProfile()
					App.checkKYC()
					App.setUpSocket()
					App.getCategories()

					// New sign-ups are treated like first-time login (interests not set yet).
					startActivity(
						Intent(mCtx, ChooseInterestActivity::class.java).putExtra(
							"isFirstTimeLogin",
							true
						)
					)
					finish()
				}

				is Resource.Error -> {
					bind.loader.isVisible = false
					viewModel.signUpRepo.value = null

					// QA-FIX (deleted-account signup): before falling through to the generic
					// parse() path (which renders raw backend copy or "No Data Found"),
					// branch on recognisable error signals from the backend and show
					// friendlier, signup-specific copy so the user knows what to do next.
					val errType = it.errorResponse?.errorType?.uppercase()
					val rawMsg = it.errorResponse?.message?.trim().orEmpty()
					val lowerMsg = rawMsg.lowercase()

					val handled = when {
						errType == "ACCOUNT_DELETED" || lowerMsg.contains("account was deleted") || lowerMsg.contains("account has been deleted") -> {
							Alerts.showBottomSheet(
								mCtx = mCtx,
								msg = "This account was previously deleted and cannot be reused. Please contact support to restore it, or sign up with a different email.",
								title = "Account Deleted",
								isError = true
							)
							true
						}
						errType == "EMAIL_TAKEN" || lowerMsg.contains("has already been taken") || lowerMsg.contains("already been registered") || lowerMsg.contains("email already") -> {
							Alerts.showBottomSheet(
								mCtx = mCtx,
								msg = "An account with this email already exists. Try logging in or use \"Forgot Password\" to reset it.",
								title = "Email Already Registered",
								isError = true
							)
							true
						}
						else -> false
					}

					if (!handled) {
						// Fallback: if the backend returned a non-empty human message, surface
						// it directly with a sensible title. If there is no message at all,
						// use a friendly signup-specific fallback instead of "No Data Found".
						if (rawMsg.isNotEmpty() && !it.isNetworkError) {
							it.parse(mCtx , TAG , object : AlertClicks {
								override fun primaryClick(dialog : AppBottomSheet) { dialog.dismiss() }
								override fun secondaryClick(dialog : AppBottomSheet) { dialog.dismiss() }
							})
						} else if (it.isNetworkError) {
							it.parse(mCtx , TAG , object : AlertClicks {
								override fun primaryClick(dialog : AppBottomSheet) { dialog.dismiss() }
								override fun secondaryClick(dialog : AppBottomSheet) { dialog.dismiss() }
							})
						} else {
							Alerts.showBottomSheet(
								mCtx = mCtx,
								msg = "Signup failed. Please try again or contact support if the problem continues.",
								title = "Error",
								isError = true
							)
						}
					}
				}

				else -> {}

			}
		}

	}

}