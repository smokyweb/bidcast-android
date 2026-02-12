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
import androidx.core.view.isVisible
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

class OrderDetailsFragment : BaseFragment<ProductViewModel, FragmentOrderDetailsBinding>() {
    override fun getModel(): Class<ProductViewModel> = ProductViewModel::class.java

    override fun getBind(
        inflater: LayoutInflater,
        view: ViewGroup?
    ): FragmentOrderDetailsBinding = FragmentOrderDetailsBinding.inflate(inflater, view, false)

    private var orderId: String? = null
    private var primaryOrderId: String? = null
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

                    bind.shippingAddress.text = if (mData?.shippingAddress != null) {
                        buildSpannedString {
                            if (!mData.shippingAddress.name.isNullOrEmpty()) {
                                append(mData.shippingAddress.name)
                                append("\n")
                            }
                            if (!mData.shippingAddress.streetAddress.isNullOrEmpty()) {
                                append(mData.shippingAddress.streetAddress)
                                append("\n")
                            }
                            if (!mData.shippingAddress.city.isNullOrEmpty()) {
                                append(mData.shippingAddress.city)
                                append(",")
                            }
                            append(mData.shippingAddress.state ?: "")
                        }
                    } else {
                        "N/A"
                    }

                    bind.productImage.loadUrl(mCtx, mData?.order?.product?.images?.get(0) ?: "")
                    bind.productName.text = mData?.order?.product?.title
                    bind.productDescription.text = mData?.order?.product?.description

                    bind.orderProgress.setProgress(mData?.order?.orderStatusPercentage ?: 0, true)

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