package io.bidswipe.app.ui.dashboard.sellerHub

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.BenefitsAdapter
import io.bidswipe.app.controller.RequirementAdapter
import io.bidswipe.app.databinding.FragmentPremierShopBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.SellModel
import io.bidswipe.app.utils.finish


class PremierShopFragment : BaseFragment<SellerHubViewModel, FragmentPremierShopBinding>() {
    override fun getModel(): Class<SellerHubViewModel>  = SellerHubViewModel::class.java

    override fun getBind(
        inflater: LayoutInflater,
        view: ViewGroup?
    ) = FragmentPremierShopBinding.inflate(inflater,view,false)

    private var gridList = mutableListOf<SellModel>()
    private var reqList = mutableListOf("","","","")
    private lateinit var gridAdapter: BenefitsAdapter
    private lateinit var reqAdapter: RequirementAdapter

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
        gridList.add(SellModel(R.drawable.ic_percent,0,"Reduced Commission","Pay only 5% commission on sales"))
        gridList.add(SellModel(R.drawable.ic_finger_print,0,"Unique Profile ID", "Custom URL for your shop"))
        gridList.add(SellModel(R.drawable.ic_speaker,0,"Marketing Boost", "Priority in search result"))
        gridList.add(SellModel(R.drawable.ic_support,0,"Priority Support", "24/7 dedicated assistance"))

        gridAdapter= BenefitsAdapter(gridList,mClick)
        bind.gridRecycler.adapter = gridAdapter

        reqAdapter = RequirementAdapter(reqList,mClick)
        bind.requirementRecycler.adapter = reqAdapter


    }

}