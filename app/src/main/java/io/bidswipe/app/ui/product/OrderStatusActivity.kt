package io.bidswipe.app.ui.product

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import androidx.activity.viewModels
import androidx.core.view.isVisible
import io.bidswipe.app.base.BaseActivity
import io.bidswipe.app.controller.ShippingUpdateAdapter
import io.bidswipe.app.databinding.ActivityOrderStatusBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetOrderDetailsResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.bind
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.setHapticClickListener

class OrderStatusActivity : BaseActivity() {

    private val bind by bind(ActivityOrderStatusBinding::inflate)
    private val viewModel by viewModels<ProductViewModel>()
    private val statusItems = mutableListOf<GetOrderDetailsResponse.Data.ShippingTracking?>()

    private lateinit var adapter: ShippingUpdateAdapter
    private var orderId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(bind.root)

        orderId = intent?.getStringExtra("orderId") ?: ""
        val from = intent?.getStringExtra("from") ?: ""

        bind.homeBtn.isVisible = from != "order_details"

        bind.header.onBackClick {
            finish()
        }

        adapter = ShippingUpdateAdapter(statusItems)
        bind.shippingRecycler.adapter = adapter

        bind.homeBtn.setHapticClickListener {
            finish()
        }

        bind.receipt.setHapticClickListener {
            bind.loader.isVisible = true
            viewModel.getOrderReceipt(orderId = orderId.request())
        }

        bind.loader.isVisible = true
        viewModel.getOrderDetails(orderId.request())
        viewModel.getOrderDetailsRepo.observe(this) {
            when (it) {
                is Resource.Success -> {
                    viewModel.getOrderDetailsRepo.value = null
                    bind.loader.isVisible = false

                    val mData = it.value.data

                    bind.productName.text = mData?.product?.title?.asCapital()
                    bind.address.text = mData?.shippingAddress ?: "N/A"
                    bind.productImage.loadUrl(this, mData?.product?.images?.get(0).toString())
                    bind.productColor.text = mData?.product?.category?.name
                    bind.category.text = mData?.product?.category?.name
                    bind.orderId.text = mData?.orderId.toString()
                    bind.orderDate.text = Utils.getFormattedDateTime(
                        "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",
                        "MMM dd, yyyy, HH:mm",
                        mData?.createdAt.toString()
                    )

                    if (mData?.shippingTracking?.isNotEmpty() == true) {
                        statusItems.addAll(mData.shippingTracking)
                    }

                    adapter.notifyDataSetChanged()

                }

                is Resource.Error -> {
                    viewModel.getOrderDetailsRepo.value = null
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

        viewModel.getOrderReceiptRepo.observe(this) {
            when (it) {
                is Resource.Success -> {
                    viewModel.getOrderReceiptRepo.value = null
                    bind.loader.isVisible = false

                    val mData = it.value.data

                    downloadPdf(this, mData.toString(), System.currentTimeMillis().toString())

                }

                is Resource.Error -> {
                    viewModel.getOrderReceiptRepo.value = null
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

    fun downloadPdf(context: Context, url: String, fileName: String) {
        val request = DownloadManager.Request(Uri.parse(url)).apply {
            setTitle(fileName)
            setDescription("Downloading PDF...")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

            // For Android 10 and above, this is app-specific external directory
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "$fileName.pdf")

            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
        }

        val downloadManager = context.getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)
    }

}