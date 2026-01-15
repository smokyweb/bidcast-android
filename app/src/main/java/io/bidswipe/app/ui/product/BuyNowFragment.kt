package io.bidswipe.app.ui.product

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.text.buildSpannedString
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.CouponsAdapter
import io.bidswipe.app.controller.SelectAddressAdapter
import io.bidswipe.app.controller.SelectPaymentCardAdapter
import io.bidswipe.app.databinding.AddressSheetBinding
import io.bidswipe.app.databinding.CouponSheetBinding
import io.bidswipe.app.databinding.FragmentBuyNowBinding
import io.bidswipe.app.databinding.PaymentSheetBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetCouponsResponse
import io.bidswipe.app.network.response.GetPaymentCardsResponse
import io.bidswipe.app.network.response.GetShippingAddressResponse
import io.bidswipe.app.ui.custom.AlertType
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.more.MoreActivity
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.asMoney
import io.bidswipe.app.utils.draw
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.goToAddCard
import io.bidswipe.app.utils.ids
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.setHapticClickListener
import io.bidswipe.app.utils.toOrderStatus
import io.bidswipe.app.utils.value

@SuppressLint("InflateParams", "NotifyDataSetChanged")
class BuyNowFragment : BaseFragment<ProductViewModel, FragmentBuyNowBinding>() {
    override fun getModel(): Class<ProductViewModel> = ProductViewModel::class.java

    override fun getBind(
        inflater: LayoutInflater,
        view: ViewGroup?,
    ) = FragmentBuyNowBinding.inflate(inflater, view, false)

