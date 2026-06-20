package uz.unnarsx.cherrygram.chats.gemini

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit-тести для валідації структури JSON-пакета ініціалізації Gemini Live API.
 *
 * Правильна структура (згідно з офіційною документацією Google, червень 2026):
 * {
 *   "setup": {
 *     "model": "models/gemini-3.1-flash-live-preview",
 *     "responseModalities": ["AUDIO"],      // <-- НА РІВНІ setup!
 *     "speechConfig": { ... },              // <-- НА РІВНІ setup!
 *     "systemInstruction": { ... }          // <-- НА РІВНІ setup!
 *   }
 * }
 *
 * УВАГА: responseModalities і speechConfig — НЕ загорнуті в generationConfig!
 * Docs: https://ai.google.dev/gemini-api/docs/live-api/get-started-websocket
 */
class GominLiveManagerPayloadTest {

    @Test
    fun testSetupPayloadStructureInVoiceMode() {
        val payload = GominLiveManager.buildSetupPayload(
            isTranscriptionMode = false,
            targetModel = "models/gemini-3.1-flash-live-preview"
        )

        // 1. Кореневий ключ має бути "setup"
        assertTrue("Кореневий ключ має бути 'setup'", payload.has("setup"))
        val setup = payload.getJSONObject("setup")

        // 2. Модель має бути під "setup"
        assertEquals("models/gemini-3.1-flash-live-preview", setup.getString("model"))

        // 3. responseModalities має бути НАПРЯМУ під "setup" (НЕ в generationConfig!)
        assertTrue("responseModalities має бути напряму в setup", setup.has("responseModalities"))
        val modalities = setup.getJSONArray("responseModalities")
        assertEquals("AUDIO", modalities.getString(0))

        // 4. speech_config має бути НАПРЯМУ під "setup" (НЕ в generationConfig!)
        assertTrue("speech_config має бути напряму в setup", setup.has("speech_config"))
        val speechConfig = setup.getJSONObject("speech_config")
        val voiceConfig = speechConfig.getJSONObject("voice_config")
        val prebuilt = voiceConfig.getJSONObject("prebuilt_voice_config")
        assertEquals("Puck", prebuilt.getString("voice_name"))

        // 5. systemInstruction має бути безпосередньо під "setup"
        assertTrue("systemInstruction має бути в setup", setup.has("systemInstruction"))
        val sysInstruction = setup.getJSONObject("systemInstruction")
        val parts = sysInstruction.getJSONArray("parts")
        assertTrue("systemInstruction.parts повинен мати хоча б один елемент", parts.length() > 0)

        // 6. generationConfig НЕ повинен існувати (структура плоска!)
        assertFalse(
            "generationConfig НЕ повинен існувати — responseModalities напряму в setup",
            setup.has("generationConfig")
        )

        // 7. Не повинно бути inputAudioTranscription та outputAudioTranscription у голосовому режимі
        assertFalse(
            "inputAudioTranscription не повинен бути в голосовому режимі",
            setup.has("inputAudioTranscription")
        )
        assertFalse(
            "outputAudioTranscription не повинен бути в голосовому режимі",
            setup.has("outputAudioTranscription")
        )
    }

    @Test
    fun testSetupPayloadStructureInTranscriptionMode() {
        val payload = GominLiveManager.buildSetupPayload(
            isTranscriptionMode = true,
            targetModel = "models/gemini-3.1-flash-live-preview"
        )

        val setup = payload.getJSONObject("setup")

        // 1. responseModalities має бути TEXT
        assertTrue("responseModalities має бути напряму в setup", setup.has("responseModalities"))
        val modalities = setup.getJSONArray("responseModalities")
        assertEquals("TEXT", modalities.getString(0))

        // 2. inputAudioTranscription має бути присутній
        assertTrue("Має бути присутній inputAudioTranscription", setup.has("inputAudioTranscription"))

        // 3. НЕ повинно бути systemInstruction у транскрипційному режимі
        assertFalse("Має бути відсутній systemInstruction", setup.has("systemInstruction"))

        // 4. НЕ повинно бути speech_config у транскрипційному режимі
        assertFalse("Має бути відсутній speech_config", setup.has("speech_config"))

        // 5. generationConfig НЕ повинен існувати
        assertFalse(
            "generationConfig НЕ повинен існувати",
            setup.has("generationConfig")
        )
    }

    @Test
    fun testSetupPayloadJsonOutput() {
        // Перевіряємо що фінальний JSON точно відповідає очікуваній структурі
        val payload = GominLiveManager.buildSetupPayload(
            isTranscriptionMode = false,
            targetModel = "models/gemini-3.1-flash-live-preview"
        )
        val jsonStr = payload.toString()

        // Має містити ці ключі на правильному рівні
        assertTrue("JSON повинен містити 'setup'", jsonStr.contains("\"setup\""))
        assertTrue("JSON повинен містити 'responseModalities'", jsonStr.contains("\"responseModalities\""))
        assertTrue("JSON повинен містити 'speech_config'", jsonStr.contains("\"speech_config\""))
        assertTrue("JSON повинен містити 'voice_name'", jsonStr.contains("\"voice_name\""))

        // НЕ повинен містити generationConfig
        assertFalse(
            "JSON НЕ повинен містити 'generationConfig'",
            jsonStr.contains("\"generationConfig\"")
        )
    }
}
