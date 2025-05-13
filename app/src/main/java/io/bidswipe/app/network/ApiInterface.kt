package io.bidswipe.app.network

import io.bidswipe.app.network.response.LoginResponse
import io.bidswipe.app.network.response.SignUpResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiInterface{

    @Multipart
    @POST("api/register")
    suspend fun signUp(
        @Part("first_name") firstName: RequestBody,
        @Part("last_name") lastName: RequestBody,
        @Part("email") email: RequestBody,
        @Part("password") password: RequestBody,
        @Part("password_confirmation") passwordConfirmation: RequestBody
    ): SignUpResponse

    @Multipart
    @POST("api/login")
    suspend fun login(
        @Part("email") email: RequestBody,
        @Part("password") password: RequestBody
    ): LoginResponse

}