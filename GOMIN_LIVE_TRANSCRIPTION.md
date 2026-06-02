# Gomin Live Transcription & Voice Call: The Ultimate Implementation Plan

*Date: June 2, 2026*
*Author: Senior AI Engineer (Gomin Project)*

## 1. Architectural Vision & Feasibility
The decision to use the **Gemini Multimodal Live API** for text transcription (instead of traditional STT like Whisper) is an advanced, highly effective approach. 
* **Benefits:** Zero-latency streaming (text appears as you speak), contextual grammar correction, and native integration into the existing WebSocket architecture.
* **Models:** 
  * **Voice Call (FAB):** `models/gemini-2.5-flash` (CRITICAL: In the Gemini Live WebSocket API, the "Native Audio" capability is unlocked by using the `BidiGenerateContent` endpoint with this specific model. Do NOT fall back to older experimental models. If the user specifies a variant like `models/gemini-2.5-flash-audio` in settings, respect it, but the base string is valid for WebSockets).
  * **Transcription (Attach Button):** `models/gemini-3.0-live` (Use this exact string for the 3.0 Live API. Do not hallucinate older models).

---

## 2. Core Protocol Fixes (The "Why it wasn't working" section)
The previous implementation failed due to protocol mismatches with Google's WebSocket API.

### A. The Model Hardcoding Bug
The Live API relies heavily on the `BidiGenerateContent` WebSocket endpoint to unlock "Native Audio" streams, rather than relying exclusively on a completely different model name. 
**Action:** In `GominLiveManager.kt`, REMOVE ANY dependency on `CherrygramMessagesConfig.geminiModelName` because the text chat config should NOT dictate the Live API models.
* For Transcription: Hardcode to `models/gemini-3.0-live`
* For Voice Call (FAB): Hardcode to `models/gemini-2.5-flash` (Unless the user explicitly asks to use a different native audio string). DO NOT use `models/gemini-2.0-flash-exp`.

### B. The `camelCase` Requirement
Google's Live API WebSocket strictly requires `camelCase` JSON keys. The old code used `snake_case`.
**Action:** In `GominLiveManager.kt`, all JSON payloads must be rewritten:
* `setup` payload: `generation_config` -> `generationConfig`, `response_modalities` -> `responseModalities`, `system_instruction` -> `systemInstruction`.
* Input payload: `realtime_input` -> `realtimeInput`, `media_chunks` -> `mediaChunks`, `mime_type` -> `mimeType`.
* Server parsing: Must parse `serverContent`, `modelTurn`, `turnComplete`, `setupComplete`.

### C. The Tool Calling Crash
Sending unfinished tool configurations (like Air Alerts) in the `setup` payload will cause Google to drop the WebSocket connection with a `400 Bad Request` or `1008 Policy Violation` if the Live API model doesn't support them perfectly.
**Action:** Entirely remove or comment out the `put("tools", toolsArray)` block from the `sendSetupMessage` in `GominLiveManager.kt`. Achieve a stable voice/text connection FIRST before reintroducing tools.

### D. The Handshake Bug (`setupComplete`)
The previous code started recording audio immediately after sending the `setup` message.
**Action:** `GominLiveManager.kt` must wait to receive `{"setupComplete": {}}` from the server before executing `startAudioThreads()`. Sending audio prematurely causes the server to drop the connection. DO NOT use `return` immediately after processing `setupComplete` in the parser, as it might swallow subsequent `serverContent` data in the same JSON packet.

### E. AudioLock Thread Crash & Spam 429 Errors
**Action 1 (Crash):** In `stopSession()`, safely wait for `recordThread` to finish or handle the interruption gracefully before calling `audioRecord?.release()` inside `synchronized(audioLock)`. Otherwise, an `IllegalStateException` occurs.
**Action 2 (Spam):** Accumulate audio byte buffers (e.g., 100ms worth of PCM data) before sending the Base64 chunk to the WebSocket. Sending 5ms chunks will result in a `429 Too Many Requests` error.

---

## 3. UI/UX: The "Smart Paperclip" Workflow
To solve the disappearing paperclip and editing issues, we will implement a hybrid toggle system in `ChatActivityEnterView.java` and `GominAiChatHelper.kt`.

### The Workflow:
1. **Start:** User long-presses the `attachButton` (Paperclip).
2. **State Change:** 
   * Haptic feedback triggers.
   * `attachButton` icon changes to a **Red Recording Dot** (`R.drawable.msg_voice_record` or similar).
   * `attachButton` is locked to `VISIBLE` (ignoring the usual rule that hides it when text is typed).
3. **Streaming:** Text appears in `messageEditText` dynamically.
4. **Stop (To Edit):** User taps the Red Recording Dot. The session closes, the icon reverts to a Paperclip, and text remains in the input field.
5. **Stop (To Send):** User taps the standard Send button. The session closes instantly, and the message is sent.

### Technical Implementation Steps:
1. **`ChatActivityEnterView.java`:**
   * Modify `checkSendButtonVisibility()` to prevent hiding the `attachButton` if `GominAiChatHelper.isTranscriptionActive()` is true.
   * Add a method `setAttachButtonToRecordMode(boolean isRecording)` to swap the icon and color filter dynamically.
2. **`GominAiChatHelper.kt`:**
   * Implement `toggleTranscriptionSession()` to handle the start/stop logic and call `setAttachButtonToRecordMode`.
   * Improve the `onTextReceived` callback. Since Live API streams text chunks, we must append them smoothly without duplicating words if the model revises its sentence mid-stream.
3. **`GominLiveManager.kt`:**
   * Accept the dynamic model name (`gemini-3.0-live` for text, `gemini-2.5-flash` for voice).
   * Ensure `responseModalities` is set to `["TEXT"]` for transcription to save bandwidth.

---

## 4. Execution Rules for the Next AI Session
1. **READ THIS FILE FIRST.**
2. Do not use `snake_case` for the Live API WebSockets.
3. Implement the `setupComplete` lock in `GominLiveManager.kt` before touching the UI.
4. Carefully inject the `ChatActivityEnterView` UI hooks without breaking Telegram's complex animation logic for the attach/send buttons.
5. Provide the user with a buildable, crash-free implementation.