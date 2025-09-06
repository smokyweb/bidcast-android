package io.bidswipe.app.network.repository

import io.bidswipe.app.base.BaseRepository
import io.bidswipe.app.network.ApiInterface
import okhttp3.RequestBody
import javax.inject.Inject


class AuthRepository @Inject constructor(private val api : ApiInterface) : BaseRepository() {

	suspend fun signUp(
		firstName : RequestBody ,
		lastName : RequestBody ,
		email : RequestBody ,
		password : RequestBody ,
		confirmPassword : RequestBody ,
		referralCode : RequestBody? ,
	) = call { api.signUp(firstName , lastName , email , password , confirmPassword , referralCode) }

	suspend fun login(
		email : RequestBody ,
		password : RequestBody ,
	) = call {
		api.login(email , password)
	}

	suspend fun forgotPassword(
		email : RequestBody ,
	) = call {
		api.forgotPassword(email)
	}

	suspend fun verifyOtp(
		email : RequestBody ,
		code : RequestBody ,
	) = call {
		api.verifyOtp(email , code)
	}

	suspend fun resetPassword(
		email : RequestBody ,
		password : RequestBody ,
		confirmPassword : RequestBody ,
	) = call { api.resetPassword(email , password , confirmPassword) }

}