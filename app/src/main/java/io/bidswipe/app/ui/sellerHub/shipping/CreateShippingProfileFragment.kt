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
import io.bidswipe.app.network.response.GetUSPSboxDimensionsResponse
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

    private var dimensionsList = mutableListOf<GetUSPSboxDimensionsResponse.Data?>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val type = activity?.intent?.getStringExtra("slug").toString()

        val from = arguments?.getString("from")

        // QA-FIX (MC task cmo7iaew500ccfi15o3mz7xns): pre-load USPS box dimension data on
        // fragment start so the dropdown is ready when the user enables the max-package
        // toggle — previously the data was only fetched after the toggle was turned on,
        // leaving the field blank and unclickable until that point.
        viewModel.getUSPSBoxDimensions()

        if (from == "edit") {
            bind.header.setHeaderText("Edit Shipping Profile")
            bind.save.text = "Update"
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

        bind.boxDimensions.setHapticClickListener {
            hideKeyboard(it)
            bind.boxDimensions.showDropDown()
        }

        bind.boxDimensions.setOnItemClickListener { _, _, position, _ ->
            bind.height.isEnabled = false
            bind.width.isEnabled = false
            bind.length.isEnabled = false
            bind.dimensionUnits.isEnabled = false

            bind.height.setText(dimensionsList[position]?.height.toString())
            bind.width.setText(dimensionsList[position]?.width.toString())
            bind.length.setText(dimensionsList[position]?.length.toString())
            bind.dimensionUnits.setText(dimensionsList[position]?.unit.toString())
        }

        bind.maxPackage.setOnCheckedChangeListener { _, checked ->
            bind.maxItemsExpand.isExpanded = checked

            if (checked) {
                if (bind.height.value().isEmpty()) bind.height.setText("12.00")
                if (bind.length.value().isEmpty()) bind.length.setText("12.00")
                if (bind.width.value().isEmpty()) bind.width.setText("12.00")
                if (bind.dimensionUnits.value().isEmpty()) bind.dimensionUnits.setText("Inch", false)
                if (bind.boxDimensions.value().isEmpty()) bind.boxDimensions.setText("Custom", false)

                if (dimensionsList.isEmpty()) {
                    bind.loader.isVisible = true
                    viewModel.getUSPSBoxDimensions()
                }
            }
        }

        bind.additionalWeight.setOnCheckedChangeListener { _, checked ->
            bind.fixedWeightExpand.isExpanded = checked
        }

        populateEditData(from)

        bind.save.setHapticClickListener {

            if (bind.name.value().isEmpty()) {
                errorToast("Please enter name")
                bind.name.requestFocus()
                return@setHapticClickListener
            }

            if (bind.weight.value().isEmpty()) {
                errorToast("Please enter weight")
                bind.weight.requestFocus()
                return@setHapticClickListener
            }

            if (bind.weightUnits.value().isEmpty()) {
                errorToast("Please enter weight units")
                return@setHapticClickListener
            }

            if (bind.maxPackage.isChecked) {

                if (bind.maxItems.value().isEmpty()) {
                    errorToast("Please enter max items")
                    bind.maxItems.requestFocus()
                    return@setHapticClickListener
                }

                if (bind.height.value().isEmpty()) {
                    errorToast("Please enter height")
                    bind.height.requestFocus()
                    return@setHapticClickListener
                }

                if (bind.width.value().isEmpty()) {
                    errorToast("Please enter width")
                    bind.width.requestFocus()
                    return@setHapticClickListener
                }

                if (bind.length.value().isEmpty()) {
                    errorToast("Please enter length")
                    bind.length.requestFocus()
                    return@setHapticClickListener
                }

                if (bind.dimensionUnits.value().isEmpty()) {
                    errorToast("Please enter dimension unit")
                    return@setHapticClickListener
                }
            }

            if (bind.additionalWeight.isChecked) {

                if (bind.incrementalWeight.value().isEmpty()) {
                    errorToast("Please enter incremental weight")
                    bind.incrementalWeight.requestFocus()
                    return@setHapticClickListener
                }

                if (bind.incrementalWeightUnits.value().isEmpty()) {
                    errorToast("Please enter incremental weight unit")
                    return@setHapticClickListener
                }
            }

            bind.loader.isVisible = true
            viewModel.storeShippingProfile(
                shippingProfileId = if (from == "edit") viewModel.selectedShippingProfile?.id.toString().request() else null,
                name = bind.name.value().request(),
                size = bind.weightUnits.value().request(),
                weight = bind.weight.value().request(),
                additionalWeight = if (bind.additionalWeight.isChecked) "1".request() else "0".request(),
                maxItems = if (bind.maxPackage.isChecked) "1".request() else "0".request(),
                maxItemUnit = if (bind.maxPackage.isChecked) bind.maxItems.value().request() else null,
                height = if (bind.maxPackage.isChecked) bind.height.value().request() else "12.00".request(),
                width = if (bind.maxPackage.isChecked) bind.width.value().request() else "12.00".request(),
                length = if (bind.maxPackage.isChecked) bind.length.value().request() else "12.00".request(),
                scale = if (bind.maxPackage.isChecked) bind.dimensionUnits.value().request() else "inch".request(),
                incrementWeight = if (bind.additionalWeight.isChecked) bind.incrementalWeight.value().request() else null,
                incrementWeightUnit = if (bind.additionalWeight.isChecked) bind.incrementalWeightUnits.value().request() else null
            )
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

        viewModel.getUSPSBoxDimensionsRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false
                    viewModel.getUSPSBoxDimensionsRepo.value = null

                    val mData = it.value.data
                    if (mData?.isNotEmpty() == true) {
                        dimensionsList.clear()
                        dimensionsList.addAll(mData)

                        val boxDimensionsAdapter = ArrayAdapter(
                            mCtx,
                            android.R.layout.simple_list_item_1,
                            dimensionsList.map { it?.name }
                        )

                        bind.boxDimensions.setAdapter(boxDimensionsAdapter)
                        bind.boxDimensions.setDropDownBackgroundDrawable(proDrawable)

                    }
                }

                is Resource.Error -> {
                    bind.loader.isVisible = false
                    viewModel.getUSPSBoxDimensionsRepo.value = null
                    it.parse(mCtx, TAG)
                }

                else -> {}

            }
        }
    }

    private fun populateEditData(from: String?) {
        if (from != "edit") return

        val profile = viewModel.selectedShippingProfile ?: return

        bind.name.setText(profile.name.orEmpty())
        bind.weight.setText(profile.weight.orEmpty())
        bind.weightUnits.setText(profile.size.orEmpty(), false)

        bind.additionalWeight.isChecked = profile.additionalWeight == true
        bind.incrementalWeight.setText(profile.incrementWeight.orEmpty())
        bind.incrementalWeightUnits.setText(profile.incrementWeightScale.orEmpty(), false)

        bind.maxPackage.isChecked = profile.maxItems == true
        bind.maxItems.setText(profile.maxItemUnit.orEmpty())
        bind.height.setText(profile.height.orEmpty())
        bind.width.setText(profile.width.orEmpty())
        bind.length.setText(profile.length.orEmpty())
        bind.dimensionUnits.setText(profile.scale.orEmpty(), false)
        bind.boxDimensions.setText("Custom", false)
    }
}