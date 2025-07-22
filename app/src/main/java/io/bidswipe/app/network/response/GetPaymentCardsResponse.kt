package io.bidswipe.app.network.response


import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep

@Keep
data class GetPaymentCardsResponse(
    @SerializedName("current_page")
    val currentPage: Int?,
    @SerializedName("data")
    val `data`: List<Data?>?,
    @SerializedName("message")
    val message: String?,
    @SerializedName("per_page")
    val perPage: Int?,
    @SerializedName("status")
    val status: String?,
    @SerializedName("total_pages")
    val totalPages: Int?,
    @SerializedName("total_records")
    val totalRecords: Int?,
) {
    @Keep
    data class Data(
        @SerializedName("card_holder_name")
        val cardHolderName: Any?,
        @SerializedName("card_id")
        val cardId: String?,
        @SerializedName("exp_month")
        val expMonth: Int?,
        @SerializedName("exp_year")
        val expYear: Int?,
        @SerializedName("fingerprint")
        val fingerprint: String?,
        @SerializedName("last4")
        val last4: String?,
        var selected: Boolean? = false,
    )
}