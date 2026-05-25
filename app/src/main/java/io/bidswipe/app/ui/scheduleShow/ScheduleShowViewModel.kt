package io.bidswipe.app.ui.scheduleShow

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.bidswipe.app.model.MediaItem
import io.bidswipe.app.network.request.StoreProductRequest
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.repository.DashRepository
import io.bidswipe.app.network.response.CheckScheduleShowResponse
import io.bidswipe.app.network.response.CommonResponse
import io.bidswipe.app.network.response.CreateProductResponse
import io.bidswipe.app.network.response.CreateShowResponse
import io.bidswipe.app.network.response.GetAllTipsResponse
import io.bidswipe.app.network.response.GetAuctionTypeResponse
import io.bidswipe.app.network.response.GetCategoryResponse
import io.bidswipe.app.network.response.GetMailClassesResponse
import io.bidswipe.app.network.response.GetProductsResponse
import io.bidswipe.app.network.response.GetPromotePlansResponse
import io.bidswipe.app.network.response.GetShippingProfilesResponse
import io.bidswipe.app.network.response.GetShowDetailsResponse
import io.bidswipe.app.network.response.Product
import io.bidswipe.app.network.response.StoreProductMetaResponse
import io.bidswipe.app.utils.Const.NO_INTERNET_ERROR
import io.bidswipe.app.utils.NetworkMonitor
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import okhttp3.RequestBody
import javax.inject.Inject

