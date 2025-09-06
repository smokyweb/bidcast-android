package io.bidswipe.app.ui.dashboard.product

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.controller.ShippingUpdateAdapter
import io.bidswipe.app.databinding.FragmentOrderStatusBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.network.response.GetOrderDetailsResponse
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request

class OrderStatusFragment : BaseFragment<ProductViewModel , FragmentOrderStatusBinding>() {

	override fun getModel() : Class<ProductViewModel> = ProductViewModel::class.java

	override fun getBind(
		inflater : LayoutInflater ,
		view : ViewGroup? ,
	) = FragmentOrderStatusBinding.inflate(inflater , view , false)

	private val statusItems = mutableListOf<GetOrderDetailsResponse.Data.ShippingTracking?>()

	private lateinit var adapter : ShippingUpdateAdapter
	private var orderId = ""

	override fun onViewCreated(view : View , savedInstanceState : Bundle?) {
		super.onViewCreated(view , savedInstanceState)

		orderId = arguments?.getString("orderId") ?: ""

		bind.header.onBackClick {
			finish()
		}

		adapter = ShippingUpdateAdapter(statusItems)

		bind.shippingRecycler.adapter = adapter

		bind.homeBtn.setOnClickListener {
			finish()
		}

		bind.receipt.setOnClickListener {

			bind.loader.isVisible = true

			viewModel.getOrderReceipt(orderId = orderId.request())

		}

		bind.loader.isVisible = true

		viewModel.getOrderDetails(orderId.request())

		viewModel.getOrderDetailsRepo.observe(viewLifecycleOwner) {
			when (it) {
				is Resource.Success -> {
					bind.loader.isVisible = false

					val mData = it.value.data

					bind.productName.text = mData?.product?.title
					bind.productImage.loadUrl(mCtx , mData?.product?.images?.get(0).toString())
					bind.orderId.text = mData?.id.toString()
					bind.orderDate.text = Utils.getFormattedDateTime(
						"yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'" ,
						"MMM dd, yyyy, HH:mm" ,
						mData?.createdAt.toString()
					)

					if (mData?.shippingTracking?.isNotEmpty() == true) {
						statusItems.addAll(mData.shippingTracking)
					}

					adapter.notifyDataSetChanged()

				}

				is Resource.Error -> {
					bind.loader.isVisible = false

					if (it.isNetworkError) {
						errorToast(getString(R.string.no_internet))
					} else {
						it.parse(mCtx , TAG , object : AlertClicks {
							override fun primaryClick(dialog : AppBottomSheet) {
								dialog.dismiss()

							}

							override fun secondaryClick(dialog : AppBottomSheet) {
								dialog.dismiss()

							}
						})
					}
				}

				else -> {}

			}
		}

		viewModel.getOrderReceiptRepo.observe(viewLifecycleOwner) {
			when (it) {
				is Resource.Success -> {
					viewModel.getOrderReceiptRepo.value = null
					bind.loader.isVisible = false

					val mData = it.value.data

					downloadPdf(mCtx , mData.toString() , System.currentTimeMillis().toString())

				}

				is Resource.Error -> {
					viewModel.getOrderReceiptRepo.value = null
					bind.loader.isVisible = false

					if (it.isNetworkError) {
						errorToast(getString(R.string.no_internet))
					} else {
						it.parse(mCtx , TAG , object : AlertClicks {
							override fun primaryClick(dialog : AppBottomSheet) {
								dialog.dismiss()

							}

							override fun secondaryClick(dialog : AppBottomSheet) {
								dialog.dismiss()

							}
						})
					}
				}

				else -> {}

			}
		}

	}

	fun downloadPdf(context : Context , url : String , fileName : String) {
		val request = DownloadManager.Request(Uri.parse(url)).apply {
			setTitle(fileName)
			setDescription("Downloading PDF...")
			setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

			// For Android 10 and above, this is app-specific external directory
			setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS , "$fileName.pdf")

			setAllowedOverMetered(true)
			setAllowedOverRoaming(true)
		}

		val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
		downloadManager.enqueue(request)
	}

}