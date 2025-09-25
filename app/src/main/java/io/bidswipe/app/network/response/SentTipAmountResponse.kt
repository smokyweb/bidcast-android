package io.bidswipe.app.network.response


import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep

@Keep
data class SentTipAmountResponse(
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
        @SerializedName("card_number")
        val cardNumber: String?,
        @SerializedName("date")
        val date: String?,
        @SerializedName("discount")
        val discount: Int?,
        @SerializedName("id")
        val id: Int?,
        @SerializedName("seller_id")
        val sellerId: String?,
        @SerializedName("shipping_charges")
        val shippingCharges: Int?,
        @SerializedName("source_type")
        val sourceType: String?,
        @SerializedName("status")
        val status: String?,
        @SerializedName("sub_total")
        val subTotal: Int?,
        @SerializedName("tax_amount")
        val taxAmount: Int?,
        @SerializedName("total")
        val total: Int?,
        @SerializedName("type")
        val type: String?,
        @SerializedName("user_id")
        val userId: Int?
    )
}