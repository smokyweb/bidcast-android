package io.bidswipe.app.ui.sellerHub.shipping

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import io.bidswipe.app.App
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.databinding.FragmentFreePickupBinding
import io.bidswipe.app.network.Resource
import io.bidswipe.app.ui.sellerHub.SellerHubViewModel
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.setHapticClickListener
import io.bidswipe.app.utils.value

class FreePickupFragment : BaseFragment<SellerHubViewModel, FragmentFreePickupBinding>() {
    override fun getModel(): Class<SellerHubViewModel> = SellerHubViewModel::class.java

    override fun getBind(
        inflater: LayoutInflater,
        view: ViewGroup?,
    ) = FragmentFreePickupBinding.inflate(inflater, view, false)

    var status: Boolean? = false
    var selectedShippingAddressId = ""
    var shippingAddress = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if(viewModel.shippingDetails!=null){
            status = viewModel.shippingDetails?.freePickup==true
            bind.freePickup.isChecked=viewModel.shippingDetails?.freePickup==true
            if (viewModel.shippingDetails?.shippingAddress != null) {
                shippingAddress = Utils.formatAddress(
                    name = viewModel.shippingDetails?.shippingAddress?.name,
                    streetAddress = viewModel.shippingDetails?.shippingAddress?.streetAddress,
                    addressLine2 = viewModel.shippingDetails?.shippingAddress?.addressLine2,
                    city = viewModel.shippingDetails?.shippingAddress?.city,
                    state = viewModel.shippingDetails?.shippingAddress?.state,
                    pincode = viewModel.shippingDetails?.shippingAddress?.pincode,
                )
            }
            bind.instruction.setText(viewModel.shippingDetails?.instruction )
        }else{
            bind.freePickup.isChecked=status==false
        }

        bind.header.onBackClick {
            findNavController().popBackStack()
        }

        bind.freePickup.isChecked = status == true
        bind.expandView.isExpanded = status == true

        if (shippingAddress.isNotEmpty()) {
            bind.pickupaddress.setText(shippingAddress)
        }

        bind.freePickup.setOnCheckedChangeListener { _, checked ->
            bind.expandView.isExpanded = checked
        }

        bind.pickupaddress.setOnClickListener {
            openAddressSheet()
        }

        bind.pickupaddressBox.setOnClickListener {
            openAddressSheet()
        }

        bind.save.setHapticClickListener {
            callAPI()
        }

        viewModel.settingsStoreRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false
                    viewModel.settingsStoreRepo.value = null
                    finish()
                }

                is Resource.Error -> {
                    bind.loader.isVisible = false
                    viewModel.settingsStoreRepo.value = null
                    it.parse(mCtx, TAG)
                }

                else -> {}
            }
        }

    }

    private fun callAPI() {
        bind.loader.isVisible = true
        App.getProfile()
        viewModel.settingsStore(
            freeShipping = if (bind.freePickup.isChecked) "1".request() else "0".request(),
            shippingAddressId = if (bind.freePickup.isChecked) selectedShippingAddressId.request() else null,
            instruction = if (bind.freePickup.isChecked) bind.instruction.value().request() else null,
        )
    }

    fun openAddressSheet() {
        val bottomSheetFragment = SelectAddressFragment { address ->
            selectedShippingAddressId = address?.id.toString()
            val addressShow = Utils.formatAddress(
                name = address?.name,
                streetAddress = address?.streetAddress,
                addressLine2 = address?.addressLine2,
                city = address?.city,
                state = address?.state,
                pincode = address?.pincode,
            )

            bind.pickupaddress.setText(addressShow)
        }
        bottomSheetFragment.show(parentFragmentManager, "SELECT_ADDRESS")
    }

}