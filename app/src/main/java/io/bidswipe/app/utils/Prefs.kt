package io.bidswipe.app.utils

import android.content.Context

class Prefs(ctx : Context) {

	companion object {
		const val SHARED_PREF = "selfelite_App"
		const val USER_EMAIL = "user_email"
		const val PUSH_TOKEN = "pushToken"
		const val TOKEN = "token"
		const val REFRESH_TOKEN = "refreshToken"
		const val USER = "user"
	}

	private val mPrefs = ctx.getSharedPreferences(SHARED_PREF , Context.MODE_PRIVATE)

	fun clear() {
		mPrefs.edit().clear().apply()
	}

	fun putString(key : String , value : String) {
		mPrefs.edit().putString(key , value).apply()
	}

	fun getString(key : String) = mPrefs.getString(key , "").toString()

	fun token() = mPrefs.getString(TOKEN , "").toString()

	fun fcmToken() = mPrefs.getString(PUSH_TOKEN , "").toString()

	fun userEmail() = mPrefs.getString(USER_EMAIL , "").toString()

/*	fun getUserData() = try {
		Gson().fromJson(mPrefs.getString(USER , "").toString() , User::class.java)
	} catch (e : Exception) {
		null
	}*/

}