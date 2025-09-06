package io.bidswipe.app.ui.dashboard.more

import android.content.res.ColorStateList
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.canhub.cropper.CropImageContract
import io.bidswipe.app.R
import io.bidswipe.app.R.*
import io.bidswipe.app.base.BaseActivity
import io.bidswipe.app.databinding.ActivityTrustedBuyerBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.bind
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.runSafe
import java.io.File

class TrustedBuyerActivity : BaseActivity() {
	private val bind by bind(ActivityTrustedBuyerBinding::inflate)
	private val viewModel by viewModels<MoreViewModel>()
	private var idPhoto = ""

	private val imageResult = registerForActivityResult(CropImageContract()) { result ->
		if (result.isSuccessful) {
			val imageUri = result.uriContent

			val imagePath = result.getUriFilePath(this , true)

			bind.uploadLayout.isVisible = false
			bind.imgCard.isVisible = true

			bind.img.setImageURI(imageUri)

			idPhoto = imagePath.toString()

		}
	}

	override fun onCreate(savedInstanceState : Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(bind.root)

		bind.header.onBackClick {
			finish()
		}

		bind.fileBtn.setOnClickListener {
			requestPerms(Const.STR_PERMS) { per ->
				if (per) {
					imageResult.launch(Utils.initCrop(this , isCamera = true , isGallery = true))
				}
			}
		}

		bind.imgCard.setOnClickListener {
			requestPerms(Const.STR_PERMS) { per ->
				if (per) {
					imageResult.launch(Utils.initCrop(this , isCamera = true , isGallery = true))
				}
			}
		}

		bind.submit.setOnClickListener {

			if (idPhoto.isEmpty()) {
				Alerts.error(this , "Please Select an Id")

			} else {
				bind.loader.isVisible = true

				val imageName = System.currentTimeMillis().toString() + "_id_photo.jpeg"
				val idPart = Utils.imagePart("image" , imageName , File(idPhoto))

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

						if (mData?.image?.isNotEmpty() == true) {

							bind.uploadLayout.isVisible = false
							bind.imgCard.isVisible = true

							bind.img.loadUrl(this , mData.image.toString())

							when (mData.status) {
								"rejected" -> {
									bind.firstDivider.dividerColor =
										ContextCompat.getColor(this , color.primary)

									bind.secondCard.setCardBackgroundColor(
										ColorStateList.valueOf(
											ContextCompat.getColor(this , R.color.primary)
										)
									)

									bind.secondText.setTextColor(
										ContextCompat.getColor(
											this ,
											R.color.background
										)
									)

									bind.secondDivider.dividerColor =
										ContextCompat.getColor(this , color.primary)
									bind.thirdCard.setCardBackgroundColor(
										ColorStateList.valueOf(
											ContextCompat.getColor(this , R.color.error)
										)
									)

									bind.thirdText.setText("!")

									bind.thirdText.setTextColor(
										ContextCompat.getColor(
											this ,
											R.color.background
										)
									)

									bind.finalStatus.text = "Rejected"

									bind.finalStatus.setTextColor(
										ContextCompat.getColor(
											this ,
											R.color.error
										)
									)

									bind.submit.isVisible = true

								}

								"verified" -> {
									bind.firstDivider.dividerColor =
										ContextCompat.getColor(this , color.primary)

									bind.secondCard.setCardBackgroundColor(
										ColorStateList.valueOf(
											ContextCompat.getColor(this , R.color.primary)
										)
									)

									bind.secondText.setTextColor(
										ContextCompat.getColor(
											this ,
											R.color.background
										)
									)

									bind.secondDivider.dividerColor =
										ContextCompat.getColor(this , color.primary)
									bind.thirdCard.setCardBackgroundColor(
										ColorStateList.valueOf(
											ContextCompat.getColor(this , R.color.primary)
										)
									)

									bind.thirdText.setTextColor(
										ContextCompat.getColor(
											this ,
											R.color.background
										)
									)

									bind.submit.isVisible = false

								}

								else -> {

									bind.firstDivider.dividerColor =
										ContextCompat.getColor(this , color.primary)
									bind.secondCard.setCardBackgroundColor(
										ColorStateList.valueOf(
											ContextCompat.getColor(this , R.color.primary)
										)
									)

									bind.secondText.setTextColor(
										ContextCompat.getColor(
											this ,
											R.color.background
										)
									)
								}


							}


						} else {

						}


					}
				}

				is Resource.Error -> {
					bind.loader.isVisible = false
					it.parse(this , TAG , object : AlertClicks {
						override fun primaryClick(dialog : AppBottomSheet) {
							dialog.dismiss()
						}

						override fun secondaryClick(dialog : AppBottomSheet) {
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
						bind.loader.isVisible = false

						val mData = it.value.data

						viewModel.fetchBuyerIdentity()

					}
				}

				is Resource.Error -> {
					bind.loader.isVisible = false
					it.parse(this , TAG , object : AlertClicks {
						override fun primaryClick(dialog : AppBottomSheet) {
							dialog.dismiss()
						}

						override fun secondaryClick(dialog : AppBottomSheet) {
							dialog.dismiss()
						}
					})
				}

				else -> {}
			}
		}

	}

}