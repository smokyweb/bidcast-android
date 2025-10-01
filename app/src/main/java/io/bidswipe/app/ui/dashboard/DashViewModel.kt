package io.bidswipe.app.ui.dashboard

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.bidswipe.app.model.PaymentCardModel
import io.bidswipe.app.model.TutorialShowModel
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.repository.DashRepository
import io.bidswipe.app.network.response.BlockedUnblockedResponse
import io.bidswipe.app.network.response.CheckKycResponse
import io.bidswipe.app.network.response.CommonResponse
import io.bidswipe.app.network.response.CreateBidResponse
import io.bidswipe.app.network.response.CreateShowResponse
import io.bidswipe.app.network.response.FetchBidResponse
import io.bidswipe.app.network.response.GenerateTokenResponse
import io.bidswipe.app.network.response.GetBlockedUsersResponse
import io.bidswipe.app.network.response.GetCategoryResponse
import io.bidswipe.app.network.response.GetHowToSellResponse
import io.bidswipe.app.network.response.GetLessonsResponse
import io.bidswipe.app.network.response.GetMailClassesResponse
import io.bidswipe.app.network.response.GetMyInventoryResponse
import io.bidswipe.app.network.response.GetMyShowResponse
import io.bidswipe.app.network.response.GetOffersResponse
import io.bidswipe.app.network.response.GetPrepareStepResponse
import io.bidswipe.app.network.response.GetProductsByStatusResponse
import io.bidswipe.app.network.response.GetProductsResponse
import io.bidswipe.app.network.response.GetPromotePlansResponse
import io.bidswipe.app.network.response.GetSubCategoriesResponse
import io.bidswipe.app.network.response.UpdateLiveStatusResponse
import io.bidswipe.app.network.response.UpdateOfferResponse
import io.bidswipe.app.network.response.UserDeviceResponse
import io.bidswipe.app.network.response.UserProfileResponse
import io.bidswipe.app.network.response.PageUrlResponse
import io.bidswipe.app.network.response.StoreProductResponse
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import okhttp3.RequestBody
import javax.inject.Inject


@HiltViewModel
class DashViewModel @Inject constructor(val repo : DashRepository) : ViewModel() {

	var showDate = ""
	var showTime = ""
	var showId = ""
	var currentShowData : CreateShowResponse.Data? = null
	var showList = mutableListOf<GetPrepareStepResponse.Data?>()
	var currentStep = 0

	var lastIndex = MutableLiveData(0)

	var showData = MutableLiveData<TutorialShowModel>()

	val selectedCategories = mutableListOf<GetCategoryResponse.Data>()
	private var _logoutResponse = MutableLiveData<Resource<CommonResponse>>()
	val logoutRepo : MutableLiveData<Resource<CommonResponse>>
		get() = _logoutResponse

	fun logout(
	) = viewModelScope.launch {
		_logoutResponse.value = repo.logout()
	}

	private var _getCategoryResponse = MutableLiveData<Resource<GetCategoryResponse>>()
	val getCategoryRepo : MutableLiveData<Resource<GetCategoryResponse>>
		get() = _getCategoryResponse

	fun getCategory(
		categoryId : String? = null ,
		type : String? = null ,
		search : String? = null ,
	) = viewModelScope.launch {
		_getCategoryResponse.value = repo.getCategory(categoryId , type , search)
	}

	private var _getSubCategoriesResponse = MutableLiveData<Resource<GetSubCategoriesResponse>>()
	val getSubCategoriesRepo : MutableLiveData<Resource<GetSubCategoriesResponse>>
		get() = _getSubCategoriesResponse

	fun getSubCategories(categoryIds : List<Int> , subCategoryIds : List<Int>? = null) = viewModelScope.launch {
		_getSubCategoriesResponse.value = repo.getSubCategories(categoryIds , subCategoryIds)
	}

	private var _userFavoriteResponse = MutableLiveData<Resource<CommonResponse>>()
	val userFavoriteRepo : MutableLiveData<Resource<CommonResponse>>
		get() = _userFavoriteResponse

