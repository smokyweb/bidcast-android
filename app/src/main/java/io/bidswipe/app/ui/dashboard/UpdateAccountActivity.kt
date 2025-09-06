package io.bidswipe.app.ui.dashboard

import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.view.isVisible
import com.canhub.cropper.CropImageContract
import io.bidswipe.app.App
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseActivity
import io.bidswipe.app.databinding.ActivityUpdateAccountBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.bind
import io.bidswipe.app.utils.hideKeyboard
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.value
import okhttp3.MultipartBody
import java.io.File
import kotlin.getValue

class UpdateAccountActivity : BaseActivity() {

	private val bind by bind(ActivityUpdateAccountBinding::inflate)

	private val viewModel by viewModels<DashViewModel>()

	private var imagePart : MultipartBody.Part? = null

	private val imageResult = registerForActivityResult(CropImageContract()) { result ->
		if (result.isSuccessful) {
			val imageUri = result.uriContent

			bind.userProfile.setImageURI(imageUri)

			val imagePath = result.getUriFilePath(this , true)

			val name = System.currentTimeMillis().toString() + "_profile_gallery.jpeg"
			imagePart = Utils.imagePart("profile_image" , name , File(imagePath ?: ""))

		}
	}

	override fun onCreate(savedInstanceState : Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(bind.root)


		bind.header.onBackClick {
			finishAfterTransition()

		}
		bind.root.setOnClickListener {
			hideKeyboard()
		}

		bind.layout.setOnClickListener {
			hideKeyboard()
		}

		bind.selectImg.setOnClickListener {
			requestPerms(Const.STR_PERMS) { per ->
				if (per) {
					imageResult.launch(Utils.initCrop(this , isCamera = true , isGallery = true))
				}
			}
		}

		bind.update.setOnClickListener {
			bind.loader.isVisible = true
			viewModel.updateProfile(

				bind.firstName.value().request() ,
				bind.lastName.value().request() ,
				imagePart ,
				bind.userName.value().request() ,
				bind.bio.value().request()

			)

		}

		bind.loader.isVisible = true

		viewModel.getUserProfile()
		viewModel.getUserProfileRepo.observe(this) {
			when (it) {
				is Resource.Success -> {
					bind.loader.isVisible = false
					val mData = it.value.data
					bind.firstName.setText(mData?.firstName.toString().uppercase())
					bind.lastName.setText(mData?.lastName.toString().uppercase())
					bind.userName.setText(mData?.username)
					bind.email.setText(mData?.email ?: "")
					bind.bio.setText(mData?.bio)
					bind.userProfile.loadUrl(this , mData?.profileImage.toString())
				}

				is Resource.Error -> {
					viewModel.getUserProfileRepo.value = null
					if (it.isNetworkError) {
						errorToast(getString(R.string.no_internet))
					} else {
						it.parse(this , TAG , object : AlertClicks {
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


		viewModel.updateProfileRepo.observe(this) {
			when (it) {
				is Resource.Success -> {
					bind.loader.isVisible = false
					it.value.data
					App.getProfile()
					Alerts.success(this , "Profile Updated")
				}

				is Resource.Error -> {
					viewModel.getUserProfileRepo.value = null
					if (it.isNetworkError) {
						errorToast(getString(R.string.no_internet))
					} else {
						it.parse(this , TAG , object : AlertClicks {
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
}