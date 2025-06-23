package io.bidswipe.app.ui.dashboard.product

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.MakeOfferAdapter
import io.bidswipe.app.databinding.FragmentProductDetailsBinding
import io.bidswipe.app.databinding.MakeOfferSheetBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.OfferModel
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetPaymentCardsResponse
import io.bidswipe.app.network.response.GetPurchaseDetail
import io.bidswipe.app.network.response.GetShippingAddressResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.asMoney
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.hideKeyboard
import io.bidswipe.app.utils.ids
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.value

class ProductDetailsFragment : BaseFragment<ProductViewModel, FragmentProductDetailsBinding>() {
    override fun getModel(): Class<ProductViewModel> = ProductViewModel::class.java

    override fun getBind(inflater: LayoutInflater, view: ViewGroup?) =
        FragmentProductDetailsBinding.inflate(inflater, view, false)

    private var productId = ""
    private var price = ""

    private var offerList = mutableListOf<OfferModel>()

    private var checkOutData: GetPurchaseDetail.Data? = null

    private var cardList = mutableListOf<GetPaymentCardsResponse.Data?>()
    private var addressList = mutableListOf<GetShippingAddressResponse.Data?>()

    //	private lateinit var makeOfferSheetBind : MakeOfferSheetBinding
    private lateinit var offerAdapter: MakeOfferAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        productId = activity?.intent?.getStringExtra("productId") ?: ""

        bind.header.onBackClick {
            finish()
        }


        bind.buyNow.setOnClickListener {

            findNavController().navigate(ids.goToBuyNowFragment)


            /*var buyNowSheetBind = BuyNowSheetBinding.bind(layoutInflater.inflate(R.layout.buy_now_sheet, null, false))
            var buyNowSheet = Alerts.appBottomSheet(mCtx, true, buyNowSheetBind)

            buyNowSheetBind.cardNumber.setCompoundDrawablesWithIntrinsicBounds(ContextCompat.getDrawable(mCtx, draw.ic_visa), null, null, null)

            buyNowSheetBind.productName.text = product?.title

            buyNowSheetBind.productDescription.text = product?.description

            buyNowSheetBind.productImg.loadUrl(mCtx, product?.images?.get(0).toString())

            if (cardList.isNotEmpty()){
                buyNowSheetBind.cardNumber.text = buildString {
                    append("**** **** **** ")
                    append(cardList[0]?.last4)
                }
            }



            if (addressList.isNotEmpty()){
                buyNowSheetBind.address.text = addressList.find { it?.isDefault == true }?.streetAddress ?: addressList[0]?.streetAddress
            }


            buyNowSheetBind.subTotal.text = checkOutData?.subTotal.toString()
            buyNowSheetBind.tax.text = checkOutData?.taxAmount.toString()
            buyNowSheetBind.shipping.text = checkOutData?.shippingCharges.toString()
            buyNowSheetBind.total.text = checkOutData?.total.toString()

            buyNowSheetBind.confirmButton.setOnClickListener {

                buyNowSheet.dismiss()

                if (buyNowSheetBind.sendAsGift.isChecked){
                    findNavController().navigate(ids.goToSendGiftFragment, bundleOf("shippingId" to shippingId.toString(),"productId" to productId.toString(),"cardId" to cardList[0]?.cardId?.toString(),"promoCode" to buyNowSheetBind.promoCode.value()))
                }else{
                    bind.loader.isVisible = true

                    viewModel.createOrder(
                        shippingId.toString().request(),
                        productId.request(),
                        cardList[0]?.cardId?.request(),
                        buyNowSheetBind.promoCode.value().ifEmpty { null }?.request(),
                        "0".request(),
                        null,
                        null,
                        buyNowSheetBind.shipping.text.toString().request(),
                        buyNowSheetBind.tax.text.toString().request(),
                        buyNowSheetBind.subTotal.text.toString().request(),
                        buyNowSheetBind.total.text.toString().request()

                    )

                }

            }

            buyNowSheet.show()*/

        }

        bind.makeOffer.setOnClickListener {
            showOfferSheet()
        }


        bind.loader.isVisible = true
        viewModel.getProductDetails(productId.request())

        viewModel.getProductDetailsRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false

                    val mData = it.value.data

                    viewModel.product = mData

