package io.bidswipe.app.model

data class OfferModel(
    val amount : String ,
    var percent : String ,
    var selected : Boolean? = false ,
)
