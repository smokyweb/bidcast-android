package io.bidswipe.app.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import io.bidswipe.app.R
import io.bidswipe.app.utils.Prefs
import io.bidswipe.app.utils.toAuth
import io.bidswipe.app.utils.toDash

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)

        Handler(Looper.getMainLooper()).postDelayed({

            if (Prefs(this@SplashActivity).token().isNotEmpty()) {

                startActivity(this.toDash())

            } else {
                startActivity(this.toAuth())
            }

            finishAfterTransition()
        }, 1500)
    }
}