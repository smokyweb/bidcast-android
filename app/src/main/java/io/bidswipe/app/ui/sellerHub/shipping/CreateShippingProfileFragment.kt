package io.bidswipe.app.ui.sellerHub.shipping

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.databinding.FragmentCreateShippingProfileBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.sellerHub.SellerHubViewModel
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.hideKeyboard
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.setHapticClickListener
import io.bidswipe.app.utils.value

class CreateShippingProfileFragment : BaseFragment<SellerHubViewModel, FragmentCreateShippingProfileBinding>() {

    override fun getModel(): Class<SellerHubViewModel> = SellerHubViewModel::class.java

    override fun getBind(
        inflater: LayoutInflater,
        view: ViewGroup?,
    ) = FragmentCreateShippingProfileBinding.inflate(inflater, view, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val type = activity?.intent?.getStringExtra("slug").toString()

        val from = arguments?.getString("from")


        if (from == "edit") {
            bind.header.setHeaderText("Edit Shipping Profile")
            bind.save.text = "Update"

            bind.name.setText(viewModel.selectedShippingProfile?.name.toString())
            bind.weight.setText(viewModel.selectedShippingProfile?.weight.toString())
            bind.weightUnits.setText(viewModel.selectedShippingProfile?.size.toString())

            if (viewModel.selectedShippingProfile?.additionalWeight == true) {
                bind.additionalWeight.isChecked = true
            }

            if (viewModel.selectedShippingProfile?.maxItems == true) {
                bind.maxPackage.isChecked = true
            }

        }


        bind.header.onBackClick {
            if (type == "createShippingProfile") {
                finish()
            } else {
                findNavController().popBackStack()
            }
        }

        bind.layout.setHapticClickListener {
            hideKeyboard(it)
        }

        val proCategoryAdapter = ArrayAdapter(
            mCtx,
            android.R.layout.simple_list_item_1,
            Const.weightScales
        )

        bind.weightUnits.setAdapter(proCategoryAdapter)
        val proDrawable = ContextCompat.getDrawable(mCtx, R.drawable.card_8)
        bind.weightUnits.setDropDownBackgroundDrawable(proDrawable)

        bind.weightUnits.setHapticClickListener {
            hideKeyboard(it)
            bind.weightUnits.showDropDown()
        }

        bind.incrementalWeightUnits.setAdapter(proCategoryAdapter)
        bind.incrementalWeightUnits.setDropDownBackgroundDrawable(proDrawable)

        bind.incrementalWeightUnits.setHapticClickListener {
            hideKeyboard(it)
            bind.incrementalWeightUnits.showDropDown()
        }

        val dimensionScaleAdapter = ArrayAdapter(
            mCtx,
            android.R.layout.simple_list_item_1,
            Const.dimensionScales
        )

        bind.dimensionUnits.setAdapter(dimensionScaleAdapter)
        bind.dimensionUnits.setDropDownBackgroundDrawable(proDrawable)

        bind.dimensionUnits.setHapticClickListener {
            hideKeyboard(it)
            bind.dimensionUnits.showDropDown()
        }

        bind.boxDimensions.setAdapter(dimensionScaleAdapter)
        bind.boxDimensions.setDropDownBackgroundDrawable(proDrawable)

        bind.boxDimensions.setHapticClickListener {
            hideKeyboard(it)
            bind.boxDimensions.showDropDown()
        }

        bind.boxDimensions.setOnItemClickListener { _, _, position, _ ->
            bind.height.isEnabled = false
            bind.width.isEnabled = false
            bind.length.isEnabled = false
            bind.dimensionUnits.isEnabled = false
        }

        bind.maxPackage.setOnCheckedChangeListener { _, checked ->
            bind.maxItemsExpand.isExpanded = checked

            bind.boxDimensions.setText("Custom",false)
            bind.height.setText("12.00")
            bind.length.setText("12.00")
            bind.width.setText("12.00")
            bind.dimensionUnits.setText("Inch",false)
        }

        bind.additionalWeight.setOnCheckedChangeListener { _, checked ->
            bind.fixedWeightExpand.isExpanded = checked
        }

        bind.save.setHapticClickListener {
            when {
                bind.name.value().isEmpty() -> {
                    bind.name.error = "Please enter name"
                    return@setHapticClickListener
                }

                bind.weight.value().isEmpty() -> {
                    bind.weight.error = "Please enter weight"
                    return@setHapticClickListener

                }

                bind.weightUnits.value().isEmpty() -> {
                    bind.weightUnits.error = "Please enter weight units"
                    return@setHapticClickListener
                }

                else -> {
                    bind.loader.isVisible = true

                    if (from == "edit") {
                        viewModel.storeShippingProfile(
                            shippingProfileId = viewModel.selectedShippingProfile?.id.toString().request(),
                            name = bind.name.value().request(),
                            size = bind.weightUnits.value().request(),
                            weight = bind.weight.value().request(),
                            additionalWeight = if (bind.additionalWeight.isChecked) "1".request() else "0".request(),
                            maxItems = if (bind.maxPackage.isChecked) "1".request() else "0".request()
                        )
                    } else {
                        viewModel.storeShippingProfile(
                            name = bind.name.value().request(),
                            size = bind.weightUnits.value().request(),
                            weight = bind.weight.value().request(),
                            additionalWeight = if (bind.additionalWeight.isChecked) "1".request() else "0".request(),
                            maxItems = if (bind.maxPackage.isChecked) "1".request() else "0".request()
                        )
                    }

                }

            }

        }

        viewModel.storeShippingProfileRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    viewModel.storeShippingProfileRepo.value = null
                    bind.loader.isVisible = false

                    if (type == "createShippingProfile") {
                        finish()
                    } else {
                        findNavController().popBackStack()
                    }
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