package io.bidswipe.app.model

data class TopBuyerModel(
    val rank: Int,
    val buyerName: String,
    val profileImage: String?,
    val value: String
)
