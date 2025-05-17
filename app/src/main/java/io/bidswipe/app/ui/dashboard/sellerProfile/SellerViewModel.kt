package io.bidswipe.app.ui.dashboard.sellerProfile

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.bidswipe.app.network.repository.DashRepository
import javax.inject.Inject


@HiltViewModel
class SellerViewModel  @Inject constructor(val repo: DashRepository) : ViewModel() {

}