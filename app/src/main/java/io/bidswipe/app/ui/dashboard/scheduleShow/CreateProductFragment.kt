package io.bidswipe.app.ui.dashboard.scheduleShow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.databinding.FragmentCreateProductBinding
import io.bidswipe.app.utils.ids

class CreateProductFragment : BaseFragment<ScheduleShowViewModel, FragmentCreateProductBinding>() {
    override fun getModel(): Class<ScheduleShowViewModel> = ScheduleShowViewModel::class.java

    override fun getBind(inflater: LayoutInflater, view: ViewGroup?) = FragmentCreateProductBinding.inflate(inflater,view,false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bind.header.onBackClick{
            findNavController().popBackStack()
        }

        bind.continueBtn.setOnClickListener {
            findNavController().navigate(ids.goToChooseSalesFormatFragment)
        }

        bind.useProduct.setOnClickListener {
            findNavController().navigate(ids.createProductAddProductFragment)
        }


    }

}