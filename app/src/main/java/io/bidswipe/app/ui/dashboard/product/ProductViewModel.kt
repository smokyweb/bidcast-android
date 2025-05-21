package io.bidswipe.app.ui.dashboard.product

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.bidswipe.app.network.repository.DashRepository
import javax.inject.Inject

@HiltViewModel
class ProductViewModel @Inject constructor(val repo: DashRepository) : ViewModel() {


}