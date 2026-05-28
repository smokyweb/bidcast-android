package io.bidswipe.app.network.request

import com.google.gson.annotations.SerializedName

data class SearchRequest(
    @SerializedName("search") val search: String,
    @SerializedName("page") val page: Int?,
    // Basecamp #9938023997: multi-select category + subcategory filter
    @SerializedName("category_ids") val categoryIds: List<Int>? = null,
    @SerializedName("sub_category_ids") val subCategoryIds: List<Int>? = null,
    // Basecamp #9938023997 round 5: full filter parity with Browse sheet
    @SerializedName("show_format") val showFormat: String? = null,
    @SerializedName("tag") val tag: String? = null,
    @SerializedName("premier_shop") val premierShop: Boolean? = null,
    @SerializedName("shipping") val shipping: String? = null,
)
