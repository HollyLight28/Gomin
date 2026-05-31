# Gomin Live Transcription: Research & Implementation Plan

## 1. Overview & Vision
The goal is to implement a "seamless" voice-to-text experience using the **Gemini Multimodal Live API**. Unlike traditional Speech-to-Text (STT) systems like OpenAI's Whisper (which typically process audio files after recording), the Live API allows for **real-time, context-aware transcription** via WebSockets.

### Why Gemini Live API?
*   **Zero Latency:** Text appears in the input field as you speak.
*   **Contextual Intelligence:** Because Gemini is an LLM, it doesn't just phonetically transcribe; it understands the sentence structure, correcting grammar and punctuation on the fly.
*   **Low Resource Usage:** The heavy lifting is done on Google's servers, not the mobile device.
*   **Free Tier Advantage:** As of May 2026, the Gemini 2.0/3.0 Flash Live API offers high limits (65k tokens/min) for free, making it ideal for a "personal Jarvis" feature.

---

## 2. Deep Dive: Technical Research
Based on the Gemini Live API documentation (WebSocket protocol):

1.  **WebSocket Handshake:** The client connects to `wss://generativelanguage.googleapis.com/ws/...`.
2.  **Configuration (Setup):**
    *   `response_modalities`: We will set this to `["TEXT"]` for transcription mode to save bandwidth.
    *   `system_instruction`: We will inject a specialized prompt: *"You are a stealth transcription assistant. Convert incoming audio to precise Ukrainian text. Fix grammar, apply punctuation, and output ONLY the transcript without comments."*
3.  **Real-time Audio Streaming:**
    *   Format: 16-bit PCM, Mono, 16kHz.
    *   Transmission: Base64-encoded chunks sent via `realtimeInput`.
4.  **Barge-in / Interruption:** Using server-side Voice Activity Detection (VAD) to know when the user stops speaking.

---

## 3. Implementation Plan (The "Silent" Approach)

### UI/UX: The "Invisible" Trigger
To avoid cluttering the already busy Telegram UI, we will use a **Long-Tap on the Attachment (Paperclip) icon**.

*   **Action:** Long-tap `attachButton`.
*   **Feedback:** A subtle haptic vibration (Tactile feedback).
*   **System Status:** The Android system microphone indicator (green dot) will appear in the status bar.
*   **Blocking Logic:** We must override the standard Telegram behavior where the paperclip icon disappears when text is entered. We will keep it visible as long as the transcription session is active.

### Technical Steps:

#### Phase 1: GominLiveManager.kt Enhancements
*   Add `isTranscriptionMode` flag.
*   Modify `sendSetupMessage` to request `TEXT` modality only.
*   Implement a `onTextReceived` callback to pipe real-time text back to the helper.

#### Phase 2: GominAiChatHelper.kt Integration
*   Implement `startQuickTranscription(activity)`.
*   Handle the insertion of text into `ChatActivityEnterView.getEditField()`.
*   Implement a "Smart Append" logic: if the field isn't empty, add a space before the new transcription.

#### Phase 3: ChatActivityEnterView.java Hook
*   Inject `OnLongClickListener` into the `attachButton`.
*   Modify `checkSendButtonVisibility` or `updateFieldRight` to prevent the attach button from hiding while `GominLiveManager.isSessionActive`.
*   Add a 3-second silence timeout: if no audio is detected, automatically close the session to allow the UI to transition back to the "Send" button.

---

## 4. Why "Long-Tap on Paperclip"?
1.  **Logical Mapping:** Paperclip means "add something to the chat". Adding your voice-as-text fits this mental model.
2.  **Compatibility:** Does not interfere with "Tap" (opens menu) or "Mic Icon" (records standard voice messages).
3.  **Ergonomics:** Easy to reach with the thumb while typing.

---

## 5. Potential Risks & Solutions
*   **Risk:** Slow network causes text to "jump" or duplicate.
    *   **Solution:** Use Gemini's `turn_complete` signals to manage text buffers correctly.
*   **Risk:** Users might be confused if there's no visual overlay.
    *   **Solution:** A very faint, pulsing glow around the input field (2dp stroke) to signal "Gomin is listening".

---
*Created by Opencode (Senior AI Engineer) - June 1, 2026*