                    bind.userName.text = mData?.user?.name

                    if (mData?.user?.sellerVerification == true) {
                        bind.sellerStatus.text = "Verified Seller"
                    } else {
                        bind.sellerStatus.text = "Unverified Seller"
                    }

                    bind.makeOffer.isVisible = mData?.acceptOffers == true

                    bind.userImage.loadUrl(mCtx, mData?.user?.profileImage.toString())

                    bind.productImage.loadUrl(mCtx, mData?.images?.get(0).toString())

                    bind.productName.text = mData?.title
                    bind.posted.text = Utils.getTimeAgo(mData?.createdAt ?: "")

                    bind.price.text = mData?.pricing.toString().asMoney()

                    offerList.clear()

                    offerList.addAll(
                        listOf(
                            OfferModel(
                                getDiscountAmount(mData?.pricing?.toDouble() ?: 0.0, 20),
                                "20% off"
                            ), OfferModel(
                                getDiscountAmount(mData?.pricing?.toDouble() ?: 0.0, 15),
                                "15% off"
                            ), OfferModel(
                                getDiscountAmount(mData?.pricing?.toDouble() ?: 0.0, 10),
                                "10% off"
                            ), OfferModel(
                                getDiscountAmount(mData?.pricing?.toDouble() ?: 0.0, 5),
                                "5% off"
                            )
                        )
                    )

                    bind.address.text = mData?.shippingAdress?.streetAddress

                }

                is Resource.Error -> {
                    bind.loader.isVisible = false

                    if (it.isNetworkError) {
                        errorToast(getString(R.string.no_internet))
                    } else {
                        it.parse(mCtx, TAG, object : AlertClicks {
                            override fun primaryClick(dialog: AppBottomSheet) {
                                dialog.dismiss()

                            }

                            override fun secondaryClick(dialog: AppBottomSheet) {
                                dialog.dismiss()

                            }
                        })
                    }
                }

                else -> {}

            }
        }

        viewModel.makeOfferRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false

                    it.value.data

                    Alerts.success(mCtx, "Offer Sent")

                }

                is Resource.Error -> {
                    bind.loader.isVisible = false

                    if (it.isNetworkError) {
                        errorToast(getString(R.string.no_internet))
                    } else {
                        it.parse(mCtx, TAG, object : AlertClicks {
                            override fun primaryClick(dialog: AppBottomSheet) {
                                dialog.dismiss()

                            }

                            override fun secondaryClick(dialog: AppBottomSheet) {
                                dialog.dismiss()

                            }
                        })
                    }
                }

                else -> {}

            }
        }

    }

    fun getDiscountAmount(originalAmount: Double, percentOff: Int): String {
        val discountedAmount = originalAmount - (originalAmount * percentOff) / 100
        return discountedAmount.toString()
    }

    fun showOfferSheet() {
        var makeOfferSheetBind = MakeOfferSheetBinding.bind(
            layoutInflater.inflate(
                R.layout.make_offer_sheet,
                null,
                false
            )
        )
        var makeOfferSheet = Alerts.appBottomSheet(mCtx, true, makeOfferSheetBind)

        makeOfferSheetBind.listedPrice.text = viewModel.product?.pricing.toString().asMoney()

        makeOfferSheetBind.offerRecycler.adapter =
            MakeOfferAdapter(offerList, object : RecyclerClicks {

                override fun itemClick(pos: Int, status: String?) {

                    makeOfferSheetBind.customOffer.setText(offerList[pos].amount)

                    offerList.forEachIndexed { index, item ->
                        item.selected = index == pos
                    }

                    makeOfferSheetBind.offerRecycler.adapter?.notifyDataSetChanged()
                }
            })

        makeOfferSheetBind.close.setOnClickListener {
            makeOfferSheet.dismiss()
        }

        makeOfferSheetBind.select.setOnClickListener {

            if (makeOfferSheetBind.customOffer.value().isEmpty()) {
                Alerts.error(mCtx, "Please Enter Offer Amount")
            } else {
                hideKeyboard(it)
                makeOfferSheet.dismiss()
                bind.loader.isVisible = true
                viewModel.makeOffer(
                    makeOfferSheetBind.customOffer.value().request(),
                    productId.request()
                )
            }

        }
        makeOfferSheet.show()
    }

}


