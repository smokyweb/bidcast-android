package io.bidswipe.app.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.LightingColorFilter
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.get
import androidx.core.graphics.set
import androidx.core.graphics.toColorInt
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.google.android.material.chip.Chip
import com.google.android.material.shape.ShapeAppearanceModel
import io.bidswipe.app.R
import io.bidswipe.app.utils.cropper.CropOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import java.util.regex.Matcher
import java.util.regex.Pattern

object Utils {

    val timezone get() = TimeZone.getDefault().id.toString()

    val currentTimeInFormat
        get() = getSimpleDate("yyyy-MM-dd_HH:mm:ss_a").format(timestamp()).toString()

    fun timestamp() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Date().toInstant().epochSecond
    } else {
        System.currentTimeMillis() / 1000L
    }

    fun getTimeFromServerTimestamp(millis: Long, format: String = "hh:mm a"): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val formatter = getSimpleDate(format)
            formatter.format(Instant.ofEpochMilli(millis).toEpochMilli()).toString()
        } else {
            val formatter = getSimpleDate(format)
            formatter.format(millis).toString()
        }
    }

    fun getTimeFromTimestamp(millis: Long, format: String = "hh:mm a") =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSimpleDate(format).format(Instant.ofEpochSecond(millis).toEpochMilli()).toString()
        } else {
            getSimpleDate(format).format(millis / 1000).toString()
        }

    fun getFormattedDateTime(inFormat: String, outFormat: String, timestamp: String): String? {
        if (timestamp.isEmpty()) return "N/A"
        return try {
            getSimpleDate(inFormat).parse(timestamp)?.let { getSimpleDate(outFormat).format(it) }
        } catch (e: Exception) {
            e.printStackTrace()
            timestamp
        }
    }

    fun initCrop(mCtx: Context, isCamera: Boolean = false, isGallery: Boolean = false) =
        CropOptions(
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

    fun getTimeStampFromServerTime(
        time: String,
        format: String = Const.SERVER_TIME_FORMAT,
        timeZone: String? = "UTC",
        outTimeZone: String = "UTC"
    ): Long {

        val inputFormat = getSimpleDate(format)
        inputFormat.timeZone = TimeZone.getTimeZone(timeZone)

        try {
            val parsedDate = inputFormat.parse(time) ?: return System.currentTimeMillis()

            val outputFormat = getSimpleDate(format)
            outputFormat.timeZone = TimeZone.getTimeZone(outTimeZone)

            return outputFormat.format(parsedDate).let {
                outputFormat.parse(it)?.time ?: System.currentTimeMillis()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return System.currentTimeMillis()
        }
    }

    fun getDateFromTimestamp(millis: Long) = getSimpleDate("dd-MM-yyyy")
        .format(millis).toString()

    fun getNotifBuilder(ctx: Context, title: String, msg: String) =
        NotificationCompat.Builder(ctx, Const.CHANNEL_ID).apply {
            color = ContextCompat.getColor(ctx, clr.primary)
            setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL)
            priority = NotificationCompat.PRIORITY_HIGH
            setDefaults(NotificationCompat.DEFAULT_ALL)
            setSmallIcon(draw.notification_icon)
            setContentTitle(title.asCapital())
            setContentText(msg.asCapital())
            setAutoCancel(true)
            setColorized(true)
        }

    fun pxFromDp(context: Context, dp: Float): Float {
        return dp * context.resources.displayMetrics.density
    }

    fun getSimpleDate(format: String) = SimpleDateFormat(format, Locale.getDefault())

    fun imagePart(param: String, name: String, file: File) =
        MultipartBody.Part.createFormData(
            param,
            name,
            file.asRequestBody("*/*".toMediaTypeOrNull())
        )

    fun getTimeAgo(time: String, format: String = Const.SERVER_TIME_FORMAT): String {

        val serverTime = time.ifEmpty { getSimpleDate(format).format(timestamp()).toString() }
        val timeInMillis = getTimeStampFromServerTime(serverTime, format)
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
        val resultBitmap =
            Bitmap.createBitmap(sourceBitmap, 0, 0, sourceBitmap.width - 1, sourceBitmap.height - 1)
        val p = Paint()
        val filter: ColorFilter = LightingColorFilter(color, 1)
        p.colorFilter = filter
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
                modifiedBitmap[x, y]

                // Modify the pixel color (change it to red in this case)
//				if (pixelColor == Color.WHITE) {  // Check if the pixel is white
                modifiedBitmap[x, y] = Color.RED // Set pixel to red
//				}
            }
        }
        return modifiedBitmap
    }

    fun hexToColor(color: String?): Int {
        return try {
            (color ?: "#E31E25").toColorInt()
        } catch (e: Exception) {
            "#E31E25".toColorInt()
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

    fun makeAChip(
        mCtx: Context,
        text: String,
        selected: Boolean,
        closeIconVisible: Boolean,
        minHeight: Int = 36,
        chipPadding: Int = 12,
        strokeWidth: Int = 0,
        chipId: Int? = null,
        iconRes: Int? = null
    ) =
        Chip(mCtx, null, R.attr.entryChipStyleNew).apply {
            setText(text)
            id = chipId ?: text.hashCode()
            isClickable = true
            isCheckable = true
            isCloseIconVisible = closeIconVisible
            closeIconTint = ContextCompat.getColorStateList(mCtx, clr.error)
//			chipCornerRadius = mCtx.resources.dpToPx(8).toFloat()
            shapeAppearanceModel = ShapeAppearanceModel.builder().also {
                it.setAllCornerSizes(ShapeAppearanceModel.PILL)
            }.build()
            chipStrokeWidth = mCtx.resources.dpToPx(strokeWidth).toFloat()
            chipStartPadding = mCtx.resources.dpToPx(chipPadding).toFloat()
            chipEndPadding = mCtx.resources.dpToPx(chipPadding).toFloat()
            chipMinHeight = mCtx.resources.dpToPx(minHeight).toFloat()
            isChecked = selected // Set the checked state
            isCheckedIconVisible = false

            iconRes?.let {
                chipIcon = ContextCompat.getDrawable(mCtx, it)
                isChipIconVisible = true
            }
        }

    fun validateEmail(email: String?): Boolean {
        val emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
        val pattern: Pattern = Pattern.compile(emailRegex)
        val matcher: Matcher = pattern.matcher(email)

        return matcher.matches()
    }

    fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        return cm.activeNetworkInfo?.isConnectedOrConnecting == true
    }

    fun notificationChannel() = NotificationChannel(
        Const.CHANNEL_ID,
        Const.CHANNEL_NAME,
        NotificationManager.IMPORTANCE_HIGH
    ).also {
        it.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        it.enableVibration(true)
        it.enableLights(false)
    }

    fun generateTextDrawable(mCtx: Context, text: String): Drawable {
        val textSize = 40f
        val bgColor = Color.LTGRAY
        val textColor = Color.WHITE

        val paint = Paint()
        paint.textSize = textSize
        paint.color = textColor
        paint.textAlign = Paint.Align.CENTER

        // Create a bitmap to draw text on
        val width = 100
        val height = 100
        val bitmap = createBitmap(width, height)
        val canvas = Canvas(bitmap)

        // Fill the canvas with the background color
        canvas.drawColor(bgColor)

        // Draw the first letter of the user's name
        canvas.drawText(text, width / 2f, height / 2f - (paint.descent() + paint.ascent()) / 2, paint)

        return bitmap.toDrawable(mCtx.resources)
    }

    fun saveImageFromUrlToCache(mCtx: Context, url: String?, callback: (uri: Uri?) -> Unit) {
        if (url.isNullOrEmpty()) return callback(null)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL(url)
                val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
                connection.connect()

                val inputStream: InputStream = connection.inputStream
                val bitmap: Bitmap = BitmapFactory.decodeStream(inputStream)

                val cacheDir: File = mCtx.cacheDir

                val imageFile = File(cacheDir, "cached_image_${System.currentTimeMillis()}.jpg")

                val outputStream = FileOutputStream(imageFile)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                outputStream.flush()
                outputStream.close()

                val uri = FileProvider.getUriForFile(
                    mCtx,
                    "${mCtx.packageName}.provider",
                    imageFile
                )

                withContext(Dispatchers.Main) {
                    callback(uri)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback(null)
                }
                e.printStackTrace()
            }
        }
    }

}