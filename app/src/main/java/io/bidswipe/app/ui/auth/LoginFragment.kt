package io.bidswipe.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import io.bidswipe.app.App
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.databinding.FragmentLoginBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.model.RememberModel
import io.bidswipe.app.network.Resource
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.interest.ChooseInterestActivity
import io.bidswipe.app.ui.more.MoreActivity
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Prefs
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.hideKeyboard
import io.bidswipe.app.utils.ids
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.setHapticClickListener
import io.bidswipe.app.utils.showKeyboard
import io.bidswipe.app.utils.toDash
import io.bidswipe.app.utils.value

class LoginFragment : BaseFragment<AuthViewModel, FragmentLoginBinding>() {

	override fun getModel(): Class<AuthViewModel> = AuthViewModel::class.java

	override fun getBind(inflater: LayoutInflater, view: ViewGroup?) = FragmentLoginBinding.inflate(inflater, view, false)

	private val remList = mutableListOf<RememberModel>()

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		bind.createAccount.setHapticClickListener {
			findNavController().navigate(ids.goToCreateAccount)
		}

		bind.forgot.setHapticClickListener {
			findNavController().navigate(ids.goToForgotPassword)
		}

		bind.layout.setHapticClickListener {
			hideKeyboard(it)
		}

		bind.privacyPolicy.setHapticClickListener {
			startActivity(Intent(mCtx, MoreActivity::class.java)
				.putExtra("slug", "privacy-policy")
				.putExtra("title", "Privacy Policy"))
		}

		bind.termsOfService.setHapticClickListener {
			startActivity(Intent(mCtx, MoreActivity::class.java)
				.putExtra("slug", "terms-condition")
				.putExtra("title", "Terms of Service"))
		}

		bind.loginBtn.setHapticClickListener {
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

				bind.password.value().isEmpty() -> {
					Alerts.error(mCtx, "Please enter password")
					bind.password.requestFocus()
					showKeyboard(bind.password)
				}

				else -> {
					hideKeyboard(it)
					bind.loader.isVisible = true
					viewModel.login(
						bind.email.text.toString().request(),
						bind.password.text.toString().request()
					)
				}

			}
		}

		viewModel.loginRepo.observe(viewLifecycleOwner) {
			when (it) {
				is Resource.Success -> {
					viewModel.loginRepo.value = null
					bind.loader.isVisible = false
					log("RESPONSE ::${it.value}")

					it.value.data?.id.toString()

					if (bind.rememberMe.isChecked) {
						Alerts.log(TAG, "REMEMBER ME CHECK")
						saveRemember()
					}

					Prefs(mCtx).putString(
						Prefs.TOKEN,
						"Bearer " + it.value.data?.token.toString().trim()
					)

					Prefs(mCtx).putString(Prefs.USER, Gson().toJson(it.value.data).toString())
					App.getProfile()
					App.checkKYC()
					App.setUpSocket()
					App.getCategories()

					if (it.value.data?.isFirsttimeLogin == true) {
						val intent = Intent(mCtx, ChooseInterestActivity::class.java)
						intent.putExtra("isFirstTimeLogin", true)
						startActivity(intent)
						finish()
					} else {
						startActivity(mCtx.toDash())
						finish()
					}
				}

				is Resource.Error -> {
					viewModel.loginRepo.value = null
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

	override fun onStart() {
		super.onStart()
		fetchRem()
	}

	private fun saveRemember() {
		try {

			val users = Prefs(mCtx).getUsers()

			if (users.isEmpty()) {
				users.add(
					RememberModel(
						bind.email.value().trim(),
						bind.password.value()
					)
				)
			} else {

				if (!users.contains(
						RememberModel(
							bind.email.value().trim(),
							bind.password.value()
						)
					)
				) {
					users.add(
						RememberModel(
							bind.email.value().trim(),
							bind.password.value()
						)
					)
				}

				if (users.find { it.email == bind.email.value() }?.password != bind.password.value()) {
					users.find { it.email == bind.email.value() }?.password =
						bind.password.value()
				}
			}

			Prefs(mCtx).saveUsers(users)

		} catch (e: Exception) {
			e.printStackTrace()
		}
	}

	private fun fetchRem() {

		val users = Prefs(mCtx).getUsers()

		if (users.isNotEmpty()) {

			remList.clear()
			val userList = mutableListOf<String>()

			users.forEach {
				if (it.email.isNotEmpty()) {
					userList.add(it.email)
					remList.add(it)
				}
			}

			userList.forEach {
				Alerts.log(TAG, "DATA: $it")
			}

			val arrAdapter = ArrayAdapter(mCtx, R.layout.remember_list_item, userList)
			bind.email.setAdapter(arrAdapter)

			bind.email.setOnItemClickListener { _, _, position, _ ->
				try {
					bind.password.setText(remList[position].password)
				} catch (e: Exception) {
					e.printStackTrace()
				}
			}
		}

	}

}