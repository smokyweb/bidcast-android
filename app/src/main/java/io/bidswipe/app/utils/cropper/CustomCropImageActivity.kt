package io.bidswipe.app.utils.cropper

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.canhub.cropper.CropImageView
import io.bidswipe.app.databinding.ActivityCustomCropImageBinding
import io.bidswipe.app.utils.bind
import java.io.File
import java.io.FileOutputStream

class CustomCropImageActivity : AppCompatActivity() {
	
	private val bind by bind(ActivityCustomCropImageBinding::inflate)
	private var imageUri: Uri? = null
	
	companion object {
		const val EXTRA_IMAGE_URI = "extra_image_uri"
		const val RESULT_CROPPED_URI = "result_cropped_uri"
	}
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(bind.root)
		WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true
		
		ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { v, insets ->
			val system = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			bind.root.setPadding(0, system.top, 0, system.bottom)
			insets
		}
		
		// Get image URI from intent
		imageUri = intent.getParcelableExtra(EXTRA_IMAGE_URI)
		
		if (imageUri == null) {
			finish()
			return
		}
		
		bind.cropImageView.cropShape = CropImageView.CropShape.RECTANGLE
		bind.cropImageView.setImageUriAsync(imageUri)
		
		bind.toolbar.onBackClick { finish() }
		
		bind.toolbar.onMorePrimaryClick {
			cropImage()
		}
		
	}
	
	private fun cropImage() {
		val croppedBitmap = bind.cropImageView.getCroppedImage()
		
		if (croppedBitmap != null) {
			// Save the cropped image
			val outputUri = saveCroppedImage(croppedBitmap)
			
			if (outputUri != null) {
				val resultIntent = Intent().apply {
					putExtra(RESULT_CROPPED_URI, outputUri.toString())
				}
				setResult(RESULT_OK, resultIntent)
				finish()
			} else {
				// If save fails, return the bitmap via data
				setResult(RESULT_CANCELED)
				finish()
			}
		} else {
			setResult(RESULT_CANCELED)
			finish()
		}
	}
	
	private fun saveCroppedImage(bitmap: Bitmap): Uri? {
		return try {
			val outputDir = File(cacheDir, "cropped_images")
			if (!outputDir.exists()) {
				outputDir.mkdirs()
			}
			
			val outputFile = File(outputDir, "cropped_${System.currentTimeMillis()}.jpg")
			val outputStream = FileOutputStream(outputFile)
			
			bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
			outputStream.flush()
			outputStream.close()
			
			// Use FileProvider for Android 10+ compatibility
			FileProvider.getUriForFile(
				this,
				"${packageName}.fileprovider",
				outputFile
			)
		} catch (e: Exception) {
			e.printStackTrace()
			// Fallback to fromFile if FileProvider fails
			try {
				val outputDir = File(cacheDir, "cropped_images")
				if (!outputDir.exists()) {
					outputDir.mkdirs()
				}
				val outputFile = File(outputDir, "cropped_${System.currentTimeMillis()}.jpg")
				val outputStream = FileOutputStream(outputFile)
				bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
				outputStream.flush()
				outputStream.close()
				Uri.fromFile(outputFile)
			} catch (e2: Exception) {
				e2.printStackTrace()
				null
			}
		}
	}
}