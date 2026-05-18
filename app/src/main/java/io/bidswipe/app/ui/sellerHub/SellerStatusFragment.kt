package io.bidswipe.app.ui.sellerHub

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
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

		// #48: status-string mapping — approved/active → green; pending → orange; rejected → red
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
						applyStatusStyle(bind.vendor.statusCard, bind.vendor.status, marketplaceVendor.status)
						bind.vendor.status.text = marketplaceVendor.status?.asCapital()
						bind.vendor.title.text = marketplaceVendor.title
						bind.vendor.subTitle.text = "Seller Rating: ${marketplaceVendor.sellerRating}/5"
					}

					if (liveSellVendor != null) {
						applyStatusStyle(bind.sender.statusCard, bind.sender.status, liveSellVendor.status)
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

	/**
	 * #48 — map status string to correct badge colour.
	 * active / approved → green (success)
	 * pending           → orange (warning)
	 * rejected          → red (error)
	 * unknown           → default warning
	 */
	private fun applyStatusStyle(
		card: MaterialCardView,
		text: TextView,
		status: String?
	) {
		when (status?.lowercase()) {
			"active", "approved" -> {
				card.setCardBackgroundColor(ContextCompat.getColor(mCtx, clr.successContainer))
				card.strokeColor = ContextCompat.getColor(mCtx, clr.success)
				text.setTextColor(ContextCompat.getColor(mCtx, clr.success))
			}
			"rejected" -> {
				card.setCardBackgroundColor(ContextCompat.getColor(mCtx, clr.errorContainer))
				card.strokeColor = ContextCompat.getColor(mCtx, clr.error)
				text.setTextColor(ContextCompat.getColor(mCtx, clr.error))
			}
			else -> { // pending or unknown
				card.setCardBackgroundColor(ContextCompat.getColor(mCtx, clr.warningContainer))
				card.strokeColor = ContextCompat.getColor(mCtx, clr.warning)
				text.setTextColor(ContextCompat.getColor(mCtx, clr.warning))
			}
		}
	}

}