	fun userFavorite(categoryIds : List<Int> , subcategoriesIds : List<Int>? = null) = viewModelScope.launch {
		_userFavoriteResponse.value = repo.userFavorite(categoryIds , subcategoriesIds)
	}

	private var _getSubCategoryResponse = MutableLiveData<Resource<GetCategoryResponse>>()
	val getSubCategoryRepo : MutableLiveData<Resource<GetCategoryResponse>>
		get() = _getSubCategoryResponse

	fun getSubCategory(
		categoryId : String? = null ,
	) = viewModelScope.launch {
		_getSubCategoryResponse.value = repo.getCategory(categoryId)
	}

	private var _getLessonResponse = MutableLiveData<Resource<GetLessonsResponse>>()
	val getLessonRepo : MutableLiveData<Resource<GetLessonsResponse>>
		get() = _getLessonResponse

	fun getLesson() = viewModelScope.launch {
		_getLessonResponse.value = repo.getLesson()
	}

	private var _storeProductResponse = MutableLiveData<Resource<CommonResponse>>()
	val storeProductRepo : MutableLiveData<Resource<CommonResponse>>
		get() = _storeProductResponse

	fun storeProduct(
		categoryId : String? ,
		title : String? ,
		description : String? ,
		quantity : String? ,
		pricing : String? ,
		flashSale : String? ,
		acceptOffers : String? ,
		reserveForLive : String? ,
		shippingProfileId : String? ,
		status : String? ,
		productImages : List<Map<String , String?>>? ,
		subCategoryId : Int? = null ,
		productId : String? = null ,
		variant : List<Map<String? , Any?>>? = null , width : String? = null ,
		height : String? = null ,
		length : String? = null ,
		weight : String? = null ,
		mailClass : String? = null ,
		processingCategory : String? = null ,

		) = viewModelScope.launch {
		_storeProductResponse.value = repo.storeProduct(
			categoryId ,
			title ,
			description ,
			quantity ,
			pricing ,
			flashSale ,
			acceptOffers ,
			reserveForLive ,
			shippingProfileId ,
			status ,
			productImages ,
			subCategoryId ,
			productId ,
			variant ,
			width ,
			height ,
			length ,
			weight ,
			mailClass ,
			processingCategory
		)
	}

	private var _storeProductMetaResponse = MutableLiveData<Resource<StoreProductResponse>>()
	val storeProductMetaRepo : MutableLiveData<Resource<StoreProductResponse>>
		get() = _storeProductMetaResponse

	fun storeProductMeta(productImages : List<MultipartBody.Part>? , thumbnail : List<MultipartBody.Part>?) = viewModelScope.launch {
		_storeProductMetaResponse.value = repo.storeProductMeta(productImages , thumbnail)
	}

	private var _getHowToSellStepResponse = MutableLiveData<Resource<GetHowToSellResponse>>()
	val getHowToSellStepRepo : MutableLiveData<Resource<GetHowToSellResponse>>
		get() = _getHowToSellStepResponse

	fun getHowToSellStep() = viewModelScope.launch {
		_getHowToSellStepResponse.value = repo.getHowToSellStep()
	}

	private var _getPrepareStepResponse = MutableLiveData<Resource<GetPrepareStepResponse>>()
	val getPrepareStepRepo : MutableLiveData<Resource<GetPrepareStepResponse>>
		get() = _getPrepareStepResponse

	fun getPrepareStep() = viewModelScope.launch {
		_getPrepareStepResponse.value = repo.getPrepareStep()
	}

	private var _getLiveShowResponse = MutableLiveData<Resource<GetMyShowResponse>>()
	val getLiveShowRepo : MutableLiveData<Resource<GetMyShowResponse>>
		get() = _getLiveShowResponse

