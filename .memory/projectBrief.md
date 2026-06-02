# Project Brief: Gomin (The Ukrainian Telegram Suite)

## Goal
Transform a Telegram fork into a high-end, professional Ukrainian client ("Gomin") with integrated "Telegram Cloud" storage and an AI-powered assistant.

## Core Features (Active & Implemented)
1. **Gomin Speed Engine (Boost Extreme & "Укриття")**: Parallel multithreaded network engine (12 TCP streams of 1MB blocks) boosting downloads/uploads by 2-3x, with a specialized "Slow Network" package stream for bomb shelters/subways.
2. **Gomin AI & Gomin Shield**: Deep Google Gemini integration. A local virtual AI Chat companion (`dialog_id = 99999999L`) and Gomin Shield — a behavioral profiler analyzing chat transcripts for toxicity and gaslighting, with seamless context forwarding.
3. **Gomin Ghost Mode**: High-level privacy guard including Read receipt bypass (Unread Guard), Typing status concealment, Anonymous stories viewing, Hidden online status, and 🛡️ Anti-Delete message retention.
4. **Gomin Air Alert**: Direct integration with Ajax Systems API (`api.ukrainealarm.com`) providing region/city-based real-time civil defense alerts and local sirens.
5. **Gomin Black Edition & Craft Typography**: Strict, ultra-contrast OLED black-and-white theme with craft Manrope typography to reduce eye strain.
6. **OTA Auto-Updater**: Native background engine pulling and installing official updates directly from the GitHub Releases API.

## Roadmap / Planned
- **Gomin Drive**: A native cloud file-system built on top of Telegram's Saved Messages utilizing custom Shared Media interfaces.

## Technical Stack

- **Base**: Cherrygram (Telegram fork)
- **Language**: Java/Kotlin
- **Build System**: Gradle
- **Fonts**: Nunito.
- **AI**: Gemini/Vertex AI integration.

## App Configuration
- **App Title**: Gomin
- **App api_id**: 35162000
- **App api_hash**: 8686113844de267311e15037880ae97b
