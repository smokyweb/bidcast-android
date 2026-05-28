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
    // Basecamp #9929090875 round 5 (2026-05-28): make id nullable defensively.
    // GSON throws and discards the entire list when a non-nullable Int field is
    // missing OR null in the response, which silently empties the search-results
    // list and looks like "search is broken". Safer to accept null + filter at
    // render time than to lose the whole list to one bad row.
    @SerializedName("id") val id: Int? = null,
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
    // Basecamp #9929090875 round 5 (2026-05-28): defensive null — see SearchShow.id.
    @SerializedName("id") val id: Int? = null,
    @SerializedName("name") val name: String?,
    @SerializedName("username") val username: String?,
    @SerializedName("profile_image") val profileImage: String?
)
