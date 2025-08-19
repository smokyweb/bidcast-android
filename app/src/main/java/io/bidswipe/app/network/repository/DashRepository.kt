package io.bidswipe.app.network.repository

import io.bidswipe.app.base.BaseRepository
import io.bidswipe.app.model.GetSubCategoriesRequest
import io.bidswipe.app.model.PaymentCardModel
import io.bidswipe.app.model.StoreProductRequest
import io.bidswipe.app.network.ApiInterface
import io.bidswipe.app.utils.request
import okhttp3.MultipartBody
import okhttp3.RequestBody
import javax.inject.Inject

class DashRepository @Inject constructor(private val api: ApiInterface) : BaseRepository() {

    suspend fun logout() = call {
        api.logout()
    }

    suspend fun aboutUs() = call { api.aboutUs() }

    suspend fun getTermsConditions() = call { api.getTermsConditions() }

    suspend fun getPrivacyPolicy() = call { api.getPrivacyPolicy() }

    suspend fun getCategory(
        categoryId: String? = null,
        type: String? = null,
        search: String? = null
    ) = call { api.getCategory(categoryId, type, search) }

    suspend fun getSubCategories(
        categoryIds: List<Int>, subCategoryIds: List<Int>? = null
    )= call{api.getSubCategories(GetSubCategoriesRequest(categoryIds, subCategoryIds))}

    suspend fun userFavorite(
        categoryIds: List<Int>,
        subcategoriesIds:  List<Int>? = null
    )= call{api.userFavorite(GetSubCategoriesRequest(categoryIds, subcategoriesIds))}

    suspend fun getLesson() = call { api.getLesson() }

    suspend fun getProduct(categoryId: RequestBody?) = call { api.getProduct(categoryId) }

    suspend fun storeProduct(
        categoryId: String?,
        title: String?,
        description: String?,
        quantity: String?,
        pricing: String?,
        flashSale: String?,
        acceptOffers: String?,
        reserveForLive: String?,
        shippingProfileId: String?,
        status: String?,
        productImages:  List<Map<String, String?>>?,
        subCategoryId: String? = null,
        productId: String? = null,
        variant: List<Map<String?, Any?>>? = null,
    ) = call {
        api.storeProduct(
            StoreProductRequest
                (
                categoryId,
                title,
                description,
                quantity,
                pricing,
                flashSale,
                acceptOffers,
                reserveForLive,
                shippingProfileId,
                status,
                productImages,
                subCategoryId,
                variant
            ),
            productId
        )
    }

    suspend fun storeProductMeta(
        productImages: List<MultipartBody.Part?>?,
        thumbnail: List<MultipartBody.Part?>? = null
    ) = call {
        api.storeProductMeta(
            productImages,
            thumbnail
        )
    }

    suspend fun getHowToSellStep() = call { api.getHowToSellStep() }

    suspend fun getPrepareStep() = call { api.getPrepareStep() }

    suspend fun getFAQ() = call { api.getFAQ() }


    suspend fun contactUs(
        name: RequestBody?,
        email: RequestBody?,
        subject: RequestBody?,
        message: RequestBody?,
    ) = call { api.contactUs(name, email, subject, message) }

    suspend fun storeScheduleShow(
        title: RequestBody?,
        date: RequestBody?,
        time: RequestBody?,
        categoryId: RequestBody?,
        auctionTypeId: RequestBody?,
        thumbnails: List<MultipartBody.Part?>?,
        productIds: RequestBody?
    ) = call {
        api.storeScheduleShow(
            title,
            date,
            time,
            categoryId,
            auctionTypeId,
            thumbnails,
            productIds
        )
    }

    suspend fun getAuctionType() = call { api.getAuctionType() }


    suspend fun getAllTips(
        type: RequestBody?,
    ) = call { api.getAllTips(type) }


    suspend fun getUserProducts(
        userId: RequestBody?,
        categoryId: RequestBody?,
    ) = call { api.getUserProducts(userId, categoryId) }

    suspend fun getMyScheduledShow(
        type: RequestBody?,
    ) = call { api.getMyScheduledShow(type) }

    suspend fun getProfileById(
        userId: RequestBody?,
    ) = call { api.getProfileById(userId) }

    suspend fun followUser(
        userId: RequestBody?,
    ) = call { api.followUser(userId) }

    suspend fun makeOffer(
        amount: RequestBody?,
        productId: RequestBody?,
    ) = call { api.makeOffer(amount, productId) }

