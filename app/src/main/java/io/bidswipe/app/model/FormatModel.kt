package io.bidswipe.app.model

class FormatModel(
	val icon: Int?,
	val title: String?,
	var selected: Boolean? = false,
)

class PromoteShowModel(
	val title: String,
	val subtitle: String,
	val description: String,
	val price: String,
	val gradientColors: List<Int>,
	val iconResId: Int
)
