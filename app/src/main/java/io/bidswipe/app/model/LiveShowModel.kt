package io.bidswipe.app.model

import androidx.annotation.Keep
import com.google.firebase.database.DataSnapshot

data class LiveShowModel(
	var products: List<Product?>? = null,
	var roomId: String? = null,
	var seller: Seller? = null,
	var showDetail: String? = null,
	var thumbnail: String? = null,
	var viewerCount: Int? = null,
	var highestBid: String? = null,
	var isLive: Boolean? = null,
	var time: String? =  System.currentTimeMillis().toString(),
	var showId: String? = null,
) {
	@Keep
	data class Product(
		var category: String? = null,
		var id: String? = null,
		var image: String? = "",
		var status: String? = "live", // "live", "sold"
		var name: String? = null,
		var price: String? = null,
		var currentBidderId: String? = null,
		var currentBidValue: String? = null,
		var isCurrent: Boolean = false,
	) {
		fun fromMap(it: DataSnapshot): Product = Product(
			category = it.child("category").getValue(String::class.java),
			id = it.child("id").getValue(String::class.java),
			image = it.child("image").getValue(String::class.java),
			status = it.child("status").getValue(String::class.java),
			name = it.child("name").getValue(String::class.java),
			price = it.child("price").getValue(String::class.java),
			currentBidderId = it.child("currentBidderId").getValue(String::class.java),
			currentBidValue = it.child("currentBidValue").getValue(String::class.java),
			isCurrent = it.child("isCurrent").getValue(Boolean::class.java) ?: false,
		)

		fun toMap() = mapOf(
			"category" to category,
			"id" to id,
			"image" to image,
			"status" to status,
			"name" to name,
			"price" to price,
			"currentBidderId" to currentBidderId,
			"currentBidValue" to currentBidValue,
			"isCurrent" to isCurrent,
		)
	}

	@Keep
	data class Seller(
		var id: String? = null,
		var image: String? = null,
		var isFollowed: Boolean? = false,
		var name: String? = "test",
		var rating: String? = null,
	) {
		fun fromMap(it: DataSnapshot): Seller = Seller(
			id = it.child("id").getValue(String::class.java),
			image = it.child("image").getValue(String::class.java),
			isFollowed = it.child("isFollowed").getValue(Boolean::class.java),
			name = it.child("name").getValue(String::class.java),
			rating = it.child("rating").getValue(String::class.java),
		)

		fun toMap() = mapOf(
			"id" to id,
			"image" to image,
			"isFollowed" to isFollowed,
			"name" to name,
			"rating" to rating,
		)
	}

	fun fromMap(it: DataSnapshot): LiveShowModel {
		val productList = mutableListOf<Product>()
		it.child("products").children.forEach { snapshot ->
			productList.add(Product().fromMap(snapshot))
		}

		return LiveShowModel(
			products = productList.ifEmpty { null },
			roomId = it.child("roomId").getValue(String::class.java),
			seller = Seller().fromMap(it.child("seller")),
			showDetail = it.child("showDetail").getValue(String::class.java),
			thumbnail = it.child("thumbnail").getValue(String::class.java),
			viewerCount = it.child("viewerCount").getValue(Int::class.java),
			highestBid = it.child("highestBid").getValue(String::class.java),
			isLive = it.child("isLive").getValue(Boolean::class.java),
			time = it.child("time").getValue(String::class.java),
			showId = it.child("showId").getValue(String::class.java),
		)
	}

	fun toMap() = mapOf(
		"products" to products?.map { it?.toMap() },
		"roomId" to roomId,
		"seller" to seller?.toMap(),
		"showDetail" to showDetail,
		"thumbnail" to thumbnail,
		"viewerCount" to viewerCount,
		"highestBid" to highestBid,
		"isLive" to isLive,
		"time" to time,
		"showId" to showId,
	)
}
