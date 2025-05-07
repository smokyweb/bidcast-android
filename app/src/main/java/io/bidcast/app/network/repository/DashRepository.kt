package io.bidcast.app.network.repository

import io.bidcast.app.base.BaseRepository
import io.bidcast.app.network.ApiInterface
import javax.inject.Inject

class DashRepository @Inject constructor(private val api: ApiInterface) : BaseRepository()