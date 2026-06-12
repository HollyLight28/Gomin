# CURRENT MISSION
1. Research Gomin Air Alert stats map extension, OTA updates logic, and project health assessment. [COMPLETED]
2. Implement Air Alert Statistics Screen in DialogsActivity options menu. [COMPLETED]
3. Re-route "About Gomin" preference button to the second slot in Gomin Settings. [COMPLETED]
4. Fix Gemini Live API mic initialization, request code permissions collision, payload serialization casing, and implement automatic voice greetings. [COMPLETED]
5. Verify OTA Auto-Updater flow (simulate downgrade version and test updater integration). [COMPLETED]
6. Diagnose and fix the Gemini Live API mic activation issue, migrate to active stable models, add full error UI indicators (Toasts) and move device initialization to background thread. [COMPLETED]
7. Adjust scale of installer and notification drawer app icon. [COMPLETED by USER]

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

# OPEN PROBLEMS
- None.

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
- `.memory/activeContext.md` -> [MODIFY] Updated progress state and mission status.
