package io.bidswipe.app.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.LightingColorFilter
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.google.android.material.chip.Chip
import io.bidswipe.app.R
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import java.util.regex.Matcher
import java.util.regex.Pattern


object Utils {

    val timezone get() = TimeZone.getDefault().id.toString()

    val currentTimeInFormat get() = getSimpleDate("yyyy-MM-dd_HH:mm:ss_a").format(timestamp()).toString()

    fun timestamp() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Date().toInstant().epochSecond
    } else {
        System.currentTimeMillis() / 1000L
    }

    /*fun getAlarmChannel() =
        NotificationChannel(Const.ALARM_CHANNEL_ID , Const.ALARM_CHANNEL_NAME , NotificationManager.IMPORTANCE_HIGH).also {
            it.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            it.enableVibration(true)
            it.enableLights(false)
        }*/

    /*fun notificationChannel() = NotificationChannel(Const.CHANNEL_ID , Const.CHANNEL_NAME , NotificationManager.IMPORTANCE_HIGH).also {
        it.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        it.enableVibration(true)
        it.enableLights(false)
    }*/

    fun getTimeFromTimestamp(millis: Long, format: String = "hh:mm a") = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        getSimpleDate(format).format(Instant.ofEpochSecond(millis).toEpochMilli()).toString()
    } else {
        getSimpleDate(format).format(millis / 1000).toString()
    }

    fun getFormattedDateTime(inFormat: String, outFormat: String, timestamp: String): String? {
        return try {
            getSimpleDate(inFormat).parse(timestamp)?.let { getSimpleDate(outFormat).format(it) }
        } catch (e: Exception) {
            e.printStackTrace()
            timestamp
        }
    }

    fun getTimeStampFromServerTime(time: String): Long {
        val inputFormat = SimpleDateFormat(Const.SERVER_TIME_FORMAT)
        inputFormat.timeZone = TimeZone.getTimeZone("UTC")
        val date = inputFormat.parse(time)
        return date?.time ?: timestamp()
    }

    fun getNotifBuilder(ctx: Context, title: String, msg: String) = NotificationCompat.Builder(ctx, Const.CHANNEL_ID).apply {
        color = ContextCompat.getColor(ctx, clr.primary)
        setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL)
        priority = NotificationCompat.PRIORITY_HIGH
        setDefaults(NotificationCompat.DEFAULT_ALL)
        setSmallIcon(draw.app_icon)
        setContentTitle(title.asCapital())
        setContentText(msg.asCapital())
        setAutoCancel(true)
        setColorized(true)
    }

    fun pxFromDp(context: Context, dp: Float): Float {
        return dp * context.resources.displayMetrics.density
    }

    private fun getSimpleDate(format: String) = SimpleDateFormat(format, Locale.getDefault())

    fun imagePart(param: String, name: String, file: File) =
        MultipartBody.Part.Companion.createFormData(param, name, file.asRequestBody("image/jpeg".toMediaTypeOrNull()))

    fun initCrop(mCtx: Context, isCamera: Boolean = false, isGallery: Boolean = false) = CropImageContractOptions(
        null, CropImageOptions(
            activityBackgroundColor = ContextCompat.getColor(mCtx, clr.background),
            toolbarBackButtonColor = ContextCompat.getColor(mCtx, clr.onSurface),
            toolbarColor = ContextCompat.getColor(mCtx, clr.surface),
            activityMenuTextColor = ContextCompat.getColor(mCtx, clr.onSurface),
            activityMenuIconColor = ContextCompat.getColor(mCtx, clr.onSurface),
            toolbarTitleColor = ContextCompat.getColor(mCtx, clr.onSurface),
            borderCornerColor = ContextCompat.getColor(mCtx, clr.primary),
            borderLineColor = ContextCompat.getColor(mCtx, clr.primary),
            guidelinesColor = ContextCompat.getColor(mCtx, clr.primary),
            outputCompressFormat = Bitmap.CompressFormat.JPEG,
            guidelines = CropImageView.Guidelines.ON,
            imageSourceIncludeGallery = isGallery,
            imageSourceIncludeCamera = isCamera,
            cropMenuCropButtonTitle = "Done",
            outputCompressQuality = 70,
        )
    )

    fun getTimeAgo(time: String): String {

        val serverTime = time.ifEmpty { getSimpleDate(Const.SERVER_TIME_FORMAT).format(timestamp()).toString() }
        val timeInMillis = getTimeStampFromServerTime(serverTime)
        val now = System.currentTimeMillis()

        val diff = now - timeInMillis
        return when {
            diff < TimeUnit.MINUTES.toMillis(1) -> {
                val seconds = TimeUnit.MILLISECONDS.toSeconds(diff)
                "$seconds seconds ago"
            }

            diff < TimeUnit.HOURS.toMillis(1) -> {
                val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
                "$minutes minutes ago"
            }

            diff < TimeUnit.DAYS.toMillis(1) -> {
                val hours = TimeUnit.MILLISECONDS.toHours(diff)
                "$hours hours ago"
            }

            diff < TimeUnit.DAYS.toMillis(30) -> {
                val days = TimeUnit.MILLISECONDS.toDays(diff)
                "$days days ago"
            }

            diff < TimeUnit.DAYS.toMillis(365) -> {
                val months = diff / (30L * TimeUnit.DAYS.toMillis(1))
                "$months months ago"
            }

            else -> {
                val years = diff / TimeUnit.DAYS.toMillis(365)
                "$years years ago"
            }
        }

    }

    fun convertWeatherTimeINFormat(format: String, timestamp: Long): String {
        val sff = SimpleDateFormat(format, Locale.getDefault())
        return sff.format((timestamp * 1000L))
    }

    fun changeBitmapColor(sourceBitmap: Bitmap, color: Int): Bitmap {
        val resultBitmap = Bitmap.createBitmap(sourceBitmap, 0, 0, sourceBitmap.width - 1, sourceBitmap.height - 1)
        val p = Paint()
        val filter: ColorFilter = LightingColorFilter(color, 1)
        p.setColorFilter(filter)
        val canvas = Canvas(resultBitmap)
        canvas.drawBitmap(resultBitmap, 0f, 0f, p)
        return resultBitmap
    }

    fun changeBitmapColor(originalBitmap: Bitmap): Bitmap {
        // Create a mutable copy of the original bitmap
        val modifiedBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)

        // Loop through all the pixels in the bitmap
        for (x in 0 until modifiedBitmap.width) {
            for (y in 0 until modifiedBitmap.height) {
                // Get the color of the pixel at (x, y)
                val pixelColor = modifiedBitmap.getPixel(x, y)

                // Modify the pixel color (change it to red in this case)
//				if (pixelColor == Color.WHITE) {  // Check if the pixel is white
                modifiedBitmap.setPixel(x, y, Color.RED) // Set pixel to red
//				}
            }
        }
        return modifiedBitmap
    }

    fun hexToColor(color: String?): Int {
        return try {
            Color.parseColor(color ?: "#E31E25")
        } catch (e: Exception) {
            Color.parseColor("#E31E25")
        }
    }

    fun createTempFileFromInputStream(mCtx: Context, inputStream: InputStream?, uri: Uri): File {
        val fileName = getFileNameFromUri(uri, mCtx)
        val tempFile = File(mCtx.cacheDir, fileName)
        FileOutputStream(tempFile).use { outputStream ->
            inputStream?.copyTo(outputStream)
        }
        return tempFile
    }

    private fun getFileNameFromUri(uri: Uri, mCtx: Context): String {
        var fileName = "temp_file"
        val cursor = mCtx.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    fileName = it.getString(nameIndex)
                }
            }
        }
        return fileName
    }

    fun makeAChip(mCtx: Context, text: String, selected: Boolean) =
        Chip(mCtx, null, R.attr.entryChipStyleNew).apply {
            setText(text)
            id = text.hashCode()
            isClickable = true
            isCheckable = true
            chipCornerRadius=mCtx.resources.dpToPx(50).toFloat()
            chipStrokeWidth = mCtx.resources.dpToPx(2).toFloat()
            chipStartPadding=mCtx.resources.dpToPx(18).toFloat()
            chipEndPadding = mCtx.resources.dpToPx(18).toFloat()
            chipMinHeight= mCtx.resources.dpToPx(44).toFloat()
            isChecked = selected // Set the checked state
            isCheckedIconVisible = false
        }

    fun validateEmail(email: String?): Boolean {
        val emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
        val pattern: Pattern = Pattern.compile(emailRegex)
        val matcher: Matcher = pattern.matcher(email)

        return matcher.matches()
    }
}