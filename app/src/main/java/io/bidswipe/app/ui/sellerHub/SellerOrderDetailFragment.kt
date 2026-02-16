package io.bidswipe.app.ui.sellerHub

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
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
import io.bidswipe.app.utils.finish
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
    private var buyerId = ""
    private var buyerName = ""
    private var buyerImage = ""

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
            if (findNavController().graph.id == R.navigation.seller_hub_nav_graph) {
                findNavController().popBackStack()
            } else {
                finish()
            }
        }

        bind.updateStatus.setHapticClickListener {

            if (selectedStatus.isEmpty()) {
                errorToast("Please select status")
                return@setHapticClickListener
            }
            bind.loader.isVisible = true
            viewModel.changeOrderStatus(orderId.request(), selectedStatus.request())
        }

        bind.message.setHapticClickListener {
            val intent = Intent(mCtx, ChatActivity::class.java).apply {
                putExtra("id", buyerId)
                putExtra("name", buyerName)
                putExtra("image", buyerImage)
            }
            startActivity(intent)
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
                            bind.category.text = (mData.productSet.items?.find { it?.id == mData.productSetItemId }?.name ?: "N/A") +"#${mData.productSetItemUnitId}"
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
                    it.value.data
                    viewModel.getOrderDetails(orderId.request())
                }

                is Resource.Error -> {
                    viewModel.changeOrderStatusRepo.value = null
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

    }
}