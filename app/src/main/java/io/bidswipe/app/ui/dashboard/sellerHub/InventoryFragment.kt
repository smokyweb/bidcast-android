package io.bidswipe.app.ui.dashboard.sellerHub

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.InventoryAdapter
import io.bidswipe.app.databinding.FragmentInventoryBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.ui.dashboard.DashActivity
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.toListProduct

class InventoryFragment : BaseFragment<SellerHubViewModel,FragmentInventoryBinding>() {
    override fun getModel(): Class<SellerHubViewModel> = SellerHubViewModel::class.java

    override fun getBind(inflater: LayoutInflater, view: ViewGroup?) = FragmentInventoryBinding.inflate(inflater,view,false)

    private var itemList  = mutableListOf("","","","","")

    private lateinit var adapter : InventoryAdapter

    private val mClick = object : RecyclerClicks{
     
        override fun itemClick(pos: Int, status: String?) {
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bind.header.onBackClick{
            finish()
        }

        adapter = InventoryAdapter(itemList,mClick)

        bind.recycler.adapter = adapter

        bind.addNewProduct.setOnClickListener {
            startActivity(mCtx.toListProduct())
        }


    }

}