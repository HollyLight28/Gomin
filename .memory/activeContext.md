# CURRENT MISSION
1. Research Gomin Air Alert stats map extension, OTA updates logic, and project health assessment. [COMPLETED]
2. Implement Air Alert Statistics Screen in DialogsActivity options menu. [COMPLETED]
3. Re-route "About Gomin" preference button to the second slot in Gomin Settings. [COMPLETED]
4. Fix Gemini Live API mic initialization, request code permissions collision, payload serialization casing, and implement automatic voice greetings. [COMPLETED]
5. Verify OTA Auto-Updater flow (simulate downgrade version and test updater integration). [COMPLETED]
6. Diagnose and fix the Gemini Live API mic activation issue, migrate to active stable models, add full error UI indicators (Toasts) and move device initialization to background thread. [COMPLETED]
7. Adjust scale of installer and notification drawer app icon to clean 0.10 scale, flattening vector path coordinates to eliminate nested groups and match selector preview dimensions. [COMPLETED]
8. Audit the Gemini Live API WebSocket connection parameters and message schemas, comparing them with the official documentation and the local implementation, and prepare a detailed explanation. [COMPLETED]


# COMPLETED ATOMIC STEPS
- Added Android Log redirects in `GominLiveManager.kt` to ensure WebSocket lifecycle events are visible in logcat.
- Cleaned setup JSON payload by removing empty `inputAudioTranscription` configuration block.
- Replaced ScrollView with NestedScrollView in `GominShieldBottomSheet.kt` to fix dismiss swipe issues.
- Created `air_alert_silent` notification channel, added cancel sound to `air_alert_info` channel, implemented self-healing channel recreation logic.
- Moved channel initialization to `ApplicationLoader.java` and removed duplicate call from `LaunchActivity.java`.
- Isolated MediaPlayer in `AirAlertController.kt` to test alerts, implemented silent notifications, and added global switch safety check.
- Removed siren shutdown call in `ScreenReceiver.java` on screen off.
- Deleted obsolete `airAlertApiKey` settings from `CherrygramCoreConfig.kt` and `CGPreferencesEntry.java`.
- Removed duplicate notifications trigger from `GcmPushListenerService.java`.
- Cleaned up obsolete python server scripts from root directory.
- Compiled project successfully.
- Built release standalone version using `assembleAfatStandalone` task.
- Copied APK outputs to User's Desktop at `C:\Users\VovA\Desktop\Cherry\Stable`.
- Audited modified files and resolved 5 critical bugs in Gomin Air Alert (unstable raw sound URIs, notification channel cached settings, test-to-real alert transitions, background setting leaks, and pre-Oreo compatibility).
- Analyzed OTA Updater (`UpdaterUtils`), DialogsActivity options menu structure, and CGChatMenuInjector extension points.
- Updated project memory with newly identified architecture patterns for OTA Auto-Updater and Air Alert Controller.
- Implemented `AirAlertStatsActivity.kt` extending `UniversalFragment` with premium design, listing 26 regions, fetching alert statuses in parallel (using Kotlin Coroutines and background thread pool), handling dynamic search filtering, and warning/peaceful states.
- Modified `CGChatMenuInjector.kt` to safely inject "Статистика тривог" item with outline notification icon in dialog activity options menu.
- Integrated `CGChatMenuInjector` call within `DialogsActivity.java` at options menu instantiation.
- Added "Про Гомін" (About Gomin) item into `CGPreferencesEntry.java` (Gomin Settings) at index 101, placed as the second option directly after the Monobank support card.
- Replaced `ActivityCompat.requestPermissions` with Telegram's native, collision-free `PermissionRequest.requestPermission` in `GominAiChatHelper.kt` to request `RECORD_AUDIO` and automatically resume session creation on grant.
- Corrected all client outgoing JSON WebSocket keys in `GominLiveManager.kt` from `snake_case` to `camelCase` per Google's official Gemini Live spec (fixing silent stream ignoring and transcription failure).
- Added `sendInitialGreetingTrigger()` in `GominLiveManager.kt` to send a conversational greeting prompt to the model voice-first upon `"setupComplete"`.
- Fixed `tool_response` / `function_responses` keys to `toolResponse` / `functionResponses` to make function calling over WebSockets function properly.
- Updated memory pattern definitions in `systemPatterns.md` to prevent future casing regressions.
- Fixed `DownloadReceiver` registration in `UpdaterUtils.java` using `ContextCompat.RECEIVER_EXPORTED` to enable OTA notifications for system `DownloadManager`.
- Replaced hardcoded "Cherrygram" title strings with dynamic `CGResourcesHelper.getAppName()` in `UpdateLayout.java` and `UpdaterBottomSheet.java` to support native Gomin branding.
- Pushed updater fixes to main repository.
- Checked if `Manifest.permission.RECORD_AUDIO` is already granted in `GominAiChatHelper.kt` before calling `PermissionRequest.requestPermission` to prevent callback issues on Android 14+.
- Implemented `AudioManager` VoIP mode (`MODE_IN_COMMUNICATION`) routing and focus management in `GominLiveManager.kt` to ensure mic priority.
- Added self-healing fallback to `MediaRecorder.AudioSource.MIC` in `GominLiveManager.kt` if `VOICE_COMMUNICATION` fails to initialize.
- Added circuit-breaker check for negative error codes in `AudioRecord.read()` inside `GominLiveManager.kt` to prevent silent infinite thread loops.
- Created `task.md` checkpoint for Live API debugging process.
- Updated `ic_launcher.xml` (User manually adjusted foreground inset to 12% to scale round icon to ~68%).
- Deployed the freshly built Gomin Standalone Universal APK to the connected device `37191JEHN05531` using ADB.
- Cleaned up `gomin.svg` by removing the outer black background box, setting path fill to `#ffffff` (white), and converting relative coordinates to absolute to prevent shift.
- Created `preview.html` to render the white bird SVG against a dark background for local browser preview.
- Fixed the shared drawable state contamination bug (Mutate Bug) in `AppIconsSelectorCell.java` by calling `mutate()` on the loaded foreground drawable.
- Flat-scaled the source bird vector path in `icon_foreground_gomin.xml` to a clean scale of 0.10 and Y-flipped translation, removing nested group transforms.
- Updated launcher icon templates in `generate_foregrounds.py` to remove redundant inner groups and re-generated all 7 adaptive launcher drawables.
- Standardized the status bar notification drawable `notification.xml` to clean scale 0.10.
- Synchronized `foreScale` inside `AppIconsSelectorCell.java`'s `AdaptiveIconImageView` to exactly `0.64f` to align selector preview sizes with home screen launcher sizes.
- Cleaned the Gradle build cache using `./gradlew clean` to work around resource merger caching failures.
- Compiled standalone release build APK successfully.
- Migrated `buildSetupPayload` in `GominLiveManager.kt` to a static, pure function inside `companion object` to enable clean unit testing on JVM without Android UI View dependencies.
- Added `org.json` JVM test dependency in `build.gradle` to allow proper `JSONObject` and `JSONArray` behavior in JVM unit tests.
- Fixed race conditions and resource leaks in `GominLiveManager.kt`'s audio threads by verifying `isSessionActive` under `synchronized(audioLock)` right before starting recording/playback and releasing resources immediately on errors.
- Verified all Gemini Live WebSocket payload initialization unit tests successfully.
- Analyzed GitHub Action workflows and verified that the repository contains all 5 required user-defined secrets (`SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD`, `SIGNING_STORE_PASSWORD`, `TELEGRAM_BOT_TOKEN`, `TELEGRAM_CHAT_ID`), confirming the 6th secret is the built-in `GITHUB_TOKEN`.
- Researched and documented Google Play Protect verification bypass steps (using the public Play Protect Appeal Form and local Play Protect disable options) for signed APK sideloading without a paid developer account.
- Solved silent background AudioRecord denial (green dot never appearing) by adding an `isForeground` tracker to `GominMicrophoneService` and making `GominLiveManager` wait until `startForeground` is fully processed by Android 14+.
- Fixed Gemini API silently dropping audio payloads by explicitly adding `;rate=16000` to the `audio/pcm` `mimeType` in WebSocket `mediaChunks`.
- Restructured `RECORD_AUDIO` permission check in `GominLiveManager.kt` to run before `startForegroundService` to prevent `SecurityException` crashes.
- Fixed `responseModalities` payload to request `TEXT` instead of `AUDIO` in transcription mode.


