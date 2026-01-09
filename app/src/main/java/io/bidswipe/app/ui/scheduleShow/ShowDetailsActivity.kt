package io.bidswipe.app.ui.scheduleShow

import android.content.Intent
import android.icu.util.TimeZone
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.CONSUMED
import androidx.core.view.isVisible
import io.bidswipe.app.App
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseActivity
import io.bidswipe.app.controller.InventoryAdapter
import io.bidswipe.app.databinding.ActivityShowDetailsBinding
import io.bidswipe.app.databinding.PaymentAndAddressSheetBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.LiveShowModel
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetShowDetailsResponse
import io.bidswipe.app.network.response.toLiveShowProduct
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.more.MoreActivity
import io.bidswipe.app.ui.product.ProductDetailsActivity
import io.bidswipe.app.ui.sellerHub.SellerVerificationActivity
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.asMoney
import io.bidswipe.app.utils.bind
import io.bidswipe.app.utils.dpToPx
import io.bidswipe.app.utils.draw
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.setHapticClickListener
import io.bidswipe.app.utils.toScheduleShow
import io.bidswipe.app.utils.toSellerShow

class ShowDetailsActivity : BaseActivity() {

    private val bind by bind(ActivityShowDetailsBinding::inflate)
    private val viewModel by viewModels<ScheduleShowViewModel>()

    private var showData: GetShowDetailsResponse.Data?? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(bind.root)

        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { v, insets ->
            val system = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            bind.header.setHeaderPadding(resources.dpToPx(8), system.top, resources.dpToPx(8), resources.dpToPx(8))
            bind.root.setPadding(0, 0, 0, system.bottom)
            CONSUMED
        }

        viewModel.showId = intent?.getStringExtra("showId")
        if (!viewModel.showId.isNullOrEmpty()) {
            bind.loader.isVisible=true
            viewModel.getShowDetails(viewModel.showId.toString())
        }else{
            errorToast("Something went wrong")
            finishAfterTransition()
        }

        bind.header.onBackClick {
            finishAfterTransition()
        }

        bind.editShow.setHapticClickListener {
            startActivity(toScheduleShow(from = "dash", showId = viewModel.showId))
        }

        bind.startShow.setHapticClickListener {
            val profile = App.profileResponse.value

            if (profile?.sellerIdentityStatus != "verified") {
                startActivity(Intent(this, SellerVerificationActivity::class.java))
	            return@setHapticClickListener
            }

            if (profile.hasCardAdded != true || profile.hasShippingAddress != true) {
                showPaymentAndAddressSheet()
	            return@setHapticClickListener
            }

            val user = showData?.user
            val products = showData?.products?.map { product -> product?.toLiveShowProduct() }

            if (products?.isEmpty() == true) {
                errorToast("No products found for this Show")
	            return@setHapticClickListener
            } else {
                products?.first()?.isCurrent = true
            }

	        val timee= Utils.getTimeStampFromServerTime( showData?.date?.replace("00:00:00",showData?.time?:"00:00:00")?:"", timeZone = TimeZone.getDefault().id).toString()

            val showData = LiveShowModel(
                seller = LiveShowModel.Seller(
                    id = user?.id.toString(),
                    image = user?.profileImage ?: "",
                    name = user?.name,
                    rating = user?.rating ?: ""
                ),
                products = emptyList<LiveShowModel.Product>(),
                roomId = "live_room_${userId}_${showData?.id.toString()}",
                showDetail = showData?.title ?: "",
                thumbnail = showData?.thumbnail?.getOrNull(0) ?: "",
                viewerCount = "1",
                highestBid = LiveShowModel.HighestBid(
                    bidAmount = "",
                    userName = "",
                    userImage = "",
                    userId = "",
                    productId = ""
                ),
                isLive = true,
                time =timee,
                showId = showData?.id.toString(),
                allowBidForAll = true,
                bidCountDown = "",
                showTimer = "",
                categoryId = showData?.category?.id.toString()
            )

            if (App.PIPMode) {
                errorToast("You are already in Live show")
            } else {
                startActivity(toSellerShow(showData.time, showData))
            }
        }

