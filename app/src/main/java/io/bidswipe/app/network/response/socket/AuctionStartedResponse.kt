package io.bidswipe.app.network.response.socket


import com.google.gson.annotations.SerializedName

data class AuctionStartedResponse(
    @SerializedName("auction_started_at")
    val auctionStartedAt: String?,
    @SerializedName("counter_bid_time")
    val counterBidTime: Int?,
    @SerializedName("product")
    val product: Product?,
    @SerializedName("product_ids")
    val productIds: List<String?>?,
    @SerializedName("require_time")
    val requireTime: Int?,
    @SerializedName("room_id")
    val roomId: String?,
    @SerializedName("starting_bid_amount")
    val startingBidAmount: String?,
    @SerializedName("status")
    val status: String?,
    @SerializedName("sudden_death")
    val suddenDeath: Boolean?
) {
    data class Product(
        @SerializedName("acceptOffers")
        val acceptOffers: Boolean?,
        @SerializedName("auction")
        val auction: Boolean?,
        @SerializedName("bid_count")
        val bidCount: Int?,
        @SerializedName("category")
        val category: Category?,
        @SerializedName("createdAt")
        val createdAt: String?,
        @SerializedName("description")
        val description: String?,
        @SerializedName("flashSale")
        val flashSale: Boolean?,
        @SerializedName("hazardousMaterial")
        val hazardousMaterial: Boolean?,
        @SerializedName("height")
        val height: Any?,
        @SerializedName("id")
        val id: Int?,
        @SerializedName("images")
        val images: List<String?>?,
        @SerializedName("length")
        val length: Any?,
        @SerializedName("mailClass")
        val mailClass: String?,
        @SerializedName("pricing")
        val pricing: String?,
        @SerializedName("processingCategory")
        val processingCategory: String?,
        @SerializedName("productCondition")
        val productCondition: String?,
        @SerializedName("productShow")
        val productShow: String?,
        @SerializedName("purchasedQuantity")
        val purchasedQuantity: String?,
        @SerializedName("quantity")
        val quantity: String?,
        @SerializedName("reserveForLive")
        val reserveForLive: Boolean?,
        @SerializedName("shippingProfileId")
        val shippingProfileId: Int?,
        @SerializedName("sku")
        val sku: String?,
        @SerializedName("status")
        val status: String?,
        @SerializedName("thumbnail")
        val thumbnail: List<String?>?,
        @SerializedName("title")
        val title: String?,
        @SerializedName("type")
        val type: Any?,
        @SerializedName("user")
        val user: User?,
        @SerializedName("userId")
        val userId: Int?,
        @SerializedName("variant")
        val variant: List<Any?>?,
        @SerializedName("videos")
        val videos: List<Any?>?,
        @SerializedName("weight")
        val weight: Any?,
        @SerializedName("width")
        val width: Any?
    ) {
        data class Category(
            @SerializedName("color")
            val color: String?,
            @SerializedName("id")
            val id: Int?,
            @SerializedName("image")
            val image: String?,
            @SerializedName("isSelected")
            val isSelected: Boolean?,
            @SerializedName("liveCount")
            val liveCount: Any?,
            @SerializedName("name")
            val name: String?,
            @SerializedName("thumbnail")
            val thumbnail: String?
        )

        data class User(
            @SerializedName("email")
            val email: String?,
            @SerializedName("id")
            val id: Int?,
            @SerializedName("name")
            val name: String?,
            @SerializedName("profileImage")
            val profileImage: String?,
            @SerializedName("username")
            val username: String?
        )
    }
}