	fun getLiveShow(
		type : RequestBody? = null ,
		category : RequestBody? = null ,
		search : RequestBody? = null ,
		page : RequestBody? = null ,
	) = viewModelScope.launch {
		_getLiveShowResponse.value = repo.getLiveShow(type , category , search, page)
	}

	private var _offerListResponse = MutableLiveData<Resource<GetOffersResponse>>()
	val offerListRepo : MutableLiveData<Resource<GetOffersResponse>>
		get() = _offerListResponse

	fun offerList(
		page : Int ,
	) = viewModelScope.launch {
		_offerListResponse.value = repo.offerList(page)
	}

	private var _offerUpdateStatusResponse = MutableLiveData<Resource<UpdateOfferResponse>>()
	val offerUpdateStatusRepo : MutableLiveData<Resource<UpdateOfferResponse>>
		get() = _offerUpdateStatusResponse

	fun offerUpdateStatus(
		offerId : RequestBody? ,
		status : RequestBody? ,
	) = viewModelScope.launch {
		_offerUpdateStatusResponse.value = repo.offerUpdateStatus(offerId , status)
	}

	private var _addPaymentCardResponse = MutableLiveData<Resource<CommonResponse>>()
	val addPaymentCardRepo : MutableLiveData<Resource<CommonResponse>>
		get() = _addPaymentCardResponse

	fun addPaymentCard(
		data : PaymentCardModel ,
	) = viewModelScope.launch {
		_addPaymentCardResponse.value = repo.addPaymentCard(data)
	}

	private var _generateTokenResponse = MutableLiveData<Resource<GenerateTokenResponse>>()
	val generateTokenRepo : MutableLiveData<Resource<GenerateTokenResponse>>
		get() = _generateTokenResponse

	fun generateToken(
		showId : RequestBody? ,
	) = viewModelScope.launch {
		_generateTokenResponse.value = repo.generateToken(showId)
	}

	private var _storeDeviceDetailsResponse = MutableLiveData<Resource<UserDeviceResponse>>()
	val storeDeviceDetailsRepo : MutableLiveData<Resource<UserDeviceResponse>>
		get() = _storeDeviceDetailsResponse

	fun storeDeviceDetails(
		deviceToken : RequestBody? ,
	) = viewModelScope.launch {
		_storeDeviceDetailsResponse.value = repo.storeDeviceDetails(deviceToken)
	}

	private var _updateLiveStatusResponse = MutableLiveData<Resource<UpdateLiveStatusResponse>>()
	val updateLiveStatusRepo : MutableLiveData<Resource<UpdateLiveStatusResponse>>
		get() = _updateLiveStatusResponse

	fun updateLiveStatus(
		showId : RequestBody? ,
		isLive : RequestBody? ,
	) = viewModelScope.launch {
		_updateLiveStatusResponse.value = repo.updateLiveStatus(showId , isLive)
	}


	/*private var _updateLiveStatusResponse = MutableLiveData<Resource<UpdateLiveStatusResponse>>()
	val updateLiveStatusRepo: MutableLiveData<Resource<UpdateLiveStatusResponse>>
		get() = _updateLiveStatusResponse

	fun updateLiveStatus(
		showId: RequestBody?,
		isLive: RequestBody?
	) = viewModelScope.launch {
		_updateLiveStatusResponse.value = repo.(showId,isLive)
	}*/


	private var _getUserProfileResponse = MutableLiveData<Resource<UserProfileResponse>>()
	val getUserProfileRepo : MutableLiveData<Resource<UserProfileResponse>>
		get() = _getUserProfileResponse

	fun getUserProfile(
	) = viewModelScope.launch {
		_getUserProfileResponse.value = repo.getUserProfile()
	}

	private var _fetchBidsResponse = MutableLiveData<Resource<FetchBidResponse>>()
	val fetchBidsRepo : MutableLiveData<Resource<FetchBidResponse>>
		get() = _fetchBidsResponse

	fun fetchBids(
		page : String? ,
	) = viewModelScope.launch {
		_fetchBidsResponse.value = repo.fetchBids(page)
	}

