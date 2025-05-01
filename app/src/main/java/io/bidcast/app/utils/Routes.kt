package io.bidcast.app.utils

import android.content.Context
import android.content.Intent
import io.bidcast.app.ui.auth.AuthActivity

fun Context.toAuth() = Intent(this , AuthActivity::class.java)