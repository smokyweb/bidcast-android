package io.bidswipe.app.model

data class MoreModel(
	val icon : Int? ,
	val title : String? ,
	val slug : String ,
)

data class SellerToolModel(
	val title : String? ,
	val list : MutableList<MoreModel> ,
)
