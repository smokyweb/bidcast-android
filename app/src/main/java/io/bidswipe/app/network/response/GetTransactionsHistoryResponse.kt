package io.bidswipe.app.network.response

import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep
import io.bidswipe.app.network.response.GetTipAmountResponse.Data.Tip.Show

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
		val userId: Int?,
		@SerializedName("show")
		val show: Show?,
		@SerializedName("show_id")
		val showId: Int?,
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
		@Keep
		data class Show(
			@SerializedName("auction_type_id")
			val auctionTypeId: Int?,
			@SerializedName("category_id")
			val categoryId: Int?,
			@SerializedName("date")
			val date: String?,
			@SerializedName("id")
			val id: Int?,
			@SerializedName("img_thumbnail")
			val imgThumbnail: List<String?>?,
			@SerializedName("is_explicit")
			val isExplicit: Boolean?,
			@SerializedName("is_live")
			val isLive: Boolean?,
			@SerializedName("is_repeat")
			val isRepeat: Boolean?,
			@SerializedName("language")
			val language: String?,
			@SerializedName("latest_viewer_count")
			val latestViewerCount: Int?,
			@SerializedName("product_ids")
			val productIds: List<String?>?,
			@SerializedName("promote_show_id")
			val promoteShowId: Any?,
			@SerializedName("promoted_at")
			val promotedAt: Any?,
			@SerializedName("recording_resource_id")
			val recordingResourceId: String?,
			@SerializedName("recording_sid")
			val recordingSid: String?,
			@SerializedName("repeat_value")
			val repeatValue: String?,
			@SerializedName("rtc_token")
			val rtcToken: String?,
			@SerializedName("share_count")
			val shareCount: Int?,
			@SerializedName("show_discoverability")
			val showDiscoverability: String?,
			@SerializedName("started_at")
			val startedAt: String?,
			@SerializedName("thumbnail")
			val thumbnail: List<String?>?,
			@SerializedName("time")
			val time: String?,
			@SerializedName("title")
			val title: String?,
			@SerializedName("user_id")
			val userId: Int?,
			@SerializedName("viewer_count")
			val viewerCount: Int?
		)
	}
}