package io.bidswipe.app.ui.more

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.CouponsAdapter
import io.bidswipe.app.databinding.FragmentCouponsBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.utils.finish

class CouponsFragment : BaseFragment<MoreViewModel, FragmentCouponsBinding>(){
    override fun getModel()= MoreViewModel::class.java

    override fun getBind(
        inflater: LayoutInflater,
        view: ViewGroup?
    )= FragmentCouponsBinding.inflate(inflater,view,false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bind.header.onBackClick {
            finish()
        }

        bind.couponRecycler.adapter= CouponsAdapter(mutableListOf("","",""),object : RecyclerClicks{
            override fun itemClick(pos: Int, status: String?) {

            }
        })



    }



}
