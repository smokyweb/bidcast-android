package io.bidswipe.app.network.response


import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep

@Keep
data class SellerStatusResponse(
	@SerializedName("status")
	val status : String? ,
	@SerializedName("message")
	val message : String? ,
	@SerializedName("error_type")
	val errorType : String? ,
	@SerializedName("data")
	val `data` : Data? ,
) {
	@Keep
	data class Data(
		@SerializedName("marketplace_vendor")
		val marketplaceVendor : MarketplaceVendor? ,
		@SerializedName("live_sell_vendor")
		val liveSellVendor : LiveSellVendor? ,
	) {
		@Keep
		data class MarketplaceVendor(
			@SerializedName("title")
			val title : String? ,
			@SerializedName("status")
			val status : String? ,
			@SerializedName("vendor_since")
			val vendorSince : String? ,
			@SerializedName("seller_rating")
			val sellerRating : Int? ,
		)

		@Keep
		data class LiveSellVendor(
			@SerializedName("title")
			val title : String? ,
			@SerializedName("status")
			val status : String? ,
			@SerializedName("submitted")
			val submitted : String? ,
		)
	}
}