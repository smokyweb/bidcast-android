package io.bidswipe.app.ui.sellerHub.shipping

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.databinding.FragmentCreateShippingProfileBinding
import io.bidswipe.app.databinding.FragmentFreePickupBinding
import io.bidswipe.app.ui.sellerHub.SellerHubViewModel
import io.bidswipe.app.utils.setHapticClickListener

class CreateShippingProfileFragment : BaseFragment<SellerHubViewModel, FragmentCreateShippingProfileBinding>() {
    override fun getModel(): Class<SellerHubViewModel> = SellerHubViewModel::class.java

    override fun getBind(
        inflater: LayoutInflater,
        view: ViewGroup?,
    ) = FragmentCreateShippingProfileBinding.inflate(inflater, view, false)

    val weightUnits: MutableList<String> = mutableListOf(
        "Ounce",
        "Pound",
        "Stone",
        "Ton (short ton)",
        "Long Ton (imperial ton)",
        "Grain",
        "Milligram",
        "Gram",
        "Kilogram",
        "Metric Ton"
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bind.header.onBackClick {
            findNavController().popBackStack()
        }

        val proCategoryAdapter = ArrayAdapter(
            mCtx,
            android.R.layout.simple_list_item_1,
            weightUnits
        )

        bind.weightUnits.setAdapter(proCategoryAdapter)
        val proDrawable = ContextCompat.getDrawable(mCtx, R.drawable.card_8)
        bind.weightUnits.setDropDownBackgroundDrawable(proDrawable)

        bind.weightUnits.setOnItemClickListener { _, _, position, _ ->
            val selectedProcessingCategory = weightUnits[position]
            log("Selected processing category: $selectedProcessingCategory")
        }

        bind.weightUnits.setHapticClickListener {
            bind.weightUnits.showDropDown()
        }

    }
}