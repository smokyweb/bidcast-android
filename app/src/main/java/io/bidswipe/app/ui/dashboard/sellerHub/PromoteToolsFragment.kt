package io.bidswipe.app.ui.dashboard.sellerHub

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.AnalyticsGridAdapter
import io.bidswipe.app.databinding.FragmentPromoteToolsBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.SellModel
import io.bidswipe.app.utils.finish

class PromoteToolsFragment : BaseFragment<SellerHubViewModel, FragmentPromoteToolsBinding>() {
    override fun getModel(): Class<SellerHubViewModel> = SellerHubViewModel::class.java

    override fun getBind(
        inflater: LayoutInflater,
        view: ViewGroup?
    ) = FragmentPromoteToolsBinding.inflate(inflater,view,false)

    private var gridList = mutableListOf<SellModel>()
    private var reqList = mutableListOf("","","","")
    private lateinit var gridAdapter: AnalyticsGridAdapter

    private val mClick = object : RecyclerClicks{
        override fun itemClick(pos: Int, status: String?) {
        }

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bind.header.onBackClick {
            finish()
        }

        gridList.clear()
        gridList.add(SellModel(R.drawable.ic_share,0,"Share","Share your show on social media"))
        gridList.add(SellModel(R.drawable.ic_ads,0,"Ads", "Create ads for your shows"))
        gridList.add(SellModel(R.drawable.ic_people,0,"Audience", "Grow your audience"))
        gridList.add(SellModel(R.drawable.ic_graph,0,"Analytics", "Track performance"))

        gridAdapter= AnalyticsGridAdapter(gridList,mClick)
        bind.gridRecycler.adapter = gridAdapter


    }

}