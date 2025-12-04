package io.bidswipe.app.ui.sellerHub.shipping

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.DomesticShipmentAdapter
import io.bidswipe.app.controller.DomesticShipmentModel
import io.bidswipe.app.databinding.FragmentDomesticShipmentsBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.ui.sellerHub.SellerHubViewModel
import io.bidswipe.app.utils.Const

class DomesticShipmentsFragment :
    BaseFragment<SellerHubViewModel, FragmentDomesticShipmentsBinding>() {
    override fun getModel(): Class<SellerHubViewModel> = SellerHubViewModel::class.java

    override fun getBind(
        inflater: LayoutInflater,
        view: ViewGroup?,
    ) = FragmentDomesticShipmentsBinding.inflate(inflater, view, false)


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bind.header.onBackClick {
            findNavController().popBackStack()
        }

        val shipmentList = mutableListOf(
            DomesticShipmentModel(
                R.drawable.placeholder_rect,
                "USPS Priority Mail",
                "Arrives in 1-3 business days. Best for time-sensitive shipments."
            ),
            DomesticShipmentModel(
                R.drawable.placeholder_rect,
                "USPS Flat-Rate Boxes",
                "Ships at a fixed rate within the United States, regardless of weight or distance.",
                Const.BASE_URL
            )
        )

        val shipment5List = mutableListOf(
            DomesticShipmentModel(
                R.drawable.placeholder_rect,
                "USPS Priority Mail",
                "Arrives in 1-3 business days. Best for time-sensitive shipments."
            ),
            DomesticShipmentModel(
                R.drawable.placeholder_rect,
                "USPS Flat-Rate Boxes",
                "Ships at a fixed rate within the United States, regardless of weight or distance.",
                Const.BASE_URL
            ),
            DomesticShipmentModel(
                R.drawable.placeholder_rect,
                "USPS Ground Advantage",
                "Best for shipping heavier items that aren't time-sensitive.",
                Const.BASE_URL
            )
        )


        val domestic15Adapter = DomesticShipmentAdapter(shipmentList, object : RecyclerClicks {
            override fun itemClick(pos: Int, status: String?) {
                if (status==null) {
                    shipmentList.forEachIndexed { index, item ->
                        item.isSelected = index == pos
                        (bind.domestic15Recycler.adapter as DomesticShipmentAdapter).notifyItemChanged(
                            index,item
                        )
                    }
                } else {
                    //Add code for link
                }
            }
        })

        val domestic5Adapter = DomesticShipmentAdapter(shipment5List, object : RecyclerClicks {
            override fun itemClick(pos: Int, status: String?) {
                if (status==null) {
                    shipment5List.forEachIndexed { index, item ->
                        item.isSelected = index == pos
                        (bind.domestic5Recycler.adapter as DomesticShipmentAdapter).notifyItemChanged(
                           index ,item
                        )
                    }
                } else {
                    //Add code for link
                }
            }
        })

        bind.domestic15Recycler.adapter = domestic15Adapter
        bind.domestic5Recycler.adapter = domestic5Adapter

    }
}