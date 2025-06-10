package io.bidswipe.app.network

import io.bidswipe.app.network.response.AboutUsResponse
import io.bidswipe.app.network.response.CommonResponse
import io.bidswipe.app.network.response.FAQResponse
import io.bidswipe.app.network.response.GenerateTokenResponse
import io.bidswipe.app.network.response.GetAllTipsResponse
import io.bidswipe.app.network.response.GetAuctionTypeResponse
import io.bidswipe.app.network.response.GetCategoryResponse
import io.bidswipe.app.network.response.GetHowToSellResponse
import io.bidswipe.app.network.response.GetLessonsResponse
import io.bidswipe.app.network.response.GetMyShowResponse
import io.bidswipe.app.network.response.GetOffersResponse
import io.bidswipe.app.network.response.GetPaymentCardsResponse
import io.bidswipe.app.network.response.GetPrepareStepResponse
import io.bidswipe.app.network.response.GetProductDetailsResponse
import io.bidswipe.app.network.response.GetProductsResponse
import io.bidswipe.app.network.response.GetPurchaseDetail
import io.bidswipe.app.network.response.GetShippingAddressResponse
import io.bidswipe.app.network.response.GetUserProfileResponse
import io.bidswipe.app.network.response.LoginResponse
import io.bidswipe.app.network.response.SettingListResponse
import io.bidswipe.app.network.response.SignUpResponse
import io.bidswipe.app.network.response.TermsConditionResponse
import io.bidswipe.app.network.response.UpdateOfferResponse
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
		@Part productImages: List<MultipartBody.Part?>?
	): CommonResponse
	
	@GET("api/how-to-sell")
	suspend fun getHowToSellStep(): GetHowToSellResponse
	
	@GET("api/get-prepare")
	suspend fun getPrepareStep(): GetPrepareStepResponse
	
	@GET("api/get-FAQ")
	suspend fun getFAQ(): FAQResponse

	@Multipart
	@POST("api/contact-us")
	suspend fun contactUs(
		@Part("name") name: RequestBody?,
		@Part("email") email: RequestBody?,
		@Part("subject") subject : RequestBody?,
		@Part("message") message : RequestBody?
	): CommonResponse

	@Multipart
	@POST("api/store-schedule-show")
	suspend fun storeScheduleShow(
		@Part("title") title: RequestBody?,
		@Part("date") date: RequestBody?,
		@Part("time") time: RequestBody?,
		@Part("category_id") categoryId: RequestBody?,
		@Part("auction_type_id") auctionTypeId: RequestBody?,
		@Part thumbnails: List<MultipartBody.Part?>?,
		@Part("product_ids[]") productIds: List<Int?>
	): CommonResponse
	
	@GET("api/get-auction-type")
	suspend fun getAuctionType(): GetAuctionTypeResponse
	
	@Multipart
	@POST("api/get-all-tips")
	suspend fun getAllTips(
		@Part("type") type: RequestBody?
	): GetAllTipsResponse
	
	
	@POST("api/get-user-product")
	suspend fun getUserProducts(
	): GetProductsResponse
	
	@Multipart
	@POST("api/get-my-schedule-show")
	suspend fun getMyScheduledShow(
		@Part("type") type: RequestBody?
	): GetMyShowResponse
	
	@Multipart
	@POST("api/get-profile-by-id")
	suspend fun getProfileById(
		@Part("id") userId: RequestBody?
	): GetUserProfileResponse
	
	@Multipart
	@POST("api/follow-unfollow")
	suspend fun followUser(
		@Part("following_id") userId: RequestBody?
	): CommonResponse
	
	@Multipart
	@POST("api/offer/make")
	suspend fun makeOffer(
		@Part("amount") amount: RequestBody?,
		@Part("product_id") productId: RequestBody?
	): CommonResponse
	
	@Multipart
	@POST("api/offer/lists")
	suspend fun offerList(
		@Part("page") page: Int
	): GetOffersResponse
	
	@Multipart
	@POST("api/offer/update-status")
	suspend fun offerUpdateStatus(
		@Part("offer_id") offerId: RequestBody?,
		@Part("status") status: RequestBody?
	): UpdateOfferResponse
	
	@POST("api/get-live-show")
	suspend fun getLiveShow(
	): GetMyShowResponse
	
	@Multipart
	@POST("api/notify-live-user")
	suspend fun notifyLiveUser(
		@Part("live_user_id") userId: RequestBody?
	): CommonResponse
	
	@Multipart
	@POST("api/fetch-product")
	suspend fun getProductDetails(
		@Part("product_id") productId: RequestBody?
	): GetProductDetailsResponse

	@Multipart
	@POST("api/upsert-shipping-address")
	suspend fun addShippingAddress(
		@Part("type") type: RequestBody?,
		@Part("name") name: RequestBody?,
		@Part("phone_number") phoneNumber: RequestBody?,
		@Part("street_address") streetAddress: RequestBody?,
		@Part("pincode") pinCode: RequestBody?
	): CommonResponse
	
	@GET("api/get-shipping-address")
	suspend fun getShippingAddress(
	): GetShippingAddressResponse
	
	@Multipart
	@POST("api/add-card")
	suspend fun addPaymentCard(
		@Part("card_token") cardToken: RequestBody?,
	): CommonResponse
	
	@GET("api/get-card")
	suspend fun getPaymentCard(
	): GetPaymentCardsResponse
@GET("api/setting/list")
	suspend fun settingsList(
	): SettingListResponse

	@Multipart
	@POST("api/setting/store")
	suspend fun settingsStore(
		@Part("country_of_residence")countryOfResidence:RequestBody?,
		@Part("direct_message")directMessage:RequestBody?,
		@Part("receive_gifts")receiveGifts:RequestBody?,
		@Part("enable_private_entry")enablePrivateEntry:RequestBody?,
		@Part("show_reward_status")showRewardStatus:RequestBody?,
		@Part("show_seller_tools")showSellerTools:RequestBody?,
		@Part("enable_clips")enableClips:RequestBody?,
		@Part("save_past_shows")savePastShows:RequestBody?,
		@Part("activity_status")activityStatus:RequestBody?,
		@Part("sync_phone_contacts")syncPhoneContacts:RequestBody?,
		@Part("suggest_my_account")suggestMyAccount:RequestBody?,
		@Part("haptic_feedback")hapticFeedback:RequestBody?,
	): CommonResponse

	@Multipart
	@POST("api/product/purchase-details")
	suspend fun getPurchaseProduct(
		@Part("shipping_id") shippingId : RequestBody?,
		@Part("product_id") productId : RequestBody?
	): GetPurchaseDetail

	@Multipart
	@POST("api/product/order")
	suspend fun createOrder(
		@Part("shipping_id")shippingId:RequestBody?,
		@Part("product_id")productId:RequestBody?,
		@Part("card_id")cardId:RequestBody?,
		@Part("promo_code")promoCode:RequestBody?,
		@Part("send_as_gift")sendAsGift:RequestBody?,
		@Part("gift_user_id")giftUserId:RequestBody?,
		@Part("gift_msg")giftMessage:RequestBody?,
		@Part("shipping_charges")shippingCharges:RequestBody?,
		@Part("tax_amount")taxAmount:RequestBody?,
		@Part("sub_total")subTotal:RequestBody?,
		@Part("total")total:RequestBody?,
		@Part("discount")discount:RequestBody?
	): CommonResponse


	@Multipart
	@POST("api/buyer-identity/store")
	suspend fun storeBuyerIdentity(
		@Part image : MultipartBody.Part?
	): CommonResponse

	@Multipart
	@POST("api/generate-token")
	suspend fun generateToken(
		@Part("schedule_show_id")showId:RequestBody?
	): GenerateTokenResponse

}

