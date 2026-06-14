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
    private val onConnectionClosed: () -> Unit
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
    private val bufferSizeIn = AudioRecord.getMinBufferSize(
        sampleRateIn,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    ) * 2

    private val bufferSizeOut = AudioTrack.getMinBufferSize(
        sampleRateOut,
        AudioFormat.CHANNEL_OUT_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    ) * 2

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
            FileLog.e("GominLiveManager: Gemini API key is missing.")
            showToastLong("Помилка: API-ключ Gemini порожній!")
            onConnectionClosed()
            return
        }

        isSessionActive = true
        showToast("🔌 Підключаюся до Gemini...")
        Thread {
            initAudioDevices()
            AndroidUtilities.runOnUIThread {
                if (isSessionActive) {
                    connectWebSocket(apiKey)
                }
            }
        }.apply {
            name = "GominAudioInitThread"
            start()
        }
    }

    @SuppressLint("MissingPermission")
    private fun initAudioDevices() {
        synchronized(audioLock) {
            try {
                val context = glowView.context
                val audioManager = context.getSystemService(android.content.Context.AUDIO_SERVICE) as? AudioManager
                audioManager?.let {
                    it.mode = AudioManager.MODE_IN_COMMUNICATION
                    @Suppress("DEPRECATION")
                    it.requestAudioFocus({ }, AudioManager.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                }

                var record: AudioRecord? = null
                try {
                    record = AudioRecord(
                        MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                        sampleRateIn,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSizeIn
                    )
                } catch (e: Exception) {
                    FileLog.e("GominLiveManager: Failed to initialize AudioRecord with VOICE_COMMUNICATION, trying MIC", e)
                }

                if (record == null || record.state != AudioRecord.STATE_INITIALIZED) {
                    record?.release()
                    record = AudioRecord(
                        MediaRecorder.AudioSource.MIC,
                        sampleRateIn,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSizeIn
                    )
                }
                audioRecord = record

                if (record.state == AudioRecord.STATE_INITIALIZED) {
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
                }

                audioTrack = AudioTrack(
                    AudioManager.STREAM_VOICE_CALL,
                    sampleRateOut,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSizeOut,
                    AudioTrack.MODE_STREAM
                )
            } catch (e: Exception) {
                FileLog.e("GominLiveManager: Error in initAudioDevices", e)
            }
        }
    }

    private fun connectWebSocket(apiKey: String) {
        val url = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent?key=$apiKey"
        val request = Request.Builder().url(url).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                FileLog.d("GominLiveManager: WebSocket opened successfully. Status: ${response.code}")
                isWebSocketOpen = true
                showToast("✅ З'єднання встановлено!")
                sendSetupMessage(webSocket)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                val preview = if (text.length > 200) text.substring(0, 200) + "..." else text
                FileLog.d("GominLiveManager: Received message (len=${text.length}): $preview")
                parseServerMessage(text)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                FileLog.d("GominLiveManager: WebSocket closed. Code: $code, Reason: $reason")
                if (code != 1000) {
                    showToastLong("🔌 З'єднання закрите: $reason (код $code)")
                }
                stopSession()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                FileLog.e("GominLiveManager: WebSocket Failure. Response code: ${response?.code}", t)
                val codeStr = if (response != null) " (код ${response.code})" else ""
                showToastLong("❌ Помилка WebSocket: ${t.message}$codeStr")
                stopSession()
            }
        })
    }

    private fun sendSetupMessage(ws: WebSocket) {
        try {
            val targetModel = if (isTranscriptionMode) MODEL_TRANSCRIPTION else MODEL_VOICE_CALL
            FileLog.d("GominLiveManager: Using model $targetModel")

            // Формат згідно з офіційним get-started-websocket прикладом (червень 2026):
            // https://ai.google.dev/gemini-api/docs/live-api/get-started-websocket
            val setupJson = JSONObject().apply {
                put("setup", JSONObject().apply {
                    put("model", targetModel)
                    put("responseModalities", JSONArray().put(if (isTranscriptionMode) "TEXT" else "AUDIO"))
                    put("systemInstruction", JSONObject().apply {
                        put("parts", JSONArray().put(JSONObject().apply {
                            put("text", "Ти — Гомін AI, дружній голосовий асистент. Відповідай коротко та природно українською мовою.")
                        }))
                    })
                })
            }
            val setupStr = setupJson.toString()
            FileLog.d("GominLiveManager: Sending setup: $setupStr")
            ws.send(setupStr)
        } catch (e: Exception) {
            FileLog.e(e)
        }
    }

    private fun startAudioThreads() {
        if (!isTranscriptionMode) {
            playThread = Thread {
                val track = synchronized(audioLock) { audioTrack }
                if (track == null || track.state != AudioTrack.STATE_INITIALIZED) {
                    showToast("Помилка ініціалізації динаміка 🔊")
                    AndroidUtilities.runOnUIThread { stopSession() }
                } else {
                    try {
                        track.play()
                    } catch (e: Exception) {
                        FileLog.e(e)
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
            val buffer = ShortArray(bufferSizeIn / 2)
            val record = synchronized(audioLock) { audioRecord }
            if (record != null && record.state == AudioRecord.STATE_INITIALIZED) {
                try {
                    record.startRecording()
                } catch (e: Exception) {
                    FileLog.e(e)
                    showToast("Не вдалося запустити запис мікрофона: ${e.message}")
                    AndroidUtilities.runOnUIThread { stopSession() }
                }
            } else {
                showToast("Мікрофон не ініціалізовано 🎙️")
                AndroidUtilities.runOnUIThread { stopSession() }
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
                        FileLog.e("GominLiveManager: AudioRecord read error: $read")
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
                            // FIX: mediaChunks is deprecated — use top-level `audio` field instead
                            val inputJson = JSONObject().apply {
                                put("realtimeInput", JSONObject().apply {
                                    put("audio", JSONObject().apply {
                                        put("mimeType", "audio/pcm;rate=16000")
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
                FileLog.e("GominLiveManager Server Error: $errMsg")
                showToastLong("❌ Помилка Gemini: $errMsg")
                AndroidUtilities.runOnUIThread { stopSession() }
                return
            }

            if (obj.has("setupComplete")) {
                isSetupComplete = true
                showToast("🎙️ Сесія активована!")
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
                }
                audioRecord?.release()
            } catch (e: Exception) { }

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
            FileLog.e("GominLiveManager: Error releasing AudioManager", e)
        }

        webSocket = null
        AndroidUtilities.runOnUIThread { onConnectionClosed() }
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