    suspend fun offerList(
        page: Int?,
    ) = call { api.offerList(page) }

    suspend fun offerUpdateStatus(
        offerId: RequestBody?,
        status: RequestBody?,
    ) = call { api.offerUpdateStatus(offerId, status) }

    suspend fun getLiveShow(
        type: RequestBody?,
        category: RequestBody?,
        search: RequestBody?,
    ) = call { api.getLiveShow(type, category, search) }

    suspend fun notifyLiveUser(
        liveUserId: RequestBody?,
    ) = call { api.notifyLiveUser(liveUserId) }

    suspend fun getProductDetails(
        productId: RequestBody?,
    ) = call { api.getProductDetails(productId) }

    suspend fun addShippingAddress(
        type: RequestBody?,
        name: RequestBody?,
        phoneNumber: RequestBody?,
        streetAddress: RequestBody?,
        pinCode: RequestBody?,
        city: RequestBody?,
        state: RequestBody?,
    ) = call {
        api.addShippingAddress(
            type,
            name,
            phoneNumber,
            streetAddress,
            pinCode,
            city,
            state
        )
    }

    suspend fun getShippingAddress() = call { api.getShippingAddress() }

    suspend fun addPaymentCard(
        data: PaymentCardModel,
    ) = call { api.addPaymentCard(data) }

    suspend fun getPaymentCard(
    ) = call { api.getPaymentCard() }

    suspend fun settingsList() = call { api.settingsList() }

    suspend fun settingsStore(
        countryOfResidence: RequestBody?,
        directMessage: RequestBody?,
        receiveGifts: RequestBody?,
        enablePrivateEntry: RequestBody?,
        showRewardStatus: RequestBody?,
        showSellerTools: RequestBody?,
        enableClips: RequestBody?,
        savePastShows: RequestBody?,
        activityStatus: RequestBody?,
        syncPhoneContacts: RequestBody?,
        suggestMyAccount: RequestBody?,
        hapticFeedback: RequestBody?,
    ) = call {
        api.settingsStore(
            countryOfResidence,
            directMessage,
            receiveGifts,
            enablePrivateEntry,
            showRewardStatus,
            showSellerTools,
            enableClips,
            savePastShows,
            activityStatus,
            syncPhoneContacts,
            suggestMyAccount,
            hapticFeedback
        )
    }

    suspend fun getPurchaseProduct(
        shippingId: RequestBody?,
        productId: RequestBody?,
    ) = call { api.getPurchaseProduct(shippingId, productId) }

    suspend fun createOrder(
        shippingId: RequestBody?,
        productId: RequestBody?,
        cardId: RequestBody?,
        promoCode: RequestBody?,
        sendAsGift: RequestBody?,
        giftUserId: RequestBody?,
        giftMsg: RequestBody?,
        shippingCharges: RequestBody?,
        taxAmount: RequestBody?,
        subTotal: RequestBody?,
        total: RequestBody?,
        discount: RequestBody?,
    ) = call {
        api.createOrder(
            shippingId,
            productId,
            cardId,
            promoCode,
            sendAsGift,
            giftUserId,
            giftMsg,
            shippingCharges,
            taxAmount,
            subTotal,
            total,
            discount
        )
    }

    suspend fun storeBuyerIdentity(
        image: MultipartBody.Part?,
    ) = call { api.storeBuyerIdentity(image) }

    suspend fun generateToken(
        showId: RequestBody?,
    ) = call { api.generateToken(showId) }


    suspend fun storeDeviceDetails(
        deviceToken: RequestBody?,
    ) = call { api.storeDeviceDetails(deviceToken) }


    suspend fun updateLiveStatus(
        showId: RequestBody?,
        isLive: RequestBody?,
    ) = call { api.updateLiveStatus(showId, isLive) }

    suspend fun createBid(
        showId: RequestBody?,
        userId: RequestBody?,
        productId: RequestBody?,
        bidPrice: RequestBody?,
    ) = call { api.createBid(showId, userId, productId, bidPrice) }


    suspend fun getUserProfile(
    ) = call { api.getUserProfile() }

    suspend fun fetchSellerVerification(
    ) = call { api.fetchSellerVerification() }

    suspend fun getMyInventory(
        status: RequestBody?,
        page: RequestBody?,
    ) = call { api.getMyInventory(status, page) }

