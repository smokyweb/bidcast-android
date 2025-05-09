package io.bidswipe.app.ui.auth

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.databinding.FragmentOTPBinding
import io.bidswipe.app.utils.ids

class OTPFragment : BaseFragment<AuthViewModel,FragmentOTPBinding>() {
    override fun getModel(): Class<AuthViewModel> = AuthViewModel::class.java

    override fun getBind(inflater: LayoutInflater, view: ViewGroup?) = FragmentOTPBinding.inflate(inflater,view, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        bind.header.onBackClick{
            findNavController().popBackStack()
        }

        bind.submit.setOnClickListener {

            findNavController().navigate(ids.goToResetPassword)

        }


    }

}