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
import android.view.ContextThemeWrapper
import androidx.core.view.isVisible
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import io.bidswipe.app.App
import io.bidswipe.app.databinding.SheetBrowseFiltersBinding
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.Prefs

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
    val shipping: String? = null,      // free | reduced | null(All)
    // Basecamp #9938023997: multi-select category + subcategory filter
    val categoryIds: List<Int> = emptyList(),
    val subCategoryIds: List<Int> = emptyList(),
) {
    val isActive: Boolean
        get() = showFormat != null
                || !tag.isNullOrBlank()
                || premierShop
                || shipCountry != null
                || !shipState.isNullOrBlank()
                || shipping != null
                || categoryIds.isNotEmpty()
                || subCategoryIds.isNotEmpty()
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

    // Basecamp #9938023997: subcategory cache keyed by category id to avoid re-fetching.
    private data class SubcatItem(val id: Int, val categoryId: Int, val name: String)
    private val subcatCacheByCategory = mutableMapOf<Int, List<SubcatItem>>()

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

        // ── Categories + Subcategories (Basecamp #9938023997) ────────────────
        setupCategoryChips()

        // ── 1. Show Format ──────────────────────────────────────────────────
        // Chip group — single-selection radio style. "All" = no format filter.
        restoreShowFormatChip(draft.showFormat)
        bind.chipFormatAll.setOnClickListener        { draft = draft.copy(showFormat = null) }
        bind.chipFormatSurprise.setOnClickListener   { draft = draft.copy(showFormat = "surprise_sets") }
        bind.chipFormatAuction.setOnClickListener    { draft = draft.copy(showFormat = "live_auction") }
        bind.chipFormatBuyNow.setOnClickListener     { draft = draft.copy(showFormat = "buy_it_now") }

        // ── 2. Tag ──────────────────────────────────────────────────────────
        // Basecamp #9933301500 (2026-05-27 round 3): replaced free-text input
        // with an exposed dropdown populated from GET /api/tags/suggest.
        bind.tagInput.setText(draft.tag ?: "")
        bind.tagInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                draft = draft.copy(tag = s?.toString()?.trim()?.ifEmpty { null })
            }
        })
        // Fetch all tags to populate the dropdown.
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                val url = java.net.URL("https://backend.bidcast.betaplanets.com/api/tags/suggest")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.setRequestProperty("Accept", "application/json")
                val body = conn.inputStream.bufferedReader().readText()
                conn.disconnect()
                // Response: { status, message, data: [ {id, name, slug, usage_count} ] }
                val json = org.json.JSONObject(body)
                val arr = json.optJSONArray("data") ?: org.json.JSONArray()
                val tagNames = mutableListOf("Any tag")
                for (i in 0 until arr.length()) {
                    arr.optJSONObject(i)?.optString("name")?.takeIf { it.isNotBlank() }?.let { tagNames.add(it) }
                }
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    val adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, tagNames)
                    (bind.tagInput as? com.google.android.material.textfield.MaterialAutoCompleteTextView)?.setAdapter(adapter)
                    (bind.tagInput as? com.google.android.material.textfield.MaterialAutoCompleteTextView)?.setOnItemClickListener { parent, _, pos, _ ->
                        val selected = parent.getItemAtPosition(pos) as? String ?: ""
                        draft = draft.copy(tag = if (selected == "Any tag") null else selected)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("BrowseFiltersSheet", "tag fetch failed: " + e.message)
            }
        }

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
            // Clear category + subcategory chips
            for (i in 0 until bind.categoryChipsGroup.childCount) {
                (bind.categoryChipsGroup.getChildAt(i) as? Chip)?.isChecked = false
            }
            bind.subcategoryChipsGroup.removeAllViews()
            bind.subcategorySection.isVisible = false
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

    // ── Category / Subcategory helpers (Basecamp #9938023997) ───────────────

    /**
     * Populate the Categories ChipGroup from App.categoryList (or fetch if empty).
     * Called once from onViewCreated.
     */
    private fun setupCategoryChips() {
        val cached = App.categoryList.filterNotNull()
        if (cached.isNotEmpty()) {
            val pairs = cached.mapNotNull { c ->
                val id = c.id ?: return@mapNotNull null
                val name = c.name?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                id to name
            }
            populateCategoryChipsFromPairs(pairs)
        } else {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val token = Prefs(requireContext()).token()
                    val url = java.net.URL("${Const.BASE_URL}/api/get-category")
                    val conn = url.openConnection() as java.net.HttpURLConnection
                    conn.setRequestProperty("Accept", "application/json")
                    if (token.isNotBlank()) conn.setRequestProperty("Authorization", "Bearer $token")
                    val body = conn.inputStream.bufferedReader().readText()
                    conn.disconnect()
                    val json = org.json.JSONObject(body)
                    val arr = json.optJSONArray("data") ?: org.json.JSONArray()
                    val pairs = mutableListOf<Pair<Int, String>>()
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i) ?: continue
                        val id = obj.optInt("id", -1)
                        val name = obj.optString("name", "").trim()
                        if (id > 0 && name.isNotBlank()) pairs.add(id to name)
                    }
                    withContext(Dispatchers.Main) {
                        if (isAdded) populateCategoryChipsFromPairs(pairs)
                    }
                } catch (e: Exception) {
                    android.util.Log.w("BrowseFiltersSheet", "category fetch failed: ${e.message}")
                }
            }
        }
    }

    /**
     * Build a Chip for every (id, name) pair and add to categoryChipsGroup.
     * Restores checked state from draft. Pre-loads subcategories if categories
     * are already selected from a previous open.
     */
    private fun populateCategoryChipsFromPairs(pairs: List<Pair<Int, String>>) {
        val ctx = requireContext()
        bind.categoryChipsGroup.removeAllViews()
        pairs.forEach { (id, name) ->
            val chip = Chip(
                ContextThemeWrapper(ctx, com.google.android.material.R.style.Widget_Material3_Chip_Filter),
                null, 0
            ).apply {
                text = name
                isCheckable = true
                isChecked = id in draft.categoryIds
                tag = id
                setOnClickListener { onCategoryChipClicked(id) }
            }
            bind.categoryChipsGroup.addView(chip)
        }
        // If categories are already selected (initial open with existing filters),
        // pre-render subcategories.
        if (draft.categoryIds.isNotEmpty()) {
            loadSubcategoriesForSelected()
        }
    }

    /** Toggle the given category id in/out of draft.categoryIds. */
    private fun onCategoryChipClicked(id: Int) {
        val newCatIds = draft.categoryIds.toMutableList()
        if (id in newCatIds) {
            newCatIds.remove(id)
            // Drop any selected subcats that belong to this category.
            val removedSubcatIds = (subcatCacheByCategory[id] ?: emptyList()).map { it.id }
            val newSubIds = draft.subCategoryIds.filter { it !in removedSubcatIds }
            draft = draft.copy(categoryIds = newCatIds, subCategoryIds = newSubIds)
        } else {
            newCatIds.add(id)
            draft = draft.copy(categoryIds = newCatIds)
        }
        loadSubcategoriesForSelected()
    }

    /**
     * Fetch subcategories for any newly selected categories (cached per-category),
     * then render. If no categories are selected, hide the section.
     */
    private fun loadSubcategoriesForSelected() {
        val selectedCatIds = draft.categoryIds
        if (selectedCatIds.isEmpty()) {
            bind.subcategorySection.isVisible = false
            bind.subcategoryChipsGroup.removeAllViews()
            return
        }
        val uncached = selectedCatIds.filter { !subcatCacheByCategory.containsKey(it) }
        if (uncached.isEmpty()) {
            renderSubcategoryChips()
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val token = Prefs(requireContext()).token()
                val reqBody = org.json.JSONObject()
                reqBody.put("category_ids", org.json.JSONArray(uncached))
                val url = java.net.URL("${Const.BASE_URL}/api/get-subcategories")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("Accept", "application/json")
                if (token.isNotBlank()) conn.setRequestProperty("Authorization", "Bearer $token")
                conn.doOutput = true
                conn.outputStream.use { it.write(reqBody.toString().toByteArray()) }
                val respBody = try {
                    conn.inputStream.bufferedReader().readText()
                } catch (e: Exception) {
                    conn.errorStream?.bufferedReader()?.readText() ?: ""
                }
                conn.disconnect()
                val json = org.json.JSONObject(respBody)
                val arr = json.optJSONArray("data") ?: org.json.JSONArray()
                for (i in 0 until arr.length()) {
                    val catObj = arr.optJSONObject(i) ?: continue
                    val catId = catObj.optInt("id", -1)
                    if (catId <= 0) continue
                    val subcats = mutableListOf<SubcatItem>()
                    val subArr = catObj.optJSONArray("subcategories") ?: org.json.JSONArray()
                    for (j in 0 until subArr.length()) {
                        val sub = subArr.optJSONObject(j) ?: continue
                        val subId = sub.optInt("id", -1)
                        val subName = sub.optString("name", "").trim()
                        if (subId > 0 && subName.isNotBlank()) {
                            subcats.add(SubcatItem(subId, catId, subName))
                        }
                    }
                    subcatCacheByCategory[catId] = subcats
                }
                withContext(Dispatchers.Main) {
                    if (isAdded) renderSubcategoryChips()
                }
            } catch (e: Exception) {
                android.util.Log.w("BrowseFiltersSheet", "subcat fetch failed: ${e.message}")
            }
        }
    }

    /**
     * Build subcategory chips from the cache for all selected categories.
     * Union of all subcategories across selected categories.
     */
    private fun renderSubcategoryChips() {
        val selectedCatIds = draft.categoryIds
        if (selectedCatIds.isEmpty()) {
            bind.subcategorySection.isVisible = false
            bind.subcategoryChipsGroup.removeAllViews()
            return
        }
        val allSubcats = selectedCatIds.flatMap { catId ->
            subcatCacheByCategory[catId] ?: emptyList()
        }
        if (allSubcats.isEmpty()) {
            bind.subcategorySection.isVisible = false
            bind.subcategoryChipsGroup.removeAllViews()
            return
        }
        bind.subcategorySection.isVisible = true
        bind.subcategoryChipsGroup.removeAllViews()
        val ctx = requireContext()
        allSubcats.forEach { subcat ->
            val chip = Chip(
                ContextThemeWrapper(ctx, com.google.android.material.R.style.Widget_Material3_Chip_Filter),
                null, 0
            ).apply {
                text = subcat.name
                isCheckable = true
                isChecked = subcat.id in draft.subCategoryIds
                tag = subcat.id
                setOnClickListener {
                    val newSubIds = draft.subCategoryIds.toMutableList()
                    if (subcat.id in newSubIds) newSubIds.remove(subcat.id)
                    else newSubIds.add(subcat.id)
                    draft = draft.copy(subCategoryIds = newSubIds)
                    isChecked = subcat.id in draft.subCategoryIds
                }
            }
            bind.subcategoryChipsGroup.addView(chip)
        }
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
