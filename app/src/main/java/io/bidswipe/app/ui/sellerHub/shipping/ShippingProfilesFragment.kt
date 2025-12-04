package io.bidswipe.app.ui.sellerHub.shipping

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.DomesticShipmentAdapter
import io.bidswipe.app.controller.ShippingProfileAdapter
import io.bidswipe.app.databinding.FragmentFreePickupBinding
import io.bidswipe.app.databinding.FragmentShippingProfilesBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.ui.sellerHub.SellerHubViewModel
import io.bidswipe.app.utils.animatedNav
import io.bidswipe.app.utils.setHapticClickListener

class ShippingProfilesFragment : BaseFragment<SellerHubViewModel, FragmentShippingProfilesBinding>() {
    override fun getModel(): Class<SellerHubViewModel> = SellerHubViewModel::class.java

    override fun getBind(
        inflater: LayoutInflater,
        view: ViewGroup?,
    ) = FragmentShippingProfilesBinding.inflate(inflater, view, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bind.header.onBackClick {
            findNavController().popBackStack()
        }

        bind.create.setHapticClickListener {
            findNavController().animatedNav(R.id.toCreateShippingProfile)
        }


        val adapter = ShippingProfileAdapter(mutableListOf("","",""), object : RecyclerClicks {
            override fun itemClick(pos: Int, status: String?) {

            }
        })

        bind.shippingProfiles.adapter = adapter

    }
}