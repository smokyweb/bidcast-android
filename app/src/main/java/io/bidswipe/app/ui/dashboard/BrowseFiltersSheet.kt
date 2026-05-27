package io.bidswipe.app.ui.dashboard

// Basecamp #9933301500 (2026-05-27): port 5 PWA browse filters to Android.
//
// The PWA's browse / category page has 5 filters (Show Format, Tags,
// Premier Shops, Shipped From, Reduced Shipping). This BottomSheetDialogFragment
// mirrors them identically. Backend already accepts all 5 field groups on
// POST /api/get-live-show — see ApiController::getLiveShow on bidcast-gitlab.
// No backend changes needed; this sheet just collects values + passes them
// to the caller via the onApply lambda.

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.bidswipe.app.databinding.SheetBrowseFiltersBinding

/**
 * Snapshot of the 5 browse filter values. All fields optional — omit by
 * passing null/empty. Shape mirrors the fields the backend validator accepts
 * 1-to-1 so callers can just convert + forward.
 */
data class BrowseFilters(
    val showFormat: String? = null,   // surprise_sets | live_auction | buy_it_now | null(All)
    val tag: String? = null,           // single tag string, no leading "#"
    val premierShop: Boolean = false,
    val shipCountry: String? = null,   // 2-letter ISO; null = Any country
    val shipState: String? = null,     // optional state/region free text
    val shipping: String? = null       // free | reduced | null(All)
) {
    val isActive: Boolean
        get() = showFormat != null
                || !tag.isNullOrBlank()
                || premierShop
                || shipCountry != null
                || !shipState.isNullOrBlank()
                || shipping != null
}

// Country list matches iOS port. ISO-3166-1 alpha-2 codes (note GB for UK,
// not "UK", since the API strips and upper-cases but GB is the correct code).
data class FilterCountry(val code: String, val label: String) {
    override fun toString() = label
}

val BROWSE_FILTER_COUNTRIES = listOf(
    FilterCountry("",   "Any country"),
    FilterCountry("US", "United States"),
    FilterCountry("CA", "Canada"),
    FilterCountry("GB", "United Kingdom"),
    FilterCountry("AU", "Australia"),
    FilterCountry("FR", "France"),
    FilterCountry("DE", "Germany"),
    FilterCountry("IT", "Italy"),
    FilterCountry("JP", "Japan"),
    FilterCountry("MX", "Mexico"),
    FilterCountry("IN", "India"),
)

class BrowseFiltersSheet(
    private val initial: BrowseFilters = BrowseFilters(),
    private val onApply: (BrowseFilters) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: SheetBrowseFiltersBinding? = null
    private val bind get() = _binding!!

    // Working copy mutated as the user changes controls; snapshotted on Apply.
    private var draft = initial.copy()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = SheetBrowseFiltersBinding.inflate(inflater, container, false)
        return bind.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val ctx = requireContext()

        // ── Close button ────────────────────────────────────────────────────
        bind.close.setOnClickListener { dismiss() }

        // ── 1. Show Format ──────────────────────────────────────────────────
        // Chip group — single-selection radio style. "All" = no format filter.
        restoreShowFormatChip(draft.showFormat)
        bind.chipFormatAll.setOnClickListener        { draft = draft.copy(showFormat = null) }
        bind.chipFormatSurprise.setOnClickListener   { draft = draft.copy(showFormat = "surprise_sets") }
        bind.chipFormatAuction.setOnClickListener    { draft = draft.copy(showFormat = "live_auction") }
        bind.chipFormatBuyNow.setOnClickListener     { draft = draft.copy(showFormat = "buy_it_now") }

        // ── 2. Tag ──────────────────────────────────────────────────────────
        bind.tagInput.setText(draft.tag ?: "")
        bind.tagInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                draft = draft.copy(tag = s?.toString()?.trim()?.ifEmpty { null })
            }
        })

        // ── 3. Premier Shops toggle ─────────────────────────────────────────
        bind.switchPremierShop.isChecked = draft.premierShop
        bind.switchPremierShop.setOnCheckedChangeListener { _, checked ->
            draft = draft.copy(premierShop = checked)
        }

        // ── 4. Shipped from — country Spinner + state text field ────────────
        val countryAdapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_item, BROWSE_FILTER_COUNTRIES)
        countryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        bind.spinnerCountry.adapter = countryAdapter

        // Restore previously selected country.
        val restoredCountryIdx = BROWSE_FILTER_COUNTRIES.indexOfFirst { it.code == (draft.shipCountry ?: "") }
        bind.spinnerCountry.setSelection(if (restoredCountryIdx >= 0) restoredCountryIdx else 0)

        bind.spinnerCountry.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                val selected = BROWSE_FILTER_COUNTRIES[pos]
                draft = draft.copy(
                    shipCountry = selected.code.ifEmpty { null },
                    // Wipe state when country is cleared.
                    shipState = if (selected.code.isEmpty()) null else draft.shipState
                )
                bind.stateInput.isEnabled = selected.code.isNotEmpty()
                bind.stateInput.alpha = if (selected.code.isNotEmpty()) 1f else 0.4f
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        bind.stateInput.setText(draft.shipState ?: "")
        bind.stateInput.isEnabled = draft.shipCountry != null
        bind.stateInput.alpha = if (draft.shipCountry != null) 1f else 0.4f
        bind.stateInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                draft = draft.copy(shipState = s?.toString()?.trim()?.ifEmpty { null })
            }
        })

        // ── 5. Reduced shipping ──────────────────────────────────────────────
        restoreShippingChip(draft.shipping)
        bind.chipShippingAll.setOnClickListener     { draft = draft.copy(shipping = null) }
        bind.chipShippingFree.setOnClickListener    { draft = draft.copy(shipping = "free") }
        bind.chipShippingReduced.setOnClickListener { draft = draft.copy(shipping = "reduced") }

        // ── Clear button ─────────────────────────────────────────────────────
        bind.btnClear.setOnClickListener {
            draft = BrowseFilters()
            bind.tagInput.setText("")
            bind.stateInput.setText("")
            bind.switchPremierShop.isChecked = false
            bind.spinnerCountry.setSelection(0)
            restoreShowFormatChip(null)
            restoreShippingChip(null)
        }

        // ── Apply button ──────────────────────────────────────────────────────
        bind.btnApply.setOnClickListener {
            onApply(draft)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Helper: mark the correct show-format chip as checked.
    private fun restoreShowFormatChip(format: String?) {
        bind.chipFormatAll.isChecked      = format == null
        bind.chipFormatSurprise.isChecked = format == "surprise_sets"
        bind.chipFormatAuction.isChecked  = format == "live_auction"
        bind.chipFormatBuyNow.isChecked   = format == "buy_it_now"
    }

    // Helper: mark the correct shipping chip as checked.
    private fun restoreShippingChip(value: String?) {
        bind.chipShippingAll.isChecked     = value == null
        bind.chipShippingFree.isChecked    = value == "free"
        bind.chipShippingReduced.isChecked = value == "reduced"
    }
}
