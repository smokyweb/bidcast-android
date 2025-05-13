package io.bidswipe.app.network.repository

import io.bidswipe.app.base.BaseRepository
import io.bidswipe.app.network.ApiInterface
import okhttp3.MultipartBody
import okhttp3.RequestBody
import javax.inject.Inject


class AuthRepository @Inject constructor(private val api: ApiInterface) : BaseRepository(){

    suspend fun signUp(
        firstName: RequestBody,
        lastName: RequestBody,
        email: RequestBody,
        password: RequestBody,
        confirmPassword: RequestBody
    ) = call { api.signUp(firstName, lastName, email, password, confirmPassword) }

    suspend fun login(
        email : RequestBody,
        password: RequestBody
    ) = call {
        api.login(email , password )
    }

}