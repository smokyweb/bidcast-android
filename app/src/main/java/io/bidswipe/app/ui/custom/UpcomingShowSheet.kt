package io.bidswipe.app.ui.custom

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import io.bidswipe.app.databinding.UpcomingShowSheetBinding
import io.bidswipe.app.utils.clr
import io.bidswipe.app.utils.loadUrl
import io.bidswipe.app.utils.draw
import io.bidswipe.app.utils.layout
import io.bidswipe.app.utils.runSafe
import io.bidswipe.app.utils.setHapticClickListener
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * BottomSheetDialog shown when a user taps an upcoming (scheduled) show
 * on the Home or Explore feed. Mirrors the iOS UpcomingBottomSheet design:
 *   – seller avatar + @username in header with close X
 *   – "Show starts on [Date] at [Time]" centred message
 *   – "Okay" primary button
 *
 * [Basecamp #9930403446]
 */
@SuppressLint("InflateParams")
class UpcomingShowSheet(
    private val mCtx: Context,
    private val profileImageUrl: String?,
    private val username: String?,
    /** Raw date string from API – expected formats: "yyyy-MM-dd", "MM/dd/yyyy", "dd-MM-yyyy" */
    private val showDate: String?,
    /** Raw time string from API – expected formats: "HH:mm:ss", "HH:mm", "h:mm a" */
    private val showTime: String?,
) : BottomSheetDialog(mCtx) {

    private val bind = UpcomingShowSheetBinding.bind(
        LayoutInflater.from(mCtx).inflate(layout.upcoming_show_sheet, null, false)
    )

    init {
        setContentView(bind.root)
        setCancelable(true)
        setCanceledOnTouchOutside(true)

        runSafe {
            window?.navigationBarColor = ContextCompat.getColor(mCtx, clr.onPrimary)
            window?.setDimAmount(0.6f)
        }

        // User avatar
        bind.userImage.loadUrl(mCtx, profileImageUrl ?: "", draw.placeholder_user)

        // @username
        val displayName = username?.let {
            if (it.startsWith("@")) it else "@$it"
        } ?: "@user"
        bind.userName.text = displayName

        // Formatted date + time
        bind.dateTimeText.text = buildDateTimeMessage(showDate, showTime)

        // Close
        bind.close.setHapticClickListener { dismiss() }
        bind.okayBtn.setHapticClickListener { dismiss() }
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private fun buildDateTimeMessage(rawDate: String?, rawTime: String?): String {
        val formatted = "${formatDate(rawDate)} at ${formatTime(rawTime)}"
        return "Show starts on $formatted"
    }

    /**
     * Tries multiple common date input formats the API may return.
     * Outputs "MMM d, yyyy" (e.g. "Dec 25, 2025").
     */
    private fun formatDate(raw: String?): String {
        val d = raw?.trim() ?: return "TBD"
        val inputFormats = listOf("yyyy-MM-dd", "MM/dd/yyyy", "dd-MM-yyyy", "MM-dd-yyyy")
        val outputFmt = SimpleDateFormat("MMM d, yyyy", Locale.US)
        for (fmt in inputFormats) {
            runCatching {
                val inFmt = SimpleDateFormat(fmt, Locale.US)
                inFmt.isLenient = false
                return outputFmt.format(inFmt.parse(d)!!)
            }
        }
        return d.ifEmpty { "TBD" }
    }

    /**
     * Tries multiple common time input formats the API may return.
     * Outputs "h:mm a" (e.g. "8:00 PM").
     */
    private fun formatTime(raw: String?): String {
        val t = raw?.trim() ?: return "--:-- --"
        val inputFormats = listOf("HH:mm:ss", "HH:mm", "h:mm a", "h:mm:ss a", "H:mm")
        val outputFmt = SimpleDateFormat("h:mm a", Locale.US)
        for (fmt in inputFormats) {
            runCatching {
                val inFmt = SimpleDateFormat(fmt, Locale.US)
                inFmt.isLenient = false
                return outputFmt.format(inFmt.parse(t)!!)
            }
        }
        return t.ifEmpty { "--:-- --" }
    }
}
