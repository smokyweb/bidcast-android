package io.bidcast.app.ui.auth

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import io.bidcast.app.R
import io.bidcast.app.base.BaseFragment
import io.bidcast.app.databinding.FragmentForgotPasswordBinding
import io.bidcast.app.utils.ids

class ForgotPasswordFragment : BaseFragment<AuthViewModel,FragmentForgotPasswordBinding>() {
    override fun getModel(): Class<AuthViewModel> = AuthViewModel::class.java

    override fun getBind(
        inflater: LayoutInflater,
        view: ViewGroup?
    ) = FragmentForgotPasswordBinding.inflate(inflater,view,false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bind.header.onBackClick{
            findNavController().popBackStack()
        }

        bind.submit.setOnClickListener {

            findNavController().navigate(ids.goToOTPFragment)

        }


    }

}