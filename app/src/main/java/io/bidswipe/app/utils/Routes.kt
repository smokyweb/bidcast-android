package io.bidswipe.app.utils

import android.content.Context
import android.content.Intent
import io.bidswipe.app.ui.auth.AuthActivity
import io.bidswipe.app.ui.dashboard.AddPaymentCardActivity
import io.bidswipe.app.ui.dashboard.DashActivity
import io.bidswipe.app.ui.dashboard.RateSellerActivity
import io.bidswipe.app.ui.dashboard.scheduleShow.ScheduleShowActivity
import io.bidswipe.app.ui.dashboard.sell.ListAProductActivity
import io.bidswipe.app.ui.dashboard.tutorials.TutorialsActivity

fun Context.toAuth() = Intent(this , AuthActivity::class.java)

fun Context.toDash() = Intent(this , DashActivity::class.java)

fun Context.toListProduct() = Intent(this , ListAProductActivity::class.java)

fun Context.toTutorials() = Intent(this , TutorialsActivity::class.java)

fun Context.toScheduleShow(from: String? = "") = Intent(this , ScheduleShowActivity::class.java).putExtra("from" , from)

fun Context.goToAddCard(from: String? = "") = Intent(this , AddPaymentCardActivity::class.java).putExtra("from" , from)

fun Context.goToRateSeller(sellerId: String?, sellerName: String?, sellerImage: String? ) = Intent(this ,
    RateSellerActivity::class.java).putExtra("sellerId" , sellerId).putExtra("sellerName", sellerName).putExtra("sellerImage", sellerImage)