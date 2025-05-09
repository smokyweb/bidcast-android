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
                ShowModel("Add Products to your show",false,false,"Select products you'll be featuring"),
                ShowModel("Rehearse going live",false,false,"Practice with our simulator"),
                ShowModel("Bring in buyers",false,false,"Share your show with potential buyer"),
                ShowModel("Preview show and go live",false,true,"Final check and start streaming")
            )

        )

        bind.stepProgress.max = showList.size
        bind.stepProgress.setProgress(1)

        val adapter = ShowAdapter(mList = showList, "getStarted",object: RecyclerClicks {
            override fun viewClick(pos: Int) {

                bind.stepProgress.setProgress(pos+1)

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
                    3 -> {
                        findNavController().navigate(
                            ids.goToShowTipsFragment,
                            bundleOf("type" to "bringInBuyers")
                        )
                    }

                }




            }

        })

        bind.recycler.adapter = adapter



    }

}