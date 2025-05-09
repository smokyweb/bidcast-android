package io.bidswipe.app

import android.app.Application
import android.content.Context
import android.view.Gravity
import dagger.hilt.android.HiltAndroidApp

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



    }

}