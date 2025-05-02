package io.bidcast.app.ui.auth

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import io.bidcast.app.R
import io.bidcast.app.base.BaseFragment
import io.bidcast.app.databinding.FragmentLoginBinding
import io.bidcast.app.utils.finish
import io.bidcast.app.utils.ids
import io.bidcast.app.utils.toDash

class LoginFragment : BaseFragment<AuthViewModel, FragmentLoginBinding>() {
    override fun getModel(): Class<AuthViewModel> = AuthViewModel::class.java

    override fun getBind(inflater: LayoutInflater, view: ViewGroup?) = FragmentLoginBinding.inflate(inflater,view, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)



        bind.createAccount.setOnClickListener {
            findNavController().navigate(ids.goToCreateAccount)
        }

        bind.forgot.setOnClickListener {

            findNavController().navigate(ids.goToForgotPassword)
        }

        bind.loginBtn.setOnClickListener {
            startActivity(mCtx.toDash())
            finish()
        }


    }

}