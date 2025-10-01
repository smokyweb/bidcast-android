package io.bidswipe.app.ui.dashboard.watchStream

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.bidswipe.app.model.LiveShowModelOld
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.repository.DashRepository
import io.bidswipe.app.network.response.CreateBidResponse
import io.bidswipe.app.network.response.FollowUnfollowResponse
import io.bidswipe.app.network.response.SentTipAmountResponse
import io.bidswipe.app.utils.Const.NO_INTERNET_ERROR
import io.bidswipe.app.utils.NetworkMonitor
import kotlinx.coroutines.launch
import okhttp3.RequestBody
import javax.inject.Inject

@HiltViewModel
class StreamViewModel @Inject constructor(
	val repo : DashRepository,
	private val networkMonitor : NetworkMonitor
) : ViewModel() {

	// LiveData to hold the list or individual streams
    private val _streams = MutableLiveData<List<String>>()

    val streams: LiveData<List<String>> = _streams

    fun setStreams(newStreams: List<String>) {
		_streams.value = newStreams
	}

	// Optionally, you can have a LiveData for the currently selected stream
    private val _selectedStream = MutableLiveData<String>()
    val selectedStream: LiveData<String> = _selectedStream

    fun selectStream(stream: String) {
		_selectedStream.value = stream
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
		if (!networkMonitor.hasInternet()) {
			_createBidResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_createBidResponse.value = repo.createBid(showId , userId , productId , bidPrice)
	}

	private var _followUserResponse = MutableLiveData<Resource<FollowUnfollowResponse>>()
	val followUserShowRepo : MutableLiveData<Resource<FollowUnfollowResponse>>
		get() = _followUserResponse

	fun followUser(
        userId : RequestBody? ,
    ) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_followUserResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_followUserResponse.value = repo.followUser(userId)
	}

	private var _sendTipAmountResponse = MutableLiveData<Resource<SentTipAmountResponse>>()
	val sendTipAmountRepo : MutableLiveData<Resource<SentTipAmountResponse>>
		get() = _sendTipAmountResponse

	fun sendTipAmount(
		sellerId : RequestBody ,
		amount : RequestBody ,
		cardNumber : RequestBody?
	) = viewModelScope.launch {
		if (!networkMonitor.hasInternet()) {
			_sendTipAmountResponse.value = NO_INTERNET_ERROR
			return@launch
		}
		_sendTipAmountResponse.value = repo.sendTipAmount(sellerId, amount, cardNumber)
	}

}