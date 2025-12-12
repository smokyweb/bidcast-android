package io.bidswipe.app.ui.product

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.repository.DashRepository
import io.bidswipe.app.network.response.CommonResponse
import io.bidswipe.app.network.response.CreateOrderResponse
import io.bidswipe.app.network.response.FetchOrderDetailResponse
import io.bidswipe.app.network.response.GetOrderDetailsResponse
import io.bidswipe.app.network.response.GetPaymentCardsResponse
import io.bidswipe.app.network.response.GetProductDetailsResponse
import io.bidswipe.app.network.response.GetProductsResponse
import io.bidswipe.app.network.response.GetPurchaseDetail
import io.bidswipe.app.network.response.GetShippingAddressResponse
import io.bidswipe.app.network.response.RaiseTicketResponse
import io.bidswipe.app.network.response.SellerInfoResponse
import io.bidswipe.app.network.response.UserSearchingResponse
import io.bidswipe.app.utils.Const.NO_INTERNET_ERROR
import io.bidswipe.app.utils.NetworkMonitor
import kotlinx.coroutines.launch
import okhttp3.RequestBody
import javax.inject.Inject

@HiltViewModel
class ProductViewModel @Inject constructor(
	val repo: DashRepository,
	private val networkMonitor: NetworkMonitor
) : ViewModel() {

	var product: GetProductDetailsResponse.Data? = null
	var checkoutData: GetPurchaseDetail.Data? = null

	private var _getShippingAddressResponse =
		MutableLiveData<Resource<GetShippingAddressResponse>>()
	val getShippingAddressRepo: MutableLiveData<Resource<GetShippingAddressResponse>>
		get() = _getShippingAddressResponse

	fun getShippingAddress() = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_getShippingAddressResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_getShippingAddressResponse.value = repo.getShippingAddress()
	}

	private var _getPaymentCardResponse = MutableLiveData<Resource<GetPaymentCardsResponse>>()
	val getPaymentCardRepo: MutableLiveData<Resource<GetPaymentCardsResponse>>
		get() = _getPaymentCardResponse

	fun getPaymentCard(
	) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_getPaymentCardResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_getPaymentCardResponse.value = repo.getPaymentCard()
	}


	private var _getPurchaseProductResponse = MutableLiveData<Resource<GetPurchaseDetail>>()
	val getPurchaseProductRepo: MutableLiveData<Resource<GetPurchaseDetail>>
		get() = _getPurchaseProductResponse

	fun getPurchaseProduct(
		shippingId: RequestBody?,
		productId: RequestBody?,
	) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_getPurchaseProductResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_getPurchaseProductResponse.value = repo.getPurchaseProduct(shippingId, productId)
	}

	private var _createOrderResponse = MutableLiveData<Resource<CreateOrderResponse>>()
	val createOrderRepo: MutableLiveData<Resource<CreateOrderResponse>>
		get() = _createOrderResponse

	fun createOrder(
		shippingId: RequestBody?,
		productId: RequestBody?,
		cardId: RequestBody?,
		promoCode: RequestBody?,
		sendAsGift: RequestBody?,
		giftUserId: RequestBody?,
		giftMsg: RequestBody?,
		shippingCharges: RequestBody?,
		taxAmount: RequestBody?,
		subTotal: RequestBody?,
		total: RequestBody?,
		discount: RequestBody? = null,
	) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_createOrderResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_createOrderResponse.value = repo.createOrder(
			shippingId,
			productId,
			cardId,
			promoCode,
			sendAsGift,
			giftUserId,
			giftMsg,
			shippingCharges,
			taxAmount,
			subTotal,
			total,
			discount
		)
	}

	private var _getProductDetailsResponse = MutableLiveData<Resource<GetProductDetailsResponse>>()
	val getProductDetailsRepo: MutableLiveData<Resource<GetProductDetailsResponse>>
		get() = _getProductDetailsResponse

	fun getProductDetails(
		productId: RequestBody?,
	) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_getProductDetailsResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_getProductDetailsResponse.value = repo.getProductDetails(productId)
	}

	private var _makeOfferResponse = MutableLiveData<Resource<CommonResponse>>()
	val makeOfferRepo: MutableLiveData<Resource<CommonResponse>>
		get() = _makeOfferResponse

	fun makeOffer(
		amount: RequestBody?,
		productId: RequestBody?,
	) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_makeOfferResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_makeOfferResponse.value = repo.makeOffer(amount, productId)
	}

	private var _getOrderReceiptResponse = MutableLiveData<Resource<CommonResponse>>()
	val getOrderReceiptRepo: MutableLiveData<Resource<CommonResponse>>
		get() = _getOrderReceiptResponse

	fun getOrderReceipt(
		orderId: RequestBody?,
	) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_getOrderReceiptResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_getOrderReceiptResponse.value = repo.getOrderReceipt(orderId)
	}

	private var _getOrderDetailsResponse = MutableLiveData<Resource<GetOrderDetailsResponse>>()
	val getOrderDetailsRepo: MutableLiveData<Resource<GetOrderDetailsResponse>>
		get() = _getOrderDetailsResponse

	fun getOrderDetails(
		orderId: RequestBody?,
	) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_getOrderDetailsResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_getOrderDetailsResponse.value = repo.getOrderDetails(orderId)
	}

	private var _searchUsersResponse = MutableLiveData<Resource<UserSearchingResponse>>()
	val searchUsersRepo: MutableLiveData<Resource<UserSearchingResponse>>
		get() = _searchUsersResponse

	fun searchUsers(
		search: RequestBody?,
	) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_searchUsersResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_searchUsersResponse.value = repo.searchUsers(search)
	}

	private var _saveSellerProductResponse = MutableLiveData<Resource<CommonResponse>>()
	val saveSellerProductRepo: MutableLiveData<Resource<CommonResponse>>
		get() = _saveSellerProductResponse

	fun saveSellerProduct(
		productId: RequestBody?,
	) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_saveSellerProductResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_saveSellerProductResponse.value = repo.saveSellerProduct(productId)
	}

	private var _getUserProductsResponse = MutableLiveData<Resource<GetProductsResponse>>()
	val getUserProductsRepo: MutableLiveData<Resource<GetProductsResponse>>
		get() = _getUserProductsResponse

	fun getUserProducts(
		userId : RequestBody? = null,
		status : RequestBody? = null,
		category: RequestBody? = null,
		format: RequestBody? = null,
		page : RequestBody? ,
		search : RequestBody? = null,
		categoryIds : RequestBody? =null,
		conditions : RequestBody? =null,
		minPrice : RequestBody? =null,
		maxPrice : RequestBody? =null,
		marketPlace : RequestBody? =null,
		type : RequestBody? =null,
		saleType : RequestBody? =null,
		sortBy : RequestBody? =null
	) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_getUserProductsResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_getUserProductsResponse.value = repo.getProducts(userId, status, category, format, page, search, categoryIds, conditions, minPrice, maxPrice,marketPlace, type, saleType, sortBy)
	}

	private var _getSellerInfoResponse = MutableLiveData<Resource<SellerInfoResponse>>()
	val getSellerInfoRepo : MutableLiveData<Resource<SellerInfoResponse>>
		get() = _getSellerInfoResponse

	fun getSellerInfo(
		sellerId : String
	) = viewModelScope.launch {
		if (! networkMonitor.hasInternet()) {
			_getSellerInfoResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_getSellerInfoResponse.value = repo.getSellerInfo(sellerId)
	}

	private var _fetchOrderDetailResponse = MutableLiveData<Resource<FetchOrderDetailResponse>>()
	val fetchOrderDetailRepo : MutableLiveData<Resource<FetchOrderDetailResponse>>
		get() = _fetchOrderDetailResponse

	fun fetchOrderDetail(
		productId: String?,
		orderId: String?
	) = viewModelScope.launch {
		if (! networkMonitor.hasInternet()) {
			_fetchOrderDetailResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_fetchOrderDetailResponse.value = repo.fetchOrderDetail(productId, orderId)
	}

	private var _raiseTicketResponse = MutableLiveData<Resource<RaiseTicketResponse>>()
	val raiseTicketRepo : MutableLiveData<Resource<RaiseTicketResponse>>
		get() = _raiseTicketResponse

	fun raiseTicket(
		orderId: RequestBody?,
		subject: RequestBody?,
		message: RequestBody?
	) = viewModelScope.launch {
		if (! networkMonitor.hasInternet()) {
			_raiseTicketResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_raiseTicketResponse.value = repo.raiseTicket( orderId, subject, message)
	}

}