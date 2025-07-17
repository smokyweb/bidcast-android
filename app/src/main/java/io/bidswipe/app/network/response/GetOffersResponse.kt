package io.bidswipe.app.network.response

import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep

@Keep
data class GetOffersResponse(
    @SerializedName("currentPage")
    val currentPage: Int?,
    @SerializedName("data")
    val `data`: List<Data?>?,
    @SerializedName("error_type")
    val errorType: String?,
    @SerializedName("message")
    val message: String?,
    @SerializedName("pending")
    val pending: Int?,
    @SerializedName("accepted")
    val accepted: Int?,
    @SerializedName("declined")
    val declined: Int?,
    @SerializedName("perPage")
    val perPage: Int?,
    @SerializedName("status")
    val status: String?,
    @SerializedName("total")
    val total: Int?,
    @SerializedName("totalPage")
    val totalPage: Int?
) {
    @Keep
    data class Data(
        @SerializedName("amount")
        val amount: String?,
        @SerializedName("id")
        val id: Int?,
        @SerializedName("product")
        val product: Product?,
        @SerializedName("product_id")
        val productId: Int?,
        @SerializedName("status")
        var status: String?,
        @SerializedName("user")
        val user: User?,
        @SerializedName("user_id")
        val userId: Int?,
        @SerializedName("created_at")
        val createdAt: String?,
    ) {
        @Keep
        data class Product(
            @SerializedName("id")
            val id: Int?,
            @SerializedName("images")
            val images: List<String?>?,
            @SerializedName("pricing")
            val pricing: String?,
            @SerializedName("title")
            val title: String?
        )

        @Keep
        data class User(
            @SerializedName("id")
            val id: Int?,
            @SerializedName("name")
            val name: String?,
            @SerializedName("profile_image")
            val profileImage: String?
        )
    }
}