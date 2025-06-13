package io.bidswipe.app.ui.dashboard.product

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.databinding.FragmentBuyNowBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetPaymentCardsResponse
import io.bidswipe.app.network.response.GetPurchaseDetail
import io.bidswipe.app.network.response.GetShippingAddressResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.asMoney
import io.bidswipe.app.utils.draw
import io.bidswipe.app.utils.ids
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.value

class BuyNowFragment : BaseFragment<ProductViewModel, FragmentBuyNowBinding>() {
    override fun getModel(): Class<ProductViewModel> = ProductViewModel::class.java

    override fun getBind(
        inflater: LayoutInflater,
        view: ViewGroup?
    ) = FragmentBuyNowBinding.inflate(inflater, view, false)

    private var checkOutData: GetPurchaseDetail.Data? = null

    private var cardList = mutableListOf<GetPaymentCardsResponse.Data?>()
    private var addressList = mutableListOf<GetShippingAddressResponse.Data?>()
    private var shippingId = 0


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bind.header.onBackClick {
            findNavController().popBackStack()
        }

        bind.loader.isVisible = true

        viewModel.getPaymentCard()

        viewModel.getShippingAddress()

        bind.productName.text = viewModel.product?.title

        bind.productDescription.text = viewModel.product?.description

        bind.productImg.loadUrl(mCtx, viewModel.product?.images?.get(0).toString())

        bind.confirmButton.setOnClickListener {

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
                                "cardId" to cardList[0]?.cardId?.toString(),
                                "promoCode" to bind.promoCode.value()
                            )
                        )
                    } else {
                        bind.loader.isVisible = true

                        viewModel.createOrder(
                            shippingId.toString().request(),
                            viewModel.product?.id.toString().request(),
                            cardList[0]?.cardId?.request(),
                            bind.promoCode.value().ifEmpty { null }?.request(),
                            "0".request(),
                            null,
                            null,
                            checkOutData?.shippingCharges.toString().request(),
                            checkOutData?.taxAmount.toString().request(),
                            checkOutData?.subTotal.toString().request(),
                            checkOutData?.total.toString().request()

                        )

                    }
                }

            }


        }

        viewModel.getShippingAddressRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false

                    val mData = it.value.data

                    addressList.clear()

                    if (mData?.isNotEmpty() == true) {
                        addressList.addAll(mData)

                        bind.address.text =
                            addressList.find { it?.isDefault == true }?.streetAddress
                                ?: addressList[0]?.streetAddress


                        shippingId = addressList.find { it?.isDefault == true }?.id
                            ?: (addressList[0]?.id?.toInt()
                                ?: 0)

                        viewModel.getPurchaseProduct(
                            shippingId.toString().request(),
                            viewModel.product?.id.toString().request()
                        )
                    }

                    if (addressList.isEmpty()) {
                        bind.address.text = "Address not Found"
                        bind.changeAddress.text = "Add Address"
                    }

//					shippingAddressAdapter.notifyDataSetChanged()

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

                        bind.cardNumber.text = "No Cards Found"

                        bind.changePayment.text = "Add Card"

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
                    }
//					cardAdapter.notifyDataSetChanged()
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

        viewModel.getPurchaseProductRepo.observe(viewLifecycleOwner) {

            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false

                    val mData = it.value.data

                    checkOutData = mData

                    bind.subTotal.text = mData?.subTotal.toString().asMoney()
                    bind.tax.text = mData?.taxAmount.toString().asMoney()
                    bind.shipping.text = mData?.shippingCharges.toString().asMoney()
                    bind.total.text = mData?.total.toString().asMoney()


//					cardAdapter.notifyDataSetChanged()
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

        viewModel.createOrderRepo.observe(viewLifecycleOwner) {

            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false

                    val mData = it.value.data

                    findNavController().navigate(ids.buyNowToOrderStatusFragment)

//					cardAdapter.notifyDataSetChanged()
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

}