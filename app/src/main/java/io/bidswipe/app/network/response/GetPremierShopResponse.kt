package io.bidswipe.app.network.response


import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep

@Keep
data class GetPremierShopResponse(
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
        @SerializedName("current_progress")
        val currentProgress: String?,
        @SerializedName("features")
        val features: List<Feature?>?,
        @SerializedName("id")
        val id: Int?,
        @SerializedName("next_review")
        val nextReview: String?,
        @SerializedName("page_details")
        val pageDetails: String?,
        @SerializedName("page_logo")
        val pageLogo: String?,
        @SerializedName("page_title")
        val pageTitle: String?,
        @SerializedName("requirements")
        val requirements: List<Requirement?>?,
        @SerializedName("review_details")
        val reviewDetails: String?,
        @SerializedName("review_logo")
        val reviewLogo: String?,
        @SerializedName("review_title")
        val reviewTitle: String?,
        @SerializedName("shop_details")
        val shopDetails: String?,
        @SerializedName("shop_logo")
        val shopLogo: String?,
        @SerializedName("shop_options")
        val shopOptions: ShopOptions?,
        @SerializedName("shop_title")
        val shopTitle: String?
    ) {
        @Keep
        data class Feature(
            @SerializedName("description")
            val description: String?,
            @SerializedName("icon")
            val icon: String?,
            @SerializedName("title")
            val title: String?
        )

        @Keep
        data class Requirement(
            @SerializedName("platform")
            val platform: String?,
            @SerializedName("url")
            val url: String?
        )

        @Keep
        data class ShopOptions(
            @SerializedName("Delivery")
            val delivery: String?,
            @SerializedName("Rating")
            val rating: Double?,
            @SerializedName("Response")
            val response: String?
        )
    }
}