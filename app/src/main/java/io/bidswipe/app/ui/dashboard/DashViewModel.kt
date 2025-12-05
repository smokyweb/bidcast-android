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
import io.bidswipe.app.network.response.CommonResponse
import io.bidswipe.app.network.response.CreateProductResponse
import io.bidswipe.app.network.response.CreateShowResponse
import io.bidswipe.app.network.response.FetchBidResponse
import io.bidswipe.app.network.response.GetAgoraTokenResponse
import io.bidswipe.app.network.response.GetBlockedUsersResponse
import io.bidswipe.app.network.response.GetCategoryResponse
import io.bidswipe.app.network.response.GetHowToSellResponse
import io.bidswipe.app.network.response.GetLessonsResponse
import io.bidswipe.app.network.response.GetLiveSellerResponse
import io.bidswipe.app.network.response.GetMailClassesResponse
import io.bidswipe.app.network.response.GetMyInventoryResponse
import io.bidswipe.app.network.response.GetMyShowResponse
import io.bidswipe.app.network.response.GetOffersResponse
import io.bidswipe.app.network.response.GetPrepareStepResponse
import io.bidswipe.app.network.response.GetProductsByStatusResponse
import io.bidswipe.app.network.response.GetPromotePlansResponse
import io.bidswipe.app.network.response.GetShippingProfilesResponse
import io.bidswipe.app.network.response.GetSubCategoriesResponse
import io.bidswipe.app.network.response.PageUrlResponse
import io.bidswipe.app.network.response.StoreProductResponse
import io.bidswipe.app.network.response.UpdateOfferResponse
import io.bidswipe.app.network.response.UserDeviceResponse
import io.bidswipe.app.network.response.UserProfileResponse
import io.bidswipe.app.utils.Const.NO_INTERNET_ERROR
import io.bidswipe.app.utils.NetworkMonitor
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import okhttp3.RequestBody
import javax.inject.Inject