        viewModel.getShowDetailsRepo.observe(this) {
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false

                    showData = it.value.data

                    bind.showTitle.text = showData?.title ?: "Show Details"

                    bind.repeat.text = showData?.repeatValue?.asCapital() ?: "N/A"
                    bind.auctionType.text = showData?.auction?.name?:"N/A"
                    bind.discoverability.text = (showData?.showDiscoverability ?: "").asCapital()

                    bind.explicitContent.text = if (showData?.isExplicit ?: false) "Yes" else "No"
                    bind.language.text = (showData?.language ?: "").asCapital()

                    if (showData?.category != null) {
                        bind.category.text = showData?.category?.name?.asCapital()
                    } else {
                        bind.category.isVisible = false
                    }

                    bind.time.text = buildString {
                        append(
                            Utils.getFormattedDateTime(
                                "yyyy-mm-dd",
                                "mm-dd-yyyy",
                                showData?.date.toString()
                            )
                        )
                        append(" ")
                        append(Const.BULLET)
                        append(" ")
                        append(
                            Utils.getFormattedDateTime(
                                "HH:mm:ss",
                                "hh:mm a",
                                showData?.time.toString()
                            )
                        )
                    }

                    bind.sales.text = buildString {
                        append((showData?.totalSalesAmount ?: 0).toString().asMoney())
                        append(" sales ")
                        append(Const.BULLET + " ")
                        append(showData?.totalOrders ?: 0)
                        append(" orders")
                    }

                    bind.image.loadUrl(this, showData?.thumbnail?.first() ?: "")
                    if (showData?.products?.isNotEmpty() == true) {
                        bind.addedProducts.isVisible = true
                        bind.recycler.isVisible = true

                        val products = showData?.products?.toMutableList() ?: mutableListOf()

                        val productAdapter = InventoryAdapter(products, false, object : RecyclerClicks {
                            override fun itemClick(pos: Int, status: String?) {
                                startActivity(
                                    Intent(this@ShowDetailsActivity, ProductDetailsActivity::class.java).putExtra(
                                        "productId", products[pos]?.id.toString()
                                    )
                                )
                            }

                        }, "show_details")

                        bind.recycler.adapter = productAdapter
                        bind.exSpace.text = buildString {
                            append(products.size)
                            append("/100")
                        }

                    } else {
                        bind.addedProducts.isVisible = false
                        bind.recycler.isVisible = false
                    }


                }

                is Resource.Error -> {
                    bind.loader.isVisible = false
                    it.parse(this, TAG, object : AlertClicks {
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

    fun showPaymentAndAddressSheet() {

        val paymentAddressBind = PaymentAndAddressSheetBinding.bind(
            layoutInflater.inflate(
                R.layout.payment_and_address_sheet,
                null,
                false
            )
        )

        val makeOfferSheet = Alerts.appBottomSheet(this, true, paymentAddressBind)

        with(paymentAddressBind.addressItem) {
            val hasAddress = App.profileResponse.value?.hasShippingAddress == true
            moreIcon.setImageDrawable(ContextCompat.getDrawable(this@ShowDetailsActivity, draw.ic_pencil))
            moreIcon.rotation = 0f

            name.isVisible = hasAddress
            address.isVisible = hasAddress

            if (hasAddress) {
                val addressData = App.profileResponse.value?.defaultShippingAddress
                address.text = addressData?.streetAddress
                name.text = addressData?.name
                type.text = addressData?.type
                defaultAddress.isVisible = addressData?.isDefault == true
            } else {
                type.text = "Address Not Added"
                defaultAddress.isVisible = false
            }
            moreIcon.setHapticClickListener {
                startActivity(
                    Intent(this@ShowDetailsActivity, MoreActivity::class.java).putExtra(
                        "slug",
                        "paymentShipping"
                    )
                )
            }
        }

        with(paymentAddressBind.paymentCard) {
            val hasCard = App.profileResponse.value?.hasCardAdded == true
            iconCard.isVisible = hasCard
            expiryDate.isVisible = hasCard
            moreIcon.setImageDrawable(ContextCompat.getDrawable(this@ShowDetailsActivity, draw.ic_pencil))
            moreIcon.rotation = 0f

            if (hasCard) {
                cardNumber.text = buildString {
                    append("•••• •••• •••• ")
                    append(App.profileResponse.value?.defaultCard?.last4)
                }

                expiryDate.text = buildString {
                    append(App.profileResponse.value?.defaultCard?.expDate)
                }
            } else {
                cardNumber.text = "Cards Not Added"
            }
            moreIcon.setHapticClickListener {
                startActivity(
                    Intent(this@ShowDetailsActivity, MoreActivity::class.java).putExtra(
                        "slug",
                        "paymentShipping"
                    )
                )
            }
        }

        paymentAddressBind.close.setHapticClickListener {
            makeOfferSheet.dismiss()
        }

        makeOfferSheet.show()

    }

}