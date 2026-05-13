package io.bidswipe.app.ui.more

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.RadioButton
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import io.bidswipe.app.App
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.databinding.FragmentAddShippingAddressBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetStatesResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.hideKeyboard
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.setHapticClickListener
import io.bidswipe.app.utils.showKeyboard
import io.bidswipe.app.utils.value

class AddShippingAddressFragment :
	BaseFragment<MoreViewModel, FragmentAddShippingAddressBinding>() {

	override fun getModel(): Class<MoreViewModel> = MoreViewModel::class.java

	override fun getBind(
		inflater: LayoutInflater,
		view: ViewGroup?,
	) = FragmentAddShippingAddressBinding.inflate(inflater, view, false)

	private var slug = ""
	private var stateList = mutableListOf<GetStatesResponse.Data?>()
	private var selectedState: GetStatesResponse.Data? = null

	// QA-FIX (MC task cmo7iaepv00cafi15wyu5k00e): Street address field should use Google
	// Places Autocomplete so the user gets address suggestions while typing instead of
	// having to enter the full address manually.
	//
	// TODO: Integrate Google Places SDK autocomplete on bind.streetAddress:
	//   1. Add dependency to build.gradle.kts:
	//      implementation("com.google.android.libraries.places:places:<latest_version>")
	//   2. Initialize the Places client in Application (App.kt):
	//      Places.initialize(applicationContext, BuildConfig.GOOGLE_MAPS_API_KEY)
	//   3. In onViewCreated, replace the plain TextInputEditText with a Places Autocomplete
	//      widget (AutocompleteSupportFragment or Autocomplete.IntentBuilder) and populate
	//      bind.streetAddress, bind.city, bind.state, and bind.zipCode from the result.
	//   BLOCKED: requires GOOGLE_MAPS_API_KEY (Places API key) from the project config.

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		slug = activity?.intent?.getStringExtra("slug") ?: ""

		bind.header.onBackClick {
			if (slug == "addAddress") {
				finish()
			} else {
				findNavController().popBackStack()
			}
		}
		bind.root.setHapticClickListener {
			hideKeyboard(it)
		}
		bind.rootView.setHapticClickListener {
			hideKeyboard(it)
		}

		bind.state.setOnItemClickListener { _, _, position, _ ->
selectedState=stateList[position]
		}

		bind.state.setHapticClickListener {
			bind.state.showDropDown()
		}

		bind.addAddress.setHapticClickListener {

			when {

				bind.name.value().isEmpty() -> {
					Alerts.error(mCtx, "Please enter name")
					bind.name.requestFocus()
					showKeyboard(bind.name)
				}

				bind.phoneNumber.value().isEmpty() -> {
					Alerts.error(mCtx, "Please enter phone number")
					bind.phoneNumber.requestFocus()
					showKeyboard(bind.phoneNumber)
				}

				bind.streetAddress.value().isEmpty() -> {
					Alerts.error(mCtx, "Please enter street address")
					bind.streetAddress.requestFocus()
					showKeyboard(bind.streetAddress)
				}

				bind.zipCode.value().isEmpty() -> {
					Alerts.error(mCtx, "Please enter zip code")
					bind.zipCode.requestFocus()
					showKeyboard(bind.zipCode)
				}

				bind.city.value().isEmpty() -> {
					Alerts.error(mCtx, "Please enter city")
					bind.city.requestFocus()
					showKeyboard(bind.city)
				}

				bind.state.value().isEmpty() || selectedState==null -> {
					Alerts.error(mCtx, "Please select state")
				}

				bind.radioGroup.checkedRadioButtonId == -1 -> {
					Alerts.error(mCtx, "Please select address type")
				}

				else -> {
					bind.loader.isVisible = true

					val buttonId = bind.radioGroup.checkedRadioButtonId

					val selectedRadioButton = bind.radioGroup.findViewById<RadioButton>(buttonId)

					val selectedText = selectedRadioButton.text

					// MC sub-task cmp4932vk00l13mx1du6mmebo: optional 2nd address
					// line. Empty -> send empty RequestBody so backend stores NULL.
					val line2Text = bind.addressLine2.value()
					viewModel.addShippingAddress(
						type = selectedText.toString().request(),
						name = bind.name.value().request(),
						phoneNumber = bind.phoneNumber.value().request(),
						streetAddress = bind.streetAddress.value().request(),
						addressLine2 = line2Text.request(),
						pinCode = bind.zipCode.value().request(),
						city = bind.city.value().request(),
						state = selectedState?.iso2?.request()
					)
				}
			}

		}

		viewModel.addShippingAddressRepo.observe(viewLifecycleOwner) {
			when (it) {
				is Resource.Success -> {
					bind.loader.isVisible = false
					viewModel.addShippingAddressRepo.value = null

					it.value.data

					App.getProfile()

					if (slug == "addAddress") {

						activity?.setResult(Activity.RESULT_OK)
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

		bind.loader.isVisible = true
		viewModel.getStates()
		viewModel.getStatesRepo.observe(viewLifecycleOwner) {
			when (it) {
				is Resource.Success -> {
					if (it.value.data?.isNotEmpty() == true) {
						bind.loader.isVisible = false
						stateList.clear()
						stateList.addAll(it.value.data)
						stateList.sortBy { selector -> selector?.name }

						val adapter = ArrayAdapter(mCtx, android.R.layout.simple_list_item_1, stateList.map { data -> data?.name + " ( " + data?.iso2 + " )" })
						bind.state.setAdapter(adapter)

						val draw = ContextCompat.getDrawable(mCtx, R.drawable.card_8)
						bind.state.setDropDownBackgroundDrawable(draw)
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