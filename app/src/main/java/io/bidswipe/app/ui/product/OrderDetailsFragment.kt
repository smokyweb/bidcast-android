package io.bidswipe.app.ui.product

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.buildSpannedString
import androidx.core.view.isVisible
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.databinding.FragmentOrderDetailsBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.parse

class OrderDetailsFragment : BaseFragment<ProductViewModel, FragmentOrderDetailsBinding>() {
	override fun getModel(): Class<ProductViewModel> = ProductViewModel::class.java

	override fun getBind(
		inflater: LayoutInflater,
		view: ViewGroup?
	): FragmentOrderDetailsBinding  = FragmentOrderDetailsBinding.inflate(inflater, view, false)

	private var orderId : String? = null
	private var productId : String? = null

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		orderId = requireActivity().intent.getStringExtra("orderId")
		productId = requireActivity().intent.getStringExtra("productId")

		bind.header.onBackClick {
			finish()
		}

		bind.loader.isVisible = true

		viewModel.fetchOrderDetail(productId,orderId )

		viewModel.fetchOrderDetailRepo.observe(viewLifecycleOwner) {
			when (it) {
				is Resource.Success -> {
					bind.loader.isVisible = false

					val mData = it.value.data

					bind.header.setHeaderText(mData?.order?.product?.title?.asCapital() ?:"Order Detail")

					bind.shippingAddress.text = buildSpannedString {
						append(mData?.shippingAddress?.name)
						append("\n")
						append(mData?.shippingAddress?.streetAddress?:"")
						append("\n")
						append(mData?.shippingAddress?.city ?:"")
						append(",")
						append(mData?.shippingAddress?.state ?:"")
					}

					bind.productImage.loadUrl(mCtx, mData?.order?.product?.images?.get(0) ?:"")
					bind.productName.text = mData?.order?.product?.title
					bind.productDescription.text = mData?.order?.product?.description

					bind.orderId.text = mData?.order?.id.toString()
					bind.orderDate.text = mData?.order?.createdAt
					bind.soldBy.text = mData?.sellerDetails?.name
					bind.quantity.text = mData?.order?.product?.quantity.toString()
					bind.category.text = mData?.order?.product?.category?.name

					bind.userName.text = mData?.sellerDetails?.name
					bind.userImage.loadUrl(mCtx, mData?.sellerDetails?.profileImage ?:"")

					bind.rating.text = mData?.ratingAvg ?:"0"

					bind.review.text = mData?.review ?:"0"

					bind.sold.text = (mData?.soldCount ?:0).toString()

					bind.shipping.text = mData?.avgShip ?:"0"
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

}