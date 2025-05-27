package io.bidswipe.app.ui.dashboard.sellerHub

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.BenifitsAdapter
import io.bidswipe.app.controller.SellAdapter
import io.bidswipe.app.databinding.FragmentAffiliateProgramBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.SellModel
import io.bidswipe.app.utils.finish

class AffiliateProgramFragment : BaseFragment<SellerHubViewModel, FragmentAffiliateProgramBinding>() {

    override fun getModel(): Class<SellerHubViewModel> = SellerHubViewModel::class.java

    override fun getBind(
        inflater: LayoutInflater,
        view: ViewGroup?
    ) = FragmentAffiliateProgramBinding.inflate(inflater,view,false)

    private val mList = mutableListOf<SellModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bind.header.onBackClick {
            finish()
        }

        mList.clear()
        mList.addAll(
            listOf(
                SellModel(R.drawable.ic_dollar,R.color.secondaryContainer,"Earn $100 Reward","When your referral makes their first sale"),
                SellModel(R.drawable.ic_gift,R.color.tertiaryContainer,"They Get Bonus Too!","Your referrals get $100 matched earnings in their first week")
            )
        )

        val adapter = SellAdapter(mList = mList, "affiliate",object: RecyclerClicks {

            override fun itemClick(pos: Int, status: String?) {

            }

        })

        bind.recycler.adapter = adapter

    }

}