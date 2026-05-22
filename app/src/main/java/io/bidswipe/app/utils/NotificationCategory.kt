package io.bidswipe.app.utils

import io.bidswipe.app.R

/**
 * Notification category taxonomy for the Bidcast Notifications screen.
 *
 * The backend currently emits notifications with a `type` string that is
 * inconsistent across flows (mix of snake_case + Title Case). This enum
 * collapses those raw type strings into the 5 user-visible categories Trey
 * requested for the Notifications page:
 *
 *  - MESSAGES       (chat / DM)
 *  - VERIFICATION   (seller identity / profile verification)
 *  - ORDERS         (order placed, confirmed, status changed, payment status)
 *  - BIDS           (bid placed, bid won)
 *  - PURCHASES      (sold / purchase / order_confirmed on the buyer side)
 *
 * Anything that doesn't match falls back to [OTHER] so the row still renders
 * with a neutral bell icon — never hidden, never crashed.
 *
 * Task: cmph7xsgt00fzms8pg83na07c
 */
enum class NotificationCategory(val labelRes: Int, val iconRes: Int) {
    MESSAGES(R.string.notif_cat_messages, R.drawable.ic_outlined_message),
    VERIFICATION(R.string.notif_cat_verification, R.drawable.ic_identity_verification),
    ORDERS(R.string.notif_cat_orders, R.drawable.ic_order),
    BIDS(R.string.notif_cat_bids, R.drawable.ic_hammer),
    PURCHASES(R.string.notif_cat_purchases, R.drawable.ic_shop),
    OTHER(R.string.notif_cat_other, R.drawable.notification);

    companion object {
        /**
         * Normalize a raw backend `type` string into a [NotificationCategory].
         *
         * Matching is case-insensitive and tolerates the snake_case ↔ Title Case
         * inconsistency that exists on the Laravel side. Keep this list in sync
         * with the backend if/when new notification types are added.
         */
        fun fromRawType(raw: String?): NotificationCategory {
            val key = (raw ?: "").trim().lowercase().replace(' ', '_')
            return when (key) {
                "message", "chat", "new_message" -> MESSAGES

                "seller_verification", "verification", "profile_verification",
                "identity_verification", "kyc" -> VERIFICATION

                "order_placed", "order_confirmed",
                "order_status_update", "payment_status_update",
                "new_order", "processing", "completed", "order" -> ORDERS

                "bid_placed", "bid", "bid_won", "bid_received",
                "bid_show_start", "bid_show_end" -> BIDS

                "sold", "purchase", "purchased" -> PURCHASES

                else -> OTHER
            }
        }
    }
}