	private var _getPurchasedProductsByStatusResponse =
		MutableLiveData<Resource<GetProductsByStatusResponse>>()
	val getPurchasedProductsByStatusRepo : MutableLiveData<Resource<GetProductsByStatusResponse>>
		get() = _getPurchasedProductsByStatusResponse

	fun getPurchasedProductsByStatus(
		type : RequestBody? ,
		page : RequestBody? ,
	) = viewModelScope.launch {
		_getPurchasedProductsByStatusResponse.value = repo.getProductsByStatus(type , page)
	}

	private var _getSavedProductsByStatusResponse =
		MutableLiveData<Resource<GetProductsByStatusResponse>>()
	val getSavedProductsByStatusRepo : MutableLiveData<Resource<GetProductsByStatusResponse>>
		get() = _getSavedProductsByStatusResponse

	fun getSavedProductsByStatus(
		type : RequestBody? ,
		page : RequestBody? ,
	) = viewModelScope.launch {
		_getSavedProductsByStatusResponse.value = repo.getProductsByStatus(type , page)
	}

	private var _checkKycResponse = MutableLiveData<Resource<CheckKycResponse>>()
	val checkKycRepo : MutableLiveData<Resource<CheckKycResponse>>
		get() = _checkKycResponse

	fun checkKyc(
	) = viewModelScope.launch {
		_checkKycResponse.value = repo.checkKyc()
	}


	private var _updateProfileResponse = MutableLiveData<Resource<CommonResponse>>()
	val updateProfileRepo : MutableLiveData<Resource<CommonResponse>>
		get() = _updateProfileResponse

	fun updateProfile(
		firstName : RequestBody ,
		lastName : RequestBody ,
		image : MultipartBody.Part? ,
		userName : RequestBody ,
		bio : RequestBody ,
	) = viewModelScope.launch {
		_updateProfileResponse.value = repo.updateProfile(firstName , lastName , image , userName , bio)
	}

	private var _getUserProductsResponse = MutableLiveData<Resource<GetProductsResponse>>()
	val getUserProductsRepo : MutableLiveData<Resource<GetProductsResponse>>
		get() = _getUserProductsResponse

	fun getUserProducts(
		userId : RequestBody? = null ,
		categoryId : RequestBody? = null ,
	) = viewModelScope.launch {
		_getUserProductsResponse.value = repo.getUserProducts(userId , categoryId)
	}

	private var _getMyInventoryResponse = MutableLiveData<Resource<GetMyInventoryResponse>>()
	val getMyInventoryRepo : MutableLiveData<Resource<GetMyInventoryResponse>>
		get() = _getMyInventoryResponse

	fun getMyInventory(
		status : RequestBody? ,
		page : RequestBody? ,
	) = viewModelScope.launch {
		_getMyInventoryResponse.value = repo.getMyInventory(status , page)
	}

	private var _storeScheduleShowResponse = MutableLiveData<Resource<CreateShowResponse>>()
	val storeScheduleShowRepo : MutableLiveData<Resource<CreateShowResponse>>
		get() = _storeScheduleShowResponse

	fun storeScheduleShow(
		title : RequestBody? ,
		date : RequestBody? ,
		time : RequestBody? ,
		categoryId : RequestBody? ,
		auctionTypeId : RequestBody? ,
		thumbnails : List<MultipartBody.Part?>? ,
		productIds : RequestBody? ,
	) = viewModelScope.launch {
		_storeScheduleShowResponse.value = repo.storeScheduleShow(
			title ,
			date ,
			time ,
			categoryId ,
			auctionTypeId ,
			thumbnails ,
			productIds
		)
	}

	private var _storeSellerRatingResponse = MutableLiveData<Resource<CommonResponse>>()
	val storeSellerRatingRepo : MutableLiveData<Resource<CommonResponse>>
		get() = _storeSellerRatingResponse

