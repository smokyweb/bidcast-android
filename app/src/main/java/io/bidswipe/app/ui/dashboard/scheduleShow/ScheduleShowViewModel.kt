package io.bidswipe.app.ui.dashboard.scheduleShow

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.repository.DashRepository
import io.bidswipe.app.network.response.CommonResponse
import io.bidswipe.app.network.response.GetAllTipsResponse
import io.bidswipe.app.network.response.GetAuctionTypeResponse
import io.bidswipe.app.network.response.GetCategoryResponse
import io.bidswipe.app.network.response.GetProductsResponse
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import okhttp3.RequestBody
import javax.inject.Inject

@HiltViewModel
class ScheduleShowViewModel @Inject constructor(val repo: DashRepository) : ViewModel() {

    var showTitle = ""
    var date = ""
    var time = ""
    var categoryId =""
    var auctionId =""
    var thumbnail =""


    private var _storeScheduleShowResponse = MutableLiveData<Resource<CommonResponse>>()
    val storeScheduleShowRepo: MutableLiveData<Resource<CommonResponse>>
        get() = _storeScheduleShowResponse

    fun storeScheduleShow(
        title: RequestBody?,
        date: RequestBody?,
        time: RequestBody?,
        categoryId: RequestBody?,
        auctionTypeId : RequestBody?,
        thumbnails: List<MultipartBody.Part?>?,
        productIds: List<Int?>
    ) = viewModelScope.launch {
        _storeScheduleShowResponse.value = repo.storeScheduleShow(title,date,time,categoryId,auctionTypeId,thumbnails,productIds)
    }

    private var _getCategoryResponse = MutableLiveData<Resource<GetCategoryResponse>>()
    val getCategoryRepo: MutableLiveData<Resource<GetCategoryResponse>>
        get() = _getCategoryResponse

    fun getCategory(
    ) = viewModelScope.launch {
        _getCategoryResponse.value = repo.getCategory()
    }

    private var _getAuctionTypeResponse = MutableLiveData<Resource<GetAuctionTypeResponse>>()
    val getAuctionTypeRepo: MutableLiveData<Resource<GetAuctionTypeResponse>>
        get() = _getAuctionTypeResponse

    fun getAuctionType(
    ) = viewModelScope.launch {
        _getAuctionTypeResponse.value = repo.getAuctionType()
    }


    private var _getAllTipsResponse = MutableLiveData<Resource<GetAllTipsResponse>>()
    val getAllTipsRepo: MutableLiveData<Resource<GetAllTipsResponse>>
        get() = _getAllTipsResponse

    fun getAllTips(
        type : RequestBody?
    ) = viewModelScope.launch {
        _getAllTipsResponse.value = repo.getAllTips(type)
    }

    private var _getUserProductsResponse = MutableLiveData<Resource<GetProductsResponse>>()
    val getUserProductsRepo: MutableLiveData<Resource<GetProductsResponse>>
        get() = _getUserProductsResponse

    fun getUserProducts(
    ) = viewModelScope.launch {
        _getUserProductsResponse.value = repo.getUserProducts()
    }

}