    suspend fun getOrderListing(
        type: RequestBody?,
    ) = call { api.getOrderListing(type) }

    suspend fun storeSellerId(
        idCard: MultipartBody.Part,
        image: MultipartBody.Part,
    ) = call { api.storeSellerId(idCard, image) }

    suspend fun storePhoneNumber(
        phoneNumber: RequestBody?,
    ) = call { api.storePhoneNumber(phoneNumber) }

    suspend fun verifyNumberOtp(
        otp: RequestBody?,
    ) = call { api.verifyNumberOtp(otp) }

    suspend fun storePaymentMethod(
        cardToken: RequestBody?,
    ) = call { api.storePaymentMethod(cardToken) }

    suspend fun fetchBuyerIdentity(
    ) = call { api.fetchBuyerIdentity() }

    suspend fun getNotification(
    ) = call { api.getNotification() }

    suspend fun fetchBids(
        page: String?,
    ) = call {
        api.fetchBids(
            page
        )
    }

    suspend fun getOrderReceipt(
        orderId: RequestBody?,
    ) = call { api.getOrderReceipt(orderId) }

    suspend fun getOrderDetails(
        orderId: RequestBody?,
    ) = call { api.getOrderDetails(orderId) }

    suspend fun searchUsers(
        search: RequestBody?,
    ) = call { api.searchUsers(search) }

    suspend fun saveSellerProduct(
        productId: RequestBody?,
    ) = call { api.saveSellerProduct(productId) }

    suspend fun getProductsByStatus(
        type: RequestBody?,
        page: RequestBody?,
    ) = call { api.getProductsByStatus(type, page) }

    suspend fun deleteNotification(
        id: RequestBody?,
    ) = call { api.deleteNotification(id) }

    suspend fun getKYCDetails(
    ) = call { api.getKYCDetails() }

    suspend fun checkKyc(
    ) = call { api.checkKyc() }

    suspend fun fundTransfer(
        amount: RequestBody?,
    ) = call { api.fundTransfer(amount) }

    suspend fun getPayoutHistory(
    ) = call { api.getPayoutHistory() }


    suspend fun getTransactionsHistory(
        page: RequestBody?,
    ) = call { api.getTransactionsHistory(page) }

    suspend fun updateProfile(
        firstName: RequestBody,
        lastName: RequestBody,
        image: MultipartBody.Part?,
        userName: RequestBody,
        bio: RequestBody,
    ) = call { api.updateProfile(firstName, lastName, image, userName, bio) }

    suspend fun fetchReferral(
    ) = call { api.fetchReferral() }

    suspend fun storeSellerRating(
        sellerId: RequestBody,
        overAllRating: RequestBody,
        shippingRating: RequestBody,
        packagingRating: RequestBody,
        accuracyRating: RequestBody,
        comment: RequestBody,
    ) = call {
        api.storeSellerRating(
            sellerId,
            overAllRating,
            shippingRating,
            packagingRating,
            accuracyRating,
            comment
        )
    }

    suspend fun getSellerRating(
        sellerId: String?,
    ) = call { api.getSellerRating(sellerId) }

    suspend fun sendChatNotification(
        receiverId: RequestBody,
        message: RequestBody,
    ) = call { api.sendChatNotification(receiverId, message) }

    suspend fun getPageUrl(
        slug: String,
    ) = call { api.getPageUrl(slug) }

    suspend fun storeSellerVerification(
        id: MultipartBody.Part?,
        image: MultipartBody.Part?,
        phoneVerification: RequestBody,
        cardId: RequestBody,
    ) = call {
        api.storeSellerVerification(
            id, image, phoneVerification, cardId
        )
    }


    suspend fun getSellerStatus(
    ) = call { api.getSellerStatus() }

    suspend fun setDefaultShippingAddress(
        addressId: RequestBody?,
    ) = call { api.setDefaultShippingAddress(addressId) }

    suspend fun setDefaultCard(
        cardId: RequestBody?,
    ) = call { api.setDefaultCard(cardId) }

    suspend fun deleteCard(
        cardId: RequestBody?,
    ) = call { api.deleteCard(cardId) }

    suspend fun deleteAddress(
        addressId: RequestBody?,
    ) = call { api.deleteAddress(addressId) }

    suspend fun getStates() = call { api.getStates() }

    suspend fun deleteProduct(productId: String?) = call { api.deleteProduct(productId) }

}