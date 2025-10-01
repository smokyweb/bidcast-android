package io.bidswipe.app.network.response


import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep

@Keep
data class PayoutHistoryResponse(
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
        @SerializedName("account_number")
        val accountNumber: String?,
        @SerializedName("card_number")
        val cardNumber: Any?,
        @SerializedName("charge_id")
        val chargeId: Any?,
        @SerializedName("date")
        val date: String?,
        @SerializedName("discount")
        val discount: Int?,
        @SerializedName("id")
        val id: Int?,
        @SerializedName("order_id")
        val orderId: Any?,
        @SerializedName("payment_intent_id")
        val paymentIntentId: Any?,
        @SerializedName("product_price")
        val productPrice: Any?,
        @SerializedName("seller_id")
        val sellerId: Any?,
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