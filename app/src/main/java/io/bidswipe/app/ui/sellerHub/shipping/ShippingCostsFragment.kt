package io.bidswipe.app.ui.sellerHub.shipping

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.DomesticShipmentAdapter
import io.bidswipe.app.controller.ShippingProfileAdapter
import io.bidswipe.app.controller.DomesticShipmentModel
import io.bidswipe.app.databinding.FragmentShippingCostsBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.ui.sellerHub.SellerHubViewModel

class ShippingCostsFragment :
    BaseFragment<SellerHubViewModel, FragmentShippingCostsBinding>() {
    override fun getModel(): Class<SellerHubViewModel> = SellerHubViewModel::class.java

    override fun getBind(
        inflater: LayoutInflater,
        view: ViewGroup?,
    ) = FragmentShippingCostsBinding.inflate(inflater, view, false)


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bind.header.onBackClick {
            findNavController().popBackStack()
        }

        val costList = mutableListOf(
            DomesticShipmentModel(
                null,
                "Seller pays all shipping costs",
                "This is only applies to domestic orders. Buyers have to pay for shipping on international orders."
            ),
            DomesticShipmentModel(
                null,
                "Buyer pays up to a set shipping cost",
                "Set a maximum shipping cost buyers will pay for unlimited orders within your show."
            ),
            DomesticShipmentModel(
                null,
                "Buyers pay all shipping costs",
            )
        )


        val costAdapter = DomesticShipmentAdapter(costList, object : RecyclerClicks {
            override fun itemClick(pos: Int, status: String?) {
                costList.forEachIndexed { index, item ->
                    item.isSelected = index == pos
                    (bind.costRecycler.adapter as DomesticShipmentAdapter).notifyItemChanged(
                        index,
                        item
                    )
                }
            }
        })

        bind.costRecycler.adapter = costAdapter

        bind.save.setOnClickListener {
            costList.firstOrNull { it.isSelected }
        }

    }
}