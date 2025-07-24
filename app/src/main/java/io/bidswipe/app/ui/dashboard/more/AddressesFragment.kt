package io.bidswipe.app.ui.dashboard.more

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.ShippingAddressAdapter
import io.bidswipe.app.databinding.FragmentAddressesBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetShippingAddressResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.ids
import io.bidswipe.app.utils.parse

class AddressesFragment : BaseFragment<MoreViewModel, FragmentAddressesBinding>() {
    override fun getModel(): Class<MoreViewModel>   = MoreViewModel::class.java

    override fun getBind(
        inflater: LayoutInflater,
        view: ViewGroup?
    ) = FragmentAddressesBinding.inflate(inflater,view,false)

    private var addressList = mutableListOf<GetShippingAddressResponse.Data?>()
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

        bind.addNewAddress.setOnClickListener {
            findNavController().navigate(ids.myAddressToAddShippingAddressFragment)
        }

        bind.loader.isVisible = true

        viewModel.getShippingAddress()

        viewModel.getShippingAddressRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false

                    val mData = it.value.data

                    addressList.clear()

                    if (mData?.isNotEmpty() == true){
                        bind.noData.isVisible = false
                        bind.addressRecycler.isVisible = true
                        addressList.addAll(mData)
                    }else{
                        bind.noData.isVisible = true
                    }

                    shippingAddressAdapter.notifyDataSetChanged()

                }

                is Resource.Error -> {
                    bind.loader.isVisible = false

                    if (it.isNetworkError) {
                        errorToast(getString(R.string.no_internet))
                    } else {
                        it.parse(mCtx, TAG, object : AlertClicks {
                            override fun primaryClick(dialog: AppBottomSheet) {
                                dialog.dismiss()

                            }

                            override fun secondaryClick(dialog: AppBottomSheet) {
                                dialog.dismiss()

                            }
                        })
                    }
                }

                else -> {}

            }
        }


    }

}