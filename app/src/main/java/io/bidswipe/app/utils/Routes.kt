package io.bidswipe.app.utils

import android.content.Context
import android.content.Intent
import io.bidswipe.app.model.LiveShowModel
import io.bidswipe.app.ui.agoraStream.AgoraPublisherActivity
import io.bidswipe.app.ui.auth.AuthActivity
import io.bidswipe.app.ui.dashboard.AddPaymentCardActivity
import io.bidswipe.app.ui.dashboard.DashActivity
import io.bidswipe.app.ui.dashboard.RateSellerActivity
import io.bidswipe.app.ui.interest.ChooseInterestActivity
import io.bidswipe.app.ui.product.OrderStatusActivity
import io.bidswipe.app.ui.scheduleShow.ScheduleShowActivity
import io.bidswipe.app.ui.sell.ListAProductActivity
import io.bidswipe.app.ui.tutorials.TutorialsActivity

fun Context.toAuth() = Intent(this, AuthActivity::class.java)

fun Context.toDash() = Intent(this, DashActivity::class.java)
fun Context.toChoose() = Intent(this, ChooseInterestActivity::class.java)

fun Context.toListProduct() = Intent(this, ListAProductActivity::class.java)

fun Context.toTutorials() = Intent(this, TutorialsActivity::class.java)

fun Context.toSellerShow(time: String?, showData: LiveShowModel?) = Intent(this, AgoraPublisherActivity::class.java)
	.putExtra("time", time)
	.putExtra("showData", showData)

fun Context.toOrderStatus(orderId: String?,from:String?=null) = Intent(this, OrderStatusActivity::class.java)
	.putExtra("orderId", orderId)
	.putExtra("from", from)

fun Context.toScheduleShow(from: String? = "",showId:String?="") = Intent(this, ScheduleShowActivity::class.java).apply {
	putExtra("from", from)
	putExtra("showId", showId)
}

fun Context.goToAddCard(from: String? = "") = Intent(this, AddPaymentCardActivity::class.java).putExtra("from", from)

fun Context.goToRateSeller(sellerId: String?, sellerName: String?, sellerImage: String?) = Intent(
	this,
	RateSellerActivity::class.java
).putExtra("sellerId", sellerId).putExtra("sellerName", sellerName).putExtra("sellerImage", sellerImage)