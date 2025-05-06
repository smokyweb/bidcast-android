package io.bidcast.app.ui.dashboard

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.bidcast.app.R
import io.bidcast.app.base.BaseFragment
import io.bidcast.app.controller.BidsAdapter
import io.bidcast.app.controller.OffersAdapter
import io.bidcast.app.databinding.FragmentOfferBinding
import io.bidcast.app.interfaces.RecyclerClicks

class OfferFragment : BaseFragment<DashViewModel,FragmentOfferBinding>() {

    override fun getModel(): Class<DashViewModel> = DashViewModel::class.java

    override fun getBind(inflater: LayoutInflater, view: ViewGroup?) = FragmentOfferBinding.inflate(inflater,view,false)

    private lateinit var offersAdapter : OffersAdapter
    private var mList = mutableListOf("","","","")

    private var mClick = object : RecyclerClicks {
        override fun viewClick(pos: Int) {
        }
        override fun itemClick(pos: Int, status: String) {
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        offersAdapter = OffersAdapter(mList,mClick)

        bind.recycler.adapter = offersAdapter


    }

}