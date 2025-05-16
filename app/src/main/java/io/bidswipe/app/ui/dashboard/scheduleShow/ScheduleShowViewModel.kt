package io.bidswipe.app.ui.dashboard.scheduleShow

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.bidswipe.app.network.repository.DashRepository
import javax.inject.Inject

@HiltViewModel
class ScheduleShowViewModel @Inject constructor(val repo: DashRepository) : ViewModel() {
}