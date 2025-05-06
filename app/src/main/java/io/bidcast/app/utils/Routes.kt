package io.bidcast.app.utils

import android.content.Context
import android.content.Intent
import io.bidcast.app.ui.auth.AuthActivity
import io.bidcast.app.ui.dashboard.DashActivity
import io.bidcast.app.ui.dashboard.sell.ListAProductActivity
import io.bidcast.app.ui.dashboard.tutorials.TutorialsActivity

fun Context.toAuth() = Intent(this , AuthActivity::class.java)

fun Context.toDash() = Intent(this , DashActivity::class.java)

fun Context.toListProduct() = Intent(this , ListAProductActivity::class.java)

fun Context.toTutorials() = Intent(this , TutorialsActivity::class.java)