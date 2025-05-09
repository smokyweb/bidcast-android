package io.bidswipe.app.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.databinding.FragmentLoginBinding
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.hideKeyboard
import io.bidswipe.app.utils.ids
import io.bidswipe.app.utils.showKeyboard
import io.bidswipe.app.utils.toDash
import io.bidswipe.app.utils.value

class LoginFragment : BaseFragment<AuthViewModel, FragmentLoginBinding>() {
    override fun getModel(): Class<AuthViewModel> = AuthViewModel::class.java

    override fun getBind(inflater: LayoutInflater, view: ViewGroup?) =
        FragmentLoginBinding.inflate(inflater, view, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bind.createAccount.setOnClickListener {
            findNavController().navigate(ids.goToCreateAccount)
        }

        bind.forgot.setOnClickListener {

            findNavController().navigate(ids.goToForgotPassword)
        }

        bind.loginBtn.setOnClickListener {

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
                    startActivity(mCtx.toDash())
                    finish()
                }
            }


        }


    }

}