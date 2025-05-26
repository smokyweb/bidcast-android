package io.bidswipe.app.ui.dashboard.more

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.databinding.FragmentSalesTaxExemptionBinding
import io.bidswipe.app.utils.finish

class SalesTaxExemptionFragment : BaseFragment<MoreViewModel, FragmentSalesTaxExemptionBinding>() {
    override fun getModel(): Class<MoreViewModel> = MoreViewModel::class.java

    override fun getBind(
        inflater: LayoutInflater,
        view: ViewGroup?
    ) = FragmentSalesTaxExemptionBinding.inflate(inflater,view,false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bind.header.onBackClick {
            finish()
        }


    }

}