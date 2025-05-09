package io.bidswipe.app.ui.dashboard.tutorials

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.ShowAdapter
import io.bidswipe.app.databinding.FragmentPrepareYourShowBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.ShowModel
import io.bidswipe.app.ui.dashboard.DashViewModel
import io.bidswipe.app.utils.ids
import io.bidswipe.app.utils.toScheduleShow

class PrepareYourShowFragment : BaseFragment<DashViewModel,FragmentPrepareYourShowBinding>() {
    override fun getModel(): Class<DashViewModel> = DashViewModel::class.java

    override fun getBind(
        inflater: LayoutInflater,
        view: ViewGroup?
    ) = FragmentPrepareYourShowBinding.inflate(inflater, view, false)

    private val showList = mutableListOf<ShowModel>()
    private val currentStep = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bind.header.onBackClick{
            findNavController().popBackStack()
        }

        showList.clear()
        showList.addAll(
            listOf(
                ShowModel("Schedule your first show",true,false,"Pick a date and time for your live show"),
                ShowModel("Schedule your first show",false,false,"Pick a date and time for your live show"),
                ShowModel("Schedule your first show",false,false,"Pick a date and time for your live show"),
                ShowModel("Schedule your first show",false,true,"Pick a date and time for your live show")
            )

        )

        val adapter = ShowAdapter(mList = showList, "getStarted",object: RecyclerClicks {
            override fun viewClick(pos: Int) {
                bind.stepProgress.setProgress(pos)

                showList.forEachIndexed { index, showModel ->
                    showModel.selected = index == pos
                }

                bind.recycler.adapter?.notifyDataSetChanged()
            }

            override fun itemClick(pos: Int, status: String) {
                when(pos){

                    0 -> {
                        startActivity(mCtx.toScheduleShow(from = "tutorial"))
                    }

                    1 -> {
                        findNavController().navigate(ids.goToShowTipsFragment, bundleOf("type" to "showTips"))
                    }

                    2 -> {
                        findNavController().navigate(ids.goToShowTipsFragment, bundleOf("type" to "liveTips"))
                    }

                }




            }

        })

        bind.recycler.adapter = adapter



    }

}