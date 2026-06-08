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
    @Volatile
    private var lastAlertTitle: String? = null
    @Volatile
    private var lastAlertBody: String? = null
    private var timer: Timer? = null
    private var mediaPlayer: MediaPlayer? = null
    private var testStopRunnable: Runnable? = null
    private var sirenStopRunnable: Runnable? = null
    private var safetyStopRunnable: Runnable? = null
    @Volatile
    private var isTesting = false
    private var savedAlertState = false
    @Volatile
    private var pendingAlertStatus: Boolean? = null
    private const val SAFETY_TIMEOUT_MS = 43200000L // 12 годин замість 15 хвилин
    private const val SIREN_DURATION_MS = 15000L

    fun init() {
        isAlertActive = CherrygramCoreConfig.airAlertLastActive

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

    private fun setAlertStatus(active: Boolean, title: String? = null, body: String? = null) {
        // Якщо повітряна тривога взагалі вимкнена в налаштуваннях — ігноруємо
        if (!CherrygramCoreConfig.airAlertEnabled) {
            isAlertActive = false
            CherrygramCoreConfig.airAlertLastActive = false
            val context = org.telegram.messenger.ApplicationLoader.applicationContext
            AirAlertNotificationHelper.cancelAll(context)
            return
        }

        if (isTesting) {
            pendingAlertStatus = active
            return
        }
        
        val changed = isAlertActive != active
        val textChanged = active && (title != null && title != lastAlertTitle || body != null && body != lastAlertBody)
        
        if (changed || textChanged) {
            isAlertActive = active
            CherrygramCoreConfig.airAlertLastActive = active
            
            val context = org.telegram.messenger.ApplicationLoader.applicationContext
            val regionName = CherrygramCoreConfig.airAlertRegionName.ifEmpty { "Ваша область" }
            
            if (isAlertActive) {
                val finalTitle = title ?: "🚨 ПОВІТРЯНА ТРИВОГА"
                val finalBody = body ?: regionName
                
                // Зберігаємо останній текст тривоги
                lastAlertTitle = finalTitle
                lastAlertBody = finalBody
                
                AirAlertNotificationHelper.showStartNotification(
                    context,
                    finalTitle,
                    finalBody
                )
                
                safetyStopRunnable?.let { AndroidUtilities.cancelRunOnUIThread(it) }
                val runnable = Runnable {
                    setAlertStatus(false) // Авто-відбій через 12 годин
                }
                safetyStopRunnable = runnable
                AndroidUtilities.runOnUIThread(runnable, SAFETY_TIMEOUT_MS)
            } else {
                safetyStopRunnable?.let { AndroidUtilities.cancelRunOnUIThread(it) }
                safetyStopRunnable = null
                
                // Прибираємо сирену тривоги та показуємо відбій
                val endTitle = title ?: "✅ ВІДБІЙ ТРИВОГИ"
                val endBody = body ?: regionName
                AirAlertNotificationHelper.showEndNotification(
                    context,
                    endTitle,
                    endBody
                )
            }
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.cgAirAlertStatusChanged)
        }
    }

    fun isAlertActive() = isAlertActive

    private fun playSound(isStart: Boolean) {
        if (isStart && !isTesting) {
            return
        }
        
        try {
            mediaPlayer?.setOnCompletionListener(null)
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null

            if (isStart) {
                val soundRes = org.telegram.messenger.R.raw.gomin_siren
                val player = MediaPlayer.create(org.telegram.messenger.ApplicationLoader.applicationContext, soundRes)
                if (player != null) {
                    mediaPlayer = player
                    player.start()
                }
            }
        } catch (e: Exception) {
            FileLog.e(e)
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    private fun stopSirenOnly() {
        try {
            mediaPlayer?.setOnCompletionListener(null)
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            FileLog.e("AirAlertController stopSirenOnly error: ${e.message}")
        } finally {
            mediaPlayer = null
        }
    }

    fun stopSiren() {
        AndroidUtilities.runOnUIThread {
            if (isTesting) {
                stopTest()
                return@runOnUIThread
            }
            
            if (isAlertActive) {
                val context = org.telegram.messenger.ApplicationLoader.applicationContext
                val title = lastAlertTitle ?: "🚨 ПОВІТРЯНА ТРИВОГА"
                val body = lastAlertBody ?: "Звук сирени вимкнено"
                AirAlertNotificationHelper.showSilentNotification(context, title, body)
            } else {
                val context = org.telegram.messenger.ApplicationLoader.applicationContext
                AirAlertNotificationHelper.cancelAll(context)
            }
        }
    }

    fun testAlert() {
        if (isTesting) {
            stopTest()
            return
        }

        isTesting = true
        savedAlertState = isAlertActive
        safetyStopRunnable?.let { AndroidUtilities.cancelRunOnUIThread(it) }
        safetyStopRunnable = null
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
        sirenStopRunnable?.let { AndroidUtilities.cancelRunOnUIThread(it) }
        sirenStopRunnable = null

        val wasAlertActiveBeforeTest = savedAlertState
        if (pendingAlertStatus != null) {
            savedAlertState = pendingAlertStatus!!
            pendingAlertStatus = null
        }

        val targetAlertState = savedAlertState
        val context = org.telegram.messenger.ApplicationLoader.applicationContext

        playSound(false) // Завжди зупиняємо тестовий MediaPlayer

        if (targetAlertState) {
            isAlertActive = false // Примусово скидаємо для виклику блоку ініціалізації тривоги
            setAlertStatus(true, lastAlertTitle, lastAlertBody)
        } else {
            if (wasAlertActiveBeforeTest) {
                isAlertActive = true // Примусово ставимо true для виклику блоку відбою
                setAlertStatus(false)
            } else {
                isAlertActive = false
                CherrygramCoreConfig.airAlertLastActive = false
                AirAlertNotificationHelper.cancelAll(context)
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.cgAirAlertStatusChanged)
            }
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

    @JvmOverloads
    fun handlePushStatus(alert: Boolean, title: String? = null, body: String? = null, regionId: String? = null) {
        if (!CherrygramCoreConfig.airAlertEnabled) {
            return
        }
        // Захист від запізнілих або перехресних пушів для інших регіонів
        val userRegionId = CherrygramCoreConfig.airAlertRegionId
        if (!AirAlertHelper.shouldProcessAlert(regionId, userRegionId)) {
            FileLog.d("AirAlertController: push region_id ($regionId) does not match user region_id ($userRegionId). Ignore.")
            return
        }
        AndroidUtilities.runOnUIThread {
            if (isTesting) {
                pendingAlertStatus = alert
            } else {
                setAlertStatus(alert, title, body)
            }
        }
    }
}
