package io.bidswipe.app.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.databinding.FragmentAccountBinding

class AccountFragment : BaseFragment<DashViewModel, FragmentAccountBinding>() {

    override fun getModel(): Class<DashViewModel> = DashViewModel::class.java

    override fun getBind(inflater : LayoutInflater , view : ViewGroup?) = FragmentAccountBinding.inflate(inflater , view , false)

    private val onTabSelectedListener = object : OnTabSelectedListener {
        override fun onTabSelected(tab : TabLayout.Tab?) {
            bind.switcher.displayedChild = tab?.position ?: 0
        }

        override fun onTabUnselected(tab : TabLayout.Tab?) {
            bind.switcher.displayedChild = tab?.position ?: 0
        }

        override fun onTabReselected(tab : TabLayout.Tab?) {
            bind.switcher.displayedChild = tab?.position ?: 0
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bind.tabs.addOnTabSelectedListener(onTabSelectedListener)
    }

    override fun onDestroyView() {
        bind.tabs.removeOnTabSelectedListener(onTabSelectedListener)
        super.onDestroyView()
    }

}