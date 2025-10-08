package io.bidswipe.app.ui.dashboard

import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.view.isVisible
import io.bidswipe.app.base.BaseActivity
import io.bidswipe.app.databinding.ActivityRateSellerBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.bind
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.setHapticClickListener
import io.bidswipe.app.utils.value

class RateSellerActivity : BaseActivity() {

	private val bind by bind(ActivityRateSellerBinding::inflate)

	private val viewModel by viewModels<DashViewModel>()

	private var sellerId = ""
	private var sellerName = ""
	private var sellerImage = ""

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(bind.root)

		sellerId = intent.getStringExtra("sellerId") ?: ""
		sellerName = intent.getStringExtra("sellerName") ?: ""
		sellerImage = intent.getStringExtra("sellerImage") ?: ""

		bind.sellerName.text = sellerName.asCapital()

		bind.profileImage.loadUrl(this, sellerImage)

		bind.header.onBackClick {
			finishAfterTransition()
		}

		bind.add.setHapticClickListener {

			when {

				bind.overAllRating.rating < 1 -> {
					Alerts.error(this, "Please select overall rating")

				}

				bind.shippingRating.rating < 1 -> {
					Alerts.error(this, "Please select shipping rating")

				}

				bind.packagingRating.rating < 1 -> {
					Alerts.error(this, "Please select packaging rating")

				}

				bind.accuracyRating.rating < 1 -> {
					Alerts.error(this, "Please select accuracy rating")

				}

				bind.description.value().isEmpty() -> {
					Alerts.error(this, "Please add description")

				}

				else -> {
					bind.loader.isVisible = true

					viewModel.storeSellerRating(
						sellerId.request(),
						bind.overAllRating.rating.toString().request(),
						bind.shippingRating.rating.toString().request(),
						bind.packagingRating.rating.toString().request(),
						bind.accuracyRating.rating.toString().request(),
						bind.description.value().request()
					)


				}

			}

		}

		viewModel.storeSellerRatingRepo.observe(this) {
			when (it) {
				is Resource.Success -> {

					it.value.data

					finishAfterTransition()

				}

				is Resource.Error -> {
					bind.loader.isVisible = false

					it.parse(this, TAG, object : AlertClicks {
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