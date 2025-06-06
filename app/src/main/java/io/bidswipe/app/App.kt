package io.bidswipe.app

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.Process
import com.google.firebase.FirebaseApp
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

        FirebaseApp.initializeApp(applicationContext)


    }

    private fun isMainProcess(): Boolean {
        val pid = Process.myPid()
        val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        for (processInfo in manager.getRunningAppProcesses()) {
            if (processInfo.pid == pid) {
                return getPackageName() == processInfo.processName
            }
        }
        return false
    }

}