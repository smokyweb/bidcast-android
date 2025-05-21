package io.bidswipe.app.network.repository

import io.bidswipe.app.base.BaseRepository
import io.bidswipe.app.network.ApiInterface
import okhttp3.MultipartBody
import okhttp3.RequestBody
import javax.inject.Inject

class DashRepository @Inject constructor(private val api: ApiInterface) : BaseRepository(){

    suspend fun logout() = call {
        api.logout()
    }

    suspend fun aboutUs() = call { api.aboutUs() }

    suspend fun getTermsConditions() = call { api.getTermsConditions() }

    suspend fun getPrivacyPolicy() = call { api.getPrivacyPolicy() }

    suspend fun getCategory() = call { api.getCategory() }
    
    suspend fun getLesson() = call { api.getLesson() }
    
    suspend fun getProduct(categoryId: RequestBody?) = call { api.getProduct(categoryId) }
    
    suspend fun storeProduct(
        categoryId: RequestBody?,
        title: RequestBody?,
        description: RequestBody?,
        quantity: RequestBody?,
        pricing: RequestBody?,
        flashSale: RequestBody?,
        acceptOffers: RequestBody?,
        reserveForLive: RequestBody?,
        shippingProfileId: RequestBody?,
        status: RequestBody?,
        productImages: List<MultipartBody.Part>?
    ) = call { api.storeProduct(categoryId, title, description, quantity, pricing, flashSale, acceptOffers, reserveForLive, shippingProfileId, status, productImages) }

}