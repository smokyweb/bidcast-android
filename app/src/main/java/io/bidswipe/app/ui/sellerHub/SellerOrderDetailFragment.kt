package io.bidswipe.app.ui.sellerHub

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.ShippingUpdateAdapter
import io.bidswipe.app.databinding.FragmentSellerOrderDetailBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetOrderDetailsResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.dashboard.ChatActivity
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.setHapticClickListener

class SellerOrderDetailFragment : BaseFragment<SellerHubViewModel, FragmentSellerOrderDetailBinding>() {

    override fun getModel(): Class<SellerHubViewModel> = SellerHubViewModel::class.java

    override fun getBind(inflater: LayoutInflater, view: ViewGroup?): FragmentSellerOrderDetailBinding =
        FragmentSellerOrderDetailBinding.inflate(inflater, view, false)

    private var statusList = mutableListOf<String?>("Pending", "Processing", "Out for delivery", "Delivered")

    private val statusItems = mutableListOf<GetOrderDetailsResponse.Data.ShippingTracking?>()

    private lateinit var adapter: ShippingUpdateAdapter

    private var selectedStatus = ""

    private var orderId = ""
    private var orderDbId = ""   // numeric DB id (for USPS label endpoint)
    private var buyerId = ""
    private var buyerName = ""
    private var buyerImage = ""
    private var currentTrackingNumber: String? = null
    private var currentLabelUrl: String? = null
    // #9934033253: current cancellation_status so we only show approve/reject when needed.
    private var currentCancellationStatus: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        orderId = arguments?.getString("orderId") ?: ""
        arguments?.getString("from") ?: ""

        bind.header.onBackClick {
            findNavController().popBackStack()
        }

        val arrayAdapter = ArrayAdapter(
            mCtx,
            android.R.layout.simple_list_item_1,
            statusList
        )

        bind.status.setAdapter(arrayAdapter)

        val drawable = ContextCompat.getDrawable(mCtx, R.drawable.card_8)
        bind.status.setDropDownBackgroundDrawable(drawable)

        bind.status.setOnItemClickListener { _, _, position, _ ->
            selectedStatus = statusList[position]?.replace(" ", "_")?.lowercase() ?: ""
            bind.status.setText(statusList[position], false)
        }

        bind.status.setHapticClickListener {
            bind.status.showDropDown()
        }

        bind.header.onBackClick {
            findNavController().popBackStack()
        }

        bind.updateStatus.setHapticClickListener {

            if (selectedStatus.isEmpty()) {
                errorToast("Please select status")
                return@setHapticClickListener
            }
            bind.loader.isVisible = true
            viewModel.changeOrderStatus(orderId.request(), selectedStatus.request())
        }

        // #32 Wave 4: Mark as Shipped with tracking number
        bind.btnMarkShipped.setHapticClickListener {
            val tracking = bind.trackingNumberInput.text?.toString()?.trim() ?: ""
            if (tracking.isEmpty()) {
                errorToast("Please enter a USPS tracking number")
                return@setHapticClickListener
            }
            bind.loader.isVisible = true
            viewModel.changeOrderStatusWithTracking(
                orderId.request(),
                "out_for_delivery".request(),
                tracking.request()
            )
        }

        // #33 Wave 4: Get Shipping Label
        bind.btnGetShippingLabel.setHapticClickListener {
            bind.loader.isVisible = true
            viewModel.createShippingLabel(orderDbId.request())
        }

        // #32 Wave 4: Track package on USPS
        bind.btnTrackPackage.setHapticClickListener {
            val tn = currentTrackingNumber ?: return@setHapticClickListener
            val uspsUrl = "https://tools.usps.com/go/TrackConfirmAction?tLabels=$tn"
            openUrl(uspsUrl)
        }

        bind.message.setHapticClickListener {
            val intent = Intent(mCtx, ChatActivity::class.java).apply {
                putExtra("id", buyerId)
                putExtra("name", buyerName)
                putExtra("image", buyerImage)
            }
            startActivity(intent)
        }

        // #9934033253: seller approves the buyer's cancellation request.
        bind.btnApproveCancellation.setHapticClickListener {
            val id = orderDbId.toIntOrNull()
            if (id == null || id <= 0) {
                errorToast("Order not ready yet, please try again.")
                return@setHapticClickListener
            }
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Approve cancellation?")
                .setMessage("The order will be cancelled and the buyer notified. This cannot be undone.")
                .setNegativeButton("Back") { d, _ -> d.dismiss() }
                .setPositiveButton("Approve") { d, _ ->
                    d.dismiss()
                    postDecideCancellation(id, "approve", null)
                }
                .show()
        }

