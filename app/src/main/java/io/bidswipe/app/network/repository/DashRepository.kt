package io.bidswipe.app.network.repository

import io.bidswipe.app.base.BaseRepository
import io.bidswipe.app.network.ApiInterface
import javax.inject.Inject

class DashRepository @Inject constructor(private val api: ApiInterface) : BaseRepository()