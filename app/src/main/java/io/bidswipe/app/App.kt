package io.bidswipe.app

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.Process
import androidx.lifecycle.MutableLiveData
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.RetrofitService
import io.bidswipe.app.network.repository.DashRepository
import io.bidswipe.app.network.response.UserProfileResponse
import io.bidswipe.app.utils.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@HiltAndroidApp
class App : Application() {

    companion object {

        private lateinit var mCtx: Context
        private lateinit var TAG: String
        val profileResponse = MutableLiveData<UserProfileResponse.Data?>()


        fun getProfile() {
            CoroutineScope(Dispatchers.IO).launch {
                val repo = DashRepository(RetrofitService(mCtx).build())
                val it = repo.getUserProfile()

                withContext(Dispatchers.Main) {
                    when (it) {
                        is Resource.Success -> {
                            profileResponse.value = it.value.data
                        }

                        is Resource.Error -> {
                            profileResponse.value = null
                        }
                    }
                }
            }
        }


    }

    override fun onCreate() {
        super.onCreate()

        mCtx = applicationContext
        TAG = mCtx.packageName

        FirebaseApp.initializeApp(applicationContext)

        if (Prefs(mCtx).token().isNotEmpty()){
            getProfile()
        }


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