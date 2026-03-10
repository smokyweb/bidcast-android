package io.bidswipe.app.model

data class SellModel(
	val icon : Int? ,
	val color : Int? ,
	val title : String? ,
	var subtitle : String? ,
	var selectedValue : String?="" ,
	var status : String?  = "",
)
