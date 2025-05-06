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
import io.bidcast.app.databinding.FragmentGetStartedBinding
import io.bidcast.app.interfaces.RecyclerClicks
import io.bidcast.app.model.SellModel
import io.bidcast.app.ui.dashboard.DashViewModel
import io.bidcast.app.utils.Alerts
import io.bidcast.app.utils.finish
import io.bidcast.app.utils.ids
import io.bidcast.app.utils.toListProduct

class GetStartedFragment : BaseFragment<DashViewModel,FragmentGetStartedBinding>() {
    override fun getModel(): Class<DashViewModel> = DashViewModel::class.java

    override fun getBind(inflater: LayoutInflater, view: ViewGroup?) = FragmentGetStartedBinding.inflate(inflater,view,false)

    private var exploreList = mutableListOf<SellModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bind.header.onBackClick{
            finish()
        }

        exploreList.clear()
        exploreList.addAll(
            listOf(
                SellModel(R.drawable.ic_hand_shake,R.color.secondaryContainer,"Honor Purchases & Freebies","Fulfill all orders promptly and honor your commitments"),
                SellModel(R.drawable.ic_block,R.color.tertiaryContainer,"Do Not Sell Counterfeits","Only sell authentic and legitimate products"),
                SellModel(R.drawable.ic_checked_tag,R.color.successContainer,"Do Not Lie About Items","Provide accurate descriptions and images"),
                SellModel(R.drawable.ic_vehicle,R.color.successContainer,"Ship Quickly & Safely","Use appropriate packaging and ship within 3 days")
            )
        )

        val adapter = SellAdapter(mList = exploreList, "getStarted",object: RecyclerClicks {
            override fun viewClick(pos: Int) {

            }

            override fun itemClick(pos: Int, status: String) {

            }

        })

        bind.recycler.adapter = adapter

        bind.continueBtn.setOnClickListener {
            if (bind.checkBox.isChecked.not()){

                Alerts.error(mCtx,"Please agree with guidlines")
            }else{
                findNavController().navigate(ids.goToPlayerFragment)
            }
        }



    }

}