    private var cardList = mutableListOf<GetPaymentCardsResponse.Data?>()
    private var addressList = mutableListOf<GetShippingAddressResponse.Data?>()
    private var shippingId = 0
    private var cardId = ""
    private var couponList = mutableListOf<GetCouponsResponse.Data?>()
    private var addCardLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                bind.loader.isVisible = true
                viewModel.getPaymentCard()
            }
        }

    private var addAddressLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                bind.loader.isVisible = true
                viewModel.getShippingAddress()
            }

        }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bind.header.onBackClick {
            findNavController().popBackStack()
        }

        bind.changeAddress.setHapticClickListener {
            if (addressList.isEmpty()) {
                addAddressLauncher.launch(
                    Intent(mCtx, MoreActivity::class.java).putExtra(
                        "slug",
                        "addAddress"
                    )
                )
            } else {
                showAddressSheet()
            }
        }

        bind.changePayment.setHapticClickListener {

            if (cardList.isEmpty()) {
                addCardLauncher.launch(mCtx.goToAddCard("buyNow"))
            } else {
                showPaymentMethodSheet()
            }
        }

        bind.promoCodeLayout.suffixTextView.setOnClickListener {
            if (bind.promoCodeLayout.suffixText != "Applied!") {
                if (bind.promoCode.value().isEmpty()) {
                    Alerts.error(mCtx, "Please enter promo code")
                } else {
                    bind.loader.isVisible = true
                    viewModel.getPurchaseProduct(
                        shippingId.toString().request(),
                        viewModel.product?.id.toString().request(),
                        bind.promoCode.value().ifEmpty { null }?.request()
                    )
                }
            }
        }

        bind.viewAllCoupons.setOnClickListener {
            showCouponSheet { code ->
                bind.promoCode.setText(code)
                bind.promoCodeLayout.suffixText = "Applied!"
                bind.loader.isVisible = true
                viewModel.getPurchaseProduct(
                    shippingId.toString().request(),
                    viewModel.product?.id.toString().request(),
                    code?.request()
                )
            }
        }

        bind.loader.isVisible = true
        viewModel.getPaymentCard()
        viewModel.getShippingAddress()

        bind.productName.text = viewModel.product?.title?.asCapital()
        bind.productDescription.text = viewModel.product?.description ?: ""

        bind.category.text = buildSpannedString {
            if (viewModel.product?.productCondition != null) {
                append((viewModel.product?.productCondition?.replace("_", " ")))
                append(" ")
                append(Const.BULLET)
                append(" ")
            }
            append(viewModel.product?.category?.name ?: "")
        }

        bind.productImg.loadUrl(mCtx, viewModel.product?.images?.get(0).toString())
        val offer = viewModel.product?.offer
        if (offer != null) {
            when (offer.status) {
                "accepted" -> {
                    bind.price.text = offer.amount.toString().asMoney()
                }

                else -> {
                    bind.price.text = viewModel.product?.pricing.toString().asMoney()
                }
            }
        } else {
            bind.price.text = viewModel.product?.pricing.toString().asMoney()
        }

        bind.confirmButton.setHapticClickListener {
            when {
                cardList.isEmpty() -> {
                    Alerts.error(mCtx, "Please add Payment card")
                }

                addressList.isEmpty() -> {
                    Alerts.error(mCtx, "Please add Shipping Address")
                }

                else -> {
                    if (bind.sendAsGift.isChecked) {
                        findNavController().navigate(
                            ids.buyNowToSendGiftFragment,
                            bundleOf(
                                "shippingId" to shippingId.toString(),
                                "productId" to viewModel.product?.id.toString(),
                                "cardId" to cardList[0]?.cardId.toString(),
                                "promoCode" to bind.promoCode.value()
                            )
                        )
                    } else {
                        bind.loader.isVisible = true
                        viewModel.createOrder(
                            shippingId = shippingId.toString().request(),
                            productId = viewModel.product?.id.toString().request(),
                            cardId = null,
                            promoCode = bind.promoCode.value().ifEmpty { null }?.request(),
                            sendAsGift = "0".request(),
                            giftUserId = null,
                            giftMsg = null,
                            shippingCharges = viewModel.checkoutData?.shippingCharges.toString().request(),
                            taxAmount = viewModel.checkoutData?.taxAmount.toString().request(),
                            subTotal = viewModel.checkoutData?.subTotal.toString().request(),
                            total = viewModel.checkoutData?.total.toString().request(),
                            discount = viewModel.checkoutData?.discountAmount.toString().request()
                        )

                    }
                }
            }
        }

        viewModel.getShippingAddressRepo.observe(viewLifecycleOwner) { response ->
            when (response) {
                is Resource.Success -> {
                    viewModel.getShippingAddressRepo.value=null
                    bind.loader.isVisible = false

                    val mData = response.value.data

                    addressList.clear()

                    if (mData?.isNotEmpty() == true) {
                        addressList.addAll(mData)

                        bind.address.text =
                            addressList.find { it?.isDefault == true }?.streetAddress
                                ?: addressList[0]?.streetAddress


                        shippingId = addressList.find { it?.isDefault == true }?.id
                            ?: (addressList[0]?.id
                                ?: 0)

                        addressList[0]?.selected = true

                        viewModel.getPurchaseProduct(
                            shippingId.toString().request(),
                            viewModel.product?.id.toString().request()
                        )
                    } else {
                        AppBottomSheet(
                            mCtx,
                            R.drawable.ic_info,
                            "No Shipping Address Found!",
                            "Please add shipping address to continue",
                            primaryBtnText = "Okay",
                            secondaryBtnText = "Cancel",
                            canCancel = false,
                            showSecondary = true,
                            alertType = AlertType.INFO,
                            clicks = object : AlertClicks {
                                override fun primaryClick(dialog: AppBottomSheet) {
                                    dialog.dismiss()
                                    addAddressLauncher.launch(
                                        Intent(mCtx, MoreActivity::class.java).putExtra(
                                            "slug",
                                            "addAddress"
                                        )
                                    )
                                }

                                override fun secondaryClick(dialog: AppBottomSheet) {
                                    dialog.dismiss()
                                    findNavController().popBackStack()
                                }
                            }

                        ).show()
                    }

                    if (addressList.isEmpty()) {
                        bind.address.text = buildString {
                            append("Address not Found")
                        }
                        bind.changeAddress.text = buildString {
                            append("Add Address")
                        }
                    }

//					shippingAddressAdapter.notifyDataSetChanged()

                }

                is Resource.Error -> {
                    viewModel.getShippingAddressRepo.value=null
                    bind.loader.isVisible = false
                    response.parse(mCtx, TAG, object : AlertClicks {
                        override fun primaryClick(dialog: AppBottomSheet) {
                            dialog.dismiss()
                        }

                        override fun secondaryClick(dialog: AppBottomSheet) {
                            dialog.dismiss()
                        }
                    })
                }

                else -> {}

            }
        }

        viewModel.getPaymentCardRepo.observe(viewLifecycleOwner) {

            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false

                    val mData = it.value.data

                    cardList.clear()

                    if (mData?.isNotEmpty() == true) {
                        cardList.addAll(mData)
                    }

                    if (cardList.isEmpty()) {

                        bind.cardNumber.text = buildString {
                            append("No Cards Found")
                        }

                        bind.changePayment.text = buildString {
                            append("Add Card")
                        }

                    } else {
                        bind.cardNumber.text = buildString {
                            append("**** **** **** ")
                            append(cardList[0]?.last4)
                        }
                        bind.cardNumber.setCompoundDrawablesWithIntrinsicBounds(
                            ContextCompat.getDrawable(
                                mCtx,
                                draw.ic_visa
                            ), null, null, null
                        )

                        cardList[0]?.selected = true

                        cardId = cardList[0]?.cardId.toString()
                    }
//					cardAdapter.notifyDataSetChanged()
                }

                is Resource.Error -> {
                    bind.loader.isVisible = false

                    it.parse(mCtx, TAG, object : AlertClicks {
                        override fun primaryClick(dialog: AppBottomSheet) {
                            dialog.dismiss()

                        }

                        override fun secondaryClick(dialog: AppBottomSheet) {
                            dialog.dismiss()

                        }
                    })
                }

                else -> {}
            }
        }

        viewModel.getPurchaseProductRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    viewModel.getPurchaseProductRepo.value = null
                    bind.loader.isVisible = false

                    val mData = it.value.data

                    viewModel.checkoutData = mData

                    bind.subTotal.text = mData?.subTotal.toString().asMoney()
                    bind.tax.text = mData?.taxAmount.toString().asMoney()
                    bind.shipping.text = mData?.shippingCharges.toString().asMoney()
                    bind.total.text = mData?.total.toString().asMoney()

                    if (mData?.discountAmount != null && (mData.discountAmount != "0.00")) {
                        bind.discountView.isVisible = true
                        bind.discount.text = mData.discountAmount?.asMoney()
                    } else {
                        bind.discountView.isVisible = false
                    }
