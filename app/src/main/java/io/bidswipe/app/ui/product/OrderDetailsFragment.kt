package io.bidswipe.app.ui.product

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context.CLIPBOARD_SERVICE
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.PopupMenu
import androidx.core.os.bundleOf
import androidx.core.text.buildSpannedString
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialog
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.databinding.FragmentOrderDetailsBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.dashboard.ChatActivity
import io.bidswipe.app.ui.more.MoreActivity
import io.bidswipe.app.ui.sellerProfile.SellerProfileActivity
import io.bidswipe.app.ui.tutorials.TutorialsActivity
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.asMoney
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.ids
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.setHapticClickListener
import io.bidswipe.app.utils.toOrderStatus
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import io.bidswipe.app.utils.Alerts

class OrderDetailsFragment : BaseFragment<ProductViewModel, FragmentOrderDetailsBinding>() {
    override fun getModel(): Class<ProductViewModel> = ProductViewModel::class.java

    override fun getBind(
        inflater: LayoutInflater,
        view: ViewGroup?
    ): FragmentOrderDetailsBinding = FragmentOrderDetailsBinding.inflate(inflater, view, false)

    private var orderId: String? = null
    private var primaryOrderId: String? = null
    // Basecamp #9934033253 (2026-05-27): track current order status so the
    // cancel menu only acts when the order is in a cancellable state.
    private var currentOrderStatus: String? = null
    private var order: String? = null
    private var sellerId: String? = null
    private var productId: String? = null
    private var sellerName: String? = null
    private var sellerImage: String? = null
    private var videoUrl: String? = null
    private var videoPlayerBottomSheet: BottomSheetDialog? = null
    private var exoPlayer: ExoPlayer? = null
    private var type="product"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        orderId = requireActivity().intent.getStringExtra("orderId")
        productId = requireActivity().intent.getStringExtra("productId")
        type = requireActivity().intent.getStringExtra("productType")?:"product"

        bind.header.onBackClick {
            finish()
        }

        bind.messageToSeller.setOnClickListener {
            val intent = Intent(mCtx, ChatActivity::class.java).apply {
                putExtra("id", sellerId)
                putExtra("name", sellerName)
                putExtra("image", sellerImage)
            }
            startActivity(intent)
        }

        bind.getHelp.setHapticClickListener {
            startActivity(Intent(mCtx, MoreActivity::class.java).putExtra("slug", "contactUs"))
        }

        bind.refer.setHapticClickListener {
            startActivity(Intent(mCtx, TutorialsActivity::class.java).putExtra("type", "refer"))
        }

        bind.orderId.setHapticClickListener {
            val clipboard = context?.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("label", bind.orderId.text)
            clipboard.setPrimaryClip(clip)
            android.widget.Toast.makeText(mCtx, "Copied!", android.widget.Toast.LENGTH_SHORT).show()
        }

        bind.userProfile.setHapticClickListener {
            startActivity(
                Intent(mCtx, SellerProfileActivity::class.java).putExtra(
                    "sellerId",
                    sellerId
                )
            )
        }

        bind.shippingDetail.setHapticClickListener {
//            findNavController().navigate(
//                ids.orderDetailToOrderStatusFragment,
//                bundleOf("orderId" to order)
//            )
            startActivity(mCtx.toOrderStatus( order,"order_details"))
        }

        bind.viewProduct.setHapticClickListener {
            bind.expandView.toggle()
        }

        bind.videoReceipt.setHapticClickListener {
            findNavController().navigate(ids.orderDetailToVideoReceiptPlayerFragment, bundleOf("videoUrl" to videoUrl))
        }

        val menu = PopupMenu(mCtx, bind.header.findViewById<AppCompatImageView>(R.id.primaryIcon))

        menu.menuInflater.inflate(R.menu.order_menu, menu.menu)

        menu.setOnMenuItemClickListener {
            when (it.itemId) {
                ids.cancel -> {
                    // Basecamp #9934033253 (2026-05-27): buyer cancel-order.
                    handleCancelOrderTap()
                }

                ids.raiseTicket -> {

                    findNavController().navigate(ids.orderDetailToRaiseTicketFragment, bundleOf("orderId" to primaryOrderId))

                }

            }
            return@setOnMenuItemClickListener true
        }

