package io.bidswipe.app.network

import android.content.Context
import com.google.gson.GsonBuilder
import io.bidswipe.app.BuildConfig
import io.bidswipe.app.utils.Const.BASE_URL
import io.bidswipe.app.utils.Prefs
import io.bidswipe.app.utils.Utils
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class RetrofitService(private val mCtx: Context) {
	val loggingInterceptor = HttpLoggingInterceptor().apply {
		level = if (BuildConfig.DEBUG) {
			HttpLoggingInterceptor.Level.BODY
		} else {
			HttpLoggingInterceptor.Level.NONE
		}
	}

	fun build(): ApiInterface {
		val gson = GsonBuilder()
			.enableComplexMapKeySerialization()
			.setPrettyPrinting()
			.serializeNulls()
			.create()

		val okHttpClient = OkHttpClient.Builder().apply {
			addInterceptor(Interceptor { chain ->
				val request = chain.request().newBuilder().apply {
					val token = Prefs(mCtx).token()
					if (token.isEmpty().not()) {
						addHeader("Authorization", token)
					}
					addHeader("time_zone", Utils.timezone)
					addHeader("Content-Type", "application/json")
					addHeader("Accept", "application/json")
				}.build()
				chain.proceed(request)
			})
			addInterceptor(loggingInterceptor)
			connectTimeout(30, TimeUnit.SECONDS)
			readTimeout(30, TimeUnit.SECONDS)
			writeTimeout(30, TimeUnit.SECONDS)
		}.build()


		return Retrofit.Builder().apply {
			baseUrl(BASE_URL)
			client(okHttpClient)
			addConverterFactory(GsonConverterFactory.create(gson))
		}.build().create(ApiInterface::class.java)
	}
}

