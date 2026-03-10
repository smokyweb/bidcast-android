package io.bidswipe.app.ui.sellerHub.shipping

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.DomesticShipmentAdapter
import io.bidswipe.app.controller.DomesticShipmentModel
import io.bidswipe.app.databinding.FragmentShippingCostsBinding
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.ui.sellerHub.SellerHubViewModel
import io.bidswipe.app.utils.hideKeyboard
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.setHapticClickListener

class ShippingCostsFragment : BaseFragment<SellerHubViewModel, FragmentShippingCostsBinding>() {

    override fun getModel(): Class<SellerHubViewModel> = SellerHubViewModel::class.java

    override fun getBind(
        inflater: LayoutInflater,
        view: ViewGroup?,
    ) = FragmentShippingCostsBinding.inflate(inflater, view, false)

    val costList = mutableListOf(
        DomesticShipmentModel(
            null,
            "Seller pays all shipping costs",
            "This is only applies to domestic orders. Buyers have to pay for shipping on international orders."
        ),

        DomesticShipmentModel(
            null,
            "Buyers pay all shipping costs",
        ),
        DomesticShipmentModel(
            null,
            "Buyer pays up to a set shipping cost",
            "Set a maximum shipping cost buyers will pay for unlimited orders within your show."
        ),
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (viewModel.shippingDetails != null) {
            costList.find { it.title == viewModel.shippingDetails?.domesticShipmentSetting?.shippingCosts }?.isSelected = true
            bind.alsoApplyScheduleShow.isChecked = viewModel.shippingDetails?.domesticShipmentSetting?.shippingCostsAlsoApplyToScheduledShows == true
            if (costList.last().isSelected) {
                bind.expandView.isExpanded = true
            } else {
                bind.expandView.isExpanded = false
                bind.price.setText("")
            }
        } else {
            costList.first().isSelected = true
            bind.expandView.isExpanded = false
            bind.price.setText("")
        }

        bind.header.onBackClick {
            findNavController().popBackStack()
        }

        bind.root.setHapticClickListener {
            hideKeyboard(it)
        }

        val costAdapter = DomesticShipmentAdapter(costList, object : RecyclerClicks {
            override fun itemClick(pos: Int, status: String?) {
                costList.forEachIndexed { index, item ->
                    item.isSelected = index == pos
                    (bind.costRecycler.adapter as DomesticShipmentAdapter).notifyItemChanged(
                        index,
                        item
                    )
                }

                if (pos == costList.lastIndex) {
                    bind.expandView.isExpanded = true
                } else {
                    bind.expandView.isExpanded = false
                    bind.price.setText("")
                }
            }
        })

        bind.costRecycler.adapter = costAdapter

        bind.save.setOnClickListener {
            if (costList.last().isSelected) {
                if (bind.price.text.toString().isEmpty()) {
                    errorToast("Please enter price")
                    return@setOnClickListener
                }
            }

            bind.loader.isVisible = true
            viewModel.saveShippingCosts(
                costList.firstOrNull { it.isSelected }?.title.toString().request(),
                bind.alsoApplyScheduleShow.isChecked.toString().request()
            )

        }

        viewModel.saveShippingCostsRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false
                    viewModel.saveShippingCostsRepo.value = null
                    findNavController().popBackStack()
                }

                is Resource.Error -> {
                    bind.loader.isVisible = false
                    viewModel.saveShippingCostsRepo.value = null
                    it.parse(mCtx, TAG)
                }

                else -> {}
            }
        }
    }
}