package io.bidswipe.app.utils.cropper

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.result.contract.ActivityResultContract
import io.bidswipe.app.utils.cropper.CustomCropImageActivity
import java.io.File

class CustomCropImageContract(
) : ActivityResultContract<Uri?, Uri?>() {

	override fun createIntent(context: Context, input: Uri?): Intent {
		return Intent(context, CustomCropImageActivity::class.java).apply {
			putExtra(CustomCropImageActivity.EXTRA_IMAGE_URI, input)
		}
	}

	override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
		return if (resultCode == Activity.RESULT_OK && intent != null) {
			val uriString = intent.getStringExtra(CustomCropImageActivity.RESULT_CROPPED_URI)
			uriString?.let { Uri.parse(it) }
		} else {
			null
		}
	}

	companion object {
		/**
		 * Get file path from URI (handles both file:// and content:// URIs)
		 */
		fun getUriFilePath(context: Context, uri: Uri?): String? {
			if (uri == null) return null
			
			return try {
				// Handle FileProvider and content:// URIs
				if (uri.scheme == "content") {
					val cursor = context.contentResolver.query(uri, null, null, null, null)
					cursor?.use {
						if (it.moveToFirst()) {
							val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
							if (nameIndex != -1) {
								val name = it.getString(nameIndex)
								val cacheFile = File(context.cacheDir, name)
								context.contentResolver.openInputStream(uri)?.use { input ->
									cacheFile.outputStream().use { output ->
										input.copyTo(output)
									}
								}
								return cacheFile.absolutePath
							}
						}
					}
					// Fallback: try to get path from URI
					val file = File(context.cacheDir, "temp_${System.currentTimeMillis()}.jpg")
					context.contentResolver.openInputStream(uri)?.use { input ->
						file.outputStream().use { output ->
							input.copyTo(output)
						}
					}
					if (file.exists()) return file.absolutePath
				} else if (uri.scheme == "file") {
					val file = File(uri.path ?: return null)
					if (file.exists()) {
						return file.absolutePath
					}
				}
				null
			} catch (e: Exception) {
				e.printStackTrace()
				null
			}
		}
	}
}
