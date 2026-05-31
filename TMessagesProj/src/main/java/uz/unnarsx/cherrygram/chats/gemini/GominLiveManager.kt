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
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

class GominLiveManager(
    private val glowView: GominLiveEdgeGlowView,
    private val onConnectionClosed: () -> Unit
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var isConnected = false

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
        val apiKey = CherrygramMessagesConfig.INSTANCE.getGeminiApiKey()
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

                // Enforce STREAM_VOICE_CALL to enable hardware acoustic echo cancellation routing on Android
                audioTrack = AudioTrack(
                    AudioManager.STREAM_VOICE_CALL,
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
                isConnected = true
                sendSetupMessage(webSocket)
                startAudioThreads()
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
            val userModel = CherrygramMessagesConfig.geminiModelName
            val targetModel = when {
                userModel.contains("3.0") -> "models/gemini-3.0-flash"
                userModel.contains("2.5") -> "models/gemini-2.5-flash"
                userModel.contains("3.1") -> "models/gemini-3.1-flash"
                else -> "models/gemini-2.5-flash" // Преміальний Native Audio фолбек за замовчуванням
            }
            FileLog.d("GominLiveManager: Using model $targetModel for Live API")

            val setupJson = JSONObject().apply {
                put("setup", JSONObject().apply {
                    put("model", targetModel)
                    put("generation_config", JSONObject().apply {
                        put("response_modalities", JSONArray().put("AUDIO"))
                        put("speech_config", JSONObject().apply {
                            put("voice_config", JSONObject().apply {
                                put("prebuilt_voice_config", JSONObject().apply {
                                    put("voice_name", "Puck") // Rich, organic voice
                                })
                            })
                        })
                    })
                    // Enable Tools: Google Search (Grounding) & Ukrainian Air Alerts
                    val toolsArray = JSONArray().apply {
                        // 1. Google Search Grounding
                        put(JSONObject().apply {
                            put("google_search", JSONObject())
                        })
                        // 2. Custom function for live Ukrainian Air Alerts
                        put(JSONObject().apply {
                            put("function_declarations", JSONArray().put(JSONObject().apply {
                                put("name", "get_air_alerts")
                                put("description", "Отримати поточний статус повітряних тривог в Україні")
                                put("parameters", JSONObject().apply {
                                    put("type", "OBJECT")
                                    put("properties", JSONObject())
                                    put("required", JSONArray())
                                })
                            }))
                        })
                    }
                    put("tools", toolsArray)
                })
            }
            ws.send(setupJson.toString())
        } catch (e: Exception) {
            FileLog.e(e)
        }
    }

    private fun startAudioThreads() {
        // Playback Thread
        playThread = Thread {
            val track = synchronized(audioLock) { audioTrack }
            if (track == null || track.state != AudioTrack.STATE_INITIALIZED) {
                FileLog.e("GominLiveManager: AudioTrack not initialized")
                isSessionActive = false
            } else {
                try {
                    track.play()
                } catch (e: Exception) {
                    FileLog.e(e)
                    isSessionActive = false
                }
            }
            while (isSessionActive) {
                try {
                    val pcmData = audioPlayQueue.poll(100, TimeUnit.MILLISECONDS)
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
                    } else {
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

        // Recording Thread
        recordThread = Thread {
            val buffer = ShortArray(bufferSizeIn / 2)
            val record = synchronized(audioLock) { audioRecord }
            if (record != null && record.state == AudioRecord.STATE_INITIALIZED) {
                try {
                    record.startRecording()
                } catch (e: Exception) {
                    FileLog.e(e)
                    isSessionActive = false
                }
            } else {
                FileLog.e("GominLiveManager: AudioRecord not initialized")
                isSessionActive = false
            }

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

                        // Send audio chunk
                        val base64Data = Base64.encodeToString(byteBuffer, Base64.NO_WRAP)
                        val inputJson = JSONObject().apply {
                            put("realtimeInput", JSONObject().apply {
                                put("mediaChunks", JSONArray().put(JSONObject().apply {
                                    put("mimeType", "audio/pcm")
                                    put("data", base64Data)
                                }))
                            })
                        }
                        
                        if (isConnected) {
                            webSocket?.send(inputJson.toString())
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

        // Send interrupt message to server using correct snake_case keys
        try {
            val interruptJson = JSONObject().apply {
                put("client_content", JSONObject().apply {
                    put("turns", JSONArray())
                    put("turn_complete", false)
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
            if (obj.has("serverContent")) {
                val serverContent = obj.getJSONObject("serverContent")
                
                // Server interruption signal
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

                // Handle Tool Calls (Google Search grounding & custom Air Alerts)
                if (serverContent.has("toolCall")) {
                    val toolCall = serverContent.getJSONObject("toolCall")
                    val functionCalls = toolCall.optJSONArray("functionCalls")
                    if (functionCalls != null) {
                        val functionResponses = JSONArray()
                        for (i in 0 until functionCalls.length()) {
                            val call = functionCalls.getJSONObject(i)
                            val name = call.getString("name")
                            val callId = call.getString("id")
                            
                            val responseData = JSONObject()
                            if (name == "get_air_alerts") {
                                val alertsStatus = try {
                                    if (uz.unnarsx.cherrygram.alerts.AirAlertController.isAlertActive()) {
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
                                put("response", JSONObject().apply { put("output", responseData) })
                            }
                            functionResponses.put(fResp)
                        }

                        // Send back Tool Response
                        val responseJson = JSONObject().apply {
                            put("toolResponse", JSONObject().apply {
                                put("functionResponses", functionResponses)
                            })
                        }
                        if (isConnected) {
                            webSocket?.send(responseJson.toString())
                        }
                    }
                }

                if (serverContent.has("modelTurn")) {
                    val modelTurn = serverContent.getJSONObject("modelTurn")
                    val parts = modelTurn.optJSONArray("parts")
                    if (parts != null) {
                        for (i in 0 until parts.length()) {
                            val part = parts.getJSONObject(i)
                            if (part.has("inlineData")) {
                                val inlineData = part.getJSONObject("inlineData")
                                val dataBase64 = inlineData.getString("data")
                                val pcmBytes = Base64.decode(dataBase64, Base64.NO_WRAP)
                                audioPlayQueue.offer(pcmBytes)
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
        isConnected = false
        
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
