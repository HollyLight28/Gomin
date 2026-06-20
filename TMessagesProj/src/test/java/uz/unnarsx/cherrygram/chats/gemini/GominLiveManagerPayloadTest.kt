package uz.unnarsx.cherrygram.chats.gemini

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit-тести для валідації структури JSON-пакета ініціалізації Gemini Live API.
 *
 * ПРАВИЛЬНА структура (ВЕРИФІКОВАНО 2026-06-20 live-тестом):
 * {
 *   "setup": {
 *     "model": "models/gemini-3.1-flash-live-preview",
 *     "generation_config": {
 *       "response_modalities": ["AUDIO"],
 *       "speech_config": { "voice_config": { "prebuilt_voice_config": { "voice_name": "Puck" } } }
 *     },
 *     "system_instruction": { "parts": [{ "text": "..." }] }
 *   }
 * }
 *
 * ВАЖЛИВО: response_modalities і speech_config — ВСЕРЕДИНІ generation_config.
 * Сервер явно відкидає їх на рівні setup:
 *   "Unknown name 'responseModalities' at 'setup': Cannot find field."
 * УСІ поля — snake_case.
 */
class GominLiveManagerPayloadTest {

    @Test
    fun testSetupPayloadStructureInVoiceMode() {
        val payload = GominLiveManager.buildSetupPayload(
            isTranscriptionMode = false,
            targetModel = "models/gemini-3.1-flash-live-preview"
        )

        // 1. Кореневий ключ "setup"
        assertTrue("Кореневий ключ має бути 'setup'", payload.has("setup"))
        val setup = payload.getJSONObject("setup")

        // 2. Модель
        assertEquals("models/gemini-3.1-flash-live-preview", setup.getString("model"))

        // 3. generation_config має існувати
        assertTrue("generation_config має бути присутній", setup.has("generation_config"))
        val genConfig = setup.getJSONObject("generation_config")

        // 4. response_modalities — всередині generation_config
        assertTrue("response_modalities має бути в generation_config", genConfig.has("response_modalities"))
        val modalities = genConfig.getJSONArray("response_modalities")
        assertEquals("AUDIO", modalities.getString(0))

        // 5. speech_config — всередині generation_config
        assertTrue("speech_config має бути в generation_config", genConfig.has("speech_config"))
        val speechConfig = genConfig.getJSONObject("speech_config")
        val voiceConfig = speechConfig.getJSONObject("voice_config")
        val prebuilt = voiceConfig.getJSONObject("prebuilt_voice_config")
        assertEquals("Puck", prebuilt.getString("voice_name"))

        // 6. system_instruction — на рівні setup
        assertTrue("system_instruction має бути в setup", setup.has("system_instruction"))
        val sysInstruction = setup.getJSONObject("system_instruction")
        val parts = sysInstruction.getJSONArray("parts")
        assertTrue("system_instruction.parts повинен мати хоча б один елемент", parts.length() > 0)

        // 7. response_modalities НЕ на рівні setup
        assertFalse("response_modalities НЕ повинен бути напряму в setup", setup.has("response_modalities"))

        // 8. speech_config НЕ на рівні setup
        assertFalse("speech_config НЕ повинен бути напряму в setup", setup.has("speech_config"))

        // 9. input_audio_transcription відсутній у голосовому режимі
        assertFalse("input_audio_transcription не повинен бути", setup.has("input_audio_transcription"))
    }

    @Test
    fun testSetupPayloadStructureInTranscriptionMode() {
        val payload = GominLiveManager.buildSetupPayload(
            isTranscriptionMode = true,
            targetModel = "models/gemini-3.1-flash-live-preview"
        )

        val setup = payload.getJSONObject("setup")
        val genConfig = setup.getJSONObject("generation_config")

        // 1. response_modalities = TEXT
        assertTrue("response_modalities має бути в generation_config", genConfig.has("response_modalities"))
        val modalities = genConfig.getJSONArray("response_modalities")
        assertEquals("TEXT", modalities.getString(0))

        // 2. input_audio_transcription присутній
        assertTrue("input_audio_transcription має бути", setup.has("input_audio_transcription"))

        // 3. system_instruction відсутній у транскрипційному режимі
        assertFalse("system_instruction має бути відсутній", setup.has("system_instruction"))

        // 4. speech_config відсутній у транскрипційному режимі
        assertFalse("speech_config має бути відсутній", genConfig.has("speech_config"))
    }

    @Test
    fun testSetupPayloadJsonOutput() {
        val payload = GominLiveManager.buildSetupPayload(
            isTranscriptionMode = false,
            targetModel = "models/gemini-3.1-flash-live-preview"
        )
        val jsonStr = payload.toString()

        // Мають бути ключі на правильних рівнях
        assertTrue("JSON повинен містити 'setup'", jsonStr.contains("\"setup\""))
        assertTrue("JSON повинен містити 'generation_config'", jsonStr.contains("\"generation_config\""))
        assertTrue("JSON повинен містити 'response_modalities'", jsonStr.contains("\"response_modalities\""))
        assertTrue("JSON повинен містити 'speech_config'", jsonStr.contains("\"speech_config\""))
        assertTrue("JSON повинен містити 'voice_name'", jsonStr.contains("\"voice_name\""))
        assertTrue("JSON повинен містити 'system_instruction'", jsonStr.contains("\"system_instruction\""))
    }
}
