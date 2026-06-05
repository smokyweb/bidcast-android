package io.bidswipe.app.ui.dashboard

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isNotEmpty
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.PurchasesAdapter
import io.bidswipe.app.databinding.FragmentPurchasesBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetProductsByStatusResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.product.ProductDetailsActivity
import io.bidswipe.app.ui.sellerProfile.SellerProfileActivity
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@SuppressLint("NotifyDataSetChanged")
class PurchasesFragment : BaseFragment<DashViewModel, FragmentPurchasesBinding>() {

    override fun getModel(): Class<DashViewModel> = DashViewModel::class.java

    override fun getBind(inflater: LayoutInflater, view: ViewGroup?) = FragmentPurchasesBinding.inflate(inflater, view, false)

    private lateinit var purchasesAdapter: PurchasesAdapter
    private var mList = mutableListOf<GetProductsByStatusResponse.Data?>()
    private var page = 1
    private var isLoading = false
    private var currentFilter = ""
    private var type = "purchased"

    private var mClick = object : RecyclerClicks {
        override fun itemClick(pos: Int, status: String?) {
            when (status) {
                "product" -> {
                    val productId = mList[pos]?.productId ?: mList[pos]?.productSet?.id

                    if (mList[pos]?.orderId?.isNotEmpty() == true) {
                        startActivity(
                            Intent(mCtx, ProductDetailsActivity::class.java)
                                .putExtra("productId", productId.toString())
                                .putExtra("type", "orderDetail")
                                .putExtra("productType", if (mList[pos]?.productId != null) "product" else "product_set")
                                .putExtra("orderId", mList[pos]?.orderId.toString())
                        )
                    }
                }

                "profile" -> {
                    val sellerId = if (mList[pos]?.productId != null) (mList[pos]?.product?.user?.id) else (mList[pos]?.productSet?.seller?.id)
                    startActivity(
                        Intent(mCtx, SellerProfileActivity::class.java).putExtra(
                            "sellerId",
                            sellerId.toString()
                        )
                    )
                }

                "request_cancel" -> {
                    showRequestCancellationDialog(pos)
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupFilterChips()
        purchasesAdapter = PurchasesAdapter(mList, mClick)

        bind.recycler.adapter = purchasesAdapter

        bind.recycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = bind.recycler.layoutManager as LinearLayoutManager
                val lastItemPosition = layoutManager.findLastVisibleItemPosition()
                if (lastItemPosition == (mList.size - 1)) {
                    if (!isLoading) {
                        isLoading = true
                        page++
                        bind.bottomLoader.isVisible = true
                        loadData()
                    }
                }
            }
        })


//        bind.swipeRefreshLayout.setOnRefreshListener {
//            page = 1
//            loadData()
//        }

        bind.noInternet.onClick {
            bind.loader.isVisible = true
            bind.noInternet.isVisible = false
            page = 1
            loadData()
        }

        bind.loader.isVisible = true
        loadData()
        viewModel.getPurchasedProductsByStatusRepo.observe(viewLifecycleOwner) {
            viewModel.isViewPagerDataLoaded.value = true
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false
                    bind.bottomLoader.isVisible = false
                    bind.noInternet.isVisible = false
//                    bind.swipeRefreshLayout.isRefreshing = false

                    val mData = it.value.data
                    if (page == 1) {
                        mList.clear()

                    }
                    if (mData != null) {
                        mList.addAll(mData)
                    }

                    if (mList.isEmpty()) {
                        bind.noData.isVisible = true
                        bind.recycler.isVisible = false
                    } else {
                        bind.noData.isVisible = false
                        bind.recycler.isVisible = true
                    }

                    isLoading = page >= (it.value.totalPage ?: 0)

                    purchasesAdapter.notifyDataSetChanged()

                }

                is Resource.Error -> {
                    bind.noData.isVisible = false
                    bind.loader.isVisible = false
                    bind.bottomLoader.isVisible = false
//                    bind.swipeRefreshLayout.isRefreshing = false

                    if (it.isNetworkError) {
                        bind.noInternet.isVisible = true
                        bind.recycler.isVisible = false
                        bind.noData.isVisible = false
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

    fun reloadData() {
        if (Utils.isOnline(mCtx)) {
            bind.loader.isVisible = true
            bind.noInternet.isVisible = false
            page = 1
            loadData()
        } else {
            bind.loader.isVisible = false
            bind.noInternet.isVisible = true
            bind.recycler.isVisible = false
            bind.noData.isVisible = false
            viewModel.isViewPagerDataLoaded.value = true
        }
    }

    fun filterByStatus(filter: String) {
        currentFilter = when (filter) {
            "in_progress" -> "in_progress"
            "completed" -> "completed"
            else -> ""
        }
        page = 1
        loadData()
    }

    private fun loadData() {
        viewModel.getPurchasedProductsByStatus(type.request(), page.toString().request(), currentFilter.ifEmpty { null }?.request())
    }

    private fun showRequestCancellationDialog(pos: Int) {
        val item = mList.getOrNull(pos) ?: run {
            Alerts.error(mCtx, "Order not ready yet, please try again.")
            return
        }
        val orderId = item?.id ?: item?.orderId?.toIntOrNull()
        if (orderId == null || orderId <= 0) {
            Alerts.error(mCtx, "Order not ready yet, please try again.")
            return
        }

        val statusLower = item.status?.lowercase().orEmpty()
        if (statusLower != "pending" && statusLower != "processing") {
            Alerts.error(mCtx, "This order can no longer be cancelled.")
            return
        }

        val input = android.widget.EditText(requireContext()).apply {
            hint = "Reason for cancellation (required)"
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

        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Request to cancel this order?")
            .setMessage("Tell the seller why you're cancelling. They'll need to approve it.")
            .setView(container)
            .setNegativeButton("Keep order") { d, _ -> d.dismiss() }
            .setPositiveButton("Request cancellation", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener {
                    val reason = input.text?.toString()?.trim().orEmpty()
                    if (reason.isEmpty()) {
                        input.error = "Please enter a reason"
                        return@setOnClickListener
                    }
                    dialog.dismiss()
                    postRequestCancellation(pos, orderId, reason)
                }
        }
        dialog.show()
    }

    private fun postRequestCancellation(pos: Int, orderId: Int, reason: String) {
        bind.loader.isVisible = true
        viewLifecycleOwner.lifecycleScope.launch {
            val code = withContext(Dispatchers.IO) {
                try {
                    val token = io.bidswipe.app.utils.Prefs(mCtx).token()
                    val body = org.json.JSONObject().apply {
                        put("order_id", orderId)
                        put("reason", reason)
                    }.toString()
                    val url = java.net.URL("${io.bidswipe.app.utils.Const.BASE_URL}/api/product/request-cancellation")
                    val conn = url.openConnection() as java.net.HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.setRequestProperty("Accept", "application/json")
                    conn.setRequestProperty("Authorization", "Bearer $token")
                    conn.doOutput = true
                    conn.outputStream.use { it.write(body.toByteArray()) }
                    val responseCode = conn.responseCode
                    conn.disconnect()
                    responseCode
                } catch (e: Exception) {
                    e.printStackTrace()
                    -1
                }
            }
            bind.loader.isVisible = false
            when (code) {
                200, 201 -> {
                    android.widget.Toast.makeText(
                        mCtx,
                        "Cancellation requested. The seller has been notified.",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                    mList.getOrNull(pos)?.let { current ->
                        mList[pos] = current.copy(
                            cancellationStatus = "requested",
                            cancellationReason = reason,
                            cancellationRejectReason = null
                        )
                        purchasesAdapter.notifyItemChanged(pos)
                    }
                }
                403 -> Alerts.error(mCtx, "You are not authorized to request cancellation on this order.")
                404 -> Alerts.error(mCtx, "Order not found.")
                409 -> Alerts.error(mCtx, "This order can no longer be cancelled.")
                -1 -> Alerts.error(mCtx, "Network error. Please try again.")
                else -> Alerts.error(mCtx, "Could not request cancellation (code $code).")
            }
        }
    }

    private fun setupFilterChips() {
        if (bind.filterChipGroup.childCount > 0) {
            return // Already set up
        }

        val filters = listOf("All", "In Progress", "Completed")
        filters.forEachIndexed { index, filter ->
            val chip = Utils.makeAChip(
                mCtx = mCtx,
                text = filter,
                selected = index == 0,
                closeIconVisible = false,
                chipPadding = 12,
            )
            chip.setOnClickListener {
                filterByStatus(filter.lowercase().replace(" ", "_"))
                // Update chip selection
                bind.filterChipGroup.check(chip.id)
            }
            bind.filterChipGroup.addView(chip)
        }

        // Select first chip (All)
        if (bind.filterChipGroup.isNotEmpty()) {
            bind.filterChipGroup.check(bind.filterChipGroup.getChildAt(0).id)
        }

    }
}
