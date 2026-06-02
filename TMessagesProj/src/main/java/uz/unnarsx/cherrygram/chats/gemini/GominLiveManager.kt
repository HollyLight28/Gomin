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
import org.telegram.messenger.FileLog
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

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // 0 means no timeout for WebSockets
        .writeTimeout(10, TimeUnit.SECONDS)
        .pingInterval(15, TimeUnit.SECONDS) // Keep connection alive
        .build()

    private var webSocket: WebSocket? = null
    private var isWebSocketOpen = false
    private var isSetupComplete = false

    // Audio Pipeline Parameters
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
    private var isSessionActive = false

    private val audioPlayQueue = LinkedBlockingQueue<ByteArray>()
    private var isAiSpeaking = false

    fun startSession() {
        val apiKey = CherrygramMessagesConfig.geminiApiKey
        if (apiKey.isEmpty()) {
            FileLog.e("GominLiveManager: Gemini API key is missing.")
            onConnectionClosed()
            return
        }

        isSessionActive = true
        initAudioDevices()
        connectWebSocket(apiKey)
    }

    @SuppressLint("MissingPermission")
    private fun initAudioDevices() {
        synchronized(audioLock) {
            try {
                val record = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRateIn,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSizeIn
                )
                audioRecord = record

                // Initialize Acoustic Echo Canceler and Noise Suppressor using record session ID
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

                // Enforce STREAM_MUSIC to enable loudspeaker routing on Android
                audioTrack = AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    sampleRateOut,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSizeOut,
                    AudioTrack.MODE_STREAM
                )
            } catch (e: Exception) {
                FileLog.e(e)
            }
        }
    }

    private fun connectWebSocket(apiKey: String) {
        val url = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent?key=$apiKey"
        val request = Request.Builder().url(url).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                isWebSocketOpen = true
                sendSetupMessage(webSocket)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                parseServerMessage(text)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                stopSession()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                FileLog.e("GominLiveManager WebSocket Failure", t)
                stopSession()
            }
        })
    }

    private fun sendSetupMessage(ws: WebSocket) {
        try {
            val targetModel = if (isTranscriptionMode) {
                "models/gemini-2.5-flash-native-audio-preview-12-2025"
            } else {
                "models/gemini-3.1-flash-live-preview"
            }
            FileLog.d("GominLiveManager: Using model $targetModel for Live API")

            val setupJson = JSONObject().apply {
                put("setup", JSONObject().apply {
                    put("model", targetModel)
                    
                    if (isTranscriptionMode) {
                        put("inputAudioTranscription", JSONObject())
                    }

                    put("generationConfig", JSONObject().apply {
                        put("responseModalities", JSONArray().put(if (isTranscriptionMode) "TEXT" else "AUDIO"))
                        
                        if (!isTranscriptionMode) {
                            put("speechConfig", JSONObject().apply {
                                put("voiceConfig", JSONObject().apply {
                                    put("prebuiltVoiceConfig", JSONObject().apply {
                                        put("voiceName", "Puck") // Rich, organic voice
                                    })
                                })
                            })
                        }
                    })
                    // Tools are temporarily disabled for Live MVP stability
                })
            }
            ws.send(setupJson.toString())
        } catch (e: Exception) {
            FileLog.e(e)
        }
    }

    private fun startAudioThreads() {
        // Playback Thread
        if (!isTranscriptionMode) {
            playThread = Thread {
                val track = synchronized(audioLock) { audioTrack }
                if (track == null || track.state != AudioTrack.STATE_INITIALIZED) {
                    FileLog.e("GominLiveManager: AudioTrack not initialized")
                    AndroidUtilities.runOnUIThread { stopSession() }
                } else {
                    try {
                        track.play()
                    } catch (e: Exception) {
                        FileLog.e(e)
                        AndroidUtilities.runOnUIThread { stopSession() }
                    }
                }
                while (isSessionActive) {
                    try {
                        val pcmData = audioPlayQueue.poll(500, TimeUnit.MILLISECONDS)
                        val activeTrack = synchronized(audioLock) { audioTrack }
                        if (pcmData != null && isSessionActive && activeTrack != null && activeTrack.state == AudioTrack.STATE_INITIALIZED) {
                            isAiSpeaking = true
                            
                            // Calculate RMS for glow view pulsing
                            val rms = calculateRms(pcmData)
                            AndroidUtilities.runOnUIThread {
                                glowView.setAmplitude(rms, false)
                            }

                            try {
                                activeTrack.write(pcmData, 0, pcmData.size)
                            } catch (e: Exception) {
                                FileLog.e(e)
                            }
                        } else if (isSessionActive) { // Only reset amplitude if session is still active
                            isAiSpeaking = false
                            AndroidUtilities.runOnUIThread {
                                glowView.setAmplitude(0f, false)
                            }
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

        // Recording Thread
        recordThread = Thread {
            val buffer = ShortArray(bufferSizeIn / 2)
            val record = synchronized(audioLock) { audioRecord }
            if (record != null && record.state == AudioRecord.STATE_INITIALIZED) {
                try {
                    record.startRecording()
                } catch (e: Exception) {
                    FileLog.e(e)
                    AndroidUtilities.runOnUIThread { stopSession() }
                }
            } else {
                FileLog.e("GominLiveManager: AudioRecord not initialized")
                AndroidUtilities.runOnUIThread { stopSession() }
            }

            val accumulatedBytes = java.io.ByteArrayOutputStream()
            // 100ms of 16kHz 16-bit mono audio is 3200 bytes.
            val bytesToAccumulate = 3200

            while (isSessionActive) {
                try {
                    val activeRecord = synchronized(audioLock) { audioRecord }
                    if (activeRecord == null || activeRecord.state != AudioRecord.STATE_INITIALIZED) {
                        Thread.sleep(100)
                        continue
                    }
                    val read = activeRecord.read(buffer, 0, buffer.size)
                    if (read <= 0) {
                        Thread.sleep(100) // Prevent 100% CPU infinite-loop
                        continue
                    }
                    if (isSessionActive) {
                        // Compute RMS
                        val byteBuffer = ByteArray(read * 2)
                        var sumOfSquares = 0.0
                        for (i in 0 until read) {
                            val sample = buffer[i]
                            sumOfSquares += sample * sample
                            
                            // Convert short to little-endian bytes
                            byteBuffer[i * 2] = (sample.toInt() and 0xFF).toByte()
                            byteBuffer[i * 2 + 1] = ((sample.toInt() shr 8) and 0xFF).toByte()
                        }
                        
                        val rms = Math.sqrt(sumOfSquares / read) / 32768.0
                        val rmsFloat = rms.toFloat()

                        AndroidUtilities.runOnUIThread {
                            glowView.setAmplitude(rmsFloat, true)
                        }

                        // Local Interruption (Barge-in): User starts speaking while AI speaks
                        if (rmsFloat > 0.08f && isAiSpeaking) { // Raised from 0.04f to 0.08f to avoid ambient noise trigger
                            triggerLocalInterruption()
                        }

                        accumulatedBytes.write(byteBuffer)

                        if (accumulatedBytes.size() >= bytesToAccumulate) {
                            val chunkToSend = accumulatedBytes.toByteArray()
                            accumulatedBytes.reset()

                            // Send audio chunk
                            val base64Data = Base64.encodeToString(chunkToSend, Base64.NO_WRAP)
                            val inputJson = JSONObject().apply {
                                put("realtimeInput", JSONObject().apply {
                                    put("mediaChunks", JSONArray().put(JSONObject().apply {
                                        put("mimeType", "audio/pcm")
                                        put("data", base64Data)
                                    }))
                                })
                            }
                            
                            if (isWebSocketOpen && isSetupComplete) {
                                webSocket?.send(inputJson.toString())
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

        // Send interrupt message to server using correct camelCase keys
        try {
            val interruptJson = JSONObject().apply {
                put("clientContent", JSONObject().apply {
                    put("turns", JSONArray())
                    put("turnComplete", false)
                })
            }
            webSocket?.send(interruptJson.toString())
        } catch (e: Exception) {
            FileLog.e(e)
        }
    }

    private fun parseServerMessage(text: String) {
        try {
            val obj = JSONObject(text)
            
            // 1. Очікуємо handshake
            if (obj.has("setupComplete")) {
                FileLog.d("GominLiveManager: Received setupComplete. Starting audio stream.")
                isSetupComplete = true
                startAudioThreads()
            }

            // 2. Нативна транскрипція (June 2026 recommended approach) - Top Level
            if (obj.has("inputAudioTranscription")) {
                val transcription = obj.getJSONObject("inputAudioTranscription")
                val text = transcription.optString("text", "")
                if (text.isNotEmpty()) {
                    onTextReceived?.invoke(text)
                }
            }

            // 3. Парсинг контенту (camelCase)
            if (obj.has("serverContent")) {
                if (serverContent.has("modelTurn")) {
                    val modelTurn = serverContent.getJSONObject("modelTurn")
                    val parts = modelTurn.optJSONArray("parts")
                    if (parts != null) {
                        for (i in 0 until parts.length()) {
                            val part = parts.getJSONObject(i)
                            
                            // Обробка виклику функцій (tools)
                            if (part.has("functionCall")) {
                                val call = part.getJSONObject("functionCall")
                                val name = call.getString("name")
                                val callId = call.optString("id", "")
                                
                                val responseData = JSONObject()
                                if (name == "get_air_alerts") {
                                    val alertsStatus = try {
                                        if (AirAlertController.isAlertActive()) {
                                            "Наразі в Україні оголошено повітряну тривогу в деяких регіонах. Ситуація під контролем."
                                        } else {
                                            "Наразі повітряних тривог в Україні немає. Все спокійно."
                                        }
                                    } catch (e: Exception) {
                                        "Не вдалося отримати статус тривог. Спробуйте пізніше."
                                    }
                                    responseData.put("status", alertsStatus)
                                } else {
                                    responseData.put("result", "Інструмент виконано успішно.")
                                }

                                val fResp = JSONObject().apply {
                                    put("name", name)
                                    put("id", callId)
                                    put("response", responseData)
                                }
                                
                                // Send back Tool Response in camelCase
                                val responseJson = JSONObject().apply {
                                    put("toolResponse", JSONObject().apply {
                                        put("functionResponses", JSONArray().put(fResp))
                                    })
                                }
                                if (isWebSocketOpen && isSetupComplete) {
                                    webSocket?.send(responseJson.toString())
                                }
                            }
                            
                            // Аудіо та текст
                            if (part.has("inlineData")) {
                                val inlineData = part.getJSONObject("inlineData")
                                val dataBase64 = inlineData.getString("data")
                                val pcmBytes = Base64.decode(dataBase64, Base64.NO_WRAP)
                                audioPlayQueue.offer(pcmBytes)
                            } else if (part.has("text")) {
                                val textPart = part.getString("text")
                                onTextReceived?.invoke(textPart)
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
        
        try {
            webSocket?.close(1000, "Session ended")
        } catch (e: Exception) {
            // Ignore
        }

        recordThread?.interrupt()
        playThread?.interrupt()

        synchronized(audioLock) {
            try {
                echoCanceler?.enabled = false
                echoCanceler?.release()
            } catch (e: Exception) {
                // Ignore
            }
            try {
                noiseSuppressor?.enabled = false
                noiseSuppressor?.release()
            } catch (e: Exception) {
                // Ignore
            }
            try {
                audioRecord?.stop()
                audioRecord?.release()
            } catch (e: Exception) {
                // Ignore
            }
            try {
                audioTrack?.stop()
                audioTrack?.release()
            } catch (e: Exception) {
                // Ignore
            }

            echoCanceler = null
            noiseSuppressor = null
            audioRecord = null
            audioTrack = null
        }

        webSocket = null

        AndroidUtilities.runOnUIThread {
            onConnectionClosed()
        }
    }
}
