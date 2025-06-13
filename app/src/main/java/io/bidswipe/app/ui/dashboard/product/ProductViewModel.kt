package io.bidswipe.app.ui.dashboard.product

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.repository.DashRepository
import io.bidswipe.app.network.response.CommonResponse
import io.bidswipe.app.network.response.GetPaymentCardsResponse
import io.bidswipe.app.network.response.GetProductDetailsResponse
import io.bidswipe.app.network.response.GetPurchaseDetail
import io.bidswipe.app.network.response.GetShippingAddressResponse
import kotlinx.coroutines.launch
import okhttp3.RequestBody
import javax.inject.Inject

@HiltViewModel
class ProductViewModel @Inject constructor(val repo: DashRepository) : ViewModel() {

    var product : GetProductDetailsResponse.Data? = null

/*    private var _createOrderResponse = MutableLiveData<Resource<CommonResponse>>()
    val createOrderRepo: MutableLiveData<Resource<CommonResponse>>
        get() = _createOrderResponse

    fun createOrder(
        shippingId : RequestBody?,
        productId : RequestBody?,
        cardId : RequestBody?,
        promoCode : RequestBody?,
        sendAsGift : RequestBody?,
        giftUserId : RequestBody?,
        giftMsg : RequestBody?,
        shippingCharges : RequestBody?,
        taxAmount : RequestBody?,
        subTotal : RequestBody?,
        total : RequestBody?,
        discount : RequestBody?
    ) = viewModelScope.launch {
        _createOrderResponse.value = repo.createOrder(shippingId,productId,cardId,promoCode,sendAsGift,giftUserId,giftMsg,shippingCharges,taxAmount,subTotal,total,discount)
    }*/

    private var _getProductResponse = MutableLiveData<Resource<CommonResponse>>()
    val getProductRepo: MutableLiveData<Resource<CommonResponse>>
        get() = _getProductResponse

    fun getProduct(categoryId: RequestBody?) = viewModelScope.launch {
        _getProductResponse.value = repo.getProduct(categoryId)
    }


    private var _getShippingAddressResponse = MutableLiveData<Resource<GetShippingAddressResponse>>()
    val getShippingAddressRepo: MutableLiveData<Resource<GetShippingAddressResponse>>
        get() = _getShippingAddressResponse

    fun getShippingAddress() = viewModelScope.launch {
        _getShippingAddressResponse.value = repo.getShippingAddress()
    }

    private var _getPaymentCardResponse = MutableLiveData<Resource<GetPaymentCardsResponse>>()
    val getPaymentCardRepo: MutableLiveData<Resource<GetPaymentCardsResponse>>
        get() = _getPaymentCardResponse

    fun getPaymentCard(
    ) = viewModelScope.launch {
        _getPaymentCardResponse.value = repo.getPaymentCard()
    }


    private var _getPurchaseProductResponse = MutableLiveData<Resource<GetPurchaseDetail>>()
    val getPurchaseProductRepo: MutableLiveData<Resource<GetPurchaseDetail>>
        get() = _getPurchaseProductResponse

    fun getPurchaseProduct(
        shippingId : RequestBody?,
        productId : RequestBody?
    ) = viewModelScope.launch {
        _getPurchaseProductResponse.value = repo.getPurchaseProduct(shippingId,productId)
    }

    private var _createOrderResponse = MutableLiveData<Resource<CommonResponse>>()
    val createOrderRepo: MutableLiveData<Resource<CommonResponse>>
        get() = _createOrderResponse

    fun createOrder(
        shippingId : RequestBody?,
        productId : RequestBody?,
        cardId : RequestBody?,
        promoCode : RequestBody?,
        sendAsGift : RequestBody?,
        giftUserId : RequestBody?,
        giftMsg : RequestBody?,
        shippingCharges : RequestBody?,
        taxAmount : RequestBody?,
        subTotal : RequestBody?,
        total : RequestBody?,
        discount : RequestBody? =null
    ) = viewModelScope.launch {
        _createOrderResponse.value = repo.createOrder(shippingId,productId,cardId,promoCode,sendAsGift,giftUserId,giftMsg,shippingCharges,taxAmount,subTotal,total,discount)
    }

    private var _getProductDetailsResponse = MutableLiveData<Resource<GetProductDetailsResponse>>()
    val getProductDetailsRepo: MutableLiveData<Resource<GetProductDetailsResponse>>
        get() = _getProductDetailsResponse

    fun getProductDetails(
        productId : RequestBody?
    ) = viewModelScope.launch {
        _getProductDetailsResponse.value = repo.getProductDetails(productId)
    }

    private var _makeOfferResponse = MutableLiveData<Resource<CommonResponse>>()
    val makeOfferRepo: MutableLiveData<Resource<CommonResponse>>
        get() = _makeOfferResponse

    fun makeOffer(
        amount : RequestBody?,
        productId : RequestBody?
    ) = viewModelScope.launch {
        _makeOfferResponse.value = repo.makeOffer(amount,productId)
    }

}