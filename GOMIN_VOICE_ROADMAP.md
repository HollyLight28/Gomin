# Gomin AI Voice & UI Roadmap (May 2026 Edition)

## 1. Research: Gemini 3 Flash & 2.5 Native Audio

### Gemini 3 Flash Live API
- **Context Limit:** 65,000 tokens per minute (TPM).
- **Latency:** Ultra-low, optimized for real-time voice interaction.
- **Usage:** Best for rapid-fire conversation, real-time feedback, and immediate responses.

### Gemini 2.5 Native Audio
- **Context Limit:** High-capacity, up to 1,000,000 tokens per minute.
- **Capabilities:** Direct audio-to-audio processing, preserves emotional nuances, pitch, and tone without STT/TTS overhead.
- **Usage:** Deep analysis of long recordings, emotional profiling, and high-context mental shielding.

---

## 2. Implementation Phases

### Phase 1: UI Overhaul (Enterprise Standard)
- **FAB Stack Logic:**
    1. **Bottom:** New Message (Primary FAB, 56dp).
    2. **Middle:** Gomin AI (Large FAB, 56dp, using `gomin_bird` icon).
    3. **Top:** Story Camera (Small FAB, transparent, auto-hide when stories are disabled).
- **Styling:** Adaptive accent colors, shadow depth matching Telegram's native elevation, removal of `isInverse` force-black style.

### Phase 2: Mental Shield 2.0 (Expert Mode)
- **Deep Integration:** Automatic injection of `shieldSystemPrompt` when transitioning from Shield to Chat.
- **Context Seeding:** Pre-briefing the AI with conversation history before the user starts typing.

### Phase 3: Live Voice Implementation
- **Audio-Native Integration:** Use Gemini 2.5 for emotional analysis of voice messages.
- **Live Mode:** WebSocket-based streaming using Gemini 3 Flash Live for real-time conversational partner.

---

## 3. Current Tasks
- [ ] Implement Task 1 (UI FAB Stack & Icon replacement).
- [ ] Implement Task 3 (Mental Shield 2.0 Expert Mode persistence).
- [ ] Setup WebSocket boilerplate for Live API.
