package io.bidswipe.app

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.Process
import android.util.Log
import android.view.Gravity
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.MutableLiveData
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp
import es.dmoral.toasty.Toasty
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.RetrofitService
import io.bidswipe.app.network.repository.DashRepository
import io.bidswipe.app.network.response.GetCategoryResponse
import io.bidswipe.app.network.response.UserProfileResponse
import io.bidswipe.app.utils.AgoraManager
import io.bidswipe.app.utils.HapticManager
import io.bidswipe.app.utils.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltAndroidApp
class App : Application() {

	companion object {

		lateinit var mCtx: Context
		private lateinit var TAG: String
		var PIPMode: Boolean = false
		var isUserOnChatScreen: Boolean = false

		var isWatchStreamInPIP = MutableLiveData<Boolean>(false)
		var currentSellerId : String? = ""

		val profileResponse = MutableLiveData<UserProfileResponse.Data?>()
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

							Log.d(
								TAG,
								" getProfile: HAPTIC FEEDBACK : ${mData?.preferences?.hapticFeedback} "
							)
							HapticManager.setEnabled(mData?.preferences?.hapticFeedback ?: false)
						}

						is Resource.Error -> {
							profileResponse.value = null
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
							mData?.forEach {
								categoryList.add(it)
							}
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

		if (BuildConfig.DEBUG) {
			FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(false)
		}

		if (Prefs(mCtx).token().isNotEmpty()) {
			getProfile()
			getCategories()
		}

		Toasty.Config.getInstance()
			.setToastTypeface(ResourcesCompat.getFont(applicationContext, R.font.poppins_semi_bold)!!)
			.setGravity(Gravity.TOP, 0, 160)
			.supportDarkTheme(true)
			.allowQueue(false)
			.setTextSize(12)
			.apply()

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