package io.bidswipe.app.network

import io.bidswipe.app.network.response.AboutUsResponse
import io.bidswipe.app.network.response.CommonResponse
import io.bidswipe.app.network.response.FAQResponse
import io.bidswipe.app.network.response.GetAllTipsResponse
import io.bidswipe.app.network.response.GetAuctionTypeResponse
import io.bidswipe.app.network.response.GetCategoryResponse
import io.bidswipe.app.network.response.GetHowToSellResponse
import io.bidswipe.app.network.response.GetLessonsResponse
import io.bidswipe.app.network.response.GetPrepareStepResponse
import io.bidswipe.app.network.response.LoginResponse
import io.bidswipe.app.network.response.SignUpResponse
import io.bidswipe.app.network.response.TermsConditionResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiInterface {
	
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
		@Part("email") email: RequestBody,
		@Part("code") code: RequestBody
	): CommonResponse
	
	@Multipart
	@POST("api/reset-password")
	suspend fun resetPassword(
		@Part("email") email: RequestBody,
		@Part("password") password: RequestBody,
		@Part("password_confirmation") confirmPassword: RequestBody
	): CommonResponse
	
	@GET("api/about-us")
	suspend fun aboutUs(): AboutUsResponse
	
	@GET("api/terms-conditions")
	suspend fun getTermsConditions(): TermsConditionResponse
	
	@GET("api/privacy-policy")
	suspend fun getPrivacyPolicy(): TermsConditionResponse
	
	@GET("api/get-category")
	suspend fun getCategory(): GetCategoryResponse
	
	@GET("api/get-lesson")
	suspend fun getLesson(): GetLessonsResponse
	
	@GET("api/get-product")
	suspend fun getProduct(
		@Part("category_id") categoryId: RequestBody?
	): CommonResponse
	
	@Multipart
	@POST("api/store-product")
	suspend fun storeProduct(
		@Part("category_id") categoryId: RequestBody?,
		@Part("title") title: RequestBody?,
		@Part("description") description: RequestBody?,
		@Part("quantity") quantity: RequestBody?,
		@Part("pricing") pricing: RequestBody?,
		@Part("flash_sale") flashSale: RequestBody?,
		@Part("accept_offers") acceptOffers: RequestBody?,
		@Part("reserve_for_live") reserveForLive: RequestBody?,
		@Part("shipping_profile_id") shippingProfileId: RequestBody?,
		@Part("status") status: RequestBody?,
		@Part productImages: List<MultipartBody.Part>?
	): CommonResponse

	@GET("api/how-to-sell")
	suspend fun getHowToSellStep(): GetHowToSellResponse

	@GET("api/get-prepare")
	suspend fun getPrepareStep() : GetPrepareStepResponse

	@GET("api/get-FAQ")
	suspend fun getFAQ() : FAQResponse

	@Multipart
	@POST("api/store-schedule-show")
	suspend fun storeScheduleShow(
		@Part("title") title: RequestBody?,
		@Part("date") date: RequestBody?,
		@Part("time") time: RequestBody?,
		@Part("category_id") categoryId : RequestBody?,
		@Part("auction_type_id")  auctionTypeId : RequestBody?,
		@Part thumbnails: List<MultipartBody.Part>?,
		@Part("product_ids[]") productIds: List<Int?>
	): CommonResponse

	@GET("api/get-auction-type")
	suspend fun getAuctionType(): GetAuctionTypeResponse

	@Multipart
	@POST("api/get-all-tips")
	suspend fun getAllTips(
		@Part("type") type: RequestBody?
		): GetAllTipsResponse
}