        bind.header.onMorePrimaryClick {
            menu.show()
        }

        bind.loader.isVisible = true
        viewModel.fetchOrderDetail(productId, orderId,type)

        viewModel.fetchOrderDetailRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false

                    val mData = it.value.data

                    bind.header.setHeaderText(mData?.order?.product?.title?.asCapital() ?: "Order Detail")

                    bind.shippingAddress.text = mData?.shippingAddress?.let { addr ->
                        io.bidswipe.app.utils.Utils.formatAddress(
                            name = addr.name,
                            streetAddress = addr.streetAddress,
                            addressLine2 = addr.addressLine2,
                            city = addr.city,
                            state = addr.state,
                            pincode = addr.pincode,
                        ).ifBlank { "N/A" }
                    } ?: "N/A"

                    bind.productImage.loadUrl(mCtx, mData?.order?.product?.images?.get(0) ?: "")
                    bind.productName.text = mData?.order?.product?.title
                    bind.productDescription.text = mData?.order?.product?.description

                    // orderStatusPercentage may come back as Int or Double from old API records
                    val statusPct = (mData?.order?.orderStatusPercentage as? Number)?.toInt() ?: 0
                    bind.orderProgress.setProgress(statusPct, true)

                    bind.orderId.text = mData?.order?.orderId.toString()

                    bind.orderTime.text = buildSpannedString {
                        append("Order placed ")
                        append(
                            Utils.getFormattedDateTime(
                                Const.SERVER_TIME_FORMAT,
                                "MMM dd, yyyy 'at' hh:mm a",
                                mData?.order?.createdAt.toString()
                            )
                        )
                    }

                    bind.orderDate.text = Utils.getFormattedDateTime(
                        Const.SERVER_TIME_FORMAT,
                        "MMM dd, yyyy",
                        mData?.order?.createdAt.toString()
                    )

                    bind.soldBy.text = mData?.sellerDetails?.name
                    bind.quantity.text = mData?.order?.product?.quantity.toString()
                    bind.category.text = mData?.order?.product?.category?.name

                    bind.productCategory.text = mData?.order?.product?.category?.name
                    bind.price.text = mData?.order?.product?.pricing.toString().asMoney()
                    
                    // QA cmpcqb2cm008fg3hg0zn8o1nh — cost breakdown below Category on the order detail.
                    // Same fallback math as the PWA orders.blade.php Wave 1 #4 fix:
                    //   shipping = 9, tax = 7% of item, total = item + tax + shipping.
                    // The backend GetOrderDetailsResponse doesn't expose a transaction subtree on Android
                    // yet, so we use the same computed values the PWA uses when transaction is empty.
                    val itemCost = mData?.order?.product?.pricing?.toDoubleOrNull() ?: 0.0
                    val shippingCost = 9.0
                    val taxAmount = (itemCost * 0.07 * 100).toInt() / 100.0
                    val totalCost = itemCost + shippingCost + taxAmount
                    bind.itemSubtotal.text = "$" + "%.2f".format(itemCost)
                    bind.itemShipping.text = "$" + "%.2f".format(shippingCost)
                    bind.itemTax.text = "$" + "%.2f".format(taxAmount)
                    bind.itemTotal.text = "$" + "%.2f".format(totalCost)
                    bind.costBreakdownSection.visibility = View.VISIBLE

                    order = mData?.order?.id.toString()

                    sellerId = mData?.sellerDetails?.id.toString()
                    sellerName = mData?.sellerDetails?.name.toString()
                    sellerImage = mData?.sellerDetails?.profileImage.toString()
                    bind.userName.text = mData?.sellerDetails?.name
                    bind.userImage.loadUrl(mCtx, mData?.sellerDetails?.profileImage ?: "")

                    bind.rating.text = mData?.ratingAvg ?: "0"

                    bind.review.text = mData?.review ?: "0"

                    bind.sold.text = (mData?.soldCount ?: 0).toString()

                    bind.shipping.text = mData?.avgShip ?: "0"

