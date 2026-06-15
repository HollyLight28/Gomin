# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

- **Debug APK** (testing): `./gradlew assembleAfatDebug`
- **Standalone Release APK** (direct distribution): `./gradlew assembleAfatStandalone`
- **Google Play AAB**: `./gradlew bundleAfatRelease`
- **Install debug on device**: `./gradlew installAfatDebug`
- **Clean**: `./gradlew clean`
- **Run unit tests**: `./gradlew test`
- **Run a single test class**: `./gradlew :TMessagesProj:test --tests "uz.unnarsx.cherrygram.chats.gemini.GominLiveManagerPayloadTest"`
- **Run a specific test method**: `./gradlew :TMessagesProj:test --tests "uz.unnarsx.cherrygram.chats.gemini.GominLiveManagerPayloadTest.testSetupPayloadStructureInVoiceMode"`

Build requires: JDK 17, Android SDK 36, NDK 21.4.7075529. All build variants use the `afat` flavor (arm64-v8a + armeabi-v7a).

## Project Architecture

This is **Гомін (Gomin)**, a Ukrainian fork of Telegram for Android. It extends the official Telegram Android client (TMessagesProj) with custom features, UI overrides, and AI integrations.

### Module Structure

| Module | Role |
|--------|------|
| `:TMessagesProj` | Core library — all Telegram logic, UI, native code, custom features |
| `:TMessagesProj_AppStandalone` | Standalone APK build (direct distribution, no Google Play) |
| `:TMessagesProj_App` | Google Play AAB build |
| `:TMessagesProj_AppHuawei` | Huawei AppGallery build (disabled in settings.gradle) |

### Key Packages

```
org.telegram.messenger/          — Core app: ApplicationLoader, MessagesController,
                                   MediaDataController, NotificationCenter,
                                   SharedConfig, UserConfig, BuildVars, FileLog
org.telegram.ui/                 — All UI screens (ChatActivity, LaunchActivity, etc.)
                                   + Components/, Cells/, ActionBar/, Adapters/
org.telegram.tgnet/              — MTProto network layer (ConnectionsManager, TLRPC,
                                   NativeByteBuffer, Requests)
org.telegram.SQLite/             — Local database layer
org.webrtc/                      — WebRTC for VoIP
uz.unnarsx.cherrygram.core/      — Cherrygram config system, CGFeatureHooks, UI helpers
uz.unnarsx.cherrygram.configs/   — Feature toggles (8 config classes, one per domain):
                                   CherrygramCoreConfig, ChatsConfig, CameraConfig,
                                   AppearanceConfig, PrivacyConfig, etc.
uz.unnarsx.cherrygram.chats/     — Chat UI overrides, filters, translator
uz.unnarsx.cherrygram.chats.gemini/  — Google Gemini AI integration:
                                   gominAiChatHelper, GominLiveManager (WebSocket Live API),
                                   GeminiButtonsLayout, GeminiResultsBottomSheet,
                                   GominShieldBottomSheet, GominMicrophoneService
uz.unnarsx.cherrygram.chats.gemini.network/ — Gemini API client (ApiClient, ApiCallback)
uz.unnarsx.cherrygram.alerts/    — Ukrainian air raid alert system (AirAlertController,
                                   AirAlertHelper, AirAlertNotificationHelper)
uz.unnarsx.cherrygram.preferences/ — Settings screens organized by domain:
                                   GeminiPreferencesEntry, AppearancePreferencesEntry,
                                   PrivacyPreferencesEntry, etc.
uz.unnarsx.cherrygram.helpers/   — NetworkHelper, StickersManager, UI helpers
uz.unnarsx.cherrygram.camera/    — CameraX integration
uz.unnarsx.cherrygram.core/icons/ — Vector icon system, icon replaces
uz.unnarsx.cherrygram.donates/   — Donation screen
```

### Build System

- `build.gradle` (root): AGP 8.11.1, Kotlin 2.2.0, Google Services, Huawei AGCP
- `TMessagesProj/build.gradle`: Android library, minSdk 23, targetSdk 35, compileSdk 36
- `TMessagesProj_AppStandalone/build.gradle`: Application module with CI/CD support (auto-copy APK to desktop)
- Gradle properties: version tracking via `APP_VERSION_NAME_CHERRY` + Telegram base `APP_VERSION_NAME`
- Native code via CMake (NDK 21): opus, rlottie, ffmpeg, boringssl, mozjpeg, tgnet, voip, exoplayer

### Gomin-Specific Features

1. **Gomin AI (Google Gemini)**: Native AI chat via Gemini API. `GeminiSDKImplementation` wraps the `generativeai` SDK. `GominLiveManager` implements WebSocket-based Live API for real-time audio streaming (low-latency dictation, voice chat). Uses `GominAiChatHelper.dedupTranscriptionChunk()` to handle cumulative streaming text.

2. **Gomin Shield**: Psychological analysis of conversations — analyzes up to 1500 recent messages for toxicity, gaslighting, red flags. Runs in `GominShieldBottomSheet`.

3. **Air Raid Alerts**: Polls official alert channels via a custom server. `AirAlertController` manages state and scheduling, `AirAlertNotificationHelper` handles notifications, `AirAlertStopReceiver` handles alarm intents. Red header overlay during active alerts.

4. **Ghost Mode**: Four-layer privacy: Unread Guard (read without marking read), activity masking, hidden online, anonymous stories, anti-delete (keeps messages cached even after "delete for everyone").

5. **Gomin Speed Engine**: Network optimization — 12 parallel TCP streams with 1MB segments (vs 4×128KB upstream), configurable Shelter Mode (32KB packets for weak connection).

6. **Black Edition**: OLED black theme, Nunito font throughout, search bar at bottom, clean experience (no sponsored channels, no ads, no trackers).

### Testing

Unit tests are in `TMessagesProj/src/test/java/` matching the source package structure. They use JUnit 4 with `org.json.JSONObject` for payload testing. No Android dependencies — `testOptions.unitTests.returnDefaultValues = true` in build.gradle. Tests cover: Gemini Live API payload structure, transcription dedup logic, air alert region matching, and string resource validation.

### Key Patterns & Conventions

- **Event bus**: `NotificationCenter` with string-keyed observers (not Kotlin Flow / LiveData)
- **Threading**: `Utilities.globalQueue` (background) / `AndroidUtilities.runOnUIThread` (UI) — avoid raw threads/coroutines unless necessary
- **Config**: All feature toggles live in `Cherrygram*Config.kt` objects loaded from `SharedPreferences`
- **Hooks**: `CGFeatureHooks.kt` injects Cherrygram features into Telegram's source with minimal edits
- **No DI framework**: Manual dependency wiring through singletons and static helpers
- **Resources**: Custom resources in `res-cherrygram/` directory with `values-uk/` for Ukrainian localizations
