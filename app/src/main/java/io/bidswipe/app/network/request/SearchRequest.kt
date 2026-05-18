package io.bidswipe.app.network.request

import com.google.gson.annotations.SerializedName

data class SearchRequest(
    @SerializedName("search") val search: String,
    @SerializedName("page") val page: Int?
)
