package io.bidswipe.app.utils.cropper

import android.net.Uri
import android.view.LayoutInflater
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import io.bidswipe.app.databinding.MediaIntentChooserSheetBinding
import io.bidswipe.app.utils.Alerts
import java.io.File

@Suppress("unused")
class ImagePickerCropManager(
	private val fragment: Fragment,
	private val cropLauncher: ActivityResultLauncher<Uri?>
) {
	private var cameraUri: Uri? = null
	
	private val imagePickerLauncher = fragment.registerForActivityResult(
		ActivityResultContracts.PickVisualMedia()
	) { uri ->
		uri?.let { cropLauncher.launch(it) }
	}
	
	private val cameraLauncher = fragment.registerForActivityResult(
		ActivityResultContracts.TakePicture()
	) { success ->
		if (success) {
			cameraUri?.let { cropLauncher.launch(it) }
		}
	}

	fun launch(isCamera: Boolean = true, isGallery: Boolean = true) {
		when {
			isCamera && isGallery -> showImageChooser(isCamera, isGallery)
			isCamera -> launchCamera()
			isGallery -> imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
			else -> imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
		}
	}

	private fun showImageChooser(showCamera: Boolean, showGallery: Boolean) {
		val context = fragment.requireContext()
		val chooserBinding = MediaIntentChooserSheetBinding.inflate(LayoutInflater.from(context))
		val chooserSheet = Alerts.appBottomSheet(context, true, chooserBinding)

		chooserBinding.camera.isVisible = showCamera
		chooserBinding.gallery.isVisible = showGallery

		chooserBinding.camera.setOnClickListener {
			chooserSheet.dismiss()
			if (showCamera) {
				launchCamera()
			}
		}

		chooserBinding.gallery.setOnClickListener {
			chooserSheet.dismiss()
			if (showGallery) {
				imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
			}
		}

		chooserBinding.cancel.setOnClickListener {
			chooserSheet.dismiss()
		}

		chooserSheet.show()
	}
	
	private fun launchCamera() {
		val photoFile = File(
			fragment.requireContext().getExternalFilesDir(null),
			"temp_photo_${System.currentTimeMillis()}.jpg"
		)
		try {
			cameraUri = FileProvider.getUriForFile(
				fragment.requireContext(),
				"${fragment.requireContext().packageName}.fileprovider",
				photoFile
			)
			cameraUri?.let { cameraLauncher.launch(it) }
		} catch (e: Exception) {
			e.printStackTrace()
		}
	}
	
}

/**
 * Manager class that handles image picking (gallery/camera) and cropping internally.
 * Only the crop launcher needs to be exposed by the fragment/activity.
 */
@Suppress("unused")
class ActivityImagePickerCropManager(
	private val activity: FragmentActivity,
	private val cropLauncher: ActivityResultLauncher<Uri?>
) {
	private var cameraUri: Uri? = null

	private val imagePickerLauncher = activity.registerForActivityResult(
		ActivityResultContracts.PickVisualMedia()
	) { uri ->
		uri?.let { cropLauncher.launch(it) }
	}

	private val cameraLauncher = activity.registerForActivityResult(
		ActivityResultContracts.TakePicture()
	) { success ->
		if (success) {
			cameraUri?.let { cropLauncher.launch(it) }
		}
	}

	fun launch(isCamera: Boolean = true, isGallery: Boolean = true) {
		when {
			isCamera && isGallery -> showImageChooser(isCamera, isGallery)
			isCamera -> launchCamera()
			isGallery -> imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
			else -> imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
		}
	}

	private fun showImageChooser(showCamera: Boolean, showGallery: Boolean) {
		val context = activity
		val chooserBinding = MediaIntentChooserSheetBinding.inflate(LayoutInflater.from(context))
		val chooserSheet = Alerts.appBottomSheet(context, true, chooserBinding)

		chooserBinding.camera.isVisible = showCamera
		chooserBinding.gallery.isVisible = showGallery

		chooserBinding.camera.setOnClickListener {
			chooserSheet.dismiss()
			if (showCamera) {
				launchCamera()
			}
		}

		chooserBinding.gallery.setOnClickListener {
			chooserSheet.dismiss()
			if (showGallery) {
				imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
			}
		}

		chooserBinding.cancel.setOnClickListener {
			chooserSheet.dismiss()
		}

		chooserSheet.show()
	}

	private fun launchCamera() {
		val photoFile = File(
			activity.getExternalFilesDir(null),
			"temp_photo_${System.currentTimeMillis()}.jpg"
		)
		try {
			cameraUri = FileProvider.getUriForFile(
				activity,
				"${activity.packageName}.fileprovider",
				photoFile
			)
			cameraUri?.let { cameraLauncher.launch(it) }
		} catch (e: Exception) {
			e.printStackTrace()
		}
	}
 
	
}

/**
 * Helper class for using CustomCropImageActivity with proper edge-to-edge support
 * Replaces the old initCrop function with a better solution for Android 16+
 * 
 * This provides a similar API to the old initCrop but uses the new custom crop activity
 * that properly handles Android 16 edge-to-edge display.
 */
object CustomCropImageHelper {

	fun createManager(
		fragment: Fragment,
		cropLauncher: ActivityResultLauncher<Uri?>
	): ImagePickerCropManager {
		return ImagePickerCropManager(fragment, cropLauncher)
	}

	fun createManager(
		activity: FragmentActivity,
		cropLauncher: ActivityResultLauncher<Uri?>
	): ActivityImagePickerCropManager {
		return ActivityImagePickerCropManager(activity, cropLauncher)
	}
}