	fun storeSellerRating(
		sellerId : RequestBody ,
		overAllRating : RequestBody ,
		shippingRating : RequestBody ,
		packagingRating : RequestBody ,
		accuracyRating : RequestBody ,
		comment : RequestBody ,
	) = viewModelScope.launch {
		_storeSellerRatingResponse.value = repo.storeSellerRating(
			sellerId ,
			overAllRating ,
			shippingRating ,
			packagingRating ,
			accuracyRating ,
			comment
		)
	}

	private var _sendChatNotificationResponse = MutableLiveData<Resource<CommonResponse>>()
	val sendChatNotificationRepo : MutableLiveData<Resource<CommonResponse>>
		get() = _sendChatNotificationResponse

	fun sendChatNotification(
		receiverId : RequestBody ,
		message : RequestBody ,

		) = viewModelScope.launch {
		_sendChatNotificationResponse.value = repo.sendChatNotification(receiverId , message)
	}

	private var _pageUrlResponse = MutableLiveData<Resource<PageUrlResponse>>()
	val pageUrlRepo : MutableLiveData<Resource<PageUrlResponse>>
		get() = _pageUrlResponse

	fun getPageUrl(slug : String) = viewModelScope.launch {
		_pageUrlResponse.value = repo.getPageUrl(slug)
	}


	private var _createBidResponse = MutableLiveData<Resource<CreateBidResponse>>()
	val createBidRepo : MutableLiveData<Resource<CreateBidResponse>>
		get() = _createBidResponse

	fun createBid(
		showId : RequestBody? ,
		userId : RequestBody? ,
		productId : RequestBody? ,
		bidPrice : RequestBody? ,
	) = viewModelScope.launch {
		_createBidResponse.value = repo.createBid(showId , userId , productId , bidPrice)
	}

	companion object {
		const val SLUG_ABOUT_US = "about-us"
		const val SLUG_PRIVACY_POLICY = "privacy-policy"
		const val SLUG_FAQ = "faq"
		const val SLUG_TERMS = "terms-condition"
	}

	private var _blockUnblockUserResponse = MutableLiveData<Resource<BlockedUnblockedResponse>>()
	val blockUnblockUserRepo : MutableLiveData<Resource<BlockedUnblockedResponse>>
		get() = _blockUnblockUserResponse

	fun blockUnblockUser(
		blockedID : RequestBody ,
	) = viewModelScope.launch {
		_blockUnblockUserResponse.value = repo.blockUnblockUser(blockedID)
	}

	private var _getBlockedUsersResponse = MutableLiveData<Resource<GetBlockedUsersResponse>>()
	val getBlockedUsersRepo : MutableLiveData<Resource<GetBlockedUsersResponse>>
		get() = _getBlockedUsersResponse

	fun getBlockedUsers() = viewModelScope.launch {
		_getBlockedUsersResponse.value = repo.getBlockedUsers()
	}

	private var _getMailClassesResponse = MutableLiveData<Resource<GetMailClassesResponse>>()
	val getMailClassesRepo : MutableLiveData<Resource<GetMailClassesResponse>>
		get() = _getMailClassesResponse

	fun getMailClasses() = viewModelScope.launch {
		_getMailClassesResponse.value = repo.getMailClasses()
	}
	
	private var _getPromoteShowListResponse = MutableLiveData<Resource<GetPromotePlansResponse>>()
	val getPromoteShowListRepo : MutableLiveData<Resource<GetPromotePlansResponse>>
		get() = _getPromoteShowListResponse

	fun getPromoteShowList() = viewModelScope.launch {
		_getPromoteShowListResponse.value = repo.getPromoteShowList()
	}
	
	private var _promoteShowResponse = MutableLiveData<Resource<CommonResponse>>()
	val promoteShowRepo : MutableLiveData<Resource<CommonResponse>>
		get() = _promoteShowResponse

	fun promoteShow(
		scheduleShowId : RequestBody,
		promoteShowId : RequestBody
	) = viewModelScope.launch {
		_promoteShowResponse.value = repo.promoteShow(scheduleShowId , promoteShowId)
	}


}