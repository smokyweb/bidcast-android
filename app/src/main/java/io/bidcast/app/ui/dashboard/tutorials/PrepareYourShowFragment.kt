package io.bidcast.app.ui.dashboard.tutorials

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import io.bidcast.app.R
import io.bidcast.app.base.BaseFragment
import io.bidcast.app.controller.SellAdapter
import io.bidcast.app.controller.ShowAdapter
import io.bidcast.app.databinding.FragmentPrepareYourShowBinding
import io.bidcast.app.interfaces.RecyclerClicks
import io.bidcast.app.model.SellModel
import io.bidcast.app.model.ShowModel
import io.bidcast.app.ui.dashboard.DashViewModel

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
                showList[pos].selected=true
            }

            override fun itemClick(pos: Int, status: String) {

            }

        })

        bind.recycler.adapter = adapter



    }

}