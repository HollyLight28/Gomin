# CURRENT MISSION
1. Diagnose and fix the build failures occurring both locally and in Github Actions. [COMPLETED]
2. Fix anti-delete message logic in MessagesStorage to keep all dialogue context intact. [COMPLETED]
3. Optimize Gomin Shield message loading limit to 1500. [COMPLETED]
4. Fix the critical air alert server parser bug where region alert state resets to False every 10 seconds. [COMPLETED]
5. Verify and fix the 3 critical bugs in GominLiveManager.kt (Live API). [COMPLETED]
6. Rebrand "Black Edition" to a premium, stylish Ukrainian name (e.g., "Гомін: Пітьма") and rewrite its manifesto. [COMPLETED]
7. Design and implement a premium "Про Гомін" (About Gomin) custom settings screen containing the emotional story and philosophy of the solo developer. [PENDING STORY]
8. Plan the premium bidirectional Jarvis features (Google Search web retrieval and Reading target Telegram channels/chats to summarize updates). [PLANNED]
9. Fix bubble design contrast, decline call button hardcoded color, and player null safety. [COMPLETED]
10. Restore correct Gomin notification sound resource and resolve siren playing on messages. [COMPLETED]
11. Implement global Gomin notification sound selection (Alara, Pylyuka, Sirius) in Gomin Settings (CGPreferencesEntry.java) and map them to Android system notifications while playing gomin_in_chat.mp3 inside open chats. [COMPLETED]

# COMPLETED ATOMIC STEPS
- Sanitized the Cyrillic/space sound file name `В ЧАТІ ЗВУК.mp3` to `gomin_in_chat.mp3` and verified its correct location in `raw` resources.
- Renamed and preserved the 3 chosen sounds in `TMessagesProj/src/main/res/raw/`: `gomin_notif_1.ogg` (Alara), `gomin_notif_2.ogg` (Pylyuka), `gomin_notif_3.ogg` (Sirius).
- Deleted all other 29 temporary files (`gomin_opt_*.ogg`) to avoid APK bloating.
- Updated `CherrygramChatsConfig.kt` to define notification sound constants (`NOTIF_SOUND_GOMIN_1`, `_2`, `_3`) and removed `NOTIF_SOUND_IOS`.
- Restored `GominBlackEditionActivity.kt` to its original clean state containing only the theme switch and the manifesto.
- Completely removed the obsolete notification sound selector, onClick listener, and helper functions from `ChatsPreferencesEntry.java`.
- Added the "System Notification Sound" settings header and button selector to `CGPreferencesEntry.java`.
- Implemented safe `MediaPlayer` audio preview playback with proper context validation and exception boundaries in `CGPreferencesEntry.java`.
- Triggered `resetInChatSound()` and `deleteAllNotificationChannels()` upon sound configuration updates in `CGPreferencesEntry.java` to enforce instant channel recreation on Android 8.0+.
- Hardcoded `R.raw.gomin_in_chat` as the static in-chat notification sound inside `NotificationsController.java` to prevent resource lookup issues.
- Added `resetInChatSound()` helper method to `NotificationsController.java` to safely unload and reset local SoundPool references.
- Mapped system notification sound setup in `NotificationsController.java` to dynamically parse and apply custom Gomin resource URIs (`gomin_notif_1`, `_2`, `_3`) for default push notifications.

# OPEN PROBLEMS
None.

# MODIFIED FILES
- `TMessagesProj/src/main/res/raw/gomin_in_chat.mp3` -> Sanitized custom in-chat notification sound.
- `TMessagesProj/src/main/res/raw/gomin_notif_1.ogg`, `_2.ogg`, `_3.ogg` -> Retained custom notification alert options (Alara, Pylyuka, Sirius).
- `TMessagesProj/src/main/java/uz/unnarsx/cherrygram/core/configs/CherrygramChatsConfig.kt` -> Defined constants for the three sounds, removed iOS sound constant.
- `TMessagesProj/src/main/java/uz/unnarsx/cherrygram/preferences/GominBlackEditionActivity.kt` -> Removed all sound-related code, returning to clean manifesto screen.
- `TMessagesProj/src/main/java/uz/unnarsx/cherrygram/preferences/ChatsPreferencesEntry.java` -> Removed deprecated sound preferences row, onClick handling, and popup helpers.
- `TMessagesProj/src/main/java/uz/unnarsx/cherrygram/preferences/CGPreferencesEntry.java` -> Added "System Notification Sound" row with selector dialogue, safety previews, and channel reset.
- `TMessagesProj/src/main/java/org/telegram/messenger/NotificationsController.java` -> Statically mapped playInChatSound to `R.raw.gomin_in_chat`, added `resetInChatSound()`, and set custom URI mapping for system push alerts.
