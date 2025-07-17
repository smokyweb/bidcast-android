package io.bidswipe.app.ui.dashboard.sellerHub

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.TipsAdapter
import io.bidswipe.app.databinding.FragmentTipsBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.utils.finish

class TipsFragment : BaseFragment<SellerHubViewModel, FragmentTipsBinding>() {
    override fun getModel(): Class<SellerHubViewModel> = SellerHubViewModel::class.java

    override fun getBind(
        inflater: LayoutInflater,
        view: ViewGroup?
    ) = FragmentTipsBinding.inflate(inflater,view,false)

    private var tipsList = mutableListOf("","","")

    private lateinit var tipsAdapter: TipsAdapter

    private val mClick = object : RecyclerClicks{
        override fun itemClick(pos: Int, status: String?) {
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        onBackPressed { finish() }

        bind.header.onBackClick {
            finish()
        }

        tipsAdapter = TipsAdapter(tipsList,mClick)

        bind.recycler.adapter = tipsAdapter


    }

}