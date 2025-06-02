package io.bidswipe.app.ui.dashboard.sellerHub

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.databinding.FragmentSellerVerificationBinding
import io.bidswipe.app.utils.finish

class SellerVerificationFragment : BaseFragment<SellerHubViewModel, FragmentSellerVerificationBinding>() {
    override fun getModel(): Class<SellerHubViewModel>  = SellerHubViewModel::class.java

    override fun getBind(
        inflater: LayoutInflater,
        view: ViewGroup?
    ) = FragmentSellerVerificationBinding.inflate(inflater,view,false)


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        bind.header.onBackClick {
            finish()
        }




    }

}