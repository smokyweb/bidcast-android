package io.bidswipe.app

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.Process
import android.view.Gravity
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.MutableLiveData
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.gson.Gson
import dagger.hilt.android.HiltAndroidApp
import es.dmoral.toasty.Toasty
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.RetrofitService
import io.bidswipe.app.network.repository.DashRepository
import io.bidswipe.app.network.response.CheckKycResponse
import io.bidswipe.app.network.response.GetCategoryResponse
import io.bidswipe.app.network.response.UserProfileResponse
import io.bidswipe.app.utils.AgoraManager
import io.bidswipe.app.utils.Alerts.log
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.HapticManager
import io.bidswipe.app.utils.Prefs
import io.bidswipe.app.utils.SocketManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltAndroidApp
class App : Application() {

    companion object {
        var socketManager: SocketManager? = null
        lateinit var mCtx: Context
        private lateinit var TAG: String
        var PIPMode: Boolean = false
        var isUserOnChatScreen: Boolean = false

        var isWatchStreamInPIP = MutableLiveData<Boolean>(false)
        var currentSellerId: String? = ""

        val profileResponse = MutableLiveData<UserProfileResponse.Data?>()
        val checkKycResponse = MutableLiveData<CheckKycResponse.Data?>()
        var categoryList = mutableListOf<GetCategoryResponse.Data?>()

        lateinit var manager: AgoraManager

        fun getProfile() {
            CoroutineScope(Dispatchers.IO).launch {
                val repo = DashRepository(RetrofitService(mCtx).build())
                val it = repo.getUserProfile()

                withContext(Dispatchers.Main) {
                    when (it) {
                        is Resource.Success -> {
                            val mData = it.value.data
                            profileResponse.value = mData

                            Prefs(mCtx).putString(Prefs.USER, Gson().toJson(mData).toString())
                            HapticManager.setEnabled(mData?.preferences?.hapticFeedback ?: false)
                        }

                        is Resource.Error -> {
                            profileResponse.value = null
                        }
                    }
                }
            }
        }

        fun checkKYC() {
            CoroutineScope(Dispatchers.IO).launch {
                val repo = DashRepository(RetrofitService(mCtx).build())
                val it = repo.checkKyc()

                withContext(Dispatchers.Main) {
                    when (it) {
                        is Resource.Success -> {
                            val mData = it.value.data
                            checkKycResponse.value = mData
                        }

                        is Resource.Error -> {
                            // QA-FIX: was mutating profileResponse by mistake on a KYC
                            // failure, which wiped the logged-in profile and caused null
                            // derefs on downstream screens right after login.
                            checkKycResponse.value = null
                        }
                    }
                }
            }
        }

        fun getCategories() {
            CoroutineScope(Dispatchers.IO).launch {
                val repo = DashRepository(RetrofitService(mCtx).build())
                val it = repo.getCategory()

                withContext(Dispatchers.Main) {
                    when (it) {
                        is Resource.Success -> {
                            val mData = it.value.data
                            // QA-FIX (MC task cmo8tx4iw00ei3u1hejjq99ai):
                            // categoryList is shared across the whole process. On every
                            // call we were APPENDING without clearing, so repeated logins
                            // duplicated categories and downstream screens iterating the
                            // list could race / ConcurrentModification when another fetch
                            // re-entered. Rebuild the list atomically instead.
                            val rebuilt = mData?.toMutableList() ?: mutableListOf()
                            categoryList.clear()
                            categoryList.addAll(rebuilt)
                        }

                        is Resource.Error -> {
                            // QA-FIX: was mutating profileResponse by mistake on a
                            // category failure. Don't knock the profile out just because
                            // category fetch failed; leave the list as-is.
                        }
                    }
                }
            }
        }

        fun setUpSocket() {
            try {
                // QA-FIX (MC task cmo8tx4iw00ei3u1hejjq99ai):
                // The previous code did `Prefs(mCtx).getUserData()?.id.toString()` which,
                // when getUserData() is null, evaluates as `(null).toString()` => literal
                // string "null" going into the socket uid. That both confuses server-side
                // routing and, combined with socket lifecycle reuse across logins, can
                // lead to callbacks firing against a reused SocketManager. Guard the uid
                // and wrap the entire setup in try/catch so a bad socket init can never
                // crash the process.
                val uid = Prefs(mCtx).getUserData()?.id?.toString().orEmpty()
                if (uid.isEmpty()) {
                    log(javaClass.simpleName, "setUpSocket: no user id yet, skipping")
                    return
                }
                socketManager = SocketManager.getInstance(mCtx)
                socketManager?.initialize(Const.SOCKET_URL, mapOf("uid" to uid))
                socketManager?.connect(onConnected = {
                    log(javaClass.simpleName, "Socket connect")
                }) { err -> log(javaClass.simpleName, "Socket connect error: $err") }
            } catch (e: Throwable) {
                log(javaClass.simpleName, "setUpSocket failed: ${e.message}")
            }
        }

    }

    override fun onCreate() {
        super.onCreate()

        mCtx = applicationContext
        TAG = mCtx.packageName

        FirebaseApp.initializeApp(applicationContext)

        if (BuildConfig.DEBUG) {
            FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = false
        }

        if (Prefs(mCtx).token().isNotEmpty()) {
            getProfile()
            getCategories()
            checkKYC()
            setUpSocket()
        }

        // QA-FIX (MC task cmo8tx4iw00ei3u1hejjq99ai):
        // The previous code force-unwrapped ResourcesCompat.getFont(...) with !! inside
        // Application.onCreate. If the font fails to resolve (OEM vendor strip, resource
        // shrinker, rare device), the whole app crashes on startup / right after login
        // when the process is restarted. Make it safe and fall back to the default font.
        val cfg = Toasty.Config.getInstance()
            .setGravity(Gravity.TOP, 0, 160)
            .supportDarkTheme(true)
            .allowQueue(false)
            .setTextSize(12)
        try {
            val tf = ResourcesCompat.getFont(applicationContext, R.font.poppins_semi_bold)
            if (tf != null) cfg.setToastTypeface(tf)
        } catch (e: Throwable) {
            log(javaClass.simpleName, "Toasty font init failed: ${e.message}")
        }
        cfg.apply()

    }

    private fun isMainProcess(): Boolean {
        val pid = Process.myPid()
        val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        for (processInfo in manager.runningAppProcesses) {
            if (processInfo.pid == pid) {
                return packageName == processInfo.processName
            }
        }
        return false
    }


}