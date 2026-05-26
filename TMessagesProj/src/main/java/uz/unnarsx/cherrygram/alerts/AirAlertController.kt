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
        if (CherrygramCoreConfig.airAlertEnabled && CherrygramCoreConfig.airAlertApiKey.isNotEmpty()) {
            startMonitoring()
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
        val apiKey = CherrygramCoreConfig.airAlertApiKey
        val regionId = CherrygramCoreConfig.airAlertRegionId
        if (apiKey.isEmpty() || regionId.isEmpty()) return

        try {
            // Using alerts.in.ua as it's often more developer friendly for quick checks
            // But user requested Option A (Ajax), so I will use api.ukrainealarm.com
            val url = URL("https://api.ukrainealarm.com/api/v3/alerts/$regionId")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Authorization", apiKey)
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(response)
                
                // If there are any active alerts in the array, it means alert is ON
                var hasAirAlert = false
                for (i in 0 until jsonArray.length()) {
                    val region = jsonArray.getJSONObject(i)
                    val activeAlerts = region.getJSONArray("activeAlerts")
                    for (j in 0 until activeAlerts.length()) {
                        val alert = activeAlerts.getJSONObject(j)
                        if (alert.getString("type") == "AIR") {
                            hasAirAlert = true
                            break
                        }
                    }
                    if (hasAirAlert) break
                }
                
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
                val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                mediaPlayer = MediaPlayer.create(org.telegram.messenger.ApplicationLoader.applicationContext, notification)
                mediaPlayer?.isLooping = true
                mediaPlayer?.start()
                
                AndroidUtilities.runOnUIThread({
                    mediaPlayer?.stop()
                    mediaPlayer?.release()
                    mediaPlayer = null
                }, 15000)
            } else {
                // Short beep for end
                val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                mediaPlayer = MediaPlayer.create(org.telegram.messenger.ApplicationLoader.applicationContext, notification)
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
        Thread {
            try {
                val url = URL("https://api.ukrainealarm.com/api/v3/regions")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Authorization", apiKey)
                
                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonArray = JSONArray(response)
                    val regions = mutableListOf<Pair<String, String>>()
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val id = obj.getString("regionId")
                        val name = obj.getString("regionName")
                        regions.add(id to name)
                        
                        // Also add children (cities)
                        if (obj.has("regionChildIds")) {
                            val children = obj.getJSONArray("regionChildIds")
                            for (j in 0 until children.length()) {
                                val child = children.getJSONObject(j)
                                regions.add(child.getString("regionId") to "  — " + child.getString("regionName"))
                            }
                        }
                    }
                    AndroidUtilities.runOnUIThread { callback(regions) }
                }
            } catch (e: Exception) {
                FileLog.e(e)
                AndroidUtilities.runOnUIThread { callback(emptyList()) }
            }
        }.start()
    }
}
