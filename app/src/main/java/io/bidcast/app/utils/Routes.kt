package io.bidcast.app.utils

import android.content.Context
import android.content.Intent
import io.bidcast.app.ui.auth.AuthActivity
import io.bidcast.app.ui.dashboard.DashActivity

fun Context.toAuth() = Intent(this , AuthActivity::class.java)

fun Context.toDash() = Intent(this , DashActivity::class.java)