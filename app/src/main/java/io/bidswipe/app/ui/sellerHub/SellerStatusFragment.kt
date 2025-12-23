package io.bidswipe.app.ui.sellerHub

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.databinding.FragmentSellerStatusBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.more.MoreActivity
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.clr
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.setHapticClickListener

class SellerStatusFragment : BaseFragment<SellerHubViewModel, FragmentSellerStatusBinding>() {

	override fun getModel(): Class<SellerHubViewModel> = SellerHubViewModel::class.java

	override fun getBind(
		inflater: LayoutInflater,
		view: ViewGroup?,
	) = FragmentSellerStatusBinding.inflate(inflater, view, false)

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		bind.header.onBackClick {
			finish()
		}

		bind.contactButton.setHapticClickListener {
			startActivity(Intent(mCtx, MoreActivity::class.java).putExtra("slug", "contactUs"))
		}

		viewModel.getSellerStatus()
		viewModel.getSellerStatusRepo.observe(viewLifecycleOwner) {
			when (it) {
				is Resource.Success -> {
					val mData = it.value.data

					val marketplaceVendor = mData?.marketplaceVendor
					val liveSellVendor = mData?.liveSellVendor

					if (marketplaceVendor != null) {
						if (marketplaceVendor.status?.lowercase().toString() != "active"){
							bind.vendor.statusCard.setCardBackgroundColor(ContextCompat.getColor(mCtx, clr.warningContainer))
							bind.vendor.statusCard.strokeColor = ContextCompat.getColor(mCtx, clr.warning)
							bind.vendor.status.setTextColor(ContextCompat.getColor(mCtx, clr.warning))
						}

						bind.vendor.status.text = marketplaceVendor.status?.asCapital()
						bind.vendor.title.text = marketplaceVendor.title
						bind.vendor.subTitle.text = "Seller Rating: ${marketplaceVendor.sellerRating}/5"
					}

					if (liveSellVendor != null) {
						if (liveSellVendor.status?.lowercase().toString() != "active"){
							bind.sender.statusCard.setCardBackgroundColor(ContextCompat.getColor(mCtx, clr.warningContainer))
							bind.sender.statusCard.strokeColor = ContextCompat.getColor(mCtx, clr.warning)
							bind.sender.status.setTextColor(ContextCompat.getColor(mCtx, clr.warning))
						}

						bind.sender.status.text = liveSellVendor.status?.asCapital()
						bind.sender.title.text = liveSellVendor.title
						bind.sender.subTitle.text = "Submitted: ${liveSellVendor.submitted}"
					}
				}

				is Resource.Error -> {
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