package io.bidswipe.app.ui.dashboard.scheduleShow

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.WeightAdapter
import io.bidswipe.app.databinding.FragmentListAProductBinding
import io.bidswipe.app.databinding.FragmentProductWeightBinding
import io.bidswipe.app.utils.ids

class ProductWeightFragment : BaseFragment<ScheduleShowViewModel, FragmentProductWeightBinding>() {
    override fun getModel(): Class<ScheduleShowViewModel> = ScheduleShowViewModel::class.java

    override fun getBind(inflater: LayoutInflater, view: ViewGroup?) = FragmentProductWeightBinding.inflate(inflater,view,false)

    private var mList = mutableListOf("","","","","","")

    private lateinit var adapter : WeightAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bind.header.onBackClick{
            findNavController().popBackStack()
        }

        adapter = WeightAdapter(mList)

        bind.recycler.adapter = adapter

        bind.continueBtn.setOnClickListener {
            findNavController().navigate(ids.goToAddProductFragment)
        }


    }

}