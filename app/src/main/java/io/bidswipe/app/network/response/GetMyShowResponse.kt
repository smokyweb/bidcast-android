package io.bidswipe.app.network.response


import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep

@Keep
data class GetMyShowResponse(
    @SerializedName("currentPage")
    val currentPage: Int?,
    @SerializedName("data")
    val `data`: List<Data?>?,
    @SerializedName("error_type")
    val errorType: String?,
    @SerializedName("message")
    val message: String?,
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
        @SerializedName("auction_type_id")
        val auctionTypeId: Int?,
        @SerializedName("category")
        val category: Category?,
        @SerializedName("category_id")
        val categoryId: Int?,
        @SerializedName("date")
        val date: String?,
        @SerializedName("id")
        val id: Int?,
        @SerializedName("product_ids")
        val productIds: List<String?>?,
        @SerializedName("thumbnail")
        val thumbnail: List<String?>?,
        @SerializedName("time")
        val time: String?,
        @SerializedName("title")
        val title: String?,
        @SerializedName("user")
        val user: User?,
        @SerializedName("user_id")
        val userId: Int?
    ) {
        @Keep
        data class Category(
            @SerializedName("color")
            val color: String?,
            @SerializedName("id")
            val id: Int?,
            @SerializedName("image")
            val image: String?,
            @SerializedName("name")
            val name: String?
        )

        @Keep
        data class User(
            @SerializedName("email")
            val email: String?,
            @SerializedName("first_name")
            val firstName: String?,
            @SerializedName("id")
            val id: Int?,
            @SerializedName("last_name")
            val lastName: String?,
            @SerializedName("name")
            val name: String?,
            @SerializedName("profile_image")
            val profileImage: String?,
            @SerializedName("role_id")
            val roleId: Int?
        )
    }
}