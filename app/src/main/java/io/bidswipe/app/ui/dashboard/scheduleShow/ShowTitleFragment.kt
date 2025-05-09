package io.bidswipe.app.ui.dashboard.scheduleShow

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.databinding.FragmentShowTitleBinding
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.ids

class ShowTitleFragment : BaseFragment<ScheduleShowViewModel,FragmentShowTitleBinding>() {

    override fun getModel(): Class<ScheduleShowViewModel> =  ScheduleShowViewModel::class.java

    override fun getBind(inflater: LayoutInflater, view: ViewGroup?) = FragmentShowTitleBinding.inflate(inflater,view,false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val from = activity?.intent?.getStringExtra("from")

        bind.header.onBackClick{
            finish()
        }

        bind.continueBtn.setOnClickListener {
            if (from =="tips"){

                findNavController().navigate(ids.goToSelectCategoryFragment)

            }else{
                findNavController().navigate(ids.goToSelectShowTimeFragment)
            }

        }



    }

}