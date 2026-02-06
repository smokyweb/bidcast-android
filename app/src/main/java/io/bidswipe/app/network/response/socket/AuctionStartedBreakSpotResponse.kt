package io.bidswipe.app.network.response.socket


import com.google.gson.annotations.SerializedName

data class AuctionStartedBreakSpotResponse(
    @SerializedName("auction_started_at")
    val auctionStartedAt: String?,
    @SerializedName("counter_bid_time")
    val counterBidTime: Int?,
    @SerializedName("productSetId")
    val productSetId: String?,
    @SerializedName("productSetItemId")
    val productSetItemId: String?,
    @SerializedName("productSetItemUnitId")
    val productSetItemUnitId: String?,
    @SerializedName("require_time")
    val requireTime: Int?,
    @SerializedName("room_id")
    val roomId: String?,
    @SerializedName("starting_bid_amount")
    val startingBidAmount: String?,
    @SerializedName("status")
    val status: String?,
    @SerializedName("sudden_death")
    val suddenDeath: Boolean?,
    @SerializedName("surprise_set_details")
    val surpriseSetDetails: SurpriseSetDetails?
) {
    data class SurpriseSetDetails(
        @SerializedName("product_set")
        val productSet: ProductSet?,
        @SerializedName("product_set_item")
        val productSetItem: ProductSetItem?,
        @SerializedName("sold_quantity")
        var soldQuantity: Int?,
        @SerializedName("total_quantity")
        val totalQuantity: Int?
    ) {
        data class ProductSet(
            @SerializedName("description")
            val description: String?,
            @SerializedName("id")
            val id: Int?,
            @SerializedName("name")
            val name: String?,
            @SerializedName("price")
            val price: Double?,
            @SerializedName("type")
            val type: String?
        )

        data class ProductSetItem(
            @SerializedName("description")
            val description: String?,
            @SerializedName("id")
            val id: Int?,
            @SerializedName("name")
            val name: String?,
            @SerializedName("quantity")
            val quantity: Int?,
            @SerializedName("sold_quantity")
            val soldQuantity: Int?,
            @SerializedName("status")
            val status: String?
        )
    }
}