package io.bidswipe.app.ui.dashboard.watchStream

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.bidswipe.app.model.LiveShowModel
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.repository.DashRepository
import io.bidswipe.app.network.response.CommonResponse
import io.bidswipe.app.network.response.CreateBidResponse
import io.bidswipe.app.network.response.FollowUnfollowResponse
import io.bidswipe.app.network.response.UpdateLiveStatusResponse
import kotlinx.coroutines.launch
import okhttp3.RequestBody
import javax.inject.Inject

@HiltViewModel
class StreamViewModel @Inject constructor(val repo: DashRepository) : ViewModel() {

    // LiveData to hold the list or individual streams
    private val _streams = MutableLiveData<List<LiveShowModel>>()
    val streams: LiveData<List<LiveShowModel>> = _streams

    var previousRoomId = ""

    fun setStreams(newStreams: List<LiveShowModel>) {
        _streams.value = newStreams
    }

    // Optionally, you can have a LiveData for the currently selected stream
    private val _selectedStream = MutableLiveData<LiveShowModel>()
    val selectedStream: LiveData<LiveShowModel> = _selectedStream

    fun selectStream(stream: LiveShowModel) {
        _selectedStream.value = stream
    }

    private var _createBidResponse = MutableLiveData<Resource<CreateBidResponse>>()
    val createBidRepo: MutableLiveData<Resource<CreateBidResponse>>
        get() = _createBidResponse

    fun createBid(
        showId: RequestBody?,
        userId: RequestBody?,
        productId: RequestBody?,
        bidPrice: RequestBody?
    ) = viewModelScope.launch {
        _createBidResponse.value = repo.createBid(showId,userId,productId,bidPrice)
    }

    private var _followUserResponse = MutableLiveData<Resource<FollowUnfollowResponse>>()
    val followUserShowRepo: MutableLiveData<Resource<FollowUnfollowResponse>>
        get() = _followUserResponse

    fun followUser(
        userId : RequestBody?
    ) = viewModelScope.launch {
        _followUserResponse.value = repo.followUser(userId)
    }

}