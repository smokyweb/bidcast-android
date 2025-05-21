package io.bidswipe.app.ui.dashboard.product

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.databinding.FragmentSendGiftBinding
import io.bidswipe.app.utils.ids

class SendGiftFragment : BaseFragment<ProductViewModel, FragmentSendGiftBinding>() {
    override fun getModel(): Class<ProductViewModel> = ProductViewModel::class.java

    override fun getBind(
        inflater: LayoutInflater,
        view: ViewGroup?
    ) = FragmentSendGiftBinding.inflate(inflater, view, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    bind.header.onBackClick {
        findNavController().popBackStack()
    }

        bind.continueBtn.setOnClickListener {
            findNavController().navigate(ids.goToOrderStatusFragment)
        }


    }

}