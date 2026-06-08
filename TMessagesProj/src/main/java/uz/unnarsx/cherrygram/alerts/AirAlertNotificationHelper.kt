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

    private const val CHANNEL_CRITICAL_ID = "air_alert_critical_v2"
    private const val CHANNEL_INFO_ID = "air_alert_info_v2"
    private const val CHANNEL_SILENT_ID = "air_alert_silent_v2"
    private const val NOTIFICATION_ID = 1001

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val existingCriticalChannel = notificationManager.getNotificationChannel(CHANNEL_CRITICAL_ID)
            if (existingCriticalChannel != null) {
                // Видаляємо старі застарілі канали першої версії, якщо вони існують
                try {
                    notificationManager.deleteNotificationChannel("air_alert_critical")
                    notificationManager.deleteNotificationChannel("air_alert_info")
                    notificationManager.deleteNotificationChannel("air_alert_silent")
                } catch (e: Exception) {
                    org.telegram.messenger.FileLog.e(e)
                }
            }

            // Канал для ТРИВОГИ (Гучний, з сиреною)
            val sirenUri = Uri.parse("android.resource://${context.packageName}/raw/gomin_siren")
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

            // Канал для ВІДБОЮ (Звичайний зі звуком відбою)
            val cancelUri = Uri.parse("android.resource://${context.packageName}/raw/gomin_cancel")
            val infoChannel = NotificationChannel(
                CHANNEL_INFO_ID,
                "Повітряна тривога (Інфо)",
                NotificationManager.IMPORTANCE_HIGH // Піднімаємо пріоритет для відтворення звуку
            ).apply {
                description = "Сповіщення про відбій тривоги"
                setSound(cancelUri, AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build())
                enableVibration(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            // Канал для ТИХОЇ ТРИВОГИ (Коли сирену зупинили, але статус висить)
            val silentChannel = NotificationChannel(
                CHANNEL_SILENT_ID,
                "Повітряна тривога (Без звуку)",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Активна тривога з вимкненим звуком сирени"
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            notificationManager.createNotificationChannel(criticalChannel)
            notificationManager.createNotificationChannel(infoChannel)
            notificationManager.createNotificationChannel(silentChannel)
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

        val sirenUri = Uri.parse("android.resource://${context.packageName}/raw/gomin_siren")

        val builder = NotificationCompat.Builder(context, CHANNEL_CRITICAL_ID)
            .setSmallIcon(R.mipmap.icon_launcher_cherry) // Використовуємо іконку додатку
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSound(sirenUri) // Для сумісності з пристроями < Android 8.0
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

    fun showSilentNotification(context: Context, title: String, body: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Bug 2 Fix: Примусово скасовуємо старе сповіщення з гучного каналу, 
        // щоб миттєво зупинити відтворення системного звуку сирени/вібрації.
        notificationManager.cancel(NOTIFICATION_ID)
        
        val contentIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.let {
            PendingIntent.getActivity(context, 0, it, PendingIntent.FLAG_IMMUTABLE)
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_SILENT_ID)
            .setSmallIcon(R.mipmap.icon_launcher_cherry)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_LOW) // Низький пріоритет
            .setSound(null) // Для сумісності з пристроями < Android 8.0
            .setAutoCancel(false)
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        notificationManager.notify(NOTIFICATION_ID, builder.build()) // Перезаписує старе сповіщення 1001
    }

    fun showEndNotification(context: Context, title: String, body: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID) // Прибираємо сповіщення про тривогу

        val cancelUri = Uri.parse("android.resource://${context.packageName}/raw/gomin_cancel")

        val builder = NotificationCompat.Builder(context, CHANNEL_INFO_ID)
            .setSmallIcon(R.mipmap.icon_launcher_cherry)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Пріоритет має відповідати каналу зі звуком
            .setSound(cancelUri) // Для сумісності з пристроями < Android 8.0
            .setAutoCancel(true)

        notificationManager.notify(NOTIFICATION_ID + 1, builder.build())
    }

    fun cancelAll(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }
}
