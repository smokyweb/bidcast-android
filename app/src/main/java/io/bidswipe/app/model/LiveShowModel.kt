package io.bidswipe.app.model

import org.json.JSONArray
import org.json.JSONObject
import java.io.Serializable

data class LiveShowModel(
    val products: List<Product?>,
    val roomId: String?,
    val seller: Seller?,
    val showDetail: String,
    val thumbnail: String,
    var viewerCount: String,
    val highestBid: HighestBid,
    val isLive: Boolean,
    val time: String?, // CURRENT TIMESTAMP
    val showId: String?,
    val allowBidForAll: Boolean? = true,
    val bidCountDown: String?,
    val showTimer: String?
) : Serializable {
	data class Product(
        val category: String? = null,
        val id: String? = null,
        val image: String? = "",
        val status: String? = "live",
        val name: String? = null,
        val price: String? = null,
        val quantity: String? = null,
        var isCurrent: Boolean? = false
    ) : Serializable {
        fun toJson() = JSONObject().apply {
            put("category", category)
            put("id", id)
            put("image", image)
            put("status", status)
            put("name", name)
            put("price", price)
            put("quantity", quantity)
            put("is_current", isCurrent)
		}

        companion object {
            fun fromJson(json: JSONObject) = Product(
                category = json.optString("category", null),
                id = json.optString("id", null),
                image = json.optString("image", ""),
                status = json.optString("status", "live"),
                name = json.optString("name", null),
                price = json.optString("price", null),
                quantity = json.optString("quantity", null),
                isCurrent = if (json.has("is_current")) json.optBoolean("is_current") else false
            )
        }
    }
	data class Seller(
        val id: String? = null,
        val image: String? = null,
        val name: String? = "test",
        val rating: String? = null
    ) : Serializable {
        fun toJson() = JSONObject().apply {
            put("id", id)
            put("image", image)
            put("name", name)
            put("rating", rating)
        }

        companion object {
            fun fromJson(json: JSONObject) = Seller(
                id = json.optString("id", null),
                image = json.optString("image", null),
                name = json.optString("name", "test"),
                rating = json.optString("rating", null)
            )
        }
	}
	data class HighestBid(
        val bidAmount: String? = null,
        val userName: String? = null,
        val userImage: String? = "test",
        val userId: String? = null,
        val productId: String? = ""
    ) : Serializable {
        fun toJson() = JSONObject().apply {
            put("bid_amount", bidAmount)
            put("user_name", userName)
            put("user_image", userImage)
            put("user_id", userId)
            put("product_id", productId)
        }

        companion object {
            fun fromJson(json: JSONObject) = HighestBid(
                bidAmount = json.optString("bid_amount", null),
                userName = json.optString("user_name", null),
                userImage = json.optString("user_image", "test"),
                userId = json.optString("user_id", null),
                productId = json.optString("product_id", "")
            )
        }
    }

    fun toJson() = JSONObject().apply {
        put("products", JSONArray().apply { products.forEach { put(it?.toJson()) } })
        put("room_id", roomId)
        put("seller", seller?.toJson())
        put("show_detail", showDetail)
        put("thumbnail", thumbnail)
        put("viewer_count", viewerCount)
        put("highest_bid", highestBid.toJson())
        put("is_live", isLive)
        put("time", time)
        put("show_id", showId)
        put("allow_bid_for_all", allowBidForAll)
        put("bid_count_down", bidCountDown)
        put("show_timer", showTimer)
	}

    companion object {
        fun fromJson(json: JSONObject) = LiveShowModel(
            products = json.optJSONArray("products")
                ?.let { array ->
                    (0 until array.length()).map { i ->
                        array.optJSONObject(i)?.let { Product.fromJson(it) }
                    }
                } ?: emptyList(),
            roomId = json.optString("room_id", null),
            seller = json.optJSONObject("seller")?.let { Seller.fromJson(it) },
            showDetail = json.optString("show_detail", ""),
            thumbnail = json.optString("thumbnail", ""),
            viewerCount = json.optString("viewer_count", "0"),
            highestBid = json.optJSONObject("highest_bid")?.let { HighestBid.fromJson(it) }
                ?: HighestBid(),
            isLive = json.optBoolean("is_live", false),
            time = json.optString("time", null),
            showId = json.optString("show_id", null),
            allowBidForAll = if (json.has("allow_bid_for_all")) json.optBoolean("allow_bid_for_all") else true,
            bidCountDown = json.optString("bid_count_down", null),
            showTimer = json.optString("show_timer", null)
		)
	}
}
