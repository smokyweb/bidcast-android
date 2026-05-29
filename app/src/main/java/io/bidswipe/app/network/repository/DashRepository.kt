package io.bidswipe.app.network.repository

import io.bidswipe.app.base.BaseRepository
import io.bidswipe.app.model.GetSubCategoriesRequest
import io.bidswipe.app.network.ApiInterface
import io.bidswipe.app.network.request.StoreProductRequest
import io.bidswipe.app.network.request.StoreSurpriseSet
import okhttp3.MultipartBody
import okhttp3.RequestBody
import javax.inject.Inject

class DashRepository @Inject constructor(private val api: ApiInterface) : BaseRepository() {

    suspend fun logout() = call {
        api.logout()
    }

    suspend fun deleteProfile(reason: RequestBody?) = call {
        api.deleteProfile(reason)
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

    // #41: Tax exemption
    suspend fun applyTaxExemption(
        state: RequestBody?,
        exemptionType: RequestBody?,
        certificate: MultipartBody.Part?,
    ) = call { api.applyTaxExemption(state, exemptionType, certificate) }

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
        showId: RequestBody? = null,
        // Browse-filter bundle (Basecamp #9928367737): pass-through tag list for backend Tag::findOrCreateByName.
        tags: List<RequestBody>? = null,
        // Basecamp #9933883175 (2026-05-27): seller-controlled verified-buyers-only gate.
        isVerifiedOnly: RequestBody? = null,
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
                showId,
                tags,
                isVerifiedOnly,
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
                isExplicit,
                tags,
                isVerifiedOnly,
            )
        }
    }

    suspend fun getAuctionType() = call { api.getAuctionType() }


    suspend fun getAllTips(
        type: RequestBody?,
    ) = call { api.getAllTips(type) }

    // M1 (2026-05-28): fetch saved per-show tip settings for prefill-on-open.
    suspend fun getTipSetting(
        scheduleShowId: String?,
    ) = call { api.getTipSetting(scheduleShowId) }

    // Basecamp #9940152629 (2026-05-29): save per-show tip settings.
    suspend fun saveTipSetting(
        scheduleShowId: String?,
        tipMessage: String?,
        showInLiveChat: Boolean,
    ) = call { api.saveTipSetting(scheduleShowId, tipMessage, if (showInLiveChat) 1 else 0) }

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
        sellerId: RequestBody? = null,
    ) = call { api.getMyScheduledShow(sellerId, type, page) }

    suspend fun getProfileById(
        userId: RequestBody?,
    ) = call { api.getProfileById(userId) }

    suspend fun followUser(
        userId: RequestBody?,
        showId: RequestBody?,
    ) = call { api.followUser(userId, showId) }

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

    // Basecamp #9933301500 (2026-05-27): 6 new optional filter params
    // Basecamp #9938023997: added category_ids + sub_category_ids arrays
    suspend fun getLiveShow(
        type: RequestBody?,
        category: RequestBody?,
        subCategory: RequestBody?,
        search: RequestBody?,
        page: RequestBody?,
        showFormat: RequestBody? = null,
        tag: RequestBody? = null,
        premierShop: RequestBody? = null,
        shipCountry: RequestBody? = null,
        shipState: RequestBody? = null,
        shipping: RequestBody? = null,
        categoryIds: List<RequestBody>? = null,
        subCategoryIds: List<RequestBody>? = null,
    ) = call { api.getLiveShow(type, category, subCategory, search, page, showFormat, tag, premierShop, shipCountry, shipState, shipping, categoryIds, subCategoryIds) }

    suspend fun getExploreLiveShow(
        type: RequestBody?,
        category: RequestBody?,
        subCategory: RequestBody?,
        search: RequestBody?,
        page: RequestBody?,
        showFormat: RequestBody? = null,
        tag: RequestBody? = null,
        premierShop: RequestBody? = null,
        shipCountry: RequestBody? = null,
        shipState: RequestBody? = null,
        shipping: RequestBody? = null,
        categoryIds: List<RequestBody>? = null,
        subCategoryIds: List<RequestBody>? = null,
    ) = call { api.getLiveShow(type, category, subCategory, search, page, showFormat, tag, premierShop, shipCountry, shipState, shipping, categoryIds, subCategoryIds) }

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
        // MC sub-task cmp4932vk00l13mx1du6mmebo: optional 2nd address line.
        addressLine2: RequestBody?,
        pinCode: RequestBody?,
        city: RequestBody?,
        state: RequestBody?,
    ) = call {
        api.addShippingAddress(
            type,
            name,
            phoneNumber,
            streetAddress,
            addressLine2,
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
        freeShipping: RequestBody?,
        shippingAddressId: RequestBody? = null,
        instruction: RequestBody? = null,
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
            freeShipping,
            shippingAddressId,
            instruction
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

    // Basecamp #9922137198 (Trey 2026-05-20): unified search across shows + products + users.
    // Basecamp #9938023997: pass optional category/subcategory filter arrays
    // Basecamp #9938023997 round 5: pass full filter set through.
    suspend fun unifiedSearch(
        search: String,
        page: Int? = null,
        categoryIds: List<Int>? = null,
        subCategoryIds: List<Int>? = null,
        showFormat: String? = null,
        tag: String? = null,
        premierShop: Boolean? = null,
        shipping: String? = null,
    ) = call { api.unifiedSearch(io.bidswipe.app.network.request.SearchRequest(
        search, page, categoryIds, subCategoryIds, showFormat, tag, premierShop, shipping
    )) }

    suspend fun saveSellerProduct(
        productId: RequestBody?,
    ) = call { api.saveSellerProduct(productId) }

    suspend fun getProductsByStatus(
        type: RequestBody?,
        page: RequestBody?,
        status: RequestBody?
    ) = call { api.getProductsByStatus(type, page, status) }

    // Basecamp #9933973683 (2026-05-29 return): active flash-sales listing.
    suspend fun getFlashSales() = call { api.getFlashSales() }

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

    // MC cmph7xsgw00g2ms8pmtc6xgz5 (Trey 2026-05-22): seller coupon CRUD.
    suspend fun listSellerCoupons() = call { api.listSellerCoupons() }
    suspend fun createSellerCoupon(
        body: io.bidswipe.app.network.request.CreateSellerCouponRequest
    ) = call { api.createSellerCoupon(body) }
    suspend fun deleteSellerCoupon(id: Int) = call { api.deleteSellerCoupon(id) }
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
        orderId: String?, productType: String?
    ) = call { api.fetchOrderDetail(productId, orderId, productType) }

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
        maxItems: RequestBody?,
        maxItemUnit: RequestBody?,
        height: RequestBody?,
        width: RequestBody?,
        length: RequestBody?,
        scale: RequestBody?,
        incrementWeight: RequestBody?,
        incrementWeightUnit: RequestBody?,
    ) = call {
        api.storeShippingProfile(
            shippingId,
            name,
            size,
            weight,
            additionalWeight,
            maxItems,
            maxItemUnit,
            height,
            width,
            length,
            scale,
            incrementWeight,
            incrementWeightUnit
        )
    }

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
    ) = call { api.checkScheduleShow(date, time, showId) }

    suspend fun updateVacationModeStatus(
        vacationMode: RequestBody?
    ) = call { api.updateVacationModeStatus(vacationMode) }

    suspend fun deleteShippingProfile(
        profileId: String
    ) = call { api.deleteShippingProfile(profileId) }

    suspend fun changeOrderStatus(
        orderId: RequestBody?,
        status: RequestBody?
    ) = call { api.changeOrderStatus(orderId, status) }

    // #32 Wave 4: change status + save tracking number
    suspend fun changeOrderStatusWithTracking(
        orderId: RequestBody?,
        status: RequestBody?,
        trackingNumber: RequestBody?
    ) = call { api.changeOrderStatusWithTracking(orderId, status, trackingNumber) }

    // #33 Wave 4: create USPS shipping label
    suspend fun createShippingLabel(
        orderId: RequestBody?
    ) = call { api.createShippingLabel(orderId) }

    suspend fun getClip(
        roomId: RequestBody?,
        // Basecamp #9929851737 (2026-05-26): optional clip duration in seconds.
        durationSec: RequestBody? = null,
    ) = call { api.getClip(roomId, durationSec) }

    suspend fun getUserClips(
        sellerId: String?,
        page: String?,
    ) = call { api.getUserClips(sellerId, page) }

    suspend fun storeSurpriseProduct(
        request: StoreSurpriseSet?
    ) = call { api.storeSurpriseProduct(request) }

    suspend fun editSurpriseProduct(
        unitId: RequestBody?,
        price: RequestBody?,
        description: RequestBody?,
    ) = call { api.editSurpriseSetUnit(unitId, price, description) }

    suspend fun deleteSurpriseSet(
        productSetId: RequestBody?,
    ) = call { api.deleteSurpriseSet(productSetId) }

    suspend fun getSurpriseProduct(
        page: String?
    ) = call { api.getSurpriseProduct(page) }

    suspend fun getSurpriseProductDetail(
        setId: String?
    ) = call { api.getSetDetails(setId) }

    suspend fun getUSPSBoxDimensions(
    ) = call { api.getUSPSBoxDimensions() }

    suspend fun saveDomesticShipmentSetting(
        domesticShipmentForm1To5Lbs: RequestBody?,
        domesticShipmentOver5Lbs: RequestBody?,
        alsoApplyScheduleShow: RequestBody?,
        uspsFirstClassMailLetter: RequestBody?
    ) = call {
        api.saveDomesticShipmentSetting(
            domesticShipmentForm1To5Lbs,
            domesticShipmentOver5Lbs,
            alsoApplyScheduleShow,
            uspsFirstClassMailLetter
        )
    }

    suspend fun saveShippingCosts(shippingCosts: RequestBody?, alsoApplyScheduleShow: RequestBody?,) = call { api.saveShippingCosts(shippingCosts ,alsoApplyScheduleShow) }
    suspend fun getShippingDetails() = call { api.getShippingDetails() }

    // 2026-05-04 (MC cmordzx1s00cuf3hgkwnkkplg) Account Security parity with iOS
    suspend fun changePassword(
        currentPassword: RequestBody,
        newPassword: RequestBody,
        newPasswordConfirmation: RequestBody,
    ) = call { api.changePassword(currentPassword, newPassword, newPasswordConfirmation) }

    suspend fun deleteAccountRequest(reason: RequestBody?) = call { api.deleteAccountRequest(reason) }

    // ── Randomizer templates (2026-05-26) ───────────────────────────────

    suspend fun getRandomizerTemplates() = call { api.getRandomizerTemplates() }

    suspend fun getRandomizerTemplate(id: Int) = call { api.getRandomizerTemplate(id) }

    suspend fun createRandomizerTemplate(body: io.bidswipe.app.network.request.RandomizerTemplateRequest) =
        call { api.createRandomizerTemplate(body) }

    suspend fun updateRandomizerTemplate(id: Int, body: io.bidswipe.app.network.request.RandomizerTemplateRequest) =
        call { api.updateRandomizerTemplate(id, body) }

    suspend fun deleteRandomizerTemplate(id: Int) = call { api.deleteRandomizerTemplate(id) }

    suspend fun releaseTemplateProducts(id: Int) = call { api.releaseTemplateProducts(id) }

    suspend fun duplicateTemplate(id: Int) = call { api.duplicateTemplate(id) }

    suspend fun attachRandomizerTemplate(showId: String, templateId: Int) =
        call { api.attachRandomizerTemplate(showId, io.bidswipe.app.network.request.AttachTemplateRequest(templateId)) }

    suspend fun detachRandomizerTemplate(showId: String) =
        call { api.detachRandomizerTemplate(showId) }

    // Basecamp #9933847997 (2026-05-29): pre-bid endpoints
    suspend fun placePrebid(productId: Int, amount: Double, scheduleShowId: Int?) =
        call { api.placePrebid(productId, amount, scheduleShowId) }

    suspend fun getMyPreBids() = call { api.getMyPreBids() }

    suspend fun withdrawPreBid(id: Int) = call { api.withdrawPreBid(id) }

    suspend fun getHighestPreBid(productId: Int) = call { api.getHighestPreBid(productId) }

    // Basecamp #9943368953 (2026-05-29): live-show chat history via REST
    suspend fun getChatHistory(roomId: String) = call { api.getChatHistory(roomId) }

}