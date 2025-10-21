package io.bidswipe.app.network.response

import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep

@Keep
data class GetTransactionsHistoryResponse(
	@SerializedName("currentPage")
	val currentPage : Int? ,
	@SerializedName("data")
	val `data` : List<Data?>? ,
	@SerializedName("error_type")
	val errorType : String? ,
	@SerializedName("message")
	val message : String? ,
	@SerializedName("perPage")
	val perPage : Int? ,
	@SerializedName("status")
	val status : String? ,
	@SerializedName("total")
	val total : String? ,
	@SerializedName("totalPage")
	val totalPage : Int? ,
)  {
	@Keep
	data class Data(
		@SerializedName("account_number")
		val accountNumber: Any?,
		@SerializedName("buyer")
		val buyer: Buyer?,
		@SerializedName("buyer_email")
		val buyerEmail: String?,
		@SerializedName("buyer_name")
		val buyerName: String?,
		@SerializedName("card_number")
		val cardNumber: String?,
		@SerializedName("charge_id")
		val chargeId: String?,
		@SerializedName("counterparty_name")
		val counterpartyName: String?,
		@SerializedName("date")
		val date: String?,
		@SerializedName("discount")
		val discount: Double,
		@SerializedName("id")
		val id: Int?,
		@SerializedName("order_id")
		val orderId: Int?,
		@SerializedName("payment_intent_id")
		val paymentIntentId: Any?,
		@SerializedName("product_price")
		val productPrice: Double,
		@SerializedName("receiver")
		val `receiver`: Receiver?,
		@SerializedName("seller_id")
		val sellerId: Int?,
		@SerializedName("sender")
		val sender: Sender?,
		@SerializedName("shipping_charges")
		val shippingCharges: Double,
		@SerializedName("source_type")
		val sourceType: String?,
		@SerializedName("status")
		val status: String?,
		@SerializedName("sub_total")
		val subTotal: String?,
		@SerializedName("tax_amount")
		val taxAmount: String?,
		@SerializedName("total")
		val total: String?,
		@SerializedName("type")
		val type: String?,
		@SerializedName("user_id")
		val userId: Int?
	) {
		@Keep
		data class Buyer(
			@SerializedName("email")
			val email: String?,
			@SerializedName("id")
			val id: Int?,
			@SerializedName("name")
			val name: String?
		)

		@Keep
		data class Receiver(
			@SerializedName("id")
			val id: Int?,
			@SerializedName("name")
			val name: String?
		)

		@Keep
		data class Sender(
			@SerializedName("id")
			val id: Int?,
			@SerializedName("name")
			val name: String?
		)
	}
}