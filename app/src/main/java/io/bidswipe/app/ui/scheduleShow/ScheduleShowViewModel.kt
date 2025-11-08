package io.bidswipe.app.ui.scheduleShow

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.repository.DashRepository
import io.bidswipe.app.network.response.CommonResponse
import io.bidswipe.app.network.response.CreateShowResponse
import io.bidswipe.app.network.response.GetAllTipsResponse
import io.bidswipe.app.network.response.GetAuctionTypeResponse
import io.bidswipe.app.network.response.GetCategoryResponse
import io.bidswipe.app.network.response.GetMailClassesResponse
import io.bidswipe.app.network.response.GetProductsResponse
import io.bidswipe.app.network.response.StoreProductResponse
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

	var showTitle = ""
	var date = ""
	var time = ""
	var categoryId = ""
	var auctionId = ""
	var thumbnail = ""
	var variantData = mutableListOf<Map<String?, Any?>>()

	var productTitle = ""
	var productDescription = ""
	var productCategoryId = ""
	var productCategoryName = ""
	var productSubCategoryId = ""
	var productSubCategoryName = ""
	var productQuantity = 1
	var productWidth = ""
	var productHeight = ""
	var productLength = ""
	var productWeight = ""
	val productImages = mutableListOf<String?>()
	var productProcessingCategory: String? = null
	var productMailClass: GetMailClassesResponse.Data.MailClasses? = null
	var productSalesFormat: String = ""
	var productPrice: String = ""

	private var _storeScheduleShowResponse = MutableLiveData<Resource<CreateShowResponse>>()
	val storeScheduleShowRepo: MutableLiveData<Resource<CreateShowResponse>>
		get() = _storeScheduleShowResponse

	fun storeScheduleShow(
		title: RequestBody?,
		date: RequestBody?,
		time: RequestBody?,
		categoryId: RequestBody?,
		auctionTypeId: RequestBody?,
		thumbnails: List<MultipartBody.Part?>?,
		productIds: List<Int>,
	) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_storeScheduleShowResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_storeScheduleShowResponse.value = repo.storeScheduleShow(
			title,
			date,
			time,
			categoryId,
			auctionTypeId,
			thumbnails,
			productIds
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
		categoryId: RequestBody? = null,
	) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_getUserProductsResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_getUserProductsResponse.value = repo.getUserProducts(userId, categoryId)
	}

	private var _storeProductResponse = MutableLiveData<Resource<CommonResponse>>()
	val storeProductRepo: MutableLiveData<Resource<CommonResponse>>
		get() = _storeProductResponse

	fun storeProduct(
		categoryId: String?,
		title: String?,
		description: String?,
		quantity: String?,
		pricing: String?,
		flashSale: String?,
		acceptOffers: String?,
		reserveForLive: String?,
		shippingProfileId: String?,
		status: String?,
		productImages: List<Map<String, String?>>?,
		subCategoryId: Int? = null,
		productId: String? = null,
		variant: List<Map<String?, Any?>>? = null,
		width: String? = null,
		height: String? = null,
		length: String? = null,
		weight: String? = null,
		mailClass: String? = null,
		processingCategory: String? = null,
	) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_storeProductResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_storeProductResponse.value = repo.storeProduct(
			categoryId = categoryId,
			title = title,
			description = description,
			quantity = quantity,
			pricing = pricing,
			flashSale = flashSale,
			acceptOffers = acceptOffers,
			reserveForLive = reserveForLive,
			shippingProfileId = shippingProfileId,
			status = status,
			productImages = productImages,
			subCategoryId = subCategoryId,
			productId = productId,
			variant = variant,
			width = width,
			height = height,
			length = length,
			weight = weight,
			mailClass = mailClass,
			processingCategory = processingCategory
		)
	}

	private var _storeProductMetaResponse = MutableLiveData<Resource<StoreProductResponse>>()
	val storeProductMetaRepo: MutableLiveData<Resource<StoreProductResponse>>
		get() = _storeProductMetaResponse

	fun storeProductMeta(productImages: List<MultipartBody.Part>?, thumbnail: List<MultipartBody.Part>?) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_storeProductMetaResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_storeProductMetaResponse.value = repo.storeProductMeta(productImages, thumbnail)
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

}