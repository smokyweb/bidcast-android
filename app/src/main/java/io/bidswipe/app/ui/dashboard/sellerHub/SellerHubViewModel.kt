package io.bidswipe.app.ui.dashboard.sellerHub

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.repository.DashRepository
import io.bidswipe.app.network.response.GetMyShowResponse
import kotlinx.coroutines.launch
import okhttp3.RequestBody
import javax.inject.Inject

@HiltViewModel
class SellerHubViewModel@Inject constructor(val repo: DashRepository) : ViewModel() {


    private var _getMyScheduledShowResponse = MutableLiveData<Resource<GetMyShowResponse>>()
    val getMyScheduledShowRepo: MutableLiveData<Resource<GetMyShowResponse>>
        get() = _getMyScheduledShowResponse

    fun getMyScheduledShow(
        type : RequestBody? = null
    ) = viewModelScope.launch {
        _getMyScheduledShowResponse.value = repo.getMyScheduledShow(type)
    }

}