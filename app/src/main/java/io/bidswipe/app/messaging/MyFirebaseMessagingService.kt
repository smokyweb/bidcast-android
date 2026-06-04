package io.bidswipe.app.messaging

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import io.bidswipe.app.App
import io.bidswipe.app.model.StreamModel
import io.bidswipe.app.ui.cohost.CoHostJoinActivity
import io.bidswipe.app.ui.dashboard.ChatActivity
import io.bidswipe.app.ui.dashboard.DashActivity
import io.bidswipe.app.ui.more.NotificationActivity
import io.bidswipe.app.ui.product.OrderStatusActivity
import io.bidswipe.app.ui.watchStream.ViewLiveShowActivity
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Prefs
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.asHtml
import io.bidswipe.app.utils.string

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        const val TAG = "FIREBASE-SERVICE"
        /** Intent extras used to carry the push-tab destination to DashActivity. */
        const val EXTRA_PUSH_TAB      = "push_tab"
        const val EXTRA_ACTIVITY_TAB  = "activity_tab_index"
    }

    private val nManager by lazy { getSystemService(NOTIFICATION_SERVICE) as NotificationManager }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Alerts.log(TAG, "NEW FCM TOKEN => $token")
        Prefs(applicationContext).putString(Prefs.PUSH_TOKEN, token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        Alerts.log(TAG, "NOTIFY DATA : " + message.data)
        Alerts.log(TAG, "NOTIFY NOTIFICATION : " + message.notification?.title)

        createNotification(message.data, message.notification)
    }

    /*private fun getPostImage(data : Map<String , String> , mNotify : RemoteMessage.Notification?) {
        if (mNotify?.imageUrl != null || data["type"] == Const.NOTIF_CHAT) {

            val imgUrl = if (data["type"] == Const.NOTIF_CHAT) JSONObject(data["sender"]).getString("sender_image").toString() else mNotify?.imageUrl.toString()

            applicationContext.getBitmapFromUrl(imgUrl) {
                createNotification(data , mNotify , it)
            }
        }
        else {
            createNotification(data , mNotify)
        }
    }*/

    private fun createNotification(
        data: Map<String, String>,
        mNotification: RemoteMessage.Notification?,
        postImage: Bitmap? = null,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Alerts.log(TAG, "NOTIFICATION PERMISSION NOT GRANTED")
            return
        } else {
            val mCtx = applicationContext

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                nManager.createNotificationChannel(Utils.notificationChannel())
            }
            val type = data["type"] ?: ""

            val flag = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val title = if (type == "message") {
                data["sender_name"] ?: ""
            } else {
                (mNotification?.title ?: getString(string.app_name)).asHtml().asCapital()
            }
            val message =
                (mNotification?.body ?: getString(string.app_name)).asHtml().asCapital()
            val notifyId = kotlin.random.Random.nextInt(8)

            val notify = try {
                if (postImage != null) {
                    NotificationCompat.BigPictureStyle().also {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            it.showBigPictureWhenCollapsed(false)
                        }
                        it.setBigContentTitle(title.asCapital())
                        it.setSummaryText(message)
                        it.bigPicture(postImage)
                    }
                } else {
                    NotificationCompat.BigTextStyle().setBigContentTitle(title.asCapital())
                        .bigText(message)
                }

                val type = data["type"] ?: ""
                // #9960387225 — push notification tap-through: each type must open
                // its actual content screen, not the generic notification inbox.
                val intent = buildPushIntent(mCtx, type, data, flag)

                Utils.getNotifBuilder(mCtx, title.asCapital(), message).apply {
                    setCategory(NotificationCompat.CATEGORY_EVENT)
                    setContentIntent(intent)
                    setStyle(
                        NotificationCompat.BigTextStyle().setBigContentTitle(title).bigText(message)
                    )
                }.build()

            } catch (e: Exception) {
                e.printStackTrace()
                Utils.getNotifBuilder(mCtx, title.asCapital(), message).apply {
                    setCategory(NotificationCompat.CATEGORY_EVENT)
                }.build()
            }

            notify.let {
                val type = data["type"] ?: ""
                if (type == "message") {
                    if (App.isUserOnChatScreen) {
                        Alerts.log(TAG, "User is on chat screen - suppressing notification")
                        return
                    } else {
                        nManager.notify(notifyId.toString(), notifyId, it)
                    }
                } else {
                    nManager.notify(notifyId.toString(), notifyId, it)
                }

            }
        }
    }

    /**
     * #9960387225 — Build a PendingIntent that opens the ACTUAL content screen
     * for each notification type.  This is used by the FOREGROUND notification
     * path (onMessageReceived).  The BACKGROUND / COLD-START path is handled by
     * DashActivity.checkPushExtras() which reads the same data extras from the
     * launcher intent when FCM's system-tray notification is tapped.
     *
     * Backend data-key reference (real keys confirmed from Laravel ApiController
     * + InquiryController + bidcast-node/notification.js):
     *
     *  Live Room Started   room_id, sender_id
     *  message             sender_id, sender_name, sender_imagee
     *  inquiry_message     thread_id, sender_id
     *  bid                 bid_id, product_id, sender_id
     *  bid_won             bid_id, product_id, show_id (no room_id from backend)
     *  bid_placed          DB-only, no FCM push
     *  offer_received      offer_id, product_id, sender_id
     *  offer_accepted /    offer_id, product_id, sender_id
     *    offer_declined
     *  purchase            order_id, product_id, sender_id
     *  sold                order_id, product_id, sender_id
     *  cancellation_*      order_id, product_id, sender_id
     *  Order Status Updated DB-only, no FCM push (handled defensively)
     *  cohost_invite       cohost_invite_id, schedule_show_id, show_title
     *  credited / debited  DB/wallet only, no FCM push
     */
    private fun buildPushIntent(
        ctx: android.content.Context,
        type: String,
        data: Map<String, String>,
        flag: Int,
    ): PendingIntent {
        return when {
            // ── Direct messages (peer DM chat) ───────────────────────────────
            type == "message" -> PendingIntent.getActivity(
                ctx, 1,
                Intent(ctx, ChatActivity::class.java).apply {
                    putExtra("id",    data["sender_id"]    ?: "")
                    putExtra("name",  data["sender_name"]  ?: "")
                    putExtra("image", data["sender_imagee"] ?: "")
                },
                flag
            )

            // ── Live room: viewer lands on the WATCH screen ──────────────────
            // BUG FIX: was routing to AgoraPublisherActivity (seller/host screen)
            // with an empty showId.  Viewers must land on ViewLiveShowActivity.
            type == "Live Room Started" -> {
                val roomId = data["room_id"] ?: ""
                PendingIntent.getActivity(
                    ctx, 10,
                    Intent(ctx, ViewLiveShowActivity::class.java).apply {
                        putExtra("roomId", roomId)
                        putParcelableArrayListExtra(
                            "streamList",
                            ArrayList(listOf(StreamModel(roomId, "", thumbnail = "")))
                        )
                        this.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    },
                    flag
                )
            }

            // ── Order-related: open OrderStatusActivity with orderId ──────────
            type == "purchase" || type == "sold"
                || type == "cancellation_requested"
                || type == "cancellation_approved"
                || type == "cancellation_rejected"
                || type == "Order Status Updated" -> PendingIntent.getActivity(
                ctx, 11,
                Intent(ctx, OrderStatusActivity::class.java).apply {
                    putExtra("orderId", data["order_id"] ?: "")
                    this.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                },
                flag
            )

            // ── Bid placed on seller's item: Activity tab → Bids (index 1) ──
            // bid_won has show_id but no room_id; both route to the Activity tab
            // so the user sees the bid row.  If a live show room_id is eventually
            // added to the bid_won payload, we can route to ViewLiveShowActivity.
            type == "bid" || type == "bid_won" || type == "bid_placed" ->
                PendingIntent.getActivity(
                    ctx, 12,
                    Intent(ctx, DashActivity::class.java).apply {
                        putExtra(EXTRA_PUSH_TAB, "activity")
                        putExtra(EXTRA_ACTIVITY_TAB, 1)
                        this.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    },
                    flag
                )

            // ── Offer received / accepted / declined: Activity tab → Offers (2)
            type.startsWith("offer_") || type == "offer" ->
                PendingIntent.getActivity(
                    ctx, 13,
                    Intent(ctx, DashActivity::class.java).apply {
                        putExtra(EXTRA_PUSH_TAB, "activity")
                        putExtra(EXTRA_ACTIVITY_TAB, 2)
                        this.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    },
                    flag
                )

            // ── Co-host invite: open CoHostJoinActivity with invite context ──
            // NOTE: CoHostJoinActivity is the code-entry join screen.  There is
            // no dedicated "accept invite" screen; routing here lets the invitee
            // enter the pairing code to join the show.
            type == "cohost_invite" -> PendingIntent.getActivity(
                ctx, 14,
                Intent(ctx, CoHostJoinActivity::class.java).apply {
                    putExtra("schedule_show_id",  data["schedule_show_id"]  ?: "")
                    putExtra("cohost_invite_id",  data["cohost_invite_id"]  ?: "")
                    putExtra("show_title",         data["show_title"]        ?: "")
                    this.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                },
                flag
            )

            // ── inquiry_message: no dedicated Android thread screen yet ───────
            // FOLLOW-UP: InquiryThreadActivity does not exist on Android as of
            // #9960387225.  Routing to NotificationActivity (inbox) for now.
            // When an Android inquiry thread screen is built, route here to it
            // passing data["thread_id"].
            type == "inquiry_message" -> PendingIntent.getActivity(
                ctx, 15,
                Intent(ctx, NotificationActivity::class.java).apply {
                    this.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                },
                flag
            )

            // ── Wallet credits / debits: NotificationActivity (no wallet screen)
            type == "credited" || type == "debited" ->
                PendingIntent.getActivity(
                    ctx, 16,
                    Intent(ctx, NotificationActivity::class.java).apply {
                        this.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    },
                    flag
                )

            // ── Default fallback ──────────────────────────────────────────────
            else -> PendingIntent.getActivity(
                ctx, 0,
                Intent(ctx, NotificationActivity::class.java).apply {
                    this.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                },
                flag
            )
        }
    }

    private fun restoreChatStyle(notificationId: Int): List<NotificationCompat.MessagingStyle.Message>? {
        return nManager.activeNotifications.find { it.id == notificationId }?.notification?.let {
            NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(it)?.messages
        }
    }


    /*  override fun onMessageReceived(message: RemoteMessage) {
          super.onMessageReceived(message)

          Log.d(javaClass.simpleName, "onMessageReceived: $message")

          try {
              Alerts.log(javaClass.simpleName, "NOTIFY DATA : " + message.data)
              Alerts.log(javaClass.simpleName, "NOTIFY NOTIFICATION : " + message.notification?.body)
              showNotification(message.data)

          } catch (e: Exception) {
              e.printStackTrace()
          }
      }

      override fun onNewToken(token: String) {
          super.onNewToken(token)
          Alerts.log(javaClass.simpleName, "FCM Token $token")
          Prefs(applicationContext).putString(Prefs.PUSH_TOKEN, token)
      }

      private fun showNotification(data: Map<String, String>) {
          Alerts.log(javaClass.simpleName, "Data: $data")

          val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
              PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
          } else {
              PendingIntent.FLAG_UPDATE_CURRENT
          }

  //        val pendingIntent = PendingIntent.getActivity(applicationContext, 0, intent, flag)
          val notificationManager = NotificationManagerCompat.from(applicationContext)

          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
              notificationManager.createNotificationChannel(notificationChannel())
          }

          try {
              val notificationBuilder = getBuilder(applicationContext).apply {
                  setContentTitle(data["title"])
                  setContentText(data["body"])
  //                setContentIntent(pendingIntent)
                  setStyle(NotificationCompat.BigTextStyle())
                  setAutoCancel(true)
                  setCategory(NotificationCompat.CATEGORY_EVENT)
              }

              if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                  return
              }
              notificationManager.notify( System.currentTimeMillis().toInt(), notificationBuilder.build())
          } catch (e: Exception) {
              e.printStackTrace()
          }
      }

      @RequiresApi(Build.VERSION_CODES.O)
      private fun notificationChannel(): NotificationChannel {
          val ringUri = Settings.System.DEFAULT_NOTIFICATION_URI
          return NotificationChannel(Const.CHANNEL_ID, Const.CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
              setSound(
                  ringUri, AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                      .setLegacyStreamType(AudioManager.STREAM_NOTIFICATION)
                      .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                      .build()
              )
              lockscreenVisibility = Notification.VISIBILITY_PUBLIC
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                  setAllowBubbles(false)
              }
              enableVibration(true)
              enableLights(false)
          }
      }

      private fun getBuilder(mContext: Context): NotificationCompat.Builder {
          return NotificationCompat.Builder(mContext, Const.CHANNEL_ID).apply {
              setSmallIcon(R.drawable.app_icon)
              setAutoCancel(true)
              setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL)
              setDefaults(NotificationCompat.DEFAULT_ALL)
              priority = NotificationCompat.PRIORITY_HIGH
              setColorized(true)
  //			color = ContextCompat.getColor(mContext , R.color.primary)
          }
      }*/
}