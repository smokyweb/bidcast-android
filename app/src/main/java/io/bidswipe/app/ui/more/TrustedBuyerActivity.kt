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
import io.bidswipe.app.R.color
import io.bidswipe.app.base.BaseActivity
import io.bidswipe.app.databinding.ActivityTrustedBuyerBinding
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
					val mData = it.value.data

					try {
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
									// "pending" or other status — show pending verification message
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

									// Show clear pending status message to user
									bind.finalStatus.text = "Pending Admin Verification"
									bind.finalStatus.setTextColor(
										ContextCompat.getColor(
											this,
											color.primary
										)
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
					} catch (e: Exception) {
						// If image loading or UI update fails, log error and show user-friendly message
						Alerts.log(TAG, "Failed to load buyer identity data: ${e.localizedMessage}")
						Alerts.error(this, "Failed to load verification data. Please try again.")
					}
					// Always hide loader after processing, regardless of success or exception
					bind.loader.isVisible = false
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
					try {
						// ID upload successful — update UI to show pending verification status
						bind.uploadLayout.isVisible = false
						bind.imgCard.isVisible = true

						// Show clear pending status message to user
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

						bind.finalStatus.text = "Your ID has been submitted.\nPlease wait while an admin verifies your account.\nYou'll be notified once approved."
						bind.finalStatus.setTextColor(
							ContextCompat.getColor(
								this,
								color.primary
							)
						)

						bind.submit.isVisible = false

						// Keep loader visible and reload to ensure pending status is reflected
						bind.loader.isVisible = true
						viewModel.fetchBuyerIdentity()

					} catch (e: Exception) {
						// If something fails during submission processing, hide loader and show error
						Alerts.log(TAG, "Failed during ID submission confirmation: ${e.localizedMessage}")
						bind.loader.isVisible = false
						Alerts.error(this, "Failed to process submission. Please try again.")
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

}
