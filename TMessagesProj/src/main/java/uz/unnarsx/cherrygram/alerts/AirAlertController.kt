package uz.unnarsx.cherrygram.alerts

import android.media.MediaPlayer
import android.media.RingtoneManager
import org.json.JSONArray
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.FileLog
import org.telegram.messenger.NotificationCenter
import uz.unnarsx.cherrygram.core.configs.CherrygramCoreConfig
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

object AirAlertController {

    private var isAlertActive = false
    private var timer: Timer? = null
    private var mediaPlayer: MediaPlayer? = null

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
        }, 0, 60000) // Every minute
    }

    fun stopMonitoring() {
        timer?.cancel()
        timer = null
    }

    private fun checkAlertStatus() {
        val regionId = CherrygramCoreConfig.airAlertRegionId
        if (regionId.isEmpty()) return

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
                }
            }
        } catch (e: Exception) {
            FileLog.e(e)
        }
    }

    private fun setAlertStatus(active: Boolean) {
        if (isAlertActive != active) {
            isAlertActive = active
            if (isAlertActive) {
                playSound(true)
            } else {
                playSound(false)
            }
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.cgAirAlertStatusChanged)
        }
    }

    fun isAlertActive() = isAlertActive

    private fun playSound(isStart: Boolean) {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null

            if (isStart) {
                // Hardcoded 15 seconds alarm or until stopped
                val soundRes = org.telegram.messenger.R.raw.gomin_siren
                mediaPlayer = MediaPlayer.create(org.telegram.messenger.ApplicationLoader.applicationContext, soundRes)
                mediaPlayer?.isLooping = true
                mediaPlayer?.start()
                
                AndroidUtilities.runOnUIThread({
                    mediaPlayer?.stop()
                    mediaPlayer?.release()
                    mediaPlayer = null
                }, 15000)
            } else {
                // Short beep for end
                val soundRes = org.telegram.messenger.R.raw.gomin_cancel
                mediaPlayer = MediaPlayer.create(org.telegram.messenger.ApplicationLoader.applicationContext, soundRes)
                mediaPlayer?.start()
            }
        } catch (e: Exception) {
            FileLog.e(e)
        }
    }
    
    fun testAlert() {
        playSound(true)
        isAlertActive = true
        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.cgAirAlertStatusChanged)
        
        AndroidUtilities.runOnUIThread({
            playSound(false)
            isAlertActive = false
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.cgAirAlertStatusChanged)
        }, 15000)
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
