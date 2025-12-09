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

class FreePickupFragment : BaseFragment<SellerHubViewModel, FragmentFreePickupBinding>() {
    override fun getModel(): Class<SellerHubViewModel> = SellerHubViewModel::class.java

    override fun getBind(
        inflater: LayoutInflater,
        view: ViewGroup?,
    ) = FragmentFreePickupBinding.inflate(inflater, view, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bind.header.onBackClick {
            findNavController().popBackStack()
        }

        bind.loader.isVisible = true
        viewModel.settingsList()
        viewModel.settingsListRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false
                    viewModel.settingsListRepo.value = null

                    val mData = it.value.data

                    bind.freePickup.isChecked = mData?.freeShipping == true
                }

                is Resource.Error -> {
                    bind.loader.isVisible = false
                    viewModel.settingsListRepo.value = null
                    it.parse(mCtx, TAG)
                }

                else -> {}

            }
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

        bind.save.setHapticClickListener {
            callAPI()
        }

    }

    private fun callAPI() {
        bind.loader.isVisible = true
        App.getProfile()
        viewModel.settingsStore(
            freeShipping = if (bind.freePickup.isChecked) "1".request() else "0".request()
        )
    }

}