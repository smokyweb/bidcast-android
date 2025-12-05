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

					viewModel.storeShippingProfile(
						name = bind.name.value().request(),
						size = bind.weightUnits.value().request(),
						weight = bind.weight.value().request()
					)
				}

			}

		}

		viewModel.storeShippingProfileRepo.observe(viewLifecycleOwner) {
			when (it) {
				is Resource.Success -> {
					viewModel.storeShippingProfileRepo.value = null
					bind.loader.isVisible = false
					findNavController().popBackStack()
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