@HiltViewModel
class ScheduleShowViewModel @Inject constructor(
	val repo: DashRepository,
	private val networkMonitor: NetworkMonitor
) : ViewModel() {

	var currentProducts = mutableListOf<Product>()

	var showId: String? = null
	var showTitle = ""
	var date = ""
	var time = ""
	var categoryId = ""
	var subCategoryId = ""
	var auctionId = ""
	var repeatMode = ""
	var repeatType = ""
	var explicitContent = ""
	var primaryLanguage = ""
	var discoverability = ""
	var thumbnail = ""
	var variantData = mutableListOf<Map<String?, Any?>>()
	var productTitle = ""
	var productDescription = ""
	var productCategoryId = ""
	var productCategoryName = ""
	var productSubCategoryId = ""
	var productSubCategoryName = ""
	var productQuantity = 1
	var condition = ""
	var productWidth = ""
	var productHeight = ""
	var productLength = ""
	var productWeight = ""
	val productImages = mutableListOf<MediaItem>()
	var productProcessingCategory: String? = null
	var productMailClass: GetMailClassesResponse.Data.MailClasses? = null
	var productSalesFormat: String = ""
	var productPrice: String = ""
	var productFormFlashSale = false
	var productFormAcceptOffers = false
	var productFormReserveForLive = false
	var shippingProfile = ""

	private var _storeScheduleShowResponse = MutableLiveData<Resource<CreateShowResponse>>()
	val storeScheduleShowRepo: MutableLiveData<Resource<CreateShowResponse>>
		get() = _storeScheduleShowResponse

	fun storeScheduleShow(
		title: RequestBody?,
		date: RequestBody?,
		time: RequestBody?,
		categoryId: RequestBody?,
		subCategoryId: RequestBody?,
		showDiscoverability: RequestBody?,
		auctionTypeId: RequestBody?,
		thumbnails: List<MultipartBody.Part?>?,
		productIds: List<Int>,
		isRepeat: RequestBody?,
		repeatValue: RequestBody?,
		language: RequestBody?,
		isExplicit: RequestBody?,
		showId: RequestBody? = null
	) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_storeScheduleShowResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_storeScheduleShowResponse.value = repo.storeScheduleShow(
			title,
			date,
			time,
			categoryId,subCategoryId,
			showDiscoverability,
			auctionTypeId,
			thumbnails,
			productIds,
			isRepeat,
			repeatValue,
			language,
			isExplicit,
			showId
		)
	}

	private var _getCategoryResponse = MutableLiveData<Resource<GetCategoryResponse>>()
	val getCategoryRepo: MutableLiveData<Resource<GetCategoryResponse>>
		get() = _getCategoryResponse

	fun getCategory(
		categoryId: String? = null,
	) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_getCategoryResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_getCategoryResponse.value = repo.getCategory(categoryId)
	}

	private var _getProductSubCategoryResponse = MutableLiveData<Resource<GetCategoryResponse>>()
	val getProductSubCategoryRepo: MutableLiveData<Resource<GetCategoryResponse>>
		get() = _getProductSubCategoryResponse

	fun getProductSubCategory(
		categoryId: String?,
		type: String?,
		search: String? = null,
		getCount: String? = null,
	) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_getProductSubCategoryResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_getProductSubCategoryResponse.value = repo.getCategory(categoryId, type, search, getCount)
	}

	private var _getAuctionTypeResponse = MutableLiveData<Resource<GetAuctionTypeResponse>>()
	val getAuctionTypeRepo: MutableLiveData<Resource<GetAuctionTypeResponse>>
		get() = _getAuctionTypeResponse

	fun getAuctionType(
	) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_getAuctionTypeResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_getAuctionTypeResponse.value = repo.getAuctionType()
	}

	private var _getAllTipsResponse = MutableLiveData<Resource<GetAllTipsResponse>>()
	val getAllTipsRepo: MutableLiveData<Resource<GetAllTipsResponse>>
		get() = _getAllTipsResponse

	fun getAllTips(
		type: RequestBody?,
	) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_getAllTipsResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_getAllTipsResponse.value = repo.getAllTips(type)
	}

	private var _getUserProductsResponse = MutableLiveData<Resource<GetProductsResponse>>()
	val getUserProductsRepo: MutableLiveData<Resource<GetProductsResponse>>
		get() = _getUserProductsResponse

	fun getUserProducts(
		userId: RequestBody? = null,
		status: RequestBody? = null,
		format: RequestBody? = null,
		page: RequestBody?,
		search: RequestBody? = null,
		categoryIds: RequestBody? = null,
		conditions: RequestBody? = null,
		minPrice: RequestBody? = null,
		maxPrice: RequestBody? = null,
		marketPlace: RequestBody? = null,
		type: RequestBody? = null,
		saleType: RequestBody? = null,
		sortBy: RequestBody? = null
	) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_getUserProductsResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_getUserProductsResponse.value =
			repo.getProducts(userId, status, format, page, search, categoryIds, conditions, minPrice, maxPrice, marketPlace, type, saleType, sortBy)
	}

	private var _storeProductResponse = MutableLiveData<Resource<CreateProductResponse>>()
	val storeProductRepo: MutableLiveData<Resource<CreateProductResponse>>
		get() = _storeProductResponse

	fun storeProduct(
		storeProductModel: StoreProductRequest, productId: String?
	) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_storeProductResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_storeProductResponse.value = repo.storeProduct(
			storeProductModel, productId
		)
	}

	private var _storeProductMetaResponse = MutableLiveData<Resource<StoreProductMetaResponse>>()
	val storeProductMetaRepo: MutableLiveData<Resource<StoreProductMetaResponse>>
		get() = _storeProductMetaResponse

	fun storeProductMeta(
		productImages: List<MultipartBody.Part>?,
		videos: List<MultipartBody.Part>?,
		thumbnail: List<MultipartBody.Part>?
	) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_storeProductMetaResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_storeProductMetaResponse.value = repo.storeProductMeta(productImages, videos, thumbnail)
	}

	private var _deleteProductResponse = MutableLiveData<Resource<CommonResponse>>()
	val deleteProductRepo: MutableLiveData<Resource<CommonResponse>>
		get() = _deleteProductResponse

	fun deleteProduct(
		productId: String?,
	) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_deleteProductResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_deleteProductResponse.value = repo.deleteProduct(productId)
	}

	private var _getMailClassesResponse = MutableLiveData<Resource<GetMailClassesResponse>>()
	val getMailClassesRepo: MutableLiveData<Resource<GetMailClassesResponse>>
		get() = _getMailClassesResponse

	fun getMailClasses() = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_getMailClassesResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_getMailClassesResponse.value = repo.getMailClasses()
	}


	private var _checkScheduleShowResponse = MutableLiveData<Resource<CheckScheduleShowResponse>>()
	val checkScheduleShowRepo: MutableLiveData<Resource<CheckScheduleShowResponse>>
		get() = _checkScheduleShowResponse

	fun checkScheduleShow(
		date: RequestBody?,
		time: RequestBody?,
		showId: RequestBody? = null
	) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_checkScheduleShowResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_checkScheduleShowResponse.value = repo.checkScheduleShow(date, time, showId)
	}

	private var _getShowDetailsResponse = MutableLiveData<Resource<GetShowDetailsResponse>>()
	val getShowDetailsRepo: MutableLiveData<Resource<GetShowDetailsResponse>>
		get() = _getShowDetailsResponse

	fun getShowDetails(
		showId: String
	) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_getShowDetailsResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_getShowDetailsResponse.value = repo.getShowDetails(showId)
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

	// Promote show — scheduled show details screen
	private var _getPromoteShowListResponse = MutableLiveData<Resource<GetPromotePlansResponse>>()
	val getPromoteShowListRepo: MutableLiveData<Resource<GetPromotePlansResponse>>
		get() = _getPromoteShowListResponse

	fun getPromoteShowList() = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_getPromoteShowListResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_getPromoteShowListResponse.value = repo.getPromoteShowList()
	}

	private var _promoteShowResponse = MutableLiveData<Resource<CommonResponse>>()
	val promoteShowRepo: MutableLiveData<Resource<CommonResponse>>
		get() = _promoteShowResponse

	fun promoteShow(
		scheduleShowId: RequestBody,
		promoteShowId: RequestBody,
	) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_promoteShowResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_promoteShowResponse.value = repo.promoteShow(scheduleShowId, promoteShowId)
	}

}