@HiltViewModel
class DashViewModel @Inject constructor(
	val repo : DashRepository ,
	private val networkMonitor : NetworkMonitor ,
) : ViewModel() {

	var showDate = ""
	var showTime = ""
	var showId = ""
	var currentShowData : CreateShowResponse.Data? = null
	var showList = mutableListOf<GetPrepareStepResponse.Data?>()
	var currentStep = 0

	var lastIndex = MutableLiveData(0)

	var showData = MutableLiveData<TutorialShowModel>()

	val selectedCategories = mutableListOf<GetCategoryResponse.Data>()

	// Product form state (persists across orientation changes)
	var productFormImageList = mutableListOf<String?>()
	var productFormCategoryId = ""
	var productFormSubCategoryId = ""
	var productFormVariantList = mutableListOf<GetCategoryResponse.Data.ExtraField?>()
	var productFormPackageWidth = 0.0
	var productFormPackageHeight = 0.0
	var productFormPackageLength = 0.0
	var productFormPackageWeight = 0.0
	var productFormSelectedMailClass: GetMailClassesResponse.Data.MailClasses? = null
	var productFormProduct: GetMyInventoryResponse.Data? = null
	var productFormIsSubCategory = false
	var productFormProductTitle = ""
	var productFormDescription = ""
	var productFormQuantity = ""
	var productFormWidth = ""
	var productFormHeight = ""
	var productFormLength = ""
	var productFormWeight = ""
	var productFormMailClassText = ""
	var productFormProcessingCategory = ""
	var productFormPrice = ""
	var productFormFlashSale = false
	var productFormAcceptOffers = false
	var productFormReserveForLive = false
	var productFormCategoryText = ""
	private var _logoutResponse = MutableLiveData<Resource<CommonResponse>>()
	val logoutRepo : MutableLiveData<Resource<CommonResponse>>
		get() = _logoutResponse

	fun logout(
	) = viewModelScope.launch {
		if (! networkMonitor.hasInternet()) {
			_logoutResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_logoutResponse.value = repo.logout()
	}

	private var _getCategoryResponse = MutableLiveData<Resource<GetCategoryResponse>>()
	val getCategoryRepo : MutableLiveData<Resource<GetCategoryResponse>>
		get() = _getCategoryResponse

	fun getCategory(
		categoryId : String? = null ,
		type : String? = null ,
		search : String? = null ,
		getCount : String? = null ,
	) = viewModelScope.launch {
		if (! networkMonitor.hasInternet()) {
			_getCategoryResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_getCategoryResponse.value = repo.getCategory(categoryId , type , search , getCount)
	}

	private var _getProductSubCategoryResponse = MutableLiveData<Resource<GetCategoryResponse>>()
	val getProductSubCategoryRepo : MutableLiveData<Resource<GetCategoryResponse>>
		get() = _getProductSubCategoryResponse

	fun getProductSubCategory(
		categoryId : String?  ,
		type : String?,
		search : String? = null ,
		getCount : String? = null ,
	) = viewModelScope.launch {
		if (! networkMonitor.hasInternet()) {
			_getProductSubCategoryResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_getProductSubCategoryResponse.value = repo.getCategory(categoryId , type , search , getCount)
	}


	private var _getSubCategoriesResponse = MutableLiveData<Resource<GetSubCategoriesResponse>>()
	val getSubCategoriesRepo : MutableLiveData<Resource<GetSubCategoriesResponse>>
		get() = _getSubCategoriesResponse

	fun getSubCategories(categoryIds : List<Int> , subCategoryIds : List<Int>? = null) = viewModelScope.launch {
		if (! networkMonitor.hasInternet()) {
			_getSubCategoriesResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_getSubCategoriesResponse.value = repo.getSubCategories(categoryIds , subCategoryIds)
	}

	private var _userFavoriteResponse = MutableLiveData<Resource<CommonResponse>>()
	val userFavoriteRepo : MutableLiveData<Resource<CommonResponse>>
		get() = _userFavoriteResponse

	fun userFavorite(categoryIds : List<Int> , subcategoriesIds : List<Int>? = null) = viewModelScope.launch {
		if (! networkMonitor.hasInternet()) {
			_userFavoriteResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_userFavoriteResponse.value = repo.userFavorite(categoryIds , subcategoriesIds)
	}

	private var _getLessonResponse = MutableLiveData<Resource<GetLessonsResponse>>()
	val getLessonRepo : MutableLiveData<Resource<GetLessonsResponse>>
		get() = _getLessonResponse

	fun getLesson() = viewModelScope.launch {
		if (! networkMonitor.hasInternet()) {
			_getLessonResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_getLessonResponse.value = repo.getLesson()
	}

	private var _storeProductResponse = MutableLiveData<Resource<CreateProductResponse>>()
	val storeProductRepo : MutableLiveData<Resource<CreateProductResponse>>
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
		auction : Boolean? ,
		shippingProfileId : String? ,
		type : String? = null ,
		status : String? ,
		productImages : List<Map<String , String?>>? ,
		subCategoryId : Int? = null ,
		productId : String? = null ,
		variant : List<Map<String? , Any?>>? = null ,
		width : String? = null ,
		height : String? = null ,
		length : String? = null ,
		weight : String? = null ,
		mailClass : String? = null ,
		processingCategory : String? = null,
		productCondition : String? = null
		) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_storeProductResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_storeProductResponse.value = repo.storeProduct(
			categoryId ,
			title ,
			description ,
			quantity ,
			pricing ,
			flashSale ,
			acceptOffers ,
			reserveForLive ,
			auction ,
			shippingProfileId ,
			type ,
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
			processingCategory,
			productCondition)
	}

	private var _storeProductMetaResponse = MutableLiveData<Resource<StoreProductResponse>>()
	val storeProductMetaRepo : MutableLiveData<Resource<StoreProductResponse>>
		get() = _storeProductMetaResponse

	fun storeProductMeta(productImages : List<MultipartBody.Part>? , thumbnail : List<MultipartBody.Part>?) = viewModelScope.launch {
		if (! networkMonitor.hasInternet()) {
			_storeProductMetaResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_storeProductMetaResponse.value = repo.storeProductMeta(productImages , thumbnail)
	}

	private var _getHowToSellStepResponse = MutableLiveData<Resource<GetHowToSellResponse>>()
	val getHowToSellStepRepo : MutableLiveData<Resource<GetHowToSellResponse>>
		get() = _getHowToSellStepResponse

	fun getHowToSellStep() = viewModelScope.launch {
		if (! networkMonitor.hasInternet()) {
			_getHowToSellStepResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_getHowToSellStepResponse.value = repo.getHowToSellStep()
	}

	private var _getPrepareStepResponse = MutableLiveData<Resource<GetPrepareStepResponse>>()
	val getPrepareStepRepo : MutableLiveData<Resource<GetPrepareStepResponse>>
		get() = _getPrepareStepResponse

	fun getPrepareStep() = viewModelScope.launch {
		if (! networkMonitor.hasInternet()) {
			_getPrepareStepResponse.value = NO_INTERNET_ERROR
			return@launch
		}
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
		if (! networkMonitor.hasInternet()) {
			_getLiveShowResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_getLiveShowResponse.value = repo.getLiveShow(type , category , search , page)
	}

	private var _offerListResponse = MutableLiveData<Resource<GetOffersResponse>>()
	val offerListRepo : MutableLiveData<Resource<GetOffersResponse>>
		get() = _offerListResponse

	fun offerList(
		page : Int ,
	) = viewModelScope.launch {
		if (! networkMonitor.hasInternet()) {
			_offerListResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_offerListResponse.value = repo.offerList(page)
	}

	private var _offerUpdateStatusResponse = MutableLiveData<Resource<UpdateOfferResponse>>()
	val offerUpdateStatusRepo : MutableLiveData<Resource<UpdateOfferResponse>>
		get() = _offerUpdateStatusResponse

	fun offerUpdateStatus(
		offerId : RequestBody? ,
		status : RequestBody? ,
	) = viewModelScope.launch {
		if (! networkMonitor.hasInternet()) {
			_offerUpdateStatusResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_offerUpdateStatusResponse.value = repo.offerUpdateStatus(offerId , status)
	}

	private var _addPaymentCardResponse = MutableLiveData<Resource<CommonResponse>>()
	val addPaymentCardRepo : MutableLiveData<Resource<CommonResponse>>
		get() = _addPaymentCardResponse

	fun addPaymentCard(
		data : PaymentCardModel ,
	) = viewModelScope.launch {
		if (! networkMonitor.hasInternet()) {
			_addPaymentCardResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_addPaymentCardResponse.value = repo.addPaymentCard(data)
	}

	private var _storeDeviceDetailsResponse = MutableLiveData<Resource<UserDeviceResponse>>()
	val storeDeviceDetailsRepo : MutableLiveData<Resource<UserDeviceResponse>>
		get() = _storeDeviceDetailsResponse

	fun storeDeviceDetails(
		deviceToken : RequestBody? ,
	) = viewModelScope.launch {
		if (! networkMonitor.hasInternet()) {
			_storeDeviceDetailsResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_storeDeviceDetailsResponse.value = repo.storeDeviceDetails(deviceToken)
	}

	private var _getUserProfileResponse = MutableLiveData<Resource<UserProfileResponse>>()
	val getUserProfileRepo : MutableLiveData<Resource<UserProfileResponse>>
		get() = _getUserProfileResponse

	fun getUserProfile(
	) = viewModelScope.launch {
		if (! networkMonitor.hasInternet()) {
			_getUserProfileResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_getUserProfileResponse.value = repo.getUserProfile()
	}

	private var _fetchBidsResponse = MutableLiveData<Resource<FetchBidResponse>>()
	val fetchBidsRepo : MutableLiveData<Resource<FetchBidResponse>>
		get() = _fetchBidsResponse

	fun fetchBids(
		page : String? ,
	) = viewModelScope.launch {
		if (! networkMonitor.hasInternet()) {
			_fetchBidsResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_fetchBidsResponse.value = repo.fetchBids(page)
	}

	private var _getPurchasedProductsByStatusResponse =
		MutableLiveData<Resource<GetProductsByStatusResponse>>()
	val getPurchasedProductsByStatusRepo : MutableLiveData<Resource<GetProductsByStatusResponse>>
		get() = _getPurchasedProductsByStatusResponse

	fun getPurchasedProductsByStatus(
		type : RequestBody? ,
		page : RequestBody? ,
		status : RequestBody?  = null,
	) = viewModelScope.launch {
		if (! networkMonitor.hasInternet()) {
			_getPurchasedProductsByStatusResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_getPurchasedProductsByStatusResponse.value = repo.getProductsByStatus(type , page, status)
	}

	private var _getSavedProductsByStatusResponse =
		MutableLiveData<Resource<GetProductsByStatusResponse>>()
	val getSavedProductsByStatusRepo : MutableLiveData<Resource<GetProductsByStatusResponse>>
		get() = _getSavedProductsByStatusResponse

	fun getSavedProductsByStatus(
		type : RequestBody? ,
		page : RequestBody? ,
		status : RequestBody? = null
	) = viewModelScope.launch {
		if (! networkMonitor.hasInternet()) {
			_getSavedProductsByStatusResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_getSavedProductsByStatusResponse.value = repo.getProductsByStatus(type , page, status)
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
		if (! networkMonitor.hasInternet()) {
			_updateProfileResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_updateProfileResponse.value = repo.updateProfile(firstName , lastName , image , userName , bio)
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
		productIds : List<Int> ,
	) = viewModelScope.launch {
		if (! networkMonitor.hasInternet()) {
			_storeScheduleShowResponse.value = NO_INTERNET_ERROR
			return@launch
		}
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
		if (! networkMonitor.hasInternet()) {
			_storeSellerRatingResponse.value = NO_INTERNET_ERROR
			return@launch
		}
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
		message : RequestBody
		) = viewModelScope.launch {
		if (! networkMonitor.hasInternet()) {
			_sendChatNotificationResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_sendChatNotificationResponse.value = repo.sendChatNotification(receiverId , message)
	}

	private var _pageUrlResponse = MutableLiveData<Resource<PageUrlResponse>>()
	val pageUrlRepo : MutableLiveData<Resource<PageUrlResponse>>
		get() = _pageUrlResponse

	fun getPageUrl(slug : String) = viewModelScope.launch {
		if (! networkMonitor.hasInternet()) {
			_pageUrlResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_pageUrlResponse.value = repo.getPageUrl(slug)
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
		if (! networkMonitor.hasInternet()) {
			_blockUnblockUserResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_blockUnblockUserResponse.value = repo.blockUnblockUser(blockedID)
	}

	private var _getBlockedUsersResponse = MutableLiveData<Resource<GetBlockedUsersResponse>>()
	val getBlockedUsersRepo : MutableLiveData<Resource<GetBlockedUsersResponse>>
		get() = _getBlockedUsersResponse

	fun getBlockedUsers() = viewModelScope.launch {
		if (! networkMonitor.hasInternet()) {
			_getBlockedUsersResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_getBlockedUsersResponse.value = repo.getBlockedUsers()
	}

	private var _getMailClassesResponse = MutableLiveData<Resource<GetMailClassesResponse>>()
	val getMailClassesRepo : MutableLiveData<Resource<GetMailClassesResponse>>
		get() = _getMailClassesResponse

	fun getMailClasses() = viewModelScope.launch {
		if (! networkMonitor.hasInternet()) {
			_getMailClassesResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_getMailClassesResponse.value = repo.getMailClasses()
	}

	private var _getPromoteShowListResponse = MutableLiveData<Resource<GetPromotePlansResponse>>()
	val getPromoteShowListRepo : MutableLiveData<Resource<GetPromotePlansResponse>>
		get() = _getPromoteShowListResponse

	fun getPromoteShowList() = viewModelScope.launch {
		if (! networkMonitor.hasInternet()) {
			_getPromoteShowListResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_getPromoteShowListResponse.value = repo.getPromoteShowList()
	}

	private var _promoteShowResponse = MutableLiveData<Resource<CommonResponse>>()
	val promoteShowRepo : MutableLiveData<Resource<CommonResponse>>
		get() = _promoteShowResponse

	fun promoteShow(
		scheduleShowId : RequestBody ,
		promoteShowId : RequestBody ,
	) = viewModelScope.launch {
		if (! networkMonitor.hasInternet()) {
			_promoteShowResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_promoteShowResponse.value = repo.promoteShow(scheduleShowId , promoteShowId)
	}

	private var _getLiveSellerResponse = MutableLiveData<Resource<GetLiveSellerResponse>>()
	val getLiveSellerRepo : MutableLiveData<Resource<GetLiveSellerResponse>>
		get() = _getLiveSellerResponse

	fun getLiveSeller() = viewModelScope.launch {
		if (! networkMonitor.hasInternet()) {
			_getLiveSellerResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_getLiveSellerResponse.value = repo.getLiveSeller()
	}


	private var _getAgoraTokenResponse = MutableLiveData<Resource<GetAgoraTokenResponse>>()
	val getAgoraTokenRepo : MutableLiveData<Resource<GetAgoraTokenResponse>>
		get() = _getAgoraTokenResponse

	fun getAgoraToken(
		channel : RequestBody ,
		uId : RequestBody? = null
	) = viewModelScope.launch {
		if (! networkMonitor.hasInternet()) {
			_getAgoraTokenResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_getAgoraTokenResponse.value = repo.getAgoraToken(channel, uId)
	}

	private var _getShippingProfileResponse = MutableLiveData<Resource<GetShippingProfilesResponse>>()
	val getShippingProfileRepo: MutableLiveData<Resource<GetShippingProfilesResponse>>
		get() = _getShippingProfileResponse

	fun getShippingProfile(
	) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_getShippingProfileResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_getShippingProfileResponse.value = repo.getShippingProfile()
	}

}