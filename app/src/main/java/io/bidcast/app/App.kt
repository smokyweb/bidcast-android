package io.bidcast.app

import android.app.Application
import android.content.Context
import android.view.Gravity
import dagger.hilt.android.HiltAndroidApp
import es.dmoral.toasty.Toasty


@HiltAndroidApp
class App : Application() {

    companion object {

        private lateinit var mCtx: Context
        private lateinit var TAG: String

    }

    override fun onCreate() {
        super.onCreate()

        mCtx = applicationContext
        TAG = mCtx.packageName


        Toasty.Config.getInstance()
            //.setToastTypeface(ResourcesCompat.getFont(applicationContext, R.font.outfit_medium)!!)
            .setGravity(Gravity.TOP, 0, 32)
            .supportDarkTheme(true)
            .allowQueue(false)
            .setTextSize(12)
            .apply()


    }

}