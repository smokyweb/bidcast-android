package io.bidswipe.app.ui.dashboard.scheduleShow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.WeightAdapter
import io.bidswipe.app.databinding.FragmentProductWeightBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.ids
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import java.io.File

class ProductWeightFragment : BaseFragment<ScheduleShowViewModel, FragmentProductWeightBinding>() {
    override fun getModel(): Class<ScheduleShowViewModel> = ScheduleShowViewModel::class.java

    override fun getBind(inflater: LayoutInflater, view: ViewGroup?) =
        FragmentProductWeightBinding.inflate(inflater, view, false)

    private var mList = mutableListOf("", "", "", "", "", "")

    private lateinit var adapter: WeightAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val productData = arguments

        bind.header.onBackClick {
            findNavController().popBackStack()
        }

        adapter = WeightAdapter(mList)

        bind.recycler.adapter = adapter

        bind.continueBtn.setOnClickListener {
            val categoryId = productData?.getString("categoryId") ?: ""
            val title = productData?.getString("title") ?: ""
            val description = productData?.getString("description") ?: ""
            val quantity = productData?.getInt("quantity") ?: 1
            val price = productData?.getString("price") ?: 1
            val imagePaths = productData?.getString("imagePaths")
            val imageFiles = imagePaths?.split(",")?.map { File(it) }
            val imageParts = imageFiles?.map {
                Utils.imagePart("images[]", it.name, it)
            }

            // Call the API
            viewModel.storeProduct(
                categoryId = categoryId.request(),
                title = title.request(),
                description = description.request(),
                quantity = quantity.toString().request(),
                pricing = price.toString().request(),
                flashSale = "0".request(),
                acceptOffers = "0".request(),
                reserveForLive = "0".request(),
                shippingProfileId = "1".request(),
                status = "active".request(),
                productImages = imageParts,
            )
        }

        viewModel.storeProductRepo.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Success -> {
                    findNavController().navigate(ids.addProductFragment)
                }

                is Resource.Error -> {
                    if (resource.isNetworkError) {
                        errorToast(getString(R.string.no_internet))
                    } else {
                        resource.parse(mCtx, TAG, object : AlertClicks {
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