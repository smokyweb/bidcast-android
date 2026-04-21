package io.bidswipe.app.ui.more

import android.app.Activity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.RadioButton
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import io.bidswipe.app.App
import io.bidswipe.app.BuildConfig
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

	private var placesClient: PlacesClient? = null
	private var streetPlacesAdapter: PlacesStreetAutocompleteAdapter? = null
	private var suppressStreetAutocomplete = false

	/** If Google returns a state before our state list is loaded, apply when [stateList] is ready. */
	private var pendingGoogleStateIso: String? = null

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

		ViewCompat.setOnApplyWindowInsetsListener(bind.root) { _, insets ->
			val system = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
			bind.scrollView2.setPadding(
				bind.scrollView2.paddingLeft,
				bind.scrollView2.paddingTop,
				bind.scrollView2.paddingRight,
				maxOf(system.bottom, ime.bottom)
			)
			insets
		}

		bind.zipCode.setOnFocusChangeListener { _, hasFocus ->
			if (hasFocus) {
				bind.scrollView2.post {
					bind.scrollView2.smoothScrollTo(0, bind.zipCode.bottom + bind.addAddress.height)
				}
			}
		}

		bind.state.setOnItemClickListener { _, _, position, _ ->
selectedState=stateList[position]
		}

		bind.state.setHapticClickListener {
			bind.state.showDropDown()
		}

		setupStreetAddressAutocomplete()

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

					viewModel.addShippingAddress(
						type = selectedText.toString().request(),
						name = bind.name.value().request(),
						phoneNumber = bind.phoneNumber.value().request(),
						streetAddress = bind.streetAddress.value().request(),
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
                    viewModel.addShippingAddressRepo.value = null

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

						pendingGoogleStateIso?.let { iso -> selectStateByIso(iso) }
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

	private fun setupStreetAddressAutocomplete() {
		if (!Places.isInitialized()) {
			if (BuildConfig.PLACES_API_KEY.isBlank()) return
			Places.initialize(requireContext().applicationContext, BuildConfig.PLACES_API_KEY)
		}
		val client = Places.createClient(requireContext())
		placesClient = client
		val sessionToken = AutocompleteSessionToken.newInstance()
		val adapter = PlacesStreetAutocompleteAdapter(requireContext(), client, sessionToken)
		streetPlacesAdapter = adapter
		bind.streetAddress.setAdapter(adapter)
		bind.streetAddress.threshold = 2
		ContextCompat.getDrawable(mCtx, R.drawable.card_8)?.let { bind.streetAddress.setDropDownBackgroundDrawable(it) }
		bind.streetAddress.addTextChangedListener(object : TextWatcher {
			override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
			override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
				if (suppressStreetAutocomplete) return
				val query = s?.toString()?.trim().orEmpty()
				if (query.length >= 2) {
					streetPlacesAdapter?.filter?.filter(query)
					bind.streetAddress.post { bind.streetAddress.showDropDown() }
				}
			}
			override fun afterTextChanged(s: Editable?) = Unit
		})
		bind.streetAddress.setOnItemClickListener { _, _, position, _ ->
			val prediction = adapter.getItem(position) ?: return@setOnItemClickListener
			suppressStreetAutocomplete = true
			bind.streetAddress.dismissDropDown()
			hideKeyboard(bind.streetAddress)
			fetchPlaceAndFill(prediction)
		}
	}

	private fun selectStateByIso(iso: String?) {
		if (iso.isNullOrBlank()) return
		val trimmed = iso.trim()
		val match = stateList.find { it?.iso2.equals(trimmed, ignoreCase = true) }
		if (match != null) {
			selectedState = match
			bind.state.setText("${match.name} ( ${match.iso2} )", false)
			pendingGoogleStateIso = null
		} else {
			pendingGoogleStateIso = trimmed
		}
	}

	private fun applyAddressFromPlace(place: Place) {
		var streetNumber = ""
		var route = ""
		var city = ""
		var stateIso = ""
		var zip = ""
		place.addressComponents?.asList()?.forEach { comp ->
			val types = comp.types
			when {
				types.contains("street_number") -> streetNumber = comp.name ?: ""
				types.contains("route") -> route = comp.name ?: ""
				types.contains("locality") -> city = comp.name ?: ""
				types.contains("sublocality_level_1") && city.isBlank() -> city = comp.name ?: ""
				types.contains("administrative_area_level_1") -> stateIso = comp.shortName ?: comp.name ?: ""
				types.contains("postal_code") -> zip = comp.name ?: ""
			}
		}
		val streetJoined = listOf(streetNumber, route).joinToString(" ").trim()
		val streetLine = streetJoined.ifBlank {
			place.address?.lineSequence()?.firstOrNull().orEmpty()
		}
		bind.streetAddress.setText(streetLine, false)
		bind.streetAddress.dismissDropDown()
		bind.city.setText(city)
		bind.zipCode.setText(zip)
		selectStateByIso(stateIso)
		suppressStreetAutocomplete = false
	}

	private fun fetchPlaceAndFill(prediction: AutocompletePrediction) {
		val client = placesClient ?: return
		bind.loader.isVisible = true
		val fields = listOf(
			Place.Field.ID,
			Place.Field.ADDRESS_COMPONENTS,
			Place.Field.ADDRESS
		)
		val request = FetchPlaceRequest.newInstance(prediction.placeId, fields)
		client.fetchPlace(request)
			.addOnSuccessListener { response ->
				if (!isAdded) return@addOnSuccessListener
				bind.loader.isVisible = false
				applyAddressFromPlace(response.place)
				streetPlacesAdapter?.newSession()
			}
			.addOnFailureListener {
				if (!isAdded) return@addOnFailureListener
				bind.loader.isVisible = false
				suppressStreetAutocomplete = false
				Alerts.error(mCtx, "Could not load address details. Try again or enter manually.")
			}
	}

}