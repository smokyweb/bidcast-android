package io.bidswipe.app.network.repository

import io.bidswipe.app.base.BaseRepository
import io.bidswipe.app.model.GetSubCategoriesRequest
import io.bidswipe.app.model.StoreProductRequest
import io.bidswipe.app.network.ApiInterface
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
        search: String? = null,
        getCount: String? = null
    ) = call { api.getCategory(categoryId, type, search, getCount) }

    suspend fun getSubCategories(
        categoryIds: List<Int>, subCategoryIds: List<Int>? = null,
    ) = call { api.getSubCategories(GetSubCategoriesRequest(categoryIds, subCategoryIds)) }

    suspend fun userFavorite(
        categoryIds: List<Int>,
        subcategoriesIds: List<Int>? = null,
    ) = call { api.userFavorite(GetSubCategoriesRequest(categoryIds, subcategoriesIds)) }

    suspend fun getLesson() = call { api.getLesson() }

    suspend fun storeProduct(
        storeProductModel: StoreProductRequest,
        productId: String?
    ) = call { api.storeProduct(storeProductModel, productId) }

    suspend fun storeProductMeta(
        productImages: List<MultipartBody.Part?>?,
        videos: List<MultipartBody.Part?>?,
        thumbnail: List<MultipartBody.Part?>? = null,
    ) = call {
        api.storeProductMeta(
            productImages,
            videos,
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
        subCategoryId: RequestBody?,
        showDiscoverability: RequestBody?,
        auctionTypeId: RequestBody?,
        thumbnails: List<MultipartBody.Part?>?,
        productIds: List<Int>,
        isRepeat: RequestBody?,
        repeatValue: RequestBody?,
        language: RequestBody?,
        isExplicit: RequestBody?,
        showId: RequestBody? = null
    ) = call {
        if (showId != null) {
            api.updateScheduleShow(
                title,
                date,
                time,
                categoryId,
                subCategoryId,
                showDiscoverability,
                auctionTypeId,
                productIds,
                thumbnails,
                isRepeat,
                repeatValue,
                language,
                isExplicit,
                showId
            )
        } else {
            api.storeScheduleShow(
                title,
                date,
                time,
                categoryId,
                subCategoryId,
                showDiscoverability,
                auctionTypeId,
                productIds,
                thumbnails,
                isRepeat,
                repeatValue,
                language,
                isExplicit
            )
        }
    }

    suspend fun getAuctionType() = call { api.getAuctionType() }


    suspend fun getAllTips(
        type: RequestBody?,
    ) = call { api.getAllTips(type) }

    /*	suspend fun getUserProducts(
            userId: RequestBody?,
            categoryId: RequestBody?,
            page: RequestBody?,
            type: RequestBody?,
            saleType: RequestBody?,
            sortBy: RequestBody?,
            search: RequestBody?,


        ) = call { api.getProducts(userId, categoryId, page, type, saleType, sortBy, search) }*/

    suspend fun getMyScheduledShow(
        type: RequestBody?,
        page: RequestBody?,
        sellerId: RequestBody?=null,
    ) = call { api.getMyScheduledShow(sellerId,type, page) }

    suspend fun getProfileById(
        userId: RequestBody?,
    ) = call { api.getProfileById(userId) }

    suspend fun followUser(
        userId: RequestBody?,
        showId: RequestBody?,
    ) = call { api.followUser(userId,showId) }

    suspend fun makeOffer(
        amount: RequestBody?,
        productId: RequestBody?,
    ) = call { api.makeOffer(amount, productId) }

    suspend fun offerList(
        page: RequestBody?,
        offerType: RequestBody?,
    ) = call { api.offerList(page, offerType) }

    suspend fun offerUpdateStatus(
        offerId: RequestBody?,
        status: RequestBody?,
    ) = call { api.offerUpdateStatus(offerId, status) }

    suspend fun getLiveShow(
        type: RequestBody?,
        category: RequestBody?,
        subCategory: RequestBody?,
        search: RequestBody?,
        page: RequestBody?
    ) = call { api.getLiveShow(type, category, subCategory,search, page) }

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
        cardToken: RequestBody?
    ) = call { api.addPaymentCard(cardToken) }

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
        freeShipping: RequestBody?
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
            hapticFeedback,
            freeShipping
        )
    }

    suspend fun getPurchaseProduct(
        shippingId: RequestBody?,
        productId: RequestBody?,
        couponName: RequestBody?,
    ) = call { api.getPurchaseProduct(shippingId, productId, couponName) }

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

    suspend fun storeDeviceDetails(
        deviceToken: RequestBody?,
    ) = call { api.storeDeviceDetails(deviceToken) }

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

    suspend fun getProducts(
        userId: RequestBody?,
        status: RequestBody?,
        format: RequestBody?,
        page: RequestBody?,
        search: RequestBody?,
        categoryIds: RequestBody?,
        conditions: RequestBody?,
        minPrice: RequestBody?,
        maxPrice: RequestBody?,
        marketPlace: RequestBody?,
        type: RequestBody?,
        saleType: RequestBody?,
        sortBy: RequestBody?
    ) = call {
        api.getProducts(
            userId,
            status,
            format,
            page,
            search,
            categoryIds,
            conditions,
            minPrice,
            maxPrice,
            marketPlace,
            type,
            saleType,
            sortBy
        )
    }

    suspend fun getOrderListing(
        page: RequestBody?,
        type: RequestBody?,
        search: RequestBody?,
    ) = call { api.getOrderListing(page, type, search) }

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
		page: String?
    ) = call { api.getNotification(page) }

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
        status: RequestBody?
    ) = call { api.getProductsByStatus(type, page, status) }

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
        status: RequestBody?,
    ) = call { api.getTransactionsHistory(page, status) }

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

    suspend fun getCoupon() = call { api.getCoupon() }
    suspend fun deleteProduct(productId: String?) = call { api.deleteProduct(productId) }

    suspend fun blockUnblockUser(
        blockedID: RequestBody,
    ) = call { api.blockUnblockUser(blockedID) }

    suspend fun getBlockedUsers(
    ) = call { api.getBlockedUsers() }

    suspend fun getMailClasses(
    ) = call { api.getMailClasses() }

    suspend fun getPremierShop(
    ) = call { api.getPremierShop() }

    suspend fun getPromoteTools(
    ) = call { api.getPromoteTools() }

    suspend fun getPromoteShowList() = call { api.getPromoteShowList() }

    suspend fun promoteShow(
        scheduleShowId: RequestBody,
        promoteShowId: RequestBody
    ) = call { api.promoteShow(scheduleShowId, promoteShowId) }

    suspend fun sendTipAmount(
        sellerId: RequestBody,
        amount: RequestBody,
        cardNumber: RequestBody?
    ) = call { api.sendTipAmount(sellerId, amount, cardNumber) }

    suspend fun payout(
        amount: RequestBody,
    ) = call { api.payout(amount) }

    suspend fun applyPremierShop(
    ) = call { api.applyPremierShop() }

    suspend fun walletInfo(
    ) = call { api.walletInfo() }

    suspend fun getTipAmount(
    ) = call { api.getTipAmount() }

    suspend fun getSellerAnalytics(
    ) = call { api.getSellerAnalytics() }

    suspend fun getVisitorsAnalytics(
    ) = call { api.getVisitorsAnalytics() }

    suspend fun getSalesPerformance(
    ) = call { api.getSalesPerformance() }

    suspend fun getLiveSeller(
    ) = call { api.getLiveSeller() }

    suspend fun getAgoraToken(
        channel: RequestBody,
        uId: RequestBody?
    ) = call { api.getAgoraToken(channel, uId) }

    suspend fun getSellerInfo(
        sellerId: String
    ) = call { api.getSellerInfo(sellerId) }

    suspend fun getReportCategories(
    ) = call { api.getReportCategories() }

    suspend fun reportSeller(
        sellerId: RequestBody,
        categoryId: RequestBody?,
        notes: RequestBody?
    ) = call { api.reportSeller(sellerId, categoryId, notes) }

    suspend fun fetchOrderDetail(
        productId: String?,
        orderId: String?
    ) = call { api.fetchOrderDetail(productId, orderId) }

    suspend fun updateProductStatus(
        productId: RequestBody?,
        status: RequestBody?
    ) = call { api.updateProductStatus(productId, status) }

    suspend fun storeShippingProfile(
        shippingId: RequestBody?,
        name: RequestBody?,
        size: RequestBody?,
        weight: RequestBody?,
        additionalWeight: RequestBody?,
        maxItems: RequestBody?
    ) = call { api.storeShippingProfile(shippingId,name, size, weight, additionalWeight, maxItems) }

    suspend fun getShippingProfile(
    ) = call { api.getShippingProfile() }

    suspend fun getShowOverview(
        showId: String
    ) = call { api.getShowOverview(showId) }

    suspend fun getShowDetails(
        showId: String
    ) = call { api.getShowDetails(showId) }

    suspend fun raiseTicket(
        orderId: RequestBody?,
        subject: RequestBody?,
        message: RequestBody?
    ) = call { api.raiseTicket(orderId, subject, message) }

    suspend fun exportAnalyticsData(
        type: String?,
        filter: String?,
        startDate: String?,
        endDate: String?
    ) = call { api.exportAnalyticsData(type, filter, startDate, endDate) }

    suspend fun getPromoteToolsDetails() = call { api.getPromoteToolsDetails() }

    suspend fun getSellerHubInfo() = call { api.getSellerHubInfo() }

    suspend fun checkScheduleShow(
        date: RequestBody?,
        time: RequestBody?,
        showId: RequestBody?
    ) = call { api.checkScheduleShow(date, time,showId) }

    suspend fun updateVacationModeStatus(
        vacationMode: RequestBody?
    ) = call { api.updateVacationModeStatus(vacationMode) }

	suspend fun deleteShippingProfile(
		profileId: String
	) = call { api.deleteShippingProfile(profileId) }

	suspend fun changeOrderStatus(
		orderId: RequestBody?,
		status: RequestBody?
	) = call { api.changeOrderStatus(orderId,status) }

	suspend fun getClip(
		roomId: RequestBody?,
	) = call { api.getClip(roomId) }

}