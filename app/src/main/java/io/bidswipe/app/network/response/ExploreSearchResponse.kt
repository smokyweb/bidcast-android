package io.bidswipe.app.network.response

import com.google.gson.annotations.SerializedName
import io.bidswipe.app.model.Category
import io.bidswipe.app.model.Product

// Top-level response matches the API wrapper: { status, message, data: { shows, products, users } }
data class ExploreSearchResponse(
    @SerializedName("status") val status: String?,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: SearchData?
)

// The 'data' object containing the three lists
data class SearchData(
    @SerializedName("shows") val shows: List<SearchShow>?,
    @SerializedName("products") val products: List<SearchProduct>?,
    @SerializedName("users") val users: List<SearchUser>?
)

// Simplified Show object for search results
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

// Simplified Product object for search results
data class SearchProduct(
    @SerializedName("id") val id: Int?, // Nullable to handle potential bad data
    @SerializedName("title") val title: String?,
    @SerializedName("pricing") val pricing: String?,
    @SerializedName("thumbnail") val thumbnail: List<String>?,
    @SerializedName("images") val images: List<String>?,
    @SerializedName("user") val user: SearchUser?
)

// Simplified User object for search results
data class SearchUser(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String?,
    @SerializedName("username") val username: String?,
    @SerializedName("profile_image") val profileImage: String?
)
