package uz.unnarsx.cherrygram.alerts

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.R

object AirAlertNotificationHelper {

    private const val CHANNEL_CRITICAL_ID = "air_alert_critical"
    private const val CHANNEL_INFO_ID = "air_alert_info"
    private const val NOTIFICATION_ID = 1001

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Канал для ТРИВОГИ (Гучний, з сиреною)
            val sirenUri = Uri.parse("android.resource://${context.packageName}/${R.raw.gomin_siren}")
            val criticalChannel = NotificationChannel(
                CHANNEL_CRITICAL_ID,
                "Повітряна тривога (Критичні)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Сповіщення про початок повітряної тривоги"
                setSound(sirenUri, AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build())
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            // Канал для ВІДБОЮ (Звичайний)
            val infoChannel = NotificationChannel(
                CHANNEL_INFO_ID,
                "Повітряна тривога (Інфо)",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Сповіщення про відбій тривоги"
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(criticalChannel)
            notificationManager.createNotificationChannel(infoChannel)
        }
    }

    fun showStartNotification(context: Context, title: String, body: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Інтент для відкриття додатку
        val contentIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.let {
            PendingIntent.getActivity(context, 0, it, PendingIntent.FLAG_IMMUTABLE)
        }

        // Інтент для кнопки "Зупинити сирену"
        val stopIntent = Intent(context, AirAlertStopReceiver::class.java).apply {
            action = "STOP_SIREN"
        }
        val stopPendingIntent = PendingIntent.getBroadcast(context, 1, stopIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val builder = NotificationCompat.Builder(context, CHANNEL_CRITICAL_ID)
            .setSmallIcon(R.mipmap.icon_launcher_cherry) // Використовуємо іконку додатку
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false)
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .addAction(0, "ЗУПИНИТИ СИРЕНУ", stopPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(contentIntent, true) // Допомагає розбудити телефон

        val notification = builder.build()
        notification.flags = notification.flags or Notification.FLAG_INSISTENT // Повторювати звук

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    fun showEndNotification(context: Context, title: String, body: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID) // Прибираємо сповіщення про тривогу

        val builder = NotificationCompat.Builder(context, CHANNEL_INFO_ID)
            .setSmallIcon(R.mipmap.icon_launcher_cherry)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        notificationManager.notify(NOTIFICATION_ID + 1, builder.build())
    }

    fun cancelAll(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }
}
