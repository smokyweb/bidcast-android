package io.bidswipe.app.ui.sellerHub

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.get
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.OrdersAdapter
import io.bidswipe.app.databinding.FragmentMyOrdersBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.interfaces.RecyclerClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetOrdersResponse
import io.bidswipe.app.ui.custom.AlertType
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.sellerProfile.SellerProfileActivity
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.Prefs
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.ids
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.runSafe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MyOrdersFragment : BaseFragment<SellerHubViewModel, FragmentMyOrdersBinding>() {
	override fun getModel(): Class<SellerHubViewModel> = SellerHubViewModel::class.java

	override fun getBind(inflater: LayoutInflater, view: ViewGroup?) =
		FragmentMyOrdersBinding.inflate(inflater, view, false)

	private var orderList = mutableListOf<GetOrdersResponse.Data?>()
	private lateinit var adapter: OrdersAdapter
	private var status = ""
	private var page = 1
	private var isLoading = false

	private val mClick = object : RecyclerClicks {
		override fun itemClick(pos: Int, status: String?) {

			when (status) {
				"profile" -> {
					if (orderList[pos]?.user != null) {
						startActivity(
							Intent(mCtx, SellerProfileActivity::class.java).putExtra(
								"sellerId",
								orderList[pos]?.user?.id.toString()
							)
						)
					}
				}

				else -> {
					findNavController().navigate(ids.myOrdersFragmentToSellerOrderDetailFragment, bundleOf("orderId" to orderList[pos]?.id.toString(), "from" to "myOrders"))
				}
			}
		}
	}

	@SuppressLint("NotifyDataSetChanged")
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		bind.header.onBackClick {
			finish()
		}

		// #45: If launched from Sold tab, pre-select the Completed filter
		val initialStatus = arguments?.getString("initialStatus")
		setUpChips(initialStatus)

		adapter = OrdersAdapter(orderList, mClick) { orderId, decision ->
			postCancellationDecision(orderId, decision)
		}

		bind.recycler.adapter = adapter

		bind.loader.isVisible = true

		bind.swipeRefreshLayout.setOnRefreshListener {
			bind.search.setText("")
			page = 1
			viewModel.getOrderListing(page = page.toString().request(), status.request())
		}

		bind.recycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
			override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
				super.onScrolled(recyclerView, dx, dy)
				val layoutManager = bind.recycler.layoutManager as LinearLayoutManager
				val lastItemPosition = layoutManager.findLastVisibleItemPosition()
				if (lastItemPosition == (orderList.size - 1)) {
					if (!isLoading) {
						isLoading = true
						page++
						bind.bottomLoader.isVisible = true
						viewModel.getOrderListing(page = page.toString().request(), status.request())
					}
				}
			}
		})

		bind.search.addTextChangedListener(object : TextWatcher {
			override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
			override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
			override fun afterTextChanged(s: Editable?) {
				val query = s?.toString()?.trim() ?: ""
				bind.searchLayout.isEndIconVisible = query.isNotEmpty()

				page = 1
				isLoading = false

				bind.loader.isVisible = true
				bind.recycler.isVisible = false
				bind.noData.isVisible = false

				if (!s.isNullOrEmpty()) {
					viewModel.getOrderListing(
						page.toString().request(),
						status.request(),
						s.toString().request()
					)
				} else {
					viewModel.getOrderListing(page.toString().request(), status.request())
				}
			}
		})

		bind.searchLayout.setEndIconOnClickListener {
			bind.search.setText("")
			bind.searchLayout.isEndIconVisible = false
			page = 1
			viewModel.getOrderListing(page = page.toString().request(), status.request())
		}

		bind.noInternet.onClick {
			bind.loader.isVisible = true
			bind.noInternet.isVisible = false
			viewModel.getOrderListing(page = page.toString().request(), status.request())
		}

		viewModel.getOrderListing(page = page.toString().request(), status.request())
		viewModel.getOrderListingRepo.observe(viewLifecycleOwner) {
			when (it) {
				is Resource.Success -> {
					bind.swipeRefreshLayout.isRefreshing = false
					bind.noInternet.isVisible = false
					bind.bottomLoader.isVisible = false
					bind.loader.isVisible = false

					val mData = it.value.data

					if (page == 1) {
						orderList.clear()
					}

					if (mData != null) {
						orderList.addAll(mData)
					}

					bind.newOrderCount.text = it.value.newOrderCount.toString()
					bind.processingOrderCount.text = it.value.processingOrderCount.toString()
					bind.completedOrderCount.text = it.value.completedOrderCount.toString()

					if (mData?.isEmpty() == true) {

						bind.noData.isVisible = true
						bind.recycler.isVisible = false

					} else {
						bind.noData.isVisible = false
						bind.recycler.isVisible = true
					}

					isLoading = page >= (it.value.totalPage ?: 0)

					adapter.notifyDataSetChanged()

				}

				is Resource.Error -> {
					bind.swipeRefreshLayout.isRefreshing = false
					bind.loader.isVisible = false
					bind.bottomLoader.isVisible = false

					if (it.isNetworkError) {
						bind.noInternet.isVisible = true
						bind.recycler.isVisible = false

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

	// Cancel-request flow (2026-05-29): seller approve/reject a cancellation request.
	private fun postCancellationDecision(orderId: Int, decision: String) {
		viewLifecycleOwner.lifecycleScope.launch {
			val code: Int = withContext(Dispatchers.IO) {
				try {
					val token = Prefs(mCtx).token()
					val body = org.json.JSONObject().apply {
						put("order_id", orderId)
						put("decision", decision)
					}.toString()
					val url = java.net.URL("${Const.BASE_URL}/api/product/decide-cancellation")
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
			when (code) {
				200, 201 -> {
					val msg = if (decision == "approve") "Order cancelled and buyer notified."
							  else "Cancellation request declined."
					android.widget.Toast.makeText(mCtx, msg, android.widget.Toast.LENGTH_SHORT).show()
					// Refresh the list
					page = 1
					viewModel.getOrderListing(page.toString().request(), status.request())
				}
				else -> {
					android.widget.Toast.makeText(mCtx, "Action failed (code $code)", android.widget.Toast.LENGTH_SHORT).show()
				}
			}
		}
	}

	private fun setUpChips(initialStatus: String? = null) {
		bind.search.setText("")
		bind.chipGroup.removeAllViews()

		val statusList = listOf("All", "Processing", "Completed", "Cancelled", "Refunded")
		statusList.forEach {
			bind.chipGroup.addView(
				Utils.makeAChip(
					mCtx = mCtx,
					text = it,
					selected = false,
					closeIconVisible = false,
					chipPadding = 12,
				)
			)
		}

		// #45: Support pre-selecting a status (e.g. "completed" when launched from Sold tab)
		val initialIndex = if (!initialStatus.isNullOrEmpty()) {
			statusList.indexOfFirst { it.lowercase() == initialStatus.lowercase() }.takeIf { it >= 0 } ?: 0
		} else {
			0
		}

		bind.chipGroup.check(bind.chipGroup[initialIndex].id)

		if (initialIndex > 0) {
			status = statusList[initialIndex].lowercase()
		}

		bind.chipGroup.setOnCheckedStateChangeListener { chipGroup, _ ->
			runSafe {
				val chipId = chipGroup.checkedChipId
				val index = chipGroup.indexOfChild(chipGroup.findViewById(chipId))
				if (index == -1) return@runSafe
				bind.loader.isVisible = true
				status = if (statusList[index].lowercase() == "all") "" else statusList[index].lowercase()
				bind.search.setText("")
			}
		}

	}
}