package io.bidswipe.app.ui.dashboard.more

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.databinding.FragmentPrivacyPolicyBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.parse

class PrivacyPolicyFragment : BaseFragment<MoreViewModel,FragmentPrivacyPolicyBinding>() {
    override fun getModel(): Class<MoreViewModel> = MoreViewModel::class.java

    override fun getBind(inflater: LayoutInflater, view: ViewGroup?) = FragmentPrivacyPolicyBinding.inflate(inflater,view,false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bind.header.onBackClick {
            finish()
        }

        bind.loader.isVisible = true

        viewModel.getPrivacyPolicy()

        viewModel.getPrivacyPolicyRepo.observe(viewLifecycleOwner){
            when(it){
                is Resource.Success ->{
                    bind.loader.isVisible = false
                    viewModel.getPrivacyPolicyRepo.value = null
                    bind.content.setHtmlFromString(it.value.data?.pageContent ?: "",false)
                }
                is Resource.Error ->{
                    bind.loader.isVisible = false
                    viewModel.getPrivacyPolicyRepo.value = null
                    if (it.isNetworkError) {
                        errorToast(getString(R.string.no_internet))
                    } else {
                        it.parse(mCtx, TAG, object : AlertClicks {
                            override fun primaryClick(dialog: AppBottomSheet) {
                                dialog.dismiss()

                            }

                            override fun secondaryClick(dialog: AppBottomSheet) {
                                dialog.dismiss()

                            }
                        })
                    }
                }
                else ->{}
            }
        }

    }
}