package io.bidswipe.app.network.response


import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep

@Keep
data class GetTransactionsHistoryResponse(
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
    val totalPage: Int?,
) {
    @Keep
    data class Data(
        @SerializedName("account_number")
        val accountNumber: String?,
        @SerializedName("card_number")
        val cardNumber: String?,
        @SerializedName("charge_id")
        val chargeId: String?,
        @SerializedName("date")
        val date: String?,
        @SerializedName("discount")
        val discount: Int?,
        @SerializedName("id")
        val id: Int?,
        @SerializedName("order_id")
        val orderId: Int?,
        @SerializedName("payment_intent_id")
        val paymentIntentId: String?,
        @SerializedName("product_price")
        val productPrice: String?,
        @SerializedName("seller_id")
        val sellerId: Int?,
        @SerializedName("shipping_charges")
        val shippingCharges: Int?,
        @SerializedName("source_type")
        val sourceType: String?,
        @SerializedName("status")
        val status: String?,
        @SerializedName("sub_total")
        val subTotal: Int?,
        @SerializedName("tax_amount")
        val taxAmount: Double?,
        @SerializedName("total")
        val total: Double?,
        @SerializedName("type")
        val type: String?,
        @SerializedName("user_id")
        val userId: Int?,
    )
}