//					cardAdapter.notifyDataSetChanged()
                }

                is Resource.Error -> {
                    viewModel.getPurchaseProductRepo.value = null
                    bind.loader.isVisible = false
                    bind.promoCodeLayout.suffixText = "Apply"
                    bind.promoCode.setText("")
                    couponList.forEach { it?.isSelected = false }

                    it.parse(mCtx, TAG, object : AlertClicks {
                        override fun primaryClick(dialog: AppBottomSheet) {
                            dialog.dismiss()
                        }

                        override fun secondaryClick(dialog: AppBottomSheet) {
                            dialog.dismiss()

                        }
                    })
                }

                else -> {}
            }
        }

        viewModel.createOrderRepo.observe(viewLifecycleOwner) {
            bind.loader.isVisible = false
            when (it) {
                is Resource.Success -> {
                    viewModel.createOrderRepo.value = null
                    val mData = it.value.data

                    startActivity(mCtx.toOrderStatus(mData?.id.toString()))
                    finish()
                }

                is Resource.Error -> {
                    viewModel.createOrderRepo.value = null

                    viewModel.createOrderRepo.value = null
                    bind.loader.isVisible = false

                    it.parse(mCtx, TAG, object : AlertClicks {
                        override fun primaryClick(dialog: AppBottomSheet) {
                            dialog.dismiss()

                        }

                        override fun secondaryClick(dialog: AppBottomSheet) {
                            dialog.dismiss()
                        }
                    })
                }

                else -> {}
            }
        }

        bind.loader.isVisible = true
        viewModel.getCoupon()
        viewModel.getCouponRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false

                    val mData = it.value.data

                    couponList.clear()
                    couponList.addAll(mData ?: mutableListOf())

                }

                is Resource.Error -> {
                    bind.loader.isVisible = false

                    it.parse(mCtx, TAG, object : AlertClicks {
                        override fun primaryClick(dialog: AppBottomSheet) {
                            dialog.dismiss()

                        }

                        override fun secondaryClick(dialog: AppBottomSheet) {
                            dialog.dismiss()

                        }
                    })
                }

                else -> {}

            }
        }

    }

    private fun showPaymentMethodSheet() {
        val paymentSheetBind =
            PaymentSheetBinding.bind(layoutInflater.inflate(R.layout.payment_sheet, null, false))
        val paymentSheet = Alerts.appBottomSheet(mCtx, true, paymentSheetBind)
        mutableListOf<String?>()

        paymentSheetBind.recycler.adapter =
            SelectPaymentCardAdapter(cardList, object : RecyclerClicks {

                override fun itemClick(pos: Int, status: String?) {

                    cardList.forEachIndexed { index, item ->

                        item?.selected = index == pos

                        cardId = item?.cardId.toString()

                        paymentSheetBind.recycler.adapter?.notifyDataSetChanged()

                        bind.cardNumber.text = buildString {
                            append("**** **** **** ")
                            append(item?.last4)
                        }
                        bind.cardNumber.setCompoundDrawablesWithIntrinsicBounds(
                            ContextCompat.getDrawable(
                                mCtx,
                                draw.ic_visa
                            ), null, null, null
                        )

                        cardList[pos]?.selected = true
                        paymentSheet.dismiss()
                    }

                }
            })

        paymentSheetBind.close.setHapticClickListener {
            paymentSheet.dismiss()
        }

        paymentSheet.show()

    }

    private fun showAddressSheet() {
        val addressSheetBind =
            AddressSheetBinding.bind(layoutInflater.inflate(R.layout.address_sheet, null, false))
        val addressSheet = Alerts.appBottomSheet(mCtx, true, addressSheetBind)

        addressSheetBind.recycler.adapter =
            SelectAddressAdapter(addressList, object : RecyclerClicks {

                override fun itemClick(pos: Int, status: String?) {

                    addressList.forEachIndexed { index, item ->

                        item?.selected = index == pos

                        addressSheetBind.recycler.adapter?.notifyDataSetChanged()

                        bind.address.text =
                            addressList.find { it?.isDefault == true }?.streetAddress
                                ?: addressList[pos]?.streetAddress


                        shippingId = addressList.find { it?.isDefault == true }?.id
                            ?: (addressList[pos]?.id ?: 0)

                        addressList[pos]?.selected = true

                        viewModel.getPurchaseProduct(
                            shippingId.toString().request(),
                            viewModel.product?.id.toString().request()
                        )

                        addressSheet.hide()

                    }

                }
            })

        addressSheetBind.close.setHapticClickListener {


            addressSheet.hide()

        }


        addressSheet.show()
    }

    private fun showCouponSheet(callback: (String?) -> Unit) {
        val couponSheetBinding =
            CouponSheetBinding.bind(layoutInflater.inflate(R.layout.coupon_sheet, null, false))
        val couponSheet = Alerts.appBottomSheet(mCtx, true, couponSheetBinding, true)
        log("COUPONS $couponList")

        if (couponList.isNotEmpty()) {
            couponSheetBinding.couponRecycler.isVisible = true
            couponSheetBinding.noData.isVisible = false

            couponSheetBinding.couponRecycler.adapter = CouponsAdapter(couponList, object : RecyclerClicks {
                override fun itemClick(pos: Int, status: String?) {
                    couponList.forEachIndexed { index, data ->
                        data?.isSelected = index == pos
                        couponSheetBinding.couponRecycler.adapter?.notifyDataSetChanged()
                    }
                    callback(couponList[pos]?.coupon?.name ?: "")
                    couponSheet.dismiss()
                }
            }, "buy_now")


            log("VISIBZLE ${couponSheetBinding.couponRecycler.isVisible}")

        } else {
            couponSheetBinding.couponRecycler.isVisible = false
            couponSheetBinding.noData.isVisible = true
        }

        couponSheetBinding.close.setHapticClickListener {
            couponSheet.dismiss()
        }

        couponSheet.show()

    }

}