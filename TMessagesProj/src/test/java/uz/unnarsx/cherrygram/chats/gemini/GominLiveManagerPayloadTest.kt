package uz.unnarsx.cherrygram.chats.gemini

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit-тести для валідації структури JSON-пакета ініціалізації Gemini Live API.
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
        
        // 3. generationConfig має бути під "setup"
        assertTrue(setup.has("generationConfig"))
        val genConfig = setup.getJSONObject("generationConfig")
        
        // 4. responseModalities має бути всередині generationConfig
        assertTrue(genConfig.has("responseModalities"))
        val modalities = genConfig.getJSONArray("responseModalities")
        assertEquals("AUDIO", modalities.getString(0))
        
        // 5. speechConfig має бути всередині generationConfig
        assertTrue(genConfig.has("speechConfig"))
        
        // 6. systemInstruction має бути безпосередньо під "setup"
        assertTrue(setup.has("systemInstruction"))
    }

    @Test
    fun testSetupPayloadStructureInTranscriptionMode() {
        val payload = GominLiveManager.buildSetupPayload(
            isTranscriptionMode = true,
            targetModel = "models/gemini-3.1-flash-live-preview"
        )
        
        val setup = payload.getJSONObject("setup")
        
        // В режимі транскрипції не повинно бути системної інструкції та speechConfig
        assertTrue(setup.has("generationConfig"))
        val genConfig = setup.getJSONObject("generationConfig")
        
        // Але має бути inputAudioTranscription
        assertTrue("Має бути присутній inputAudioTranscription", setup.has("inputAudioTranscription"))
        assertTrue("Має бути відсутній systemInstruction", !setup.has("systemInstruction"))
        assertTrue("Має бути відсутній speechConfig", !genConfig.has("speechConfig"))
    }
}
