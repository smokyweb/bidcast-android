package io.bidcast.app.ui.dashboard

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.bidcast.app.network.repository.AuthRepository
import io.bidcast.app.network.repository.DashRepository
import javax.inject.Inject


@HiltViewModel
class DashViewModel @Inject constructor(val repo: DashRepository) : ViewModel() {
    var lastIndex =MutableLiveData(0)
}