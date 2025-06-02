package io.bidswipe.app.ui.dashboard.product

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.repository.DashRepository
import io.bidswipe.app.network.response.CommonResponse
import kotlinx.coroutines.launch
import okhttp3.RequestBody
import javax.inject.Inject

@HiltViewModel
class ProductViewModel @Inject constructor(val repo: DashRepository) : ViewModel() {

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
        discount : RequestBody?
    ) = viewModelScope.launch {
        _createOrderResponse.value = repo.createOrder(shippingId,productId,cardId,promoCode,sendAsGift,giftUserId,giftMsg,shippingCharges,taxAmount,subTotal,total,discount)
    }


}