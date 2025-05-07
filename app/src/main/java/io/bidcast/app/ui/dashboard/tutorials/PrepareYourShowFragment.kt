package io.bidcast.app.ui.dashboard.tutorials

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import io.bidcast.app.base.BaseFragment
import io.bidcast.app.controller.ShowAdapter
import io.bidcast.app.databinding.FragmentPrepareYourShowBinding
import io.bidcast.app.interfaces.RecyclerClicks
import io.bidcast.app.model.ShowModel
import io.bidcast.app.ui.dashboard.DashViewModel
import io.bidcast.app.utils.ids
import io.bidcast.app.utils.toScheduleShow

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
                ShowModel("Schedule your first show",false,true,"Pick a date and time for your live show"),
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

                    0->{
                        startActivity(mCtx.toScheduleShow(from = "tutorial"))
                    }
                    1->{
                        findNavController().navigate(ids.goToShowTipsFragment)
                    }

                }




            }

        })

        bind.recycler.adapter = adapter



    }

}