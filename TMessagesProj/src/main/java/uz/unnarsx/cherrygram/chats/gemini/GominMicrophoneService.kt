package uz.unnarsx.cherrygram.chats.gemini

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.R

/**
 * Minimal foreground service that keeps the microphone "green dot" indicator
 * active while Gomin Live voice session is running (Android 14+).
 *
 * Started / stopped from [GominLiveManager].
 *
 * Thread-safe singleton pattern: [start]/[stop] access [instance] under [serviceLock]
 * to prevent the TOCTOU race between startForegroundService() (async) and
 * onCreate()/onDestroy() (main thread).
 */
class GominMicrophoneService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 4001
        private const val CHANNEL_ID = "gomin_mic_channel"
        private val serviceLock = Any()

        @Volatile
        private var instance: GominMicrophoneService? = null

        fun start() {
            synchronized(serviceLock) {
                if (instance != null) return
            }
            try {
                val intent = Intent(ApplicationLoader.applicationContext, GominMicrophoneService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ApplicationLoader.applicationContext.startForegroundService(intent)
                } else {
                    ApplicationLoader.applicationContext.startService(intent)
                }
            } catch (e: Exception) {
                android.util.Log.e("GominMicrophoneService", "Failed to start service", e)
            }
        }

        fun stop() {
            val srv: GominMicrophoneService?
            synchronized(serviceLock) {
                srv = instance
            }
            srv?.stopSelf()
        }
    }

    override fun onCreate() {
        super.onCreate()
        synchronized(serviceLock) { instance = this }
        ensureNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildNotification()
        val fgsType: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        } else {
            0
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                startForeground(NOTIFICATION_ID, notification, fgsType)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            android.util.Log.e("GominMicrophoneService", "startForeground failed", e)
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        synchronized(serviceLock) { instance = null }
        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (e: Exception) {
            android.util.Log.e("GominMicrophoneService", "stopForeground failed", e)
        }
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val existing = nm.getNotificationChannel(CHANNEL_ID)
        if (existing == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Gomin Live мікрофон",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
                setSound(null, null)
                description = "Працює мікрофон для голосового спілкування з Gemini"
            }
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.voicechat_active)
            .setContentTitle("Gomin Live")
            .setContentText("🎙️ Мікрофон активовано")
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
