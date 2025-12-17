package io.bidswipe.app.network.response


import com.google.gson.annotations.SerializedName

data class GetPromoteToolsDetailsResponse(
    @SerializedName("data")
    val `data`: Data?,
    @SerializedName("error_type")
    val errorType: String?,
    @SerializedName("message")
    val message: String?,
    @SerializedName("status")
    val status: String?
) {
    data class Data(
        @SerializedName("bids_from_promotion")
        val bidsFromPromotion: Int?,
        @SerializedName("community_boot")
        val communityBoot: Int?,
        @SerializedName("ctr")
        val ctr: Any?,
        @SerializedName("7_day_return_on_spend")
        val dayReturnOnSpend: Any?,
        @SerializedName("direct_sales_form_promotion")
        val directSalesFormPromotion: Any?,
        @SerializedName("first_time_buyers_from_promotion")
        val firstTimeBuyersFromPromotion: Any?,
        @SerializedName("follows_from_promotion")
        val followsFromPromotion: Any?,
        @SerializedName("immediate_return_on_spend")
        val immediateReturnOnSpend: Any?,
        @SerializedName("impession_per_hours")
        val impessionPerHours: Any?,
        @SerializedName("impressions")
        val impressions: Int?,
        @SerializedName("number_of_boost")
        val numberOfBoost: Int?,
        @SerializedName("number_of_show_promote")
        val numberOfShowPromote: Any?,
        @SerializedName("promote_hours")
        val promoteHours: Any?,
        @SerializedName("spend")
        val spend: Any?,
        @SerializedName("sustained_watches")
        val sustainedWatches: Any?,
        @SerializedName("sustained_watches_rate")
        val sustainedWatchesRate: Any?,
        @SerializedName("total_tapsa_and_clicks")
        val totalTapsaAndClicks: Any?
    )
}