package io.bidswipe.app.network

import io.bidswipe.app.network.response.AboutUsResponse
import io.bidswipe.app.network.response.CommonResponse
import io.bidswipe.app.network.response.LoginResponse
import io.bidswipe.app.network.response.SignUpResponse
import io.bidswipe.app.network.response.TermsConditionResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.GET
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

    @POST("api/logout")
    suspend fun logout(): CommonResponse

    @Multipart
    @POST("api/forgot-password")
    suspend fun forgotPassword(
        @Part("email") email: RequestBody
    ): CommonResponse

    @Multipart
    @POST("api/verify-otp")
    suspend fun verifyOtp(
        @Part("email") email : RequestBody,
        @Part("code") code : RequestBody
    ): CommonResponse

    @Multipart
    @POST("api/reset-password")
    suspend fun resetPassword(
        @Part("email") email : RequestBody,
        @Part("password") password: RequestBody,
        @Part("password_confirmation") confirmPassword: RequestBody
    ): CommonResponse

    @GET("api/about-us")
    suspend fun aboutUs(): AboutUsResponse

    @GET("api/terms-conditions")
    suspend fun getTermsConditions(): TermsConditionResponse

    @GET("api/privacy-policy")
    suspend fun getPrivacyPolicy(): TermsConditionResponse

}