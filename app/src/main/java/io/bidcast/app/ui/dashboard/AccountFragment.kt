package io.bidcast.app.ui.dashboard

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.bidcast.app.R
import io.bidcast.app.base.BaseFragment
import io.bidcast.app.databinding.FragmentAccountBinding

class AccountFragment : BaseFragment<DashViewModel, FragmentAccountBinding>() {

    override fun getModel(): Class<DashViewModel> = DashViewModel::class.java

    override fun getBind(inflater: LayoutInflater, view: ViewGroup?) = FragmentAccountBinding.inflate(inflater,view,false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


    }

}