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
        productImages: List<MultipartBody.Part?>?
    ) = call { api.storeProduct(categoryId, title, description, quantity, pricing, flashSale, acceptOffers, reserveForLive, shippingProfileId, status, productImages) }

    suspend fun getHowToSellStep() = call { api.getHowToSellStep() }

    suspend fun getPrepareStep() = call { api.getPrepareStep() }

    suspend fun getFAQ() = call { api.getFAQ() }


    suspend fun contactUs(
        name : RequestBody?,
        email: RequestBody?,
        subject: RequestBody?,
        message : RequestBody?
    ) = call { api.contactUs(name,email,subject,message) }

    suspend fun storeScheduleShow(
        title: RequestBody?,
        date: RequestBody?,
        time: RequestBody?,
        categoryId: RequestBody?,
        auctionTypeId : RequestBody?,
        thumbnails: List<MultipartBody.Part?>?,
        productIds: List<Int?>
    ) = call { api.storeScheduleShow(title,date,time,categoryId,auctionTypeId,thumbnails,productIds) }

    suspend fun getAuctionType() = call { api.getAuctionType() }


    suspend fun getAllTips(
        type : RequestBody?,
    ) = call { api.getAllTips(type) }


    suspend fun getUserProducts(
    ) = call { api.getUserProducts() }

    suspend fun getMyScheduledShow(
        type : RequestBody?,
    ) = call { api.getMyScheduledShow(type) }

    suspend fun getProfileById(
        userId : RequestBody?
    ) = call { api.getProfileById(userId) }

    suspend fun followUser(
        userId : RequestBody?
    ) = call { api.followUser(userId) }

    suspend fun makeOffer(
        amount : RequestBody?,
        productId : RequestBody?
    ) = call { api.makeOffer(amount, productId) }

    suspend fun offerList(
        page: Int
    ) = call { api.offerList(page) }

    suspend fun offerUpdateStatus(
        offerId : RequestBody?,
        status : RequestBody?
    ) = call { api.offerUpdateStatus(offerId,status) }

 suspend fun getLiveShow(
    ) = call { api.getLiveShow() }

    suspend fun notifyLiveUser(
        liveUserId : RequestBody?
    ) = call { api.notifyLiveUser(liveUserId) }

    suspend fun getProductDetails(
        productId : RequestBody?
    ) = call { api.getProductDetails(productId) }

    suspend fun addShippingAddress(
        type : RequestBody?,
        name : RequestBody?,
        phoneNumber : RequestBody?,
        streetAddress : RequestBody?,
        pinCode : RequestBody?
    ) = call { api.addShippingAddress(type,name,phoneNumber,streetAddress,pinCode) }

    suspend fun getShippingAddress() = call { api.getShippingAddress() }

    suspend fun addPaymentCard(
        cardToken : RequestBody?
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
        shippingId : RequestBody?,
        productId : RequestBody?
    ) = call { api.getPurchaseProduct(shippingId,productId) }

    suspend fun createOrder(
        shippingId : RequestBody?,
        productId : RequestBody?,
        cardId : RequestBody?,
        promoCode : RequestBody?,
        sendAsGift : RequestBody?,
        giftUserId : RequestBody?,
        giftMsg : RequestBody?,
        shippingCharges : RequestBody?,
        taxAmount : RequestBody?,
        subTotal : RequestBody?,
        total : RequestBody?,
        discount : RequestBody?
    ) = call { api.createOrder(shippingId,productId,cardId,promoCode,sendAsGift,giftUserId,giftMsg,shippingCharges,taxAmount,subTotal,total,discount) }

}