                    // Store video URL and show/hide video receipt button
                    videoUrl = mData?.bidVideoUrl

                    bind.videoReceipt.isVisible = !videoUrl.isNullOrEmpty()

                    bind.videoReceiptDivider.isVisible = !videoUrl.isNullOrEmpty()

                    primaryOrderId = mData?.order?.id.toString()
                    currentOrderStatus = mData?.order?.status

                    // Cost breakdown: sub_total, shipping_charges, tax_amount, total
                    val subTotal = (mData?.order?.subTotal as? Number)?.toDouble()
                    val shipping = (mData?.order?.shippingCharges as? Number)?.toDouble()
                    val tax = (mData?.order?.taxAmount as? Number)?.toDouble()
                    val total = (mData?.order?.total as? Number)?.toDouble()
                    val hasBreakdown = subTotal != null || shipping != null || tax != null || total != null
                    bind.costBreakdownSection.isVisible = hasBreakdown
                    if (hasBreakdown) {
                        bind.itemSubtotal.text = "$${String.format("%.2f", subTotal ?: 0.0)}"
                        bind.itemShipping.text = "$${String.format("%.2f", shipping ?: 0.0)}"
                        bind.itemTax.text = "$${String.format("%.2f", tax ?: 0.0)}"
                        bind.itemTotal.text = "$${String.format("%.2f", total ?: 0.0)}"
                    }

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
            }

        }

    }

    // Basecamp #9934033253 (2026-05-27): buyer cancel-order. Allowed only when
    // order.status ∈ {pending, processing}. Confirms with the user, POSTs to
    // /api/product/cancel-order, refreshes the screen on success.
    private fun handleCancelOrderTap() {
        val orderId = primaryOrderId?.toIntOrNull()
        if (orderId == null || orderId <= 0) {
            Alerts.error(mCtx, "Order not ready yet, please try again.")
            return
        }
        val statusLower = (currentOrderStatus ?: "").lowercase()
        if (statusLower != "pending" && statusLower != "processing") {
            Alerts.error(mCtx, "This order can no longer be cancelled (already shipped or delivered).")
            return
        }
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Cancel this order?")
            .setMessage("You can't undo this. The seller will be notified.")
            .setNegativeButton("Keep order") { d, _ -> d.dismiss() }
            .setPositiveButton("Cancel order") { d, _ ->
                d.dismiss()
                postCancelOrder(orderId)
            }
            .show()
    }

    private fun postCancelOrder(orderId: Int) {
        bind.loader.isVisible = true
        viewLifecycleOwner.lifecycleScope.launch {
            val code: Int = withContext(Dispatchers.IO) {
                try {
                    val token = io.bidswipe.app.utils.Prefs(mCtx).token()
                    val body = org.json.JSONObject().apply { put("order_id", orderId) }.toString()
                    val url = java.net.URL("${io.bidswipe.app.utils.Const.BASE_URL}/api/product/cancel-order")
                    val conn = url.openConnection() as java.net.HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.setRequestProperty("Accept", "application/json")
                    conn.setRequestProperty("Authorization", "Bearer $token")
                    conn.doOutput = true
                    conn.outputStream.use { it.write(body.toByteArray()) }
                    val rc = conn.responseCode
                    conn.disconnect()
                    rc
                } catch (e: Exception) {
                    e.printStackTrace()
                    -1
                }
            }
            bind.loader.isVisible = false
            when (code) {
                200, 201 -> {
                    Alerts.success(mCtx, "Order cancelled.")
                    viewModel.fetchOrderDetail(productId, orderId.toString(), type)
                }
                403 -> Alerts.error(mCtx, "You are not authorized to cancel this order.")
                404 -> Alerts.error(mCtx, "Order not found.")
                409 -> Alerts.error(mCtx, "Order can no longer be cancelled.")
                -1 -> Alerts.error(mCtx, "Network error.")
                else -> Alerts.error(mCtx, "Could not cancel order (code $code).")
            }
        }
    }

    private fun releasePlayer() {
        exoPlayer?.release()
        exoPlayer = null
        videoPlayerBottomSheet = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        releasePlayer()
    }

}