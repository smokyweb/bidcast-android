package io.bidswipe.app.ui.sellerHub.shipping

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.ShippingProfileAdapter
import io.bidswipe.app.databinding.FragmentShippingProfilesBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetShippingProfilesResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.sellerHub.SellerHubViewModel
import io.bidswipe.app.utils.animatedNav
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.setHapticClickListener

class ShippingProfilesFragment : BaseFragment<SellerHubViewModel, FragmentShippingProfilesBinding>() {
    override fun getModel(): Class<SellerHubViewModel> = SellerHubViewModel::class.java

    override fun getBind(
        inflater: LayoutInflater,
        view: ViewGroup?,
    ) = FragmentShippingProfilesBinding.inflate(inflater, view, false)

    private lateinit var profileAdapter : ShippingProfileAdapter
    private var profiles = mutableListOf<GetShippingProfilesResponse.Data?>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bind.header.onBackClick {
            findNavController().popBackStack()
        }

        bind.create.setHapticClickListener {
            findNavController().animatedNav(R.id.toCreateShippingProfile)
        }


        profileAdapter = ShippingProfileAdapter(profiles, object : RecyclerClicks {
            override fun itemClick(pos: Int, status: String?) {

            }
        })

        bind.shippingProfiles.adapter = profileAdapter

        bind.loader.isVisible = true

        viewModel.getShippingProfile()

        viewModel.getShippingProfileRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false

                    val mData = it.value.data

                    if(mData != null){
                        profiles.clear()
                        profiles.addAll(mData)
                    }

                    if (profiles.isNotEmpty()){
                        bind.noData.isVisible = false
                        bind.shippingProfiles.isVisible = true

                    }else{
                        bind.noData.isVisible = true
                        bind.shippingProfiles.isVisible = false
                    }

                    profileAdapter.notifyDataSetChanged()

                    log( mData.toString())

                }

                is Resource.Error -> {
                    bind.loader.isVisible = false

                    it.parse(mCtx, TAG, object : AlertClicks {
                        override fun primaryClick(dialog: AppBottomSheet) {
                            dialog.dismiss()

                        }

                        override fun secondaryClick(dialog: AppBottomSheet) {
                            dialog.dismiss()

                        }
                    })

                }

                else -> {}

            }

        }

    }
}