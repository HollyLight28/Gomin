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

    companion object {
        private const val MODEL_TRANSCRIPTION = "models/gemini-2.5-flash-native-audio-preview-12-2025"
        private const val MODEL_VOICE_CALL    = "models/gemini-3.1-flash-live-preview"
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
            val targetModel = if (isTranscriptionMode) MODEL_TRANSCRIPTION else MODEL_VOICE_CALL
            FileLog.d("GominLiveManager: Using model $targetModel")

            val setupJson = JSONObject().apply {
                put("setup", JSONObject().apply {
                    put("model", targetModel)
                    put("inputAudioTranscription", JSONObject())
                    if (!isTranscriptionMode) {
                        put("outputAudioTranscription", JSONObject())
                    }
                    put("generationConfig", JSONObject().apply {
                        put("responseModalities", JSONArray().put("AUDIO"))
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
                })
            }
            ws.send(setupJson.toString())
        } catch (e: Exception) {
            FileLog.e(e)
        }
    }

    private fun startAudioThreads() {
        if (!isTranscriptionMode) {
            playThread = Thread {
                val track = synchronized(audioLock) { audioTrack }
                if (track == null || track.state != AudioTrack.STATE_INITIALIZED) {
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
                    AndroidUtilities.runOnUIThread { stopSession() }
                }
            } else {
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
                    if (read <= 0) {
                        Thread.sleep(100)
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
                                    val chunk = JSONObject().apply {
                                        put("mimeType", "audio/pcm;rate=16000")
                                        put("data", base64Data)
                                    }
                                    put("mediaChunks", JSONArray().put(chunk))
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
            
            if (obj.has("error")) {
                val error = obj.getJSONObject("error")
                FileLog.e("GominLiveManager Server Error: ${error.optString("message")}")
                AndroidUtilities.runOnUIThread { stopSession() }
                return
            }

            if (obj.has("setupComplete")) {
                isSetupComplete = true
                startAudioThreads()
            }

            if (obj.has("serverContent")) {
                val serverContent = obj.getJSONObject("serverContent")

                if (serverContent.has("inputTranscription")) {
                    val inputTranscription = serverContent.getJSONObject("inputTranscription")
                    val transcriptionText = inputTranscription.optString("text", "")
                    val isPartial = inputTranscription.optBoolean("partial", true)
                    if (transcriptionText.isNotEmpty()) {
                        onTextReceived?.invoke(transcriptionText)
                        if (!isPartial) {
                            onTurnComplete?.invoke()
                        }
                    }
                }
                
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
                                    webSocket?.send(responseJson.toString())
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

        synchronized(audioLock) {
            try { echoCanceler?.enabled = false; echoCanceler?.release() } catch (e: Exception) { }
            try { noiseSuppressor?.enabled = false; noiseSuppressor?.release() } catch (e: Exception) { }
            try { audioRecord?.stop(); audioRecord?.release() } catch (e: Exception) { }
            try { audioTrack?.stop(); audioTrack?.release() } catch (e: Exception) { }

            echoCanceler = null
            noiseSuppressor = null
            audioRecord = null
            audioTrack = null
        }
        webSocket = null
        AndroidUtilities.runOnUIThread { onConnectionClosed() }
    }
}
