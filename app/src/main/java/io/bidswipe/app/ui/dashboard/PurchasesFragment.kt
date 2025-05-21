package io.bidswipe.app.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.PurchasesAdapter
import io.bidswipe.app.databinding.FragmentPurchasesBinding
import io.bidswipe.app.interfaces.RecyclerClicks

class PurchasesFragment : BaseFragment<DashViewModel, FragmentPurchasesBinding>() {

    override fun getModel(): Class<DashViewModel> = DashViewModel::class.java

    override fun getBind(inflater: LayoutInflater, view: ViewGroup?) = FragmentPurchasesBinding.inflate(inflater,view,false)

    private lateinit var purchasesAdapter : PurchasesAdapter
    private var mList = mutableListOf("","","","")

    private var mClick = object : RecyclerClicks {
        override fun itemClick(pos: Int, status: String?) {
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        purchasesAdapter = PurchasesAdapter(mList,mClick,"")

        bind.recycler.adapter = purchasesAdapter


    }

}