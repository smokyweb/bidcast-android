package io.bidswipe.app.model

import com.google.gson.annotations.SerializedName

data class GetSubCategoriesRequest(
    @SerializedName("category_ids")
    val categoryIds: List<Int>,
    @SerializedName("sub_category_id")
    val subcategoryIds: List<Int>? = null
)