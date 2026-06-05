package io.bidswipe.app.ui.product

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.bidswipe.app.base.BaseActivity
import io.bidswipe.app.controller.ShippingUpdateAdapter
import io.bidswipe.app.databinding.ActivityOrderStatusBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetOrderDetailsResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.asMoney
import io.bidswipe.app.utils.bind
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.setHapticClickListener
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import kotlin.concurrent.thread

class OrderStatusActivity : BaseActivity() {

    private val bind by bind(ActivityOrderStatusBinding::inflate)
    private val viewModel by viewModels<ProductViewModel>()
    private val statusItems = mutableListOf<GetOrderDetailsResponse.Data.ShippingTracking?>()

    private lateinit var adapter: ShippingUpdateAdapter
    private var orderId = ""
    private var shippingAddress: String? = null

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

        bind.shippingDetailsBtn.setHapticClickListener {
            val msg = shippingAddress?.takeIf { it.isNotBlank() } ?: "No shipping address on file."
            MaterialAlertDialogBuilder(this)
                .setTitle("Shipping Details")
                .setMessage(msg)
                .setPositiveButton("OK") { d, _ -> d.dismiss() }
                .show()
        }

        bind.loader.isVisible = true
        viewModel.getOrderDetails(orderId.request())
        viewModel.getOrderDetailsRepo.observe(this) {
            when (it) {
                is Resource.Success -> {
                    viewModel.getOrderDetailsRepo.value = null
                    bind.loader.isVisible = false

                    val mData = it.value.data

                    shippingAddress = mData?.shippingAddress
                    bind.productName.text = mData?.product?.title?.asCapital()
                    bind.address.text = shippingAddress ?: "N/A"
                    bind.productImage.loadUrl(this, mData?.product?.images?.get(0).toString())
                    bind.productColor.text = mData?.product?.category?.name
                    bind.category.text = mData?.product?.category?.name
                    bind.orderId.text = mData?.orderId.toString()
                    bind.orderDate.text = Utils.getFormattedDateTime(
                        "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",
                        "MMM dd, yyyy, HH:mm",
                        mData?.createdAt.toString()
                    )

                    val summary = mData?.summary
                    val productPrice = summary?.productPrice ?: mData?.product?.pricing ?: 0.0
                    val shippingCost = summary?.shippingCharge ?: 0.0
                    val taxAmount = summary?.taxAmount ?: 0.0
                    val total = summary?.total ?: (productPrice + shippingCost + taxAmount)

                    bind.itemPrice.text = productPrice.toString().asMoney()
                    bind.shippingCost.text = shippingCost.toString().asMoney()
                    bind.taxAmount.text = taxAmount.toString().asMoney()
                    bind.receiptTotal.text = total.toString().asMoney()

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

                    val url = it.value.data?.toString()
                    if (url.isNullOrBlank() || url == "null") {
                        Toast.makeText(this, "Receipt not available", Toast.LENGTH_SHORT).show()
                        return@observe
                    }
                    openPdfInApp(this, url)

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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        val nextOrderId = intent.getStringExtra("orderId") ?: ""
        if (nextOrderId.isBlank()) return

        orderId = nextOrderId
        bind.homeBtn.isVisible = (intent.getStringExtra("from") ?: "") != "order_details"
        statusItems.clear()
        adapter.notifyDataSetChanged()
        bind.loader.isVisible = true
        viewModel.getOrderDetails(orderId.request())
    }

    /** Download PDF to cache and open it in-app using FileProvider + Intent.ACTION_VIEW.
     *  #8: Receipt now opens inside the device PDF viewer rather than being pushed to
     *  the Downloads folder via DownloadManager.  Zero new library dependencies.
     *  Uses cache/receipts/ (declared in file_paths.xml) so FileProvider can serve it.
     */
    private fun openPdfInApp(context: Context, url: String) {
        Toast.makeText(context, "Opening receipt…", Toast.LENGTH_SHORT).show()
        thread {
            try {
                val client = OkHttpClient()
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
                val bytes = response.body?.bytes() ?: throw IOException("Empty response")
                val receiptsDir = File(context.cacheDir, "receipts").also { it.mkdirs() }
                val file = File(receiptsDir, "receipt_${System.currentTimeMillis()}.pdf")
                file.writeBytes(bytes)
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    file
                )
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/pdf")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                runOnUiThread {
                    try {
                        startActivity(intent)
                    } catch (e: Exception) {
                        Log.e(TAG, "No PDF viewer found, falling back to browser", e)
                        // Fallback: open original URL in browser
                        try {
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        } catch (ex: Exception) {
                            Toast.makeText(context, "Cannot open receipt", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Receipt download failed", e)
                runOnUiThread {
                    Toast.makeText(context, "Could not load receipt: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

}
