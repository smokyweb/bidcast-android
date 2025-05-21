package io.bidswipe.app.ui.dashboard.product

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.ShippingUpdateAdapter
import io.bidswipe.app.databinding.FragmentOrderStatusBinding
import io.bidswipe.app.utils.finish

class OrderStatusFragment : BaseFragment<ProductViewModel, FragmentOrderStatusBinding>() {

    override fun getModel(): Class<ProductViewModel> = ProductViewModel::class.java

    override fun getBind(
        inflater: LayoutInflater,
        view: ViewGroup?
    ) = FragmentOrderStatusBinding.inflate(inflater,view,false)

    private val statusItems = mutableListOf("","")

    private lateinit var adapter : ShippingUpdateAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bind.header.onBackClick {
            finish()
        }

        adapter = ShippingUpdateAdapter(statusItems)

        bind.shippingRecycler.adapter = adapter

        bind.homeBtn.setOnClickListener {
            finish()
        }
    }

}