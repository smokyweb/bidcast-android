package io.bidcast.app.ui.dashboard.tutorials

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import io.bidcast.app.R
import io.bidcast.app.base.BaseFragment
import io.bidcast.app.databinding.FragmentHowToSellBinding
import io.bidcast.app.ui.dashboard.DashViewModel
import io.bidcast.app.utils.ids

class HowToSellFragment : BaseFragment<DashViewModel,FragmentHowToSellBinding>() {

    override fun getModel(): Class<DashViewModel> = DashViewModel::class.java

    override fun getBind(inflater: LayoutInflater, view: ViewGroup?) = FragmentHowToSellBinding.inflate(inflater,view,false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bind.header.onBackClick{
            findNavController().popBackStack()
        }

        bind.accept.setOnClickListener {
            findNavController().navigate(ids.prepareYourShowFragment)
        }

        bind.decline.setOnClickListener {
            findNavController().popBackStack()
        }



    }

}