package io.bidswipe.app.network.response

import com.google.gson.annotations.SerializedName

// MC cmpaj2fex0000w5hgq64jp9k4 (2026-05-24) / Basecamp #9922137198 (Trey 2026-05-20):
// "Search returns only categories instead of livestreams, products, users."
// Originally added on the stale feature/unified-search-cmpak4q4j branch which
// also deleted unrelated newer code; instead of merging that branch we
// cherry-picked just the response model + request DTO + API method + adapter.
//
// Top-level wrapper matches the backend ApiController::unifiedSearch shape:
//   { status, message, data: { shows: [...], products: [...], users: [...] } }
data class ExploreSearchResponse(
    @SerializedName("status") val status: String?,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: SearchData?
)

data class SearchData(
    @SerializedName("shows") val shows: List<SearchShow>?,
    @SerializedName("products") val products: List<SearchProduct>?,
    @SerializedName("users") val users: List<SearchUser>?
)

data class SearchShow(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String?,
    @SerializedName("date") val date: String?,
    @SerializedName("time") val time: String?,
    @SerializedName("thumbnail") val thumbnail: List<String>?,
    @SerializedName("user") val user: SearchUser?,
    @SerializedName("is_live") val isLive: Boolean?,
    @SerializedName("user_id") val userId: Int?
)

data class SearchProduct(
    @SerializedName("id") val id: Int?,
    @SerializedName("title") val title: String?,
    @SerializedName("pricing") val pricing: String?,
    @SerializedName("thumbnail") val thumbnail: List<String>?,
    @SerializedName("images") val images: List<String>?,
    @SerializedName("user") val user: SearchUser?
)

data class SearchUser(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String?,
    @SerializedName("username") val username: String?,
    @SerializedName("profile_image") val profileImage: String?
)
