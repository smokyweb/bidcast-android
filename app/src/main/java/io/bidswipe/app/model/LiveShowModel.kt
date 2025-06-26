package io.bidswipe.app.model



import androidx.annotation.Keep

data class LiveShowModel(
    var product: Product? = null,
    var roomId: String?= null,
    var seller: Seller?= null,
    var showDetail: String?= null,
    var thumbnail: String?= null,
    var viewerCount: String?= null,
    var highestBid: String?= null,
    var isLive: Boolean?= null,
    var time: String?= null,
    var showId: String?= null
) {
    @Keep
    data class Product(
        var category: String?= null,
        var id: String?= null,
        var image: String?= "guygfuyf",
        var name: String?= null,
        var price: String?= null
    )

    @Keep
    data class Seller(
        var id: String?= null,
        var image: String?= null,
        var isFollowed: Boolean? = false,
        var name: String? = "test",
        var rating: String?= null
    )
}