# OPEN PROBLEMS
None


# MODIFIED FILES
- `uz/unnarsx/cherrygram/alerts/AirAlertStatsActivity.kt` -> [NEW] Implemented premium Statistics screen with live parallel HTTP fetching of 26 Ukrainian regions, dynamic search filter, and reactive refresh.
- `uz/unnarsx/cherrygram/chats/CGChatMenuInjector.kt` -> [MODIFY] Added `injectAirAlertStats` method to inject statistics screen entry into DialogsActivity options menu.
- `org/telegram/ui/DialogsActivity.java` -> [MODIFY] Injected Air Alert Stats entry into options menu under the three-dots button.
- `uz/unnarsx/cherrygram/preferences/CGPreferencesEntry.java` -> [MODIFY] Added `cgAboutRow` constant and registered it in `fillItems()` and `onClick()` as the second item under the Monobank support card.
- `uz/unnarsx/cherrygram/chats/gemini/GominAiChatHelper.kt` -> [MODIFY] Use collision-free `PermissionRequest` wrapper for requesting RECORD_AUDIO permission.
- `uz/unnarsx/cherrygram/chats/gemini/GominLiveManager.kt` -> [MODIFY] Fixed outgoing JSON casing to camelCase, updated models, implemented background thread audio init, and detailed Toast logging.
- `.memory/systemPatterns.md` -> [MODIFY] Updated memory rules to enforce camelCase for the Gemini WebSocket Live API.
- `uz/unnarsx/cherrygram/core/updater/UpdaterUtils.java` -> [MODIFY] Fixed broadcast registration flags (`RECEIVER_EXPORTED`) for systemic `DownloadManager` callbacks.
- `org/telegram/ui/Components/UpdateLayout.java` -> [MODIFY] Refactored hardcoded app titles to dynamic brand names from resource helper.
- `uz/unnarsx/cherrygram/core/updater/UpdaterBottomSheet.java` -> [MODIFY] Replaced hardcoded brand names with helper calls.
- `res-cherrygram/mipmap-anydpi-v26/ic_launcher.xml` -> [MODIFY] Adjusted foreground inset to 12% to scale the round/installer/notification icon bird to ~68%.
- `gomin.svg` -> [MODIFY] Converted bird silhouette to solid white and removed outer black background.
- `preview.html` -> [NEW] Created HTML preview to test and view SVG.
- `generate_previews.py` -> [NEW] Created automated python flat coordinate scaler and SVG preview page generator.
- `TMessagesProj/src/main/res-cherrygram/drawable/icon_foreground_gomin.xml` -> [MODIFY] Updated vector drawable path to use flat pre-scaled coordinates and removed nested transform groups.
- `generate_foregrounds.py` -> [MODIFY] Cleaned templates to remove nested group wrappers and support flat coordinates.
- `TMessagesProj/src/main/res/drawable/notification.xml` -> [MODIFY] Updated notification icon scale to 1.0 (clean 0.10 scale).
- `TMessagesProj/src/main/java/org/telegram/ui/Cells/AppIconsSelectorCell.java` -> [MODIFY] Adjusted preview scale factor to 0.64f to precisely match adaptive launcher dimensions, and fixed the Drawable Mutate Bug.
- `TMessagesProj/build.gradle` -> [MODIFY] Added JVM unit test dependency for org.json.
- `TMessagesProj/src/test/java/uz/unnarsx/cherrygram/chats/gemini/GominLiveManagerPayloadTest.kt` -> [NEW] Implemented unit tests for voice and transcription modes Live API handshake configurations.
- `.memory/activeContext.md` -> [MODIFY] Updated progress state and mission status.
