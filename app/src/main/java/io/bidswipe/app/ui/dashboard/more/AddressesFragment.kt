package io.bidswipe.app.ui.dashboard.more

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.ShippingAddressAdapter
import io.bidswipe.app.databinding.FragmentAddressesBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.utils.finish

class AddressesFragment : BaseFragment<MoreViewModel, FragmentAddressesBinding>() {
    override fun getModel(): Class<MoreViewModel>   = MoreViewModel::class.java

    override fun getBind(
        inflater: LayoutInflater,
        view: ViewGroup?
    ) = FragmentAddressesBinding.inflate(inflater,view,false)

    private var addressList = mutableListOf("","","")
    private lateinit var shippingAddressAdapter: ShippingAddressAdapter

    private val mClick = object : RecyclerClicks{
        override fun itemClick(pos: Int, status: String?) {

        }

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bind.header.onBackClick {
            finish()
        }

        shippingAddressAdapter= ShippingAddressAdapter(addressList,mClick)

        bind.addressRecycler.adapter = shippingAddressAdapter

    }

}