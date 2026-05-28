package io.bidswipe.app.network.request

import com.google.gson.annotations.SerializedName

data class SearchRequest(
    @SerializedName("search") val search: String,
    @SerializedName("page") val page: Int?,
    // Basecamp #9938023997: multi-select category + subcategory filter
    @SerializedName("category_ids") val categoryIds: List<Int>? = null,
    @SerializedName("sub_category_ids") val subCategoryIds: List<Int>? = null,
)
