package uz.unnarsx.cherrygram.alerts

import android.media.MediaPlayer
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.FileLog
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.Utilities
import uz.unnarsx.cherrygram.core.configs.CherrygramCoreConfig
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

object AirAlertController {

    @Volatile
    private var isAlertActive = false
    private var timer: Timer? = null
    private var mediaPlayer: MediaPlayer? = null
    private var testStopRunnable: Runnable? = null
    private var safetyStopRunnable: Runnable? = null
    @Volatile
    private var isTesting = false
    private var savedAlertState = false
    private const val SAFETY_TIMEOUT_MS = 900000L // 15 хвилин safety timeout для реальних тривог

    fun init() {
        if (CherrygramCoreConfig.airAlertEnabled) {
            startMonitoring()
            val regionId = CherrygramCoreConfig.airAlertRegionId
            if (regionId.isNotEmpty()) {
                com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic("region_$regionId")
            }
        }
    }

    fun startMonitoring() {
        stopMonitoring()
        timer = Timer()
        timer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                checkAlertStatus()
            }
        }, 0, 60000)
    }

    fun stopMonitoring() {
        timer?.cancel()
        timer = null
    }

    fun checkAlertStatus(callback: ((Boolean) -> Unit)? = null) {
        if (isTesting) {
            if (callback != null) AndroidUtilities.runOnUIThread { callback(isAlertActive) }
            return
        }

        val regionId = CherrygramCoreConfig.airAlertRegionId
        if (regionId.isEmpty()) {
            if (callback != null) AndroidUtilities.runOnUIThread { callback(false) }
            return
        }

        Utilities.globalQueue.postRunnable {
            try {
                val url = URL("http://204.168.201.148:5000/status?region_id=$regionId")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonObject = org.json.JSONObject(response)
                    val hasAirAlert = jsonObject.optBoolean("alert", false)

                    AndroidUtilities.runOnUIThread {
                        setAlertStatus(hasAirAlert)
                        callback?.invoke(hasAirAlert)
                    }
                } else {
                    AndroidUtilities.runOnUIThread { callback?.invoke(isAlertActive) }
                }
            } catch (e: Exception) {
                FileLog.e(e)
                AndroidUtilities.runOnUIThread { callback?.invoke(isAlertActive) }
            }
        }
    }

    private fun setAlertStatus(active: Boolean) {
        if (isTesting) return
        if (isAlertActive != active) {
            isAlertActive = active
            if (isAlertActive) {
                playSound(true)
                safetyStopRunnable?.let { AndroidUtilities.cancelRunOnUIThread(it) }
                val runnable = Runnable {
                    playSound(false)
                    isAlertActive = false
                    safetyStopRunnable = null
                    NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.cgAirAlertStatusChanged)
                }
                safetyStopRunnable = runnable
                AndroidUtilities.runOnUIThread(runnable, SAFETY_TIMEOUT_MS)
            } else {
                safetyStopRunnable?.let { AndroidUtilities.cancelRunOnUIThread(it) }
                safetyStopRunnable = null
                playSound(false)
            }
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.cgAirAlertStatusChanged)
        }
    }

    fun isAlertActive() = isAlertActive

    private fun playSound(isStart: Boolean) {
        try {
            mediaPlayer?.setOnCompletionListener(null)
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null

            if (isStart) {
                val soundRes = org.telegram.messenger.R.raw.gomin_siren
                val player = MediaPlayer.create(org.telegram.messenger.ApplicationLoader.applicationContext, soundRes)
                mediaPlayer = player
                player.isLooping = true
                player.start()
            } else {
                val soundRes = org.telegram.messenger.R.raw.gomin_cancel
                val player = MediaPlayer.create(org.telegram.messenger.ApplicationLoader.applicationContext, soundRes)
                mediaPlayer = player
                player.setOnCompletionListener {
                    if (mediaPlayer === player) {
                        mediaPlayer?.release()
                        mediaPlayer = null
                    } else {
                        player.release()
                    }
                }
                player.start()
            }
        } catch (e: Exception) {
            FileLog.e(e)
            mediaPlayer = null
        }
    }

    fun testAlert() {
        if (isTesting) {
            stopTest()
            return
        }

        isTesting = true
        savedAlertState = isAlertActive
        isAlertActive = true
        playSound(true)
        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.cgAirAlertStatusChanged)

        val stopRunnable = Runnable {
            stopTest()
        }
        testStopRunnable = stopRunnable
        AndroidUtilities.runOnUIThread(stopRunnable, 15000)
    }

    private fun stopTest() {
        isTesting = false
        testStopRunnable?.let { AndroidUtilities.cancelRunOnUIThread(it) }
        testStopRunnable = null
        isAlertActive = savedAlertState
        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.cgAirAlertStatusChanged)
        if (savedAlertState) {
            // Real alert була активною до тесту — сирена вже грає, просто оновлюємо safety таймер
            safetyStopRunnable?.let { AndroidUtilities.cancelRunOnUIThread(it) }
            val runnable = Runnable {
                playSound(false)
                isAlertActive = false
                safetyStopRunnable = null
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.cgAirAlertStatusChanged)
            }
            safetyStopRunnable = runnable
            AndroidUtilities.runOnUIThread(runnable, SAFETY_TIMEOUT_MS)
        } else {
            playSound(false)
            checkAlertStatus()
        }
    }

    fun fetchRegions(apiKey: String, callback: (List<Pair<String, String>>) -> Unit) {
        val regions = listOf(
            "1" to "Вінницька", "2" to "Волинська", "3" to "Дніпропетровська", "4" to "Донецька",
            "5" to "Житомирська", "6" to "Закарпатська", "7" to "Запорізька", "8" to "Івано-Франківська",
            "9" to "Київська", "10" to "Кіровоградська", "11" to "Луганська", "12" to "Львівська",
            "13" to "Миколаївська", "14" to "Одеська", "15" to "Полтавська", "16" to "Рівненська",
            "17" to "Сумська", "18" to "Тернопільська", "19" to "Харківська", "20" to "Херсонська",
            "21" to "Хмельницька", "22" to "Черкаська", "23" to "Чернівецька", "24" to "Чернігівська",
            "25" to "м. Київ", "26" to "АР Крим"
        )
        AndroidUtilities.runOnUIThread { callback(regions) }
    }

    fun handlePushStatus(alert: Boolean) {
        AndroidUtilities.runOnUIThread {
            setAlertStatus(alert)
        }
    }
}
