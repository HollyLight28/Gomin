# CURRENT MISSION
1. Compile the standalone release version (`assembleAfatStandalone`) locally. [COMPLETED]
2. Conduct a deep investigation of the Gemini Live WebSocket API transcription failure in `GominLiveManager.kt` based on official Google documentation. [IN PROGRESS] (Note: Gemini Live API changes are currently frozen by user request).
3. Analyze three new notification sounds and implement the agreed setup. [COMPLETED]
4. Fix the white-on-white send button icon in the share panel on Monet dark theme and resolve bottom bar layout border misalignment. [COMPLETED]

# COMPLETED ATOMIC STEPS
- Performed spectral and temporal audio analysis on `1.mp3`, `2.mp3`, and `3 Mario coin.mp3` using python `soundfile` and `numpy.fft` in scratch space.
- Identified Gemini Live WebSocket protocol violations in `GominLiveManager.kt`.
- Deleted old `.ogg` notification sounds (`gomin_notif_1.ogg` and `gomin_notif_2.ogg`) inside `TMessagesProj/src/main/res/raw/` to prevent duplicate resource conflicts.
- Renamed and placed new notification sounds: `3 Mario coin.mp3` -> `gomin_notif_1.mp3`, `2.mp3` -> `gomin_notif_2.mp3`.
- Updated notification sound labels in `CGPreferencesEntry.java` (Alara -> Маріо, Пилюка -> Сурма, Сіріус -> Дзвіночок).
- Resolved bottom bar outline alignment bug by changing `MAIN_TABS_MARGIN` to raw integer `6` in `DialogsActivity.java` and wrapping it in `dp()` explicitly inside `MainTabsActivity.java` canvas drawing and search button border inset setter.
- Resolved invisible white-on-white send button icon inside share panel for Monet themes in `ChatActivityEnterView.java` by using `Theme.key_chat_messagePanelSendIcon` color when `shouldDrawBackground()` is true.
- Verified compilation and layout constants validity via GominTabsMarginTest unit test execution (successful compilation).

# OPEN PROBLEMS
- Live transcription and voice calls are failing due to JSON casing (snake_case instead of camelCase) and deprecated schema elements in `GominLiveManager.kt` (code modifications on hold per user freeze).

# MODIFIED FILES
- `TMessagesProj/src/main/java/uz/unnarsx/cherrygram/preferences/CGPreferencesEntry.java` -> Updated notification sound labels in settings.
- `TMessagesProj/src/main/res/raw/gomin_notif_1.ogg` (DELETED)
- `TMessagesProj/src/main/res/raw/gomin_notif_2.ogg` (DELETED)
- `TMessagesProj/src/main/res/raw/gomin_notif_1.mp3` (NEW)
- `TMessagesProj/src/main/res/raw/gomin_notif_2.mp3` (NEW)
- `TMessagesProj/src/main/java/org/telegram/ui/DialogsActivity.java` -> Modified MAIN_TABS_MARGIN to raw value 6.
- `TMessagesProj/src/main/java/org/telegram/ui/MainTabsActivity.java` -> Wrapped MAIN_TABS_MARGIN in dp() in drawing functions.
- `TMessagesProj/src/main/java/org/telegram/ui/Components/ChatActivityEnterView.java` -> Fixed send button icon coloring to use key_chat_messagePanelSendIcon when background is drawn.
