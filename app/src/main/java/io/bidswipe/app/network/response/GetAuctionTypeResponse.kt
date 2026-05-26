package io.bidswipe.app.network.response


import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep

@Keep
data class GetAuctionTypeResponse(
	@SerializedName("data")
	val `data` : List<Data?>? ,
	@SerializedName("error_type")
	val errorType : String? ,
	@SerializedName("message")
	val message : String? ,
	@SerializedName("status")
	val status : String? ,
) {
	@Keep
	data class Data(
		@SerializedName("id")
		val id : Int? ,
		@SerializedName("name")
		val name : String? ,
	)
}

enum class AuctionType(val id: Int, val displayName: String) {

	BUY_NOW(5, "Buy Now Auction"),
	LIVE(8, "Live Auction"),
	SURPRISE_SETS(9, "Surprise Sets");

	companion object {
		fun fromId(id: Int): AuctionType? = entries.find { it.id == id }
	}
}
