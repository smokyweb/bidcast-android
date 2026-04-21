package io.bidswipe.app.ui.more

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import com.google.android.gms.tasks.Tasks
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.TypeFilter
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient

/**
 * Dropdown suggestions for a street address field backed by Places Autocomplete (session-scoped).
 */
class PlacesStreetAutocompleteAdapter(
	context: Context,
	private val placesClient: PlacesClient,
	private var sessionToken: AutocompleteSessionToken,
) : ArrayAdapter<AutocompletePrediction>(context, android.R.layout.simple_dropdown_item_1line),
	Filterable {

	private val resultFilter = object : Filter() {
		override fun performFiltering(constraint: CharSequence?): FilterResults {
			val out = FilterResults()
			if (constraint.isNullOrBlank() || constraint.length < 2) {
				out.values = emptyList<AutocompletePrediction>()
				out.count = 0
				return out
			}
			return try {
				val response = Tasks.await(
					placesClient.findAutocompletePredictions(
						FindAutocompletePredictionsRequest.builder()
							.setSessionToken(sessionToken)
							.setQuery(constraint.toString())
							.setTypeFilter(TypeFilter.ADDRESS)
							.setCountries(listOf("US"))
							.build()
					)
				)
				out.values = response.autocompletePredictions
				out.count = response.autocompletePredictions.size
				out
			} catch (_: Exception) {
				out.values = emptyList<AutocompletePrediction>()
				out.count = 0
				out
			}
		}

		@Suppress("UNCHECKED_CAST")
		override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
			clear()
			val list = results?.values as? List<AutocompletePrediction> ?: emptyList()
			addAll(list)
			notifyDataSetChanged()
		}

		override fun convertResultToString(resultValue: Any?): CharSequence {
			return (resultValue as? AutocompletePrediction)?.getFullText(null) ?: ""
		}
	}

	fun newSession() {
		sessionToken = AutocompleteSessionToken.newInstance()
	}

	override fun getFilter(): Filter = resultFilter

	override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
		val row = super.getView(position, convertView, parent)
		row.findViewById<TextView>(android.R.id.text1)?.text =
			getItem(position)?.getFullText(null)
		return row
	}
}
