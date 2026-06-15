package uz.unnarsx.cherrygram.chats.gemini

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import android.util.Base64
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import org.telegram.messenger.AndroidUtilities
import uz.unnarsx.cherrygram.core.configs.CherrygramMessagesConfig
import uz.unnarsx.cherrygram.alerts.AirAlertController
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

class GominLiveManager(
    private val glowView: GominLiveEdgeGlowView,
    private val isTranscriptionMode: Boolean = false,
    private val onTextReceived: ((String) -> Unit)? = null,
    private val onTurnComplete: (() -> Unit)? = null,
    private val onConnectionClosed: () -> Unit,
    private val onStatusUpdate: ((String, String) -> Unit)? = null
) {

    companion object {
        private const val MODEL_TRANSCRIPTION = "models/gemini-3.1-flash-live-preview"
        private const val MODEL_VOICE_CALL    = "models/gemini-3.1-flash-live-preview"

        private object FileLog {
            fun d(msg: String) {
                android.util.Log.d("GominLiveManager", msg)
            }
            fun e(msg: String) {
                android.util.Log.e("GominLiveManager", msg)
            }
            fun e(e: Throwable) {
                android.util.Log.e("GominLiveManager", "Exception", e)
            }
            fun e(msg: String, e: Throwable) {
                android.util.Log.e("GominLiveManager", msg, e)
            }
            fun e(e: Exception) {
                android.util.Log.e("GominLiveManager", "Exception", e)
            }
        }
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .pingInterval(15, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var isWebSocketOpen = false
    private var isSetupComplete = false

    private val sampleRateIn = 16000
    private val sampleRateOut = 24000

    private fun getBufferSizeIn(): Int {
        val minBuf = AudioRecord.getMinBufferSize(
            sampleRateIn,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        return if (minBuf <= 0) 4096 else minBuf * 2
    }

    private fun getBufferSizeOut(): Int {
        val minBuf = AudioTrack.getMinBufferSize(
            sampleRateOut,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        return if (minBuf <= 0) 4096 else minBuf * 2
    }

    private fun updateStatus(main: String, sub: String = "") {
        FileLog.d("STATUS: $main | $sub")
        AndroidUtilities.runOnUIThread {
            glowView.setStatusText(main, sub)
            onStatusUpdate?.invoke(main, sub)
        }
    }

    private val audioLock = Any()
    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var echoCanceler: AcousticEchoCanceler? = null
    private var noiseSuppressor: NoiseSuppressor? = null

    private var recordThread: Thread? = null
    private var playThread: Thread? = null
    @Volatile
    private var isSessionActive = false

    private val audioPlayQueue = LinkedBlockingQueue<ByteArray>()
    private var isAiSpeaking = false

    fun startSession() {
        val apiKey = CherrygramMessagesConfig.geminiApiKey
        if (apiKey.isEmpty()) {
            FileLog.e("API key missing")
            updateStatus("❌ API ключ порожній", "Перевір налаштування Gemini")
            showToastLong("Помилка: API-ключ Gemini порожній!")
            onConnectionClosed()
            return
        }

        isSessionActive = true
        updateStatus("🔌 Підключення до Gemini...", "Ініціалізація аудіо")
        // Start foreground service so Android 14+ shows the green mic dot
        GominMicrophoneService.start()
        Thread {
            val audioOk = initAudioDevices()
            AndroidUtilities.runOnUIThread {
                if (isSessionActive && audioOk) {
                    updateStatus("🔌 Підключення WebSocket...", "")
                    connectWebSocket(apiKey)
                } else if (isSessionActive && !audioOk) {
                    updateStatus("❌ Аудіо не ініціалізовано", "Мікрофон недоступний")
                    showToastLong("❌ Не вдалося ініціалізувати мікрофон")
                    stopSession()
                }
            }
        }.apply {
            name = "GominAudioInitThread"
            start()
        }
    }

    @SuppressLint("MissingPermission")
    private fun initAudioDevices(): Boolean {
        return synchronized(audioLock) {
            try {
                updateStatus("🎤 Ініціалізація аудіо...", "Налаштування мікрофона")

                val context = glowView.context
                val audioManager = context.getSystemService(android.content.Context.AUDIO_SERVICE) as? AudioManager
                audioManager?.let {
                    it.mode = AudioManager.MODE_IN_COMMUNICATION
                    @Suppress("DEPRECATION")
                    it.requestAudioFocus({ }, AudioManager.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                }

                val bufSize = getBufferSizeIn()
                FileLog.d("bufferSizeIn = $bufSize")

                var record: AudioRecord? = null
                try {
                    record = AudioRecord(
                        MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                        sampleRateIn,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufSize
                    )
                    FileLog.d("AudioRecord(VOICE_COMMUNICATION) state = ${record.state}")
                } catch (e: Exception) {
                    FileLog.e("VOICE_COMMUNICATION failed: ${e.message}")
                }

                if (record == null || record.state != AudioRecord.STATE_INITIALIZED) {
                    record?.release()
                    FileLog.d("Falling back to MIC source")
                    record = AudioRecord(
                        MediaRecorder.AudioSource.MIC,
                        sampleRateIn,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufSize
                    )
                    FileLog.d("AudioRecord(MIC) state = ${record.state}")
                }

                if (record.state != AudioRecord.STATE_INITIALIZED) {
                    FileLog.e("AudioRecord NOT initialized, state = ${record.state}")
                    updateStatus("❌ Помилка мікрофона", "Не вдалося ініціалізувати AudioRecord")
                    audioRecord = null
                    return@synchronized false
                }

                audioRecord = record
                FileLog.d("AudioRecord initialized SUCCESS")

                val sessionId = record.audioSessionId
                if (AcousticEchoCanceler.isAvailable()) {
                    echoCanceler = AcousticEchoCanceler.create(sessionId)?.apply {
                        enabled = true
                    }
                }
                if (NoiseSuppressor.isAvailable()) {
                    noiseSuppressor = NoiseSuppressor.create(sessionId)?.apply {
                        enabled = true
                    }
                }

                val bufOut = getBufferSizeOut()
                audioTrack = AudioTrack(
                    AudioManager.STREAM_VOICE_CALL,
                    sampleRateOut,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufOut,
                    AudioTrack.MODE_STREAM
                )
                FileLog.d("AudioTrack initialized, state = ${audioTrack?.state}")

                updateStatus("🎤 Мікрофон готовий", "Очікування WebSocket...")
                true
            } catch (e: Exception) {
                FileLog.e("Error in initAudioDevices", e)
                updateStatus("❌ Помилка аудіо", e.message ?: "Невідома помилка")
                false
            }
        }
    }

    private fun connectWebSocket(apiKey: String) {
        val url = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent?key=$apiKey"
        FileLog.d("Connecting WebSocket: ${url.take(80)}...")
        val request = Request.Builder().url(url).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                FileLog.d("WebSocket opened. Status: ${response.code}")
                isWebSocketOpen = true
                updateStatus("✅ WebSocket підключено", "Надсилання налаштувань...")
                sendSetupMessage(webSocket)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                val preview = if (text.length > 200) text.substring(0, 200) + "..." else text
                FileLog.d("Received (len=${text.length}): $preview")
                parseServerMessage(text)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                FileLog.d("WebSocket closed. Code: $code, Reason: $reason")
                updateStatus("🔌 З'єднання закрите", "Код: $code")
                if (code != 1000) {
                    showToastLong("🔌 З'єднання закрите: $reason (код $code)")
                }
                stopSession()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                val respCode = response?.code
                FileLog.e("WebSocket Failure. Code: $respCode", t)
                updateStatus("❌ Помилка WebSocket", "${t.message} (код $respCode)")
                showToastLong("❌ Помилка WebSocket: ${t.message} (код $respCode)")
                stopSession()
            }
        })
    }

    private fun sendSetupMessage(ws: WebSocket) {
        try {
            val targetModel = if (isTranscriptionMode) MODEL_TRANSCRIPTION else MODEL_VOICE_CALL
            FileLog.d("Using model $targetModel")
            updateStatus("📡 Надсилання конфігурації...", targetModel)

            // Формат згідно з офіційним get-started-websocket прикладом (червень 2026):
            // https://ai.google.dev/gemini-api/docs/live-api/get-started-websocket
            // ВАЖЛИВО: ключ має бути "config", а не "setup"!
            val setupJson = JSONObject().apply {
                put("config", JSONObject().apply {
                    put("model", targetModel)
                    // CRITICAL: Live models do NOT support ["TEXT"] modality!
                    // Always use ["AUDIO"] and get text via inputAudioTranscription side-channel
                    put("responseModalities", JSONArray().put("AUDIO"))
                    put("generationConfig", JSONObject().apply {
                        if (!isTranscriptionMode) {
                            put("speechConfig", JSONObject().apply {
                                put("voiceConfig", JSONObject().apply {
                                    put("prebuiltVoiceConfig", JSONObject().apply {
                                        put("voiceName", "Puck")
                                    })
                                })
                            })
                        }
                    })
                    put("systemInstruction", JSONObject().apply {
                        put("parts", JSONArray().put(JSONObject().apply {
                            put("text", "Ти — Гомін AI, дружній голосовий асистент. Відповідай коротко та природно українською мовою.")
                        }))
                    })
                    if (isTranscriptionMode) {
                        put("inputAudioTranscription", JSONObject())
                    }
                })
            }
>>>>>>> 6969a1265 (fix(gemini): live mic green dot + diagnostics + 3 critical bugfixes)
                    })
                    if (isTranscriptionMode) {
                        put("inputAudioTranscription", JSONObject())
                    }
                })
            }
            val setupStr = setupJson.toString()
            FileLog.d("Setup: $setupStr")
            ws.send(setupStr)
        } catch (e: Exception) {
            FileLog.e("Error sending setup", e)
            updateStatus("❌ Помилка конфігурації", e.message ?: "")
        }
    }

    private fun startAudioThreads() {
        updateStatus("🎤 Запуск аудіо-потоків...", "Старт запису")

        if (!isTranscriptionMode) {
            playThread = Thread {
                val track = synchronized(audioLock) { audioTrack }
                if (track == null || track.state != AudioTrack.STATE_INITIALIZED) {
                    updateStatus("❌ Динамік не готовий", "Помилка ініціалізації AudioTrack")
                    showToast("Помилка ініціалізації динаміка 🔊")
                    AndroidUtilities.runOnUIThread { stopSession() }
                } else {
                    try {
                        track.play()
                        FileLog.d("AudioTrack.play() started")
                    } catch (e: Exception) {
                        FileLog.e("AudioTrack.play() failed", e)
                        updateStatus("❌ Динамік не запустився", e.message ?: "")
                        showToast("Не вдалося запустити динамік 🔊")
                        AndroidUtilities.runOnUIThread { stopSession() }
                    }
                }
                while (isSessionActive) {
                    try {
                        val pcmData = audioPlayQueue.poll(500, TimeUnit.MILLISECONDS)
                        val activeTrack = synchronized(audioLock) { audioTrack }
                        if (pcmData != null && isSessionActive && activeTrack != null && activeTrack.state == AudioTrack.STATE_INITIALIZED) {
                            isAiSpeaking = true
                            val rms = calculateRms(pcmData)
                            AndroidUtilities.runOnUIThread { glowView.setAmplitude(rms, false) }
                            try {
                                activeTrack.write(pcmData, 0, pcmData.size)
                            } catch (e: Exception) {
                                FileLog.e(e)
                            }
                        } else if (isSessionActive) {
                            isAiSpeaking = false
                            AndroidUtilities.runOnUIThread { glowView.setAmplitude(0f, false) }
                        }
                    } catch (e: InterruptedException) {
                        break
                    } catch (e: Exception) {
                        FileLog.e(e)
                    }
                }
            }.apply {
                priority = Thread.MAX_PRIORITY
                start()
            }
        }

        recordThread = Thread {
            val bufSize = getBufferSizeIn()
            val buffer = ShortArray(if (bufSize > 0) bufSize / 2 else 1024)
            val record = synchronized(audioLock) { audioRecord }

            if (record != null && record.state == AudioRecord.STATE_INITIALIZED) {
                try {
                    FileLog.d("Calling record.startRecording()...")
                    record.startRecording()
                    FileLog.d("record.startRecording() SUCCESS")
                    updateStatus("🎙️ Мікрофон АКТИВОВАНО", "Зелена точка має з'явитися")
                } catch (e: Exception) {
                    FileLog.e("startRecording() FAILED: ${e.message}", e)
                    updateStatus("❌ Мікрофон не запустився", e.message ?: "IllegalStateException")
                    showToast("❌ Не вдалося запустити мікрофон: ${e.message}")
                    AndroidUtilities.runOnUIThread { stopSession() }
                    return@Thread
                }
            } else {
                val stateStr = record?.state?.toString() ?: "null"
                FileLog.e("AudioRecord not initialized. record=$record state=$stateStr")
                updateStatus("❌ Мікрофон не ініціалізовано", "State: $stateStr")
                showToast("Мікрофон не ініціалізовано 🎙️ (state=$stateStr)")
                AndroidUtilities.runOnUIThread { stopSession() }
                return@Thread
            }

            val accumulatedBytes = java.io.ByteArrayOutputStream()
            val bytesToAccumulate = 3200

            while (isSessionActive) {
                try {
                    val activeRecord = synchronized(audioLock) { audioRecord }
                    if (activeRecord == null || activeRecord.state != AudioRecord.STATE_INITIALIZED) {
                        Thread.sleep(100)
                        continue
                    }
                    val read = activeRecord.read(buffer, 0, buffer.size)
                    if (read < 0) {
                        FileLog.e("AudioRecord read error: $read")
                        updateStatus("❌ Помилка читання мікрофона", "read() = $read")
                        AndroidUtilities.runOnUIThread { stopSession() }
                        break
                    }
                    if (read == 0) {
                        Thread.sleep(50)
                        continue
                    }
                    if (isSessionActive) {
                        val byteBuffer = ByteArray(read * 2)
                        var sumOfSquares = 0.0
                        for (i in 0 until read) {
                            val sample = buffer[i]
                            sumOfSquares += sample * sample
                            byteBuffer[i * 2] = (sample.toInt() and 0xFF).toByte()
                            byteBuffer[i * 2 + 1] = ((sample.toInt() shr 8) and 0xFF).toByte()
                        }

                        val rms = Math.sqrt(sumOfSquares / read) / 32768.0
                        val rmsFloat = rms.toFloat()

                        AndroidUtilities.runOnUIThread { glowView.setAmplitude(rmsFloat, true) }

                        if (rmsFloat > 0.08f && isAiSpeaking) {
                            triggerLocalInterruption()
                        }

                        accumulatedBytes.write(byteBuffer)

                        if (accumulatedBytes.size() >= bytesToAccumulate) {
                            val chunkToSend = accumulatedBytes.toByteArray()
                            accumulatedBytes.reset()

                            val base64Data = Base64.encodeToString(chunkToSend, Base64.NO_WRAP)
                            val inputJson = JSONObject().apply {
                                put("realtimeInput", JSONObject().apply {
                                    put("audio", JSONObject().apply {
                                        put("mimeType", "audio/pcm")
                                        put("data", base64Data)
                                    })
                                })
                            }

                            if (isWebSocketOpen && isSetupComplete) {
                                val inputStr = inputJson.toString()
                                webSocket?.send(inputStr)
                            }
                        }
                    }
                } catch (e: Exception) {
                    FileLog.e(e)
                }
            }
        }.apply {
            priority = Thread.MAX_PRIORITY
            start()
        }
    }

    private fun calculateRms(pcmData: ByteArray): Float {
        var sum = 0.0
        val numSamples = pcmData.size / 2
        if (numSamples == 0) return 0f
        for (i in 0 until numSamples) {
            val sample = (pcmData[i * 2 + 1].toInt() shl 8) or (pcmData[i * 2].toInt() and 0xFF)
            sum += sample * sample
        }
        val rms = Math.sqrt(sum / numSamples) / 32768.0
        return rms.toFloat()
    }

    private fun triggerLocalInterruption() {
        isAiSpeaking = false
        audioPlayQueue.clear()

        synchronized(audioLock) {
            try {
                audioTrack?.pause()
                audioTrack?.flush()
                audioTrack?.play()
            } catch (e: Exception) {
                FileLog.e(e)
            }
        }
    }

    private fun parseServerMessage(text: String) {
        try {
            val obj = JSONObject(text)

            if (obj.has("error")) {
                val error = obj.getJSONObject("error")
                val errMsg = error.optString("message", "Невідома помилка")
                FileLog.e("Server Error: $errMsg")
                updateStatus("❌ Помилка Gemini", errMsg)
                showToastLong("❌ Помилка Gemini: $errMsg")
                AndroidUtilities.runOnUIThread { stopSession() }
                return
            }

            if (obj.has("setupComplete")) {
                isSetupComplete = true
                FileLog.d(">>> SETUP COMPLETE <<<")
                updateStatus("🎙️ Сесію активовано!", "Запуск мікрофона...")
                startAudioThreads()
                sendInitialGreetingTrigger()
            }

            if (obj.has("inputTranscription")) {
                val inputTranscription = obj.getJSONObject("inputTranscription")
                val transcriptionText = inputTranscription.optString("text", "")
                val isPartial = inputTranscription.optBoolean("partial", true)
                if (transcriptionText.isNotEmpty()) {
                    onTextReceived?.invoke(transcriptionText)
                    if (!isPartial) {
                        onTurnComplete?.invoke()
                    }
                }
            }

            if (obj.has("serverContent")) {
                val serverContent = obj.getJSONObject("serverContent")

                if (serverContent.optBoolean("interrupted", false)) {
                    isAiSpeaking = false
                    audioPlayQueue.clear()
                    synchronized(audioLock) {
                        try {
                            audioTrack?.pause()
                            audioTrack?.flush()
                            audioTrack?.play()
                        } catch (e: Exception) {
                            FileLog.e(e)
                        }
                    }
                }

                if (serverContent.optBoolean("turnComplete", false)) {
                    isAiSpeaking = false
                    AndroidUtilities.runOnUIThread { glowView.setAmplitude(0f, false) }
                    onTurnComplete?.invoke()
                }

                if (serverContent.has("modelTurn")) {
                    val modelTurn = serverContent.getJSONObject("modelTurn")
                    val parts = modelTurn.optJSONArray("parts")
                    if (parts != null) {
                        for (i in 0 until parts.length()) {
                            val part = parts.getJSONObject(i)

                            if (part.has("functionCall")) {
                                val call = part.getJSONObject("functionCall")
                                val name = call.getString("name")
                                val callId = call.optString("id", "")

                                val responseData = JSONObject()
                                if (name == "get_air_alerts") {
                                    val alertsStatus = if (AirAlertController.isAlertActive()) {
                                        "Наразі в Україні оголошено повітряну тривогу в деяких регіонах."
                                    } else {
                                        "Наразі повітряних тривог в Україні немає."
                                    }
                                    responseData.put("status", alertsStatus)
                                } else {
                                    responseData.put("result", "Виконано.")
                                }

                                val fResp = JSONObject().apply {
                                    put("name", name)
                                    put("id", callId)
                                    put("response", responseData)
                                }

                                val responseJson = JSONObject().apply {
                                    put("toolResponse", JSONObject().apply {
                                        put("functionResponses", JSONArray().put(fResp))
                                    })
                                }
                                if (isWebSocketOpen && isSetupComplete) {
                                    val respStr = responseJson.toString()
                                    FileLog.d("GominLiveManager: Sending tool response: $respStr")
                                    webSocket?.send(respStr)
                                }
                            }

                            if (part.has("inlineData")) {
                                if (!isTranscriptionMode) {
                                    val inlineData = part.getJSONObject("inlineData")
                                    val dataBase64 = inlineData.getString("data")
                                    val pcmBytes = Base64.decode(dataBase64, Base64.NO_WRAP)
                                    audioPlayQueue.offer(pcmBytes)
                                }
                            } else if (part.has("text")) {
                                if (!isTranscriptionMode) {
                                    val textPart = part.getString("text")
                                    onTextReceived?.invoke(textPart)
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            FileLog.e(e)
        }
    }

    fun stopSession() {
        synchronized(audioLock) {
            if (!isSessionActive) return
            isSessionActive = false
        }
        isWebSocketOpen = false
        isSetupComplete = false

        FileLog.d("Stopping session...")
        updateStatus("🛑 Зупинка сесії...", "")

        GominMicrophoneService.stop()

        try { webSocket?.close(1000, "Session ended") } catch (e: Exception) { }

        recordThread?.interrupt()
        playThread?.interrupt()
        try { recordThread?.join(500) } catch (e: Exception) {}
        try { playThread?.join(500) } catch (e: Exception) {}

        synchronized(audioLock) {
            try { echoCanceler?.enabled = false; echoCanceler?.release() } catch (e: Exception) { }
            try { noiseSuppressor?.enabled = false; noiseSuppressor?.release() } catch (e: Exception) { }

            try {
                if (audioRecord?.state == AudioRecord.STATE_INITIALIZED) {
                    audioRecord?.stop()
                    FileLog.d("AudioRecord stopped")
                }
                audioRecord?.release()
                FileLog.d("AudioRecord released")
            } catch (e: Exception) {
                FileLog.e("Error releasing AudioRecord", e)
            }

            try {
                if (audioTrack?.state == AudioTrack.STATE_INITIALIZED) {
                    audioTrack?.pause()
                    audioTrack?.flush()
                    audioTrack?.stop()
                }
                audioTrack?.release()
            } catch (e: Exception) { }

            echoCanceler = null
            noiseSuppressor = null
            audioRecord = null
            audioTrack = null
        }

        try {
            val context = glowView.context
            val audioManager = context.getSystemService(android.content.Context.AUDIO_SERVICE) as? AudioManager
            audioManager?.let {
                it.mode = AudioManager.MODE_NORMAL
                @Suppress("DEPRECATION")
                it.abandonAudioFocus { }
            }
        } catch (e: Exception) {
            FileLog.e("Error releasing AudioManager", e)
        }

        webSocket = null
        AndroidUtilities.runOnUIThread {
            glowView.setStatusText("", "")
            onConnectionClosed()
        }
    }

    private fun sendInitialGreetingTrigger() {
        if (isTranscriptionMode) return
        try {
            val greetingJson = JSONObject().apply {
                put("clientContent", JSONObject().apply {
                    put("turns", JSONArray().put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().put(JSONObject().apply {
                            put("text", "Привіт! Будь ласка, привітайся зі мною голосом коротко та дружелюбно українською мовою та запитай, про що поспілкуємося сьогодні.")
                        }))
                    }))
                    put("turnComplete", true)
                })
            }
            val greetingStr = greetingJson.toString()
            FileLog.d("GominLiveManager: Sending greeting trigger: $greetingStr")
            webSocket?.send(greetingStr)
        } catch (e: Exception) {
            FileLog.e(e)
        }
    }

    private fun showToast(msg: String) {
        AndroidUtilities.runOnUIThread {
            try {
                android.widget.Toast.makeText(glowView.context, msg, android.widget.Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                FileLog.e(e)
            }
        }
    }

    private fun showToastLong(msg: String) {
        AndroidUtilities.runOnUIThread {
            try {
                android.widget.Toast.makeText(glowView.context, msg, android.widget.Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                FileLog.e(e)
            }
        }
    }
}
