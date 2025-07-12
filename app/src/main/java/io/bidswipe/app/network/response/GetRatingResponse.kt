package io.bidswipe.app.network.response


import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep

@Keep
data class GetRatingResponse(
    @SerializedName("data")
    val `data`: Data?,
    @SerializedName("error_type")
    val errorType: String?,
    @SerializedName("message")
    val message: String?,
    @SerializedName("status")
    val status: String?
) {
    @Keep
    data class Data(
        @SerializedName("ratings")
        val ratings: List<Rating?>?,
        @SerializedName("total_reviews")
        val totalReviews: Int?
    ) {
        @Keep
        data class Rating(
            @SerializedName("accuracy_rating")
            val accuracyRating: String?,
            @SerializedName("comment")
            val comment: String?,
            @SerializedName("created_at")
            val createdAt: String?,
            @SerializedName("id")
            val id: Int?,
            @SerializedName("overall_rating")
            val overallRating: String?,
            @SerializedName("packaging_rating")
            val packagingRating: String?,
            @SerializedName("seller_id")
            val sellerId: Int?,
            @SerializedName("shipping_rating")
            val shippingRating: String?,
            @SerializedName("updated_at")
            val updatedAt: String?,
            @SerializedName("user")
            val user: User?,
            @SerializedName("user_id")
            val userId: Int?
        ) {
            @Keep
            data class User(
                @SerializedName("email")
                val email: String?,
                @SerializedName("id")
                val id: Int?,
                @SerializedName("name")
                val name: String?
            )
        }
    }
}