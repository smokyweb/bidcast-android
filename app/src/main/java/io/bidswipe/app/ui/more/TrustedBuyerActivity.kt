package io.bidswipe.app.ui.more

import android.content.res.ColorStateList
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.CONSUMED
import androidx.core.view.isVisible
import io.bidswipe.app.R
import io.bidswipe.app.R.color
import io.bidswipe.app.base.BaseActivity
import io.bidswipe.app.databinding.ActivityTrustedBuyerBinding
import io.bidswipe.app.network.response.GetBuyerIdentityResponse
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Const

import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.bind
import io.bidswipe.app.utils.cropper.CustomCropImageContract
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.runSafe
import io.bidswipe.app.utils.setHapticClickListener
import java.io.File

class TrustedBuyerActivity : BaseActivity() {
	private val bind by bind(ActivityTrustedBuyerBinding::inflate)
	private val viewModel by viewModels<MoreViewModel>()
	private var idPhoto = ""

	private val imageResult = registerForActivityResult(CustomCropImageContract()) { result ->
		if (result.isSuccessful) {
			val imagePath = result.getUriFilePath(this, true)
			if (imagePath != null) {
				bind.uploadLayout.isVisible = false
				bind.imgCard.isVisible = true
				bind.img.setImageURI(imagePath.toUri())
				idPhoto = imagePath
			}
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(bind.root)

		ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { v, insets ->
			val system = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			bind.root.setPadding(0, system.top, 0, system.bottom)
			CONSUMED
		}

		bind.header.onBackClick {
			finish()
		}

		bind.fileBtn.setHapticClickListener {
			requestPerms(Const.STR_PERMS) { per ->
				if (per) {
					imageResult.launch(Utils.initCrop(this, isCamera = true, isGallery = true))
				}
			}
		}

		bind.imgCard.setHapticClickListener {
			requestPerms(Const.STR_PERMS) { per ->
				if (per) {
					imageResult.launch(Utils.initCrop(this, isCamera = true, isGallery = true))
				}
			}
		}

		bind.submit.setHapticClickListener {

			if (idPhoto.isEmpty()) {
				Alerts.error(this, "Please Select an Id")

			} else {
				bind.loader.isVisible = true

				val imageName = System.currentTimeMillis().toString() + "_id_photo.jpeg"
				val idPart = Utils.imagePart("image", imageName, File(idPhoto))

				viewModel.storeBuyerIdentity(idPart)
			}

		}

		bind.loader.isVisible = true

		viewModel.fetchBuyerIdentity()

		viewModel.fetchBuyerIdentityRepo.observe(this) {
			when (it) {
				is Resource.Success -> {
					runSafe {
						bind.loader.isVisible = false

						val mData = it.value.data
						updateVerificationStatusBar(mData)

						if (mData?.image?.isNotEmpty() == true) {

							bind.uploadLayout.isVisible = false
							bind.imgCard.isVisible = true

							bind.img.loadUrl(this, mData.image)

							when (mData.status) {
								"rejected" -> {
									bind.firstDivider.dividerColor =
										ContextCompat.getColor(this, color.primary)

									bind.secondCard.setCardBackgroundColor(
										ColorStateList.valueOf(
											ContextCompat.getColor(this, color.primary)
										)
									)

									bind.secondText.setTextColor(
										ContextCompat.getColor(
											this,
											color.background
										)
									)

									bind.secondDivider.dividerColor =
										ContextCompat.getColor(this, color.primary)
									bind.thirdCard.setCardBackgroundColor(
										ColorStateList.valueOf(
											ContextCompat.getColor(this, color.error)
										)
									)

									bind.thirdText.text = "!"

									bind.thirdText.setTextColor(
										ContextCompat.getColor(
											this,
											color.background
										)
									)

									bind.finalStatus.text = "Rejected"

									bind.finalStatus.setTextColor(
										ContextCompat.getColor(
											this,
											color.error
										)
									)

									bind.submit.isVisible = true

								}

								"verified" -> {
									bind.firstDivider.dividerColor =
										ContextCompat.getColor(this, color.primary)

									bind.secondCard.setCardBackgroundColor(
										ColorStateList.valueOf(
											ContextCompat.getColor(this, color.primary)
										)
									)

									bind.secondText.setTextColor(
										ContextCompat.getColor(
											this,
											color.background
										)
									)

									bind.secondDivider.dividerColor =
										ContextCompat.getColor(this, color.primary)
									bind.thirdCard.setCardBackgroundColor(
										ColorStateList.valueOf(
											ContextCompat.getColor(this, color.primary)
										)
									)

									bind.thirdText.setTextColor(
										ContextCompat.getColor(
											this,
											color.background
										)
									)

									bind.submit.isVisible = false

								}

								else -> {
									// Pending admin review — show clear pending status to user
									bind.firstDivider.dividerColor =
										ContextCompat.getColor(this, color.primary)
									bind.secondCard.setCardBackgroundColor(
										ColorStateList.valueOf(
											ContextCompat.getColor(this, color.primary)
										)
									)

									bind.secondText.setTextColor(
										ContextCompat.getColor(
											this,
											color.background
										)
									)

									// Show "Pending" in step-3 circle and status label
									bind.thirdText.text = getString(io.bidswipe.app.R.string.pending)
									bind.finalStatus.text = getString(io.bidswipe.app.R.string.pending)
									bind.finalStatus.setTextColor(
										ContextCompat.getColor(this, color.primary)
									)

									bind.submit.isVisible = false

								}
							}
						} else {
							// No submission yet — show upload form
							bind.uploadLayout.isVisible = true
							bind.imgCard.isVisible = false
							bind.submit.isVisible = true
						}
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

		viewModel.storeBuyerIdentityRepo.observe(this) {
			when (it) {
				is Resource.Success -> {
					runSafe {
						// Show confirmation so user knows submission was received;
						// keep loader visible during the re-fetch that follows
						Alerts.success(
							this,
							"Your ID has been submitted. Please wait while an admin verifies your account. You'll be notified once approved."
						)
						viewModel.fetchBuyerIdentity()
					}
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

	private fun updateVerificationStatusBar(mData: GetBuyerIdentityResponse.Data?) {
		val raw = mData?.status?.trim()?.lowercase()
		val statusKey = when {
			raw.isNullOrBlank() || raw == "null" -> null
			else -> raw
		}
		when (statusKey) {
			"verified" -> {
				bind.verificationStatusValue.setText(R.string.verified)
				bind.verificationStatusValue.setTextColor(
					ContextCompat.getColor(this, color.primary)
				)
			}
			"rejected" -> {
				bind.verificationStatusValue.setText(R.string.buyer_verification_rejected)
				bind.verificationStatusValue.setTextColor(
					ContextCompat.getColor(this, color.error)
				)
			}
			"pending" -> {
				bind.verificationStatusValue.setText(R.string.buyer_verification_under_review)
				bind.verificationStatusValue.setTextColor(
					ContextCompat.getColor(this, color.onSurfaceVariant)
				)
			}
			null -> {
				if (mData == null) {
					bind.verificationStatusValue.setText(R.string.buyer_verification_not_submitted)
				} else {
					bind.verificationStatusValue.setText(R.string.buyer_verification_under_review)
				}
				bind.verificationStatusValue.setTextColor(
					ContextCompat.getColor(this, color.onSurfaceVariant)
				)
			}
			else -> {
				bind.verificationStatusValue.text =
					statusKey.replaceFirstChar { c -> c.uppercase() }
				bind.verificationStatusValue.setTextColor(
					ContextCompat.getColor(this, color.onSurfaceVariant)
				)
			}
		}
	}

}