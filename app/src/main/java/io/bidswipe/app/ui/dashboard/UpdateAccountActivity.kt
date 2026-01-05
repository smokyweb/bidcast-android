package io.bidswipe.app.ui.dashboard

import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.CONSUMED
import androidx.core.view.isVisible
import io.bidswipe.app.App
import io.bidswipe.app.base.BaseActivity
import io.bidswipe.app.databinding.ActivityUpdateAccountBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.bind
import io.bidswipe.app.utils.cropper.CustomCropImageContract
import io.bidswipe.app.utils.hideKeyboard
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.setHapticClickListener
import io.bidswipe.app.utils.value
import okhttp3.MultipartBody
import java.io.File

class UpdateAccountActivity : BaseActivity() {
	
	private val bind by bind(ActivityUpdateAccountBinding::inflate)
	
	private val viewModel by viewModels<DashViewModel>()
	
	private var imagePart: MultipartBody.Part? = null
	
	private val imageResult = registerForActivityResult(CustomCropImageContract()) { result ->
		if (result.isSuccessful) {
			val imagePath = result.getUriFilePath(this, true)
			if (imagePath != null) {
				bind.userProfile.setImageURI(imagePath.toUri())
				val name = System.currentTimeMillis().toString() + "_profile_gallery.jpeg"
				imagePart = Utils.imagePart("profile_image", name, File(imagePath))
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
			finishAfterTransition()
			
		}
		bind.root.setHapticClickListener {
			hideKeyboard()
		}
		
		bind.layout.setHapticClickListener {
			hideKeyboard()
		}
		
		bind.selectImg.setHapticClickListener {
			requestPerms(Const.STR_PERMS) { per ->
				if (per) {
					imageResult.launch(Utils.initCrop(this, isCamera = true, isGallery = true))
				}
			}
		}
		
		bind.update.setHapticClickListener {
			bind.loader.isVisible = true
			viewModel.updateProfile(
				bind.firstName.value().request(),
				bind.lastName.value().request(),
				imagePart,
				bind.userName.value().request(),
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
					bind.firstName.setText(mData?.firstName?.asCapital())
					bind.lastName.setText(mData?.lastName?.asCapital())
					bind.userName.setText(mData?.username)
					bind.email.setText(mData?.email ?: "")
					bind.bio.setText(mData?.bio)
					bind.userProfile.loadUrl(this, mData?.profileImage.toString())
				}

				is Resource.Error -> {
					bind.loader.isVisible = false
					viewModel.getUserProfileRepo.value = null
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
		
		viewModel.updateProfileRepo.observe(this) {
			when (it) {
				is Resource.Success -> {
					bind.loader.isVisible = false
					it.value.data
					App.getProfile()
					Alerts.success(this, "Profile Updated")
				}
				
				is Resource.Error -> {
					bind.loader.isVisible = false
					viewModel.getUserProfileRepo.value = null
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