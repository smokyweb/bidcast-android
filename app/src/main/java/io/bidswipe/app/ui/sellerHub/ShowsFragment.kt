package io.bidswipe.app.ui.sellerHub

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import com.google.android.material.tabs.TabLayout
import io.bidswipe.app.App
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.ShowListingAdapter
import io.bidswipe.app.databinding.FragmentShowsBinding
import io.bidswipe.app.databinding.PaymentAndAddressSheetBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.model.LiveShowModel
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetMyShowResponse
import io.bidswipe.app.network.response.toLiveShowProduct
import io.bidswipe.app.ui.agoraStream.AgoraPublisherActivity
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.more.MoreActivity
import io.bidswipe.app.ui.scheduleShow.ShowDetailsActivity
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.animatedNav
import io.bidswipe.app.utils.draw
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.setHapticClickListener
import io.bidswipe.app.utils.toScheduleShow
import io.bidswipe.app.utils.toSellerShow

@SuppressLint("NotifyDataSetChanged")
class ShowsFragment : BaseFragment<SellerHubViewModel, FragmentShowsBinding>() {
    override fun getModel(): Class<SellerHubViewModel> = SellerHubViewModel::class.java

    override fun getBind(
        inflater: LayoutInflater,
        view: ViewGroup?,
    ) = FragmentShowsBinding.inflate(inflater, view, false)

    private lateinit var showAdapter: ShowListingAdapter

    private var showList = mutableListOf<GetMyShowResponse.Data?>()

    private val mClicks = object : RecyclerClicks {
        override fun itemClick(pos: Int, status: String?) {
//            startActivity(Intent(mCtx, ShowDetailsActivity::class.java).putExtra("showId",showList[pos]?.id.toString()))

            if (status == "edit") {
                startActivity(mCtx.toScheduleShow(from = "dash", showId = showList[pos]?.id.toString()))
             } else {

                val profile = App.profileResponse.value

                if (profile?.sellerIdentityStatus != "verified") {
                    startActivity(Intent(mCtx, SellerVerificationActivity::class.java))
                    return
                }

                if (profile.hasCardAdded != true || profile.hasShippingAddress != true) {
                    showPaymentAndAddressSheet()
                    return
                }

                val data = showList[pos]

                val user = data?.user

                val products = data?.products?.map { product -> product?.toLiveShowProduct() }

                if (products?.isEmpty() == true) {
                    Alerts.error(mCtx, "No products found for this Show")
                    return
                } else {
                    products?.first()?.isCurrent = true
                }

                val showData = LiveShowModel(
                    seller = LiveShowModel.Seller(
                        id = user?.id.toString(),
                        image = user?.profileImage,
                        name = user?.name,
                        rating = user?.rating ?: ""
                    ),
                    products = emptyList<LiveShowModel.Product>(),
                    /* products = products?.map { p ->
                    LiveShowModel.Product(
                        data.category?.name,
                        p?.id,
                        p?.image,
                        p?.status,
                        p?.name,
                        p?.price,
                        "1",
                    )
                }?.toList() ?: mutableListOf(),*/
                    roomId = "live_room_${userId}_${data?.id.toString()}",
                    showDetail = data?.title ?: "",
                    thumbnail = data?.thumbnail?.getOrNull(0) ?: "",
                    viewerCount = "1",
                    highestBid = LiveShowModel.HighestBid(
                        bidAmount = "",
                        userName = "",
                        userImage = "",
                        userId = "",
                        productId = ""
                    ),
                    isLive = true,
                    time = Utils.timestamp().toString(),
                    showId = data?.id.toString(),
                    allowBidForAll = true,
                    bidCountDown = "",
                    showTimer = "",
                    categoryId = data?.category?.id.toString()
                )

                if (App.PIPMode) {
                    Alerts.error(mCtx, "You are already in Live show")
                } else if (bind.tabs.selectedTabPosition == 1) {

                    viewModel.selectedShow = showData
                    viewModel.showTime = showList[pos]?.time

                    findNavController().animatedNav(R.id.toShowDetails, bundleOf("showId" to showList[pos]?.id.toString()))
                } else {
                    startActivity(mCtx.toSellerShow(showList[pos]?.time, showData))
                }
            }
        }

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bind.header.onBackClick {
            finish()
        }

        showAdapter = ShowListingAdapter(showList, mClicks)

        bind.recycler.adapter = showAdapter

        bind.swipeRefreshLayout.setOnRefreshListener {
            bind.loader.isVisible = true
            val currentTab = bind.tabs.selectedTabPosition
            val requestType = when (currentTab) {
                0 -> "upcoming"
                1 -> "past"
                else -> "upcoming"
            }
            viewModel.getMyScheduledShow(requestType.request())
        }

        bind.noInternet.onClick {
            bind.loader.isVisible = true
            bind.noInternet.isVisible = false

            val currentTab = bind.tabs.selectedTabPosition
            val requestType = when (currentTab) {
                0 -> "upcoming"
                1 -> "past"
                else -> "upcoming"
            }
            viewModel.getMyScheduledShow(requestType.request())
        }

        bind.tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                showList.clear()
                showAdapter.notifyDataSetChanged()
                bind.loader.isVisible = true

                when (tab?.position) {
                    0 -> viewModel.getMyScheduledShow("upcoming".request())
                    1 -> viewModel.getMyScheduledShow("past".request())
                }

            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}

            override fun onTabReselected(tab: TabLayout.Tab?) {
                onTabSelected(tab)
            }

        })

        bind.addNewProduct.setHapticClickListener {
            startActivity(mCtx.toScheduleShow(from = "dash"))
        }

        bind.loader.isVisible = true

        viewModel.getMyScheduledShow("upcoming".request())
        viewModel.getMyScheduledShowRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    bind.noInternet.isVisible = false
                    bind.swipeRefreshLayout.isRefreshing = false
                    bind.loader.isVisible = false
                    bind.addNewProduct.isVisible = true

                    showList.clear()
                    it.value.data?.let { data ->
                        showList.addAll(data.distinctBy { show -> show?.id })
                    }
                    showAdapter.notifyDataSetChanged()

                    if (showList.isEmpty()) {
                        bind.noData.isVisible = true
                        bind.recycler.isVisible = false
                    } else {
                        bind.noData.isVisible = false
                        bind.recycler.isVisible = true
                    }

                }

                is Resource.Error -> {
                    bind.swipeRefreshLayout.isRefreshing = false
                    bind.noInternet.isVisible = false
                    bind.loader.isVisible = false

                    if (it.isNetworkError) {
                        bind.noInternet.isVisible = true
                        bind.recycler.isVisible = false
                        bind.addNewProduct.isVisible = false
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

    fun showPaymentAndAddressSheet() {

        val paymentAddressBind = PaymentAndAddressSheetBinding.bind(
            layoutInflater.inflate(
                R.layout.payment_and_address_sheet,
                null,
                false
            )
        )

        val makeOfferSheet = Alerts.appBottomSheet(mCtx, true, paymentAddressBind)

        with(paymentAddressBind.addressItem) {
            val hasAddress = App.profileResponse.value?.hasShippingAddress == true
            moreIcon.setImageDrawable(ContextCompat.getDrawable(mCtx, draw.ic_pencil))
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
                    Intent(mCtx, MoreActivity::class.java).putExtra(
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
            moreIcon.setImageDrawable(ContextCompat.getDrawable(mCtx, draw.ic_pencil))
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
                    Intent(mCtx, MoreActivity::class.java).putExtra(
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