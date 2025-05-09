package io.bidswipe.app.ui.dashboard

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.BidsAdapter
import io.bidswipe.app.databinding.FragmentBidsBinding
import io.bidswipe.app.interfaces.RecyclerClicks

class BidsFragment : BaseFragment<DashViewModel,FragmentBidsBinding>() {

    override fun getModel(): Class<DashViewModel> = DashViewModel::class.java

    override fun getBind(inflater: LayoutInflater, view: ViewGroup?) = FragmentBidsBinding.inflate(inflater,view, false)

    private lateinit var bidsAdapter : BidsAdapter
    private var mList = mutableListOf("","","","")

    private var mClick = object : RecyclerClicks {
        override fun viewClick(pos: Int) {
        }
        override fun itemClick(pos: Int, status: String) {
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bidsAdapter = BidsAdapter(mList,mClick)

        bind.recycler.adapter = bidsAdapter



    }

}