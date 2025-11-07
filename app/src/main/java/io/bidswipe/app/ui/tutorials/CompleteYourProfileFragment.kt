package io.bidswipe.app.ui.tutorials

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import io.bidswipe.app.App
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.databinding.FragmentCompleteYourProfileBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.ui.dashboard.DashViewModel
import io.bidswipe.app.utils.Const
import io.bidswipe.app.utils.cropper.CustomCropImageContract
import io.bidswipe.app.utils.cropper.CustomCropImageHelper
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.ids
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.request
import io.bidswipe.app.utils.setHapticClickListener
import io.bidswipe.app.utils.value
import okhttp3.MultipartBody
import java.io.File

class CompleteYourProfileFragment : BaseFragment<DashViewModel , FragmentCompleteYourProfileBinding>() {
	override fun getModel() : Class<DashViewModel> = DashViewModel::class.java

	override fun getBind(
        inflater : LayoutInflater ,
        view : ViewGroup? ,
    ) = FragmentCompleteYourProfileBinding.inflate(inflater , view , false)

	private var imagePart: MultipartBody.Part? = null

	private val cropImageLauncher = registerForActivityResult(CustomCropImageContract()) { uri ->
		if (uri != null) {
			bind.userProfile.setImageURI(uri)
			val imagePath = CustomCropImageContract.getUriFilePath(mCtx, uri)
			if (imagePath != null) {
				val name = System.currentTimeMillis().toString() + "_profile_gallery.jpeg"
				imagePart = Utils.imagePart("profile_image", name, File(imagePath))
			}
		}
	}

	private val imagePickerManager = CustomCropImageHelper.createManager(this, cropImageLauncher)

	override fun onViewCreated(view : View , savedInstanceState : Bundle?) {
		super.onViewCreated(view , savedInstanceState)

		bind.header.onBackClick {
			findNavController().navigate(ids.action_completeYourProfileFragment_to_prepareYourShowFragment)
		}

		bind.userProfile.setHapticClickListener {
			requestPerms(Const.STR_PERMS) { per ->
				if (per) {
					imagePickerManager.launch(isCamera = true, isGallery = true)
				}
			}
		}

		bind.continueBtn.setHapticClickListener {
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
		viewModel.getUserProfileRepo.observe(viewLifecycleOwner) {
			when (it) {
				is Resource.Success -> {
					bind.loader.isVisible = false
					val mData = it.value.data
					bind.firstName.setText(mData?.firstName?.asCapital())
					bind.lastName.setText(mData?.lastName?.asCapital())
					bind.userName.setText(mData?.username)
					bind.bio.setText(mData?.bio)
					bind.userProfile.loadUrl(mCtx, mData?.profileImage.toString())
				}

				is Resource.Error -> {
					bind.loader.isVisible = false
					viewModel.getUserProfileRepo.value = null
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

		viewModel.updateProfileRepo.observe(viewLifecycleOwner) {
			when (it) {
				is Resource.Success -> {
					bind.loader.isVisible = false
					it.value.data
					App.getProfile()
					viewModel.currentStep = 4
					findNavController().navigate(ids.action_completeYourProfileFragment_to_prepareYourShowFragment)
				}

				is Resource.Error -> {
					viewModel.getUserProfileRepo.value = null
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