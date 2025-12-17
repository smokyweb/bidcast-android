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
import io.bidswipe.app.ui.agoraStream.AgoraPublisherActivity
import io.bidswipe.app.ui.dashboard.ChatActivity
import io.bidswipe.app.ui.dashboard.DashActivity
import io.bidswipe.app.utils.Alerts
import io.bidswipe.app.utils.Prefs
import io.bidswipe.app.utils.Utils
import io.bidswipe.app.utils.asCapital
import io.bidswipe.app.utils.asHtml
import io.bidswipe.app.utils.string

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        const val TAG = "FIREBASE-SERVICE"
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
                val intent = when (type) {
                    "message" -> {
                        PendingIntent.getActivity(
                            mCtx,
                            1,
                            Intent(mCtx, ChatActivity::class.java).apply {
                                putExtra("id", data["sender_id"]?:"")
                                putExtra("name",   data["sender_name"] ?: "")
                                putExtra("image",   data["sender_imagee"] ?: "")
                            },
                            flag
                        )
                    }

                    "Live Room Started" -> {
                        PendingIntent.getActivity(
                            mCtx,
                            0,
                            Intent(mCtx, AgoraPublisherActivity::class.java).putExtra(
                                "showId",
                                ""
                            ),
                            flag)
                    }

                    else -> {
                        PendingIntent.getActivity(
                            mCtx,
                            0,
                            Intent(applicationContext, DashActivity::class.java),
                            flag
                        )
                    }
                }

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