        // #9934033253: seller rejects the request (optional reject reason).
        bind.btnRejectCancellation.setHapticClickListener {
            val id = orderDbId.toIntOrNull()
            if (id == null || id <= 0) {
                errorToast("Order not ready yet, please try again.")
                return@setHapticClickListener
            }
            val input = android.widget.EditText(requireContext()).apply {
                hint = "Reason (optional, shown to buyer)"
                inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                    android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                setLines(3)
                gravity = android.view.Gravity.TOP or android.view.Gravity.START
                filters = arrayOf(android.text.InputFilter.LengthFilter(500))
            }
            val pad = (16 * resources.displayMetrics.density).toInt()
            val container = android.widget.FrameLayout(requireContext()).apply {
                setPadding(pad, pad / 2, pad, 0)
                addView(input)
            }
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Reject cancellation?")
                .setMessage("The buyer will be told the order could not be cancelled.")
                .setView(container)
                .setNegativeButton("Back") { d, _ -> d.dismiss() }
                .setPositiveButton("Reject") { d, _ ->
                    d.dismiss()
                    val reason = input.text?.toString()?.trim().orEmpty().ifEmpty { null }
                    postDecideCancellation(id, "reject", reason)
                }
                .show()
        }

        adapter = ShippingUpdateAdapter(statusItems)

        bind.shippingRecycler.adapter = adapter

        bind.loader.isVisible = true
        viewModel.getOrderDetails(orderId.request())
        viewModel.getOrderDetailsRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    viewModel.getOrderDetailsRepo.value = null
                    bind.loader.isVisible = false

                    val mData = it.value.data

                    if (mData?.product != null) {
                        bind.productName.text = mData.product.title?.asCapital()
                        bind.productImage.loadUrl(mCtx, mData.product.images?.get(0).toString())
                        bind.category.text = mData.product.category?.name ?: ""
                    } else {
                        bind.productCard.isVisible = false
                        if (mData?.productSet != null) {
                            bind.productName.text = mData.productSet.name?.asCapital()
                            bind.category.text =
                                (mData.productSet.items?.find { it?.id == mData.productSetItemId }?.name ?: "N/A") + "#${mData.productSetItemUnitId}"
                        }
                    }

                    if (mData?.shippingAddress?.isEmpty() == true) {
                        bind.address.text = "N/A"
                    } else {
                        bind.address.text = mData?.shippingAddress
                    }

                    bind.orderId.text = mData?.orderId.toString()
                    bind.orderDate.text = Utils.getFormattedDateTime(
                        "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",
                        "MMM dd, yyyy, HH:mm",
                        mData?.createdAt.toString()
                    )

                    selectedStatus = mData?.status ?: ""

                    bind.status.setText(selectedStatus.replace("_", " ").asCapital(), false)

                    statusItems.clear()

                    if (mData?.shippingTracking?.isNotEmpty() == true) {
                        statusItems.addAll(mData.shippingTracking)
                    }

                    buyerId = (mData?.user?.id ?: 0).toString()
                    buyerName = mData?.user?.name ?: ""
                    buyerImage = mData?.user?.profileImage ?: ""

                    bind.userProfile.loadUrl(mCtx, mData?.user?.profileImage ?: "")

                    bind.userName.text = mData?.user?.name

                    bind.email.text = mData?.user?.email

                    adapter.notifyDataSetChanged()

                    // #31/#32/#33/#34 Wave 4: update order-workflow sections
                    orderDbId = (mData?.id ?: 0).toString()
                    currentTrackingNumber = mData?.trackingNumber
                    currentLabelUrl = mData?.labelUrl
                    currentCancellationStatus = mData?.cancellationStatus
                    updateWave4Ui(mData)
                    updateCancellationUi(mData)

                }

                is Resource.Error -> {
                    viewModel.getOrderDetailsRepo.value = null
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

        viewModel.changeOrderStatusRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    viewModel.changeOrderStatusRepo.value = null
                    viewModel.getOrderDetails(orderId.request())
                }

                is Resource.Error -> {
                    viewModel.changeOrderStatusRepo.value = null
                    bind.loader.isVisible = false
                    it.parse(mCtx, TAG, object : AlertClicks {
                        override fun primaryClick(dialog: AppBottomSheet) { dialog.dismiss() }
                        override fun secondaryClick(dialog: AppBottomSheet) { dialog.dismiss() }
                    })
                }

                else -> {}
            }
        }

        // #32 Wave 4: observer for mark-as-shipped with tracking
        viewModel.changeOrderStatusWithTrackingRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    viewModel.changeOrderStatusWithTrackingRepo.value = null
                    bind.trackingNumberInput.setText("")
                    successToast("Order marked as shipped!")
                    viewModel.getOrderDetails(orderId.request())
                }
                is Resource.Error -> {
                    viewModel.changeOrderStatusWithTrackingRepo.value = null
                    bind.loader.isVisible = false
                    it.parse(mCtx, TAG, object : AlertClicks {
                        override fun primaryClick(dialog: AppBottomSheet) { dialog.dismiss() }
                        override fun secondaryClick(dialog: AppBottomSheet) { dialog.dismiss() }
                    })
                }
                else -> {}
            }
        }

        // #33 Wave 4: observer for create shipping label
        viewModel.createShippingLabelRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    viewModel.createShippingLabelRepo.value = null
                    // Refresh order to get label_url from DB, then open it
                    bind.loader.isVisible = true
                    viewModel.getOrderDetails(orderId.request())
                    // After refresh, openLabelAfterRefresh flag triggers open in observer
                    pendingOpenLabel = true
                }
                is Resource.Error -> {
                    viewModel.createShippingLabelRepo.value = null
                    bind.loader.isVisible = false
                    it.parse(mCtx, TAG, object : AlertClicks {
                        override fun primaryClick(dialog: AppBottomSheet) { dialog.dismiss() }
                        override fun secondaryClick(dialog: AppBottomSheet) { dialog.dismiss() }
                    })
                }
                else -> {}
            }
        }
    }

    private var pendingOpenLabel = false

    // #9934033253: show the approve/reject card only when a request is pending.
    private fun updateCancellationUi(data: GetOrderDetailsResponse.Data?) {
        val pending = (data?.cancellationStatus?.lowercase() == "requested")
        bind.cancellationRequestSection.isVisible = pending
        if (pending) {
            val reason = data?.cancellationReason?.takeIf { it.isNotBlank() }
            bind.cancellationReasonText.text = if (reason != null)
                "The buyer has requested to cancel this order.\nReason: $reason"
            else
                "The buyer has requested to cancel this order."
        }
    }

    // #9934033253: POST /api/product/decide-cancellation {order_id, decision, reject_reason?}.
    // Mirrors the buyer-side raw-HTTP pattern in OrderDetailsFragment to avoid
    // touching the shared Retrofit/ViewModel plumbing for a one-off action.
    private fun postDecideCancellation(orderId: Int, decision: String, rejectReason: String?) {
        bind.loader.isVisible = true
        viewLifecycleOwner.lifecycleScope.launch {
            val code: Int = withContext(Dispatchers.IO) {
                try {
                    val token = io.bidswipe.app.utils.Prefs(mCtx).token()
                    val body = org.json.JSONObject().apply {
                        put("order_id", orderId)
                        put("decision", decision)
                        if (!rejectReason.isNullOrBlank()) put("reject_reason", rejectReason)
                    }.toString()
                    val url = java.net.URL("${io.bidswipe.app.utils.Const.BASE_URL}/api/product/decide-cancellation")
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
                    successToast(
                        if (decision == "approve") "Order cancelled. The buyer has been notified."
                        else "Request rejected. The buyer has been notified."
                    )
                    // Refresh using the fragment's order token (same value the initial
                    // load used), not the numeric id passed to the decide endpoint.
                    viewModel.getOrderDetails(this@SellerOrderDetailFragment.orderId.request())
                }
                403 -> errorToast("You are not authorized to decide this cancellation.")
                404 -> errorToast("Order not found.")
                409 -> errorToast("This cancellation request can no longer be decided.")
                -1 -> errorToast("Network error. Please try again.")
                else -> errorToast("Could not update the request (code $code).")
            }
        }
    }

    // #31/#32/#33/#34: Update Wave 4 sections based on order status
    private fun updateWave4Ui(data: GetOrderDetailsResponse.Data?) {
        val status = data?.status ?: ""
        val trackingNumber = data?.trackingNumber
        val labelUrl = data?.labelUrl

        // #31: Mark-as-Shipped section — show when status is 'processing'
        bind.markShippedSection.isVisible = (status == "processing")

        // #33: Get Shipping Label — show when processing or shipped (out_for_delivery)
        bind.shippingLabelSection.isVisible =
            (status == "processing" || status == "out_for_delivery")

        // #32: Tracking info — show when tracking number is present
        val hasTracking = !trackingNumber.isNullOrBlank()
        bind.trackingInfoSection.isVisible = hasTracking
        if (hasTracking) {
            bind.trackingNumberDisplay.text = trackingNumber
        }

        // #34: Completed badge — show when delivered
        bind.completedBadgeSection.isVisible = (status == "delivered")

        // If we just created a label, open it now
        if (pendingOpenLabel) {
            pendingOpenLabel = false
            if (!labelUrl.isNullOrBlank()) {
                openUrl(labelUrl)
            } else {
                errorToast("Label URL not available yet. Try again shortly.")
            }
        }
    }

    // Open URL in Chrome Custom Tab or browser
    private fun openUrl(url: String) {
        try {
            val customTabsIntent = CustomTabsIntent.Builder().build()
            customTabsIntent.launchUrl(mCtx, Uri.parse(url))
        } catch (e: Exception) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        }
    }
}