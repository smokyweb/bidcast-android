package io.bidswipe.app.network

import io.bidswipe.app.BuildConfig
import io.bidswipe.app.model.GetSubCategoriesRequest
import io.bidswipe.app.model.PaymentCardModel
import io.bidswipe.app.model.StoreProductRequest
import io.bidswipe.app.network.response.AboutUsResponse
import io.bidswipe.app.network.response.BlockedUnblockedResponse
import io.bidswipe.app.network.response.CheckKycResponse
import io.bidswipe.app.network.response.CommonResponse
import io.bidswipe.app.network.response.CreateBidResponse
import io.bidswipe.app.network.response.CreateOrderResponse
import io.bidswipe.app.network.response.CreateShowResponse
import io.bidswipe.app.network.response.FAQResponse
import io.bidswipe.app.network.response.FetchBidResponse
import io.bidswipe.app.network.response.FetchReferralResponse
import io.bidswipe.app.network.response.FetchSellerVerificationResponse
import io.bidswipe.app.network.response.FollowUnfollowResponse
import io.bidswipe.app.network.response.GenerateTokenResponse
import io.bidswipe.app.network.response.GetAgoraTokenResponse
import io.bidswipe.app.network.response.GetAllTipsResponse
import io.bidswipe.app.network.response.GetAuctionTypeResponse
import io.bidswipe.app.network.response.GetBlockedUsersResponse
import io.bidswipe.app.network.response.GetBuyerIdentityResponse
import io.bidswipe.app.network.response.GetCategoryResponse
import io.bidswipe.app.network.response.GetHowToSellResponse
import io.bidswipe.app.network.response.GetKYCDetailsRespnse
import io.bidswipe.app.network.response.GetLessonsResponse
import io.bidswipe.app.network.response.GetLiveSellerResponse
import io.bidswipe.app.network.response.GetMailClassesResponse
import io.bidswipe.app.network.response.GetMyInventoryResponse
import io.bidswipe.app.network.response.GetMyShowResponse
import io.bidswipe.app.network.response.GetNotificationResponse
import io.bidswipe.app.network.response.GetOffersResponse
import io.bidswipe.app.network.response.GetOrderDetailsResponse
import io.bidswipe.app.network.response.GetOrdersResponse
import io.bidswipe.app.network.response.GetPaymentCardsResponse
import io.bidswipe.app.network.response.GetPremierShopResponse
import io.bidswipe.app.network.response.GetPrepareStepResponse
import io.bidswipe.app.network.response.GetProductDetailsResponse
import io.bidswipe.app.network.response.GetProductsByStatusResponse
import io.bidswipe.app.network.response.GetProductsResponse
import io.bidswipe.app.network.response.GetPromotePlansResponse
import io.bidswipe.app.network.response.GetPromoteToolsResponse
import io.bidswipe.app.network.response.GetPurchaseDetail
import io.bidswipe.app.network.response.GetRatingResponse
import io.bidswipe.app.network.response.GetShippingAddressResponse
import io.bidswipe.app.network.response.GetStatesResponse
import io.bidswipe.app.network.response.GetSubCategoriesResponse
import io.bidswipe.app.network.response.GetTipAmountResponse
import io.bidswipe.app.network.response.GetTransactionsHistoryResponse
import io.bidswipe.app.network.response.GetUserProfileResponse
import io.bidswipe.app.network.response.LoginResponse
import io.bidswipe.app.network.response.SettingListResponse
import io.bidswipe.app.network.response.SignUpResponse
import io.bidswipe.app.network.response.StorePhoneNumberResponse
import io.bidswipe.app.network.response.StoreSellerIdResponse
import io.bidswipe.app.network.response.TermsConditionResponse
import io.bidswipe.app.network.response.UpdateLiveStatusResponse
import io.bidswipe.app.network.response.UpdateOfferResponse
import io.bidswipe.app.network.response.UserDeviceResponse
import io.bidswipe.app.network.response.UserProfileResponse
import io.bidswipe.app.network.response.UserSearchingResponse
import io.bidswipe.app.network.response.PageUrlResponse
import io.bidswipe.app.network.response.PayoutHistoryResponse
import io.bidswipe.app.network.response.SalesAnalyticsResponse
import io.bidswipe.app.network.response.SellerAnalyticsResponse
import io.bidswipe.app.network.response.SellerStatusResponse
import io.bidswipe.app.network.response.SentTipAmountResponse
import io.bidswipe.app.network.response.SetDefaultAddressResponse
import io.bidswipe.app.network.response.StoreProductResponse
import io.bidswipe.app.network.response.VisitorsAnalyticsResponse
import io.bidswipe.app.network.response.WalletInfoResponse
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.request
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiInterface {

	@Multipart
	@POST("api/register")
	suspend fun signUp(
		@Part("first_name") firstName : RequestBody ,
		@Part("last_name") lastName : RequestBody ,
		@Part("email") email : RequestBody ,
		@Part("password") password : RequestBody ,
		@Part("password_confirmation") passwordConfirmation : RequestBody ,
		@Part("referral_code") referralCode : RequestBody? ,
	) : SignUpResponse

	@Multipart
	@POST("api/login")
	suspend fun login(
		@Part("email") email : RequestBody ,
		@Part("password") password : RequestBody ,
	) : LoginResponse

	@POST("api/logout")
	suspend fun logout() : CommonResponse

	@Multipart
	@POST("api/forgot-password")
	suspend fun forgotPassword(
		@Part("email") email : RequestBody ,
	) : CommonResponse

	@Multipart
	@POST("api/verify-otp")
	suspend fun verifyOtp(
		@Part("email") email : RequestBody ,
		@Part("code") code : RequestBody ,
	) : CommonResponse

	@Multipart
	@POST("api/reset-password")
	suspend fun resetPassword(
		@Part("email") email : RequestBody ,
		@Part("password") password : RequestBody ,
		@Part("password_confirmation") confirmPassword : RequestBody ,
	) : CommonResponse

	@GET("api/about-us")
	suspend fun aboutUs() : AboutUsResponse

	@GET("api/terms-conditions")
	suspend fun getTermsConditions() : TermsConditionResponse

	@GET("api/privacy-policy")
	suspend fun getPrivacyPolicy() : TermsConditionResponse

	@GET("api/get-category")
	suspend fun getCategory(
		@Query("category_id") categoryId : String? = null ,
		@Query("type") type : String? = null ,
		@Query("search") search : String? = null ,
		@Query("get_count") getCount : String? = null
	) : GetCategoryResponse

	@POST("api/get-subcategories")
	suspend fun getSubCategories(
		@Body getSubCategoriesModel : GetSubCategoriesRequest
	) : GetSubCategoriesResponse

	@POST("api/user/favorite")
	suspend fun userFavorite(
		@Body storeProductModel : GetSubCategoriesRequest ,
	) : CommonResponse


	@GET("api/get-lesson")
	suspend fun getLesson() : GetLessonsResponse

	@GET("api/get-product")
	suspend fun getProduct(
		@Part("category_id") categoryId : RequestBody? ,
	) : CommonResponse


	@POST("api/store-product")
	suspend fun storeProduct(
		@Body storeProductModel : StoreProductRequest ,
		@Query("product_id") productId : String? = null ,
	) : CommonResponse

	@Multipart
	@POST("api/store-product-meta")
	suspend fun storeProductMeta(
		@Part productImages : List<MultipartBody.Part?>? ,
		@Part thumbnail : List<MultipartBody.Part?>? ,
	) : StoreProductResponse

	@GET("api/how-to-sell")
	suspend fun getHowToSellStep() : GetHowToSellResponse

	@GET("api/get-prepare")
	suspend fun getPrepareStep() : GetPrepareStepResponse

	@GET("api/get-FAQ")
	suspend fun getFAQ() : FAQResponse

	@Multipart
	@POST("api/contact-us")
	suspend fun contactUs(
		@Part("name") name : RequestBody? ,
		@Part("email") email : RequestBody? ,
		@Part("subject") subject : RequestBody? ,
		@Part("message") message : RequestBody? ,
	) : CommonResponse

	@Multipart
	@POST("api/store-schedule-show")
	suspend fun storeScheduleShow(
		@Part("title") title : RequestBody? ,
		@Part("date") date : RequestBody? ,
		@Part("time") time : RequestBody? ,
		@Part("category_id") categoryId : RequestBody? ,
		@Part("auction_type_id") auctionTypeId : RequestBody? ,
		@Part("product_ids[]") productIds :  List<Int> ,
		@Part thumbnails : List<MultipartBody.Part?>? ,
	) : CreateShowResponse

	@GET("api/get-auction-type")
	suspend fun getAuctionType() : GetAuctionTypeResponse

	@Multipart
	@POST("api/get-all-tips")
	suspend fun getAllTips(
		@Part("type") type : RequestBody? ,
	) : GetAllTipsResponse

	@Multipart
	@POST("api/get-user-product")
	suspend fun getUserProducts(
		@Part("user_id") userId : RequestBody? ,
		@Part("category_id") categoryId : RequestBody? ,
		@Part("page") page : RequestBody?
	) : GetProductsResponse

	@Multipart
	@POST("api/get-my-schedule-show")
	suspend fun getMyScheduledShow(
		@Part("type") type : RequestBody? ,
	) : GetMyShowResponse

	@Multipart
	@POST("api/get-profile-by-id")
	suspend fun getProfileById(
		@Part("id") userId : RequestBody? ,
	) : GetUserProfileResponse

	@Multipart
	@POST("api/follow-unfollow")
	suspend fun followUser(
		@Part("following_id") userId : RequestBody? ,
	) : FollowUnfollowResponse

	@Multipart
	@POST("api/offer/make")
	suspend fun makeOffer(
		@Part("amount") amount : RequestBody? ,
		@Part("product_id") productId : RequestBody? ,
	) : CommonResponse

	@Multipart
	@POST("api/offer/lists")
	suspend fun offerList(
		@Part("page") page : Int? ,
	) : GetOffersResponse

	@Multipart
	@POST("api/offer/update-status")
	suspend fun offerUpdateStatus(
		@Part("offer_id") offerId : RequestBody? ,
		@Part("status") status : RequestBody? ,
	) : UpdateOfferResponse

	@Multipart
	@POST("api/get-live-show")
	suspend fun getLiveShow(
		@Part("type") type : RequestBody? ,
		@Part("category") category : RequestBody? ,
		@Part("search") search : RequestBody? ,
		@Part("page") page : RequestBody?
	) : GetMyShowResponse

	@Multipart
	@POST("api/notify-live-user")
	suspend fun notifyLiveUser(
		@Part("live_user_id") userId : RequestBody? ,
	) : CommonResponse

	@Multipart
	@POST("api/fetch-product")
	suspend fun getProductDetails(
		@Part("product_id") productId : RequestBody? ,
	) : GetProductDetailsResponse

	@Multipart
	@POST("api/upsert-shipping-address")
	suspend fun addShippingAddress(
		@Part("type") type : RequestBody? ,
		@Part("name") name : RequestBody? ,
		@Part("phone_number") phoneNumber : RequestBody? ,
		@Part("street_address") streetAddress : RequestBody? ,
		@Part("pincode") pinCode : RequestBody? ,
		@Part("city") city : RequestBody? ,
		@Part("state") state : RequestBody? ,
	) : CommonResponse

	@GET("api/get-shipping-address")
	suspend fun getShippingAddress(
	) : GetShippingAddressResponse

	@POST("api/add-card-net")
	suspend fun addPaymentCard(
		@Body data : PaymentCardModel ,
	) : CommonResponse

	@GET("api/get-card-net")
	suspend fun getPaymentCard(
	) : GetPaymentCardsResponse

	@GET("api/setting/list")
	suspend fun settingsList(
	) : SettingListResponse

	@Multipart
	@POST("api/setting/store")
	suspend fun settingsStore(
		@Part("country_of_residence") countryOfResidence : RequestBody? ,
		@Part("direct_message") directMessage : RequestBody? ,
		@Part("receive_gifts") receiveGifts : RequestBody? ,
		@Part("enable_private_entry") enablePrivateEntry : RequestBody? ,
		@Part("show_reward_status") showRewardStatus : RequestBody? ,
		@Part("show_seller_tools") showSellerTools : RequestBody? ,
		@Part("enable_clips") enableClips : RequestBody? ,
		@Part("save_past_shows") savePastShows : RequestBody? ,
		@Part("activity_status") activityStatus : RequestBody? ,
		@Part("sync_phone_contacts") syncPhoneContacts : RequestBody? ,
		@Part("suggest_my_account") suggestMyAccount : RequestBody? ,
		@Part("haptic_feedback") hapticFeedback : RequestBody? ,
	) : CommonResponse

	@Multipart
	@POST("api/product/purchase-details")
	suspend fun getPurchaseProduct(
		@Part("shipping_id") shippingId : RequestBody? ,
		@Part("product_id") productId : RequestBody? ,
	) : GetPurchaseDetail

	@Multipart
	@POST("api/product/order")
	suspend fun createOrder(
		@Part("shipping_id") shippingId : RequestBody? ,
		@Part("product_id") productId : RequestBody? ,
		@Part("card_id") cardId : RequestBody? ,
		@Part("promo_code") promoCode : RequestBody? ,
		@Part("send_as_gift") sendAsGift : RequestBody? ,
		@Part("gift_user_id") giftUserId : RequestBody? ,
		@Part("gift_msg") giftMessage : RequestBody? ,
		@Part("shipping_charges") shippingCharges : RequestBody? ,
		@Part("tax_amount") taxAmount : RequestBody? ,
		@Part("sub_total") subTotal : RequestBody? ,
		@Part("total") total : RequestBody? ,
		@Part("discount") discount : RequestBody? ,
	) : CreateOrderResponse


	@Multipart
	@POST("api/buyer-identity/store")
	suspend fun storeBuyerIdentity(
		@Part image : MultipartBody.Part? ,
	) : CommonResponse

	@Multipart
	@POST("api/generate-token")
	suspend fun generateToken(
		@Part("schedule_show_id") showId : RequestBody? ,
	) : GenerateTokenResponse

	@Multipart
	@POST("api/upsert-device-details")
	suspend fun storeDeviceDetails(
		@Part("device_token") deviceToken : RequestBody? ,
		@Part("platform") plateform : RequestBody = "android".request() ,
		@Part("app_version") appVersion : RequestBody = BuildConfig.VERSION_NAME.request() ,
		@Part("time_zone") timeZone : RequestBody = Utils.timezone.request() ,
	) : UserDeviceResponse

	@Multipart
	@POST("api/schedule-show/update-live-status")
	suspend fun updateLiveStatus(
		@Part("schedule_show_id") showId : RequestBody? ,
		@Part("is_live") isLive : RequestBody? ,
	) : UpdateLiveStatusResponse

	@Multipart
	@POST("api/bid/store")
	suspend fun createBid(
		@Part("schedule_show_id") showId : RequestBody? ,
		@Part("user_id") userId : RequestBody? ,
		@Part("product_id") productId : RequestBody? ,
		@Part("bid_price") bidPrice : RequestBody? ,
	) : CreateBidResponse

	@GET("api/get-profile")
	suspend fun getUserProfile(
	) : UserProfileResponse

	@GET("api/seller-identity/fetch")
	suspend fun fetchSellerVerification(
	) : FetchSellerVerificationResponse


	@Multipart
	@POST("api/get-my-inventory")
	suspend fun getMyInventory(
		@Part("status") status : RequestBody? ,
		@Part("page") page : RequestBody? ,
		@Part("search") search : RequestBody? ,
	) : GetMyInventoryResponse

	@Multipart
	@POST("api/product/order-listing")
	suspend fun getOrderListing(
		@Part("page") page : RequestBody? ,
		@Part("type") type : RequestBody? ,
		@Part("search") search : RequestBody? ,
	) : GetOrdersResponse

	@Multipart
	@POST("api/seller-identity/store-id-card")
	suspend fun storeSellerId(
		@Part idCard : MultipartBody.Part ,
		@Part image : MultipartBody.Part ,
	) : StoreSellerIdResponse

	@Multipart
	@POST("api/seller-identity/store-phone-number")
	suspend fun storePhoneNumber(
		@Part("phone_number") phoneNumber : RequestBody? ,
	) : StorePhoneNumberResponse

	@Multipart
	@POST("api/seller-identity/otp-verify")
	suspend fun verifyNumberOtp(
		@Part("otp") otp : RequestBody? ,
	) : CommonResponse

	@Multipart
	@POST("api/seller-identity/store-payment-method")
	suspend fun storePaymentMethod(
		@Part("card_token") cardToken : RequestBody? ,
	) : CommonResponse

	@GET("api/buyer-identity/list")
	suspend fun fetchBuyerIdentity(
	) : GetBuyerIdentityResponse

	@POST("api/notification/listing")
	suspend fun getNotification(
	) : GetNotificationResponse

	@GET("api/bid/fetch")
	suspend fun fetchBids(
		@Query("page") page : String? ,
	) : FetchBidResponse

	@Multipart
	@POST("api/product/order-receipt")
	suspend fun getOrderReceipt(
		@Part("order_id") orderId : RequestBody? ,
	) : CommonResponse

	@Multipart
	@POST("api/product/order-details")
	suspend fun getOrderDetails(
		@Part("order_id") orderId : RequestBody? ,
	) : GetOrderDetailsResponse

	@Multipart
	@POST("promo/verify-code")
	suspend fun verifyPromoCode(
		@Part("order_id") orderId : RequestBody? ,
	) : GetOrderDetailsResponse

	@Multipart
	@POST("api/user/searching")
	suspend fun searchUsers(
		@Part("search") search : RequestBody? ,
	) : UserSearchingResponse

	@Multipart
	@POST("api/product/save")
	suspend fun saveSellerProduct(
		@Part("product_id") productId : RequestBody? ,
	) : CommonResponse

	@Multipart
	@POST("api/product/fetch-by-status")
	suspend fun getProductsByStatus(
		@Part("type") type : RequestBody? ,
		@Part("page") page : RequestBody? ,
	) : GetProductsByStatusResponse

	@Multipart
	@POST("api/notification/delete")
	suspend fun deleteNotification(
		@Part("id") id : RequestBody? ,
	) : CommonResponse

	@GET("api/stripe/kyc-details")
	suspend fun getKYCDetails(
	) : GetKYCDetailsRespnse

	@Multipart
	@POST("api/store-seller-verification")
	suspend fun storeSellerVerification(
		@Part idCard : MultipartBody.Part? ,
		@Part image : MultipartBody.Part? ,
		@Part("phone_verification") phoneVerification : RequestBody ,
		@Part("customerPaymentProfileId") cardId : RequestBody ,
	) : CommonResponse


	@POST("api/stripe/check-Kyc")
	suspend fun checkKyc(
	) : CheckKycResponse

	@POST("api/stripe/fund-transfer")
	suspend fun fundTransfer(
		@Part("amount") amount : RequestBody? ,
	) : CheckKycResponse

	@POST("api/stripe/payout-history")
	suspend fun getPayoutHistory(
	) : PayoutHistoryResponse

	@Multipart
	@POST("api/transaction-history/listing")
	suspend fun getTransactionsHistory(
		@Part("page") page : RequestBody? ,
		@Part("status") status : RequestBody?
	) : GetTransactionsHistoryResponse

	@Multipart
	@POST("api/update-profile")
	suspend fun updateProfile(
		@Part("first_name") firstName : RequestBody ,
		@Part("last_name") lastName : RequestBody ,
		@Part image : MultipartBody.Part? ,
		@Part("username") userName : RequestBody ,
		@Part("bio") bio : RequestBody ,
	) : CommonResponse

	@GET("api/referral-code/fetch")
	suspend fun fetchReferral(
	) : FetchReferralResponse

	@Multipart
	@POST("api/seller-rating")
	suspend fun storeSellerRating(
		@Part("seller_id") sellerId : RequestBody ,
		@Part("overall_rating") overAllRating : RequestBody ,
		@Part("shipping_rating") shippingRating : RequestBody ,
		@Part("packaging_rating") packagingRating : RequestBody ,
		@Part("accuracy_rating") accuracyRating : RequestBody ,
		@Part("comment") comment : RequestBody ,
	) : CommonResponse

	@GET("api/get-seller-rating")
	suspend fun getSellerRating(
		@Query("seller_id") sellerId : String? ,
	) : GetRatingResponse

	@Multipart
	@POST("api/send-chat-notification")
	suspend fun sendChatNotification(
		@Part("receiver_id") receiverId : RequestBody ,
		@Part("message") message : RequestBody ,
	) : CommonResponse

	@GET("api/get-pages-url/{slug}")
	suspend fun getPageUrl(
		@Path("slug") slug : String ,
		@Query("noheader") noheader : String = "1" ,
	) : PageUrlResponse

	@GET("api/seller-status")
	suspend fun getSellerStatus(
	) : SellerStatusResponse

	@Multipart
	@POST("api/set-default-shipping-address")
	suspend fun setDefaultShippingAddress(
		@Part("address_id") addressId : RequestBody? ,
	) : SetDefaultAddressResponse

	@Multipart
	@POST("api/set-default-card")
	suspend fun setDefaultCard(
		@Part("card_id") cardId : RequestBody? ,
	) : CommonResponse

	@Multipart
	@POST("api/delete-card-net")
	suspend fun deleteCard(
		@Part("payment_profile_id") cardId : RequestBody? ,
	) : CommonResponse

	@Multipart
	@POST("api/delete-shipping-address")
	suspend fun deleteAddress(
		@Part("address_id") addressId : RequestBody? ,
	) : CommonResponse

	@GET("api/get-states")
	suspend fun getStates(
	) : GetStatesResponse

	@POST("api/delete-product")
	suspend fun deleteProduct(
		@Query("product_id") productId : String? ,
	) : CommonResponse

	@Multipart
	@POST("api/block-unblock")
	suspend fun blockUnblockUser(
        @Part("blocked_id") blockedId : RequestBody ,
    ) : BlockedUnblockedResponse

	@GET("api/blocked-users")
	suspend fun getBlockedUsers(
	) : GetBlockedUsersResponse

	@GET("api/usps/mail-classes")
	suspend fun getMailClasses(
	) : GetMailClassesResponse

	@GET("api/get-premier-shop")
	suspend fun getPremierShop(
	) : GetPremierShopResponse

	@GET("api/get-promote-tools")
	suspend fun getPromoteTools(
	) : GetPromoteToolsResponse

	@GET("api/get-promote-show")
	suspend fun getPromoteShowList(
	) : GetPromotePlansResponse

	@Multipart
	@POST("api/schedule-show/store-promote-show")
	suspend fun promoteShow(
		@Part("schedule_show_id") scheduleShowId : RequestBody ,
		@Part("promote_show_id") promoteShowId : RequestBody ,
	) : CommonResponse

	@Multipart
	@POST("api/send-tip-amount")
	suspend fun sendTipAmount(
		@Part("seller_id") sellerId : RequestBody ,
		@Part("amount") amount : RequestBody ,
		@Part("card_number") cardNumber : RequestBody?
	) : SentTipAmountResponse

	@Multipart
	@POST("api/stripe/fund-transfer")
	suspend fun payout(
		@Part("amount") amount: RequestBody
	) : CommonResponse

	@POST("api/apply-premier-shop")
	suspend fun applyPremierShop(
	) : CommonResponse

	@GET("api/wallet-info")
	suspend fun walletInfo(
	) : WalletInfoResponse

	@GET("api/get-tip-amount")
	suspend fun getTipAmount(
	) : GetTipAmountResponse

	@GET("api/seller-analytic")
	suspend fun getSellerAnalytics(
	) : SellerAnalyticsResponse

	@GET("api/seller/visitor-analytics")
	suspend fun getVisitorsAnalytics(
	) : VisitorsAnalyticsResponse

	@GET("api/seller/sales-performance")
	suspend fun getSalesPerformance(
	) : SalesAnalyticsResponse

	@GET("api/get-live-seller")
	suspend fun getLiveSeller(
	): GetLiveSellerResponse

	@Multipart
	@POST("api/agora-token")
	suspend fun getAgoraToken(
		@Part("channel") channel: RequestBody,
		@Part("uid") uId: RequestBody?
	) : GetAgoraTokenResponse

}

