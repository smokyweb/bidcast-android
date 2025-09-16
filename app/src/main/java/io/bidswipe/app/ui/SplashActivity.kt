package io.bidswipe.app.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import io.bidswipe.app.R

class SplashActivity : AppCompatActivity() {

	private var referrerCode : String? = null

	override fun onCreate(savedInstanceState : Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_splash)

		val referrer = intent.data?.getQueryParameter("referrer")

		Log.d("TAG" , "onCreate: $referrer ")

		if (referrer != null) {
			referrerCode = referrer

		} else {
			// Use Install Referrer API to get the referrer code
		}

		startActivity(Intent(this@SplashActivity, SpoofSocketActivity::class.java))

//		Handler(Looper.getMainLooper()).postDelayed({
//
//			if (Prefs(this@SplashActivity).token().isNotEmpty()) {
//				startActivity(this.toDash())
//			} else {
//				startActivity(this.toAuth())
//			}
//
//			finishAfterTransition()
//		} , 1500)
	}
}