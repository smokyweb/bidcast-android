package io.bidswipe.app.utils.cropper

import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import java.io.File

/**
 * Helper class to simplify image picking and cropping workflow
 * Usage example:
 * 
 * In Fragment or Activity:
 * 
 * private val imagePicker = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
 *     uri?.let { cropImageLauncher.launch(it) }
 * }
 * 
 * private val cropImageLauncher = registerForActivityResult(CustomCropImageContract()) { uri ->
 *     uri?.let { 
 *         val imagePath = CustomCropImageContract.getUriFilePath(context, it)
 *         // Use the cropped image
 *     }
 * }
 * 
 * // To pick from gallery:
 * imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
 * 
 * // To pick from camera, use:
 * private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
 *     if (success) {
 *         cameraUri?.let { cropImageLauncher.launch(it) }
 *     }
 * }
 */
object CropImageHelper {
	
	/**
	 * Launch image picker (gallery) and then crop
	 * For Android 13+ (API 33+), uses PickVisualMedia
	 */
	fun launchImagePickerAndCrop(
		fragment: Fragment,
		imagePickerLauncher: ActivityResultLauncher<Intent>,
		cropLauncher: ActivityResultLauncher<Uri?>
	) {
		val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
		imagePickerLauncher.launch(intent)
	}
	
	/**
	 * Launch camera and then crop
	 */
	fun launchCameraAndCrop(
		fragment: Fragment,
		cameraLauncher: ActivityResultLauncher<Uri>,
		cropLauncher: ActivityResultLauncher<Uri?>
	): Uri? {
		val photoFile = File(fragment.requireContext().getExternalFilesDir(null), "temp_photo_${System.currentTimeMillis()}.jpg")
		val photoUri = FileProvider.getUriForFile(
			fragment.requireContext(),
			"${fragment.requireContext().packageName}.fileprovider",
			photoFile
		)
		cameraLauncher.launch(photoUri)
		return photoUri
	}
	
	/**
	 * Directly launch crop activity with existing URI
	 */
	fun launchCrop(fragment: Fragment, imageUri: Uri, cropLauncher: ActivityResultLauncher<Uri?>) {
		cropLauncher.launch(imageUri)
	}
}
