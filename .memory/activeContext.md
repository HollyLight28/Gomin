# CURRENT MISSION
1. Implement a high-end, 100% native Gomin AI Chat Assistant and 🛡️ Shield (Manipulation & Gaslighting Analyzer) inside the Gomin Android app (Telegram fork). [COMPLETED]
2. Deeply study the integrated Air Raid Alert feature (Повітряна тривога) in Gomin and write complete documentation on it. [COMPLETED]
3. Relocate Gomin Shield option from ActionBar to the three-dots (overflow) menu in private chats and localized it as "Ментальний щит". [COMPLETED]
4. Fully restore the complete 200-line clinical Mental Shield prompt and implement a premium, gender-neutral default chat prompt with guardrails, while purging legacy 1.5/2.0 models from status maps. [COMPLETED]

# COMPLETED ATOMIC STEPS
- Defined `const val GOMIN_AI_DIALOG_ID = 99999999L` inside `Constants.kt` as a unique virtual user ID for Gomin AI.
- Patched `MessagesController.java` to dynamically generate a mock `TLRPC.User` for `99999999L` (named "Gomin AI", bot, username `gomin_ai_bot`) which enables the app to launch a native private chat activity seamlessly.
- Created `GominAiHistoryManager.kt` to act as a virtual local storage layer (loading/saving native `MessageObject` entities to a custom `gomin_ai_history.json` file, with a 1000-message cap).
- Patched `MessagesController.loadMessagesInternal` to intercept messages requested for dialog ID `99999999L`, loading them from `GominAiHistoryManager` instead of hitting the remote database/server, posting `NotificationCenter.messagesDidLoad` to update the native UI.
- Patched `SendMessagesHelper.java` to intercept message sending in the Gomin AI dialog, saving the user's message locally, refreshing the UI instantly, and delegating the query to the Gemini SDK.
- Created `GominAiChatHelper.kt` to coordinate the multi-turn session with the Gemini API (supporting Flash and Pro models via a customizable model setting). Displays native "typing..." states in the ActionBar subtitle and manages context pre-seeding.
- Added a Lucide-style outline Shield (🛡️) icon (`outline_shield_plain_24` / `shield_solar`) in `ChatActivity.java` ActionBar for private chats. [RELOCATED TO OVERFLOW]
- Created `GominShieldBottomSheet.java` as a premium native bottom sheet that extracts the last 1000 messages from the active chat history, sends them to Gemini for manipulation/gaslighting analysis, renders the analysis using beautiful markdown with custom emoticons, and provides a direct "💬 Почати чат про це" button that seamlessly starts a Gomin AI chat with the pre-seeded context.
- Added Gomin AI specific menu subitems in `ChatActivity.java` (Model Selector and Clear History) and handled their actions cleanly.
- Designed a custom Vector Drawable `lucide_sparkles.xml` — a premium, thin Lucide-style Sparkles/stars icon.
- Added a dedicated, beautiful floating action button (FAB) in `DialogsActivity.java` (using the new `lucide_sparkles` icon) that floats right above the stories button (at 104dp translation offset) to open the Gomin AI chat directly.
- Studied and analyzed the Gomin Air Alert (`AirAlertController.kt`) subsystem, which connects directly to `api.ukrainealarm.com` (Ajax systems) to fetch real-time air raid threat statuses for selected regions, trigger loop alarms, and notify UI elements.
- Added localized string `CG_GominShield` in Ukrainian (`🛡️ Ментальний щит`) and English (`🛡️ Mental Shield`) resource files inside `res-cherrygram`.
- Relocated Gomin Shield option from ActionBar to the three-dots (overflow) menu inside `ChatActivity.java` (using `headerItem.lazilyAddSubItem`), preserving peer-type conditions and seamless action routing via `gomin_shield_menu_item`.
- Senior Refinement: Optimized `GominShieldBottomSheet.show` to reconstruct chronological message logs using an $O(N)$ ArrayList builder instead of the slow $O(N^2)$ StringBuilder insert mechanism, and injected absolute `[yyyy-MM-dd HH:mm]` timestamps for high-fidelity behavior/gaslighting profiling by Gemini.
- Removed useless Cherrygram diagnostic debug menu option ("Profile" dialog printing `isCherryPremium`, `isDonated` etc.) from `ProfileActivityHelper.java` so it never pollutes the profile screen's 3-dots menu.
- Fully localized all title and alert strings inside `GominShieldBottomSheet.kt` using `LocaleController.getString(R.string.CG_GominShield)` to dynamically match "Ментальний щит" / "Mental Shield" across languages.
- Dynamic Model Selector: Replaced the static hardcoded array of models in `GominAiChatHelper.showModelSelector` with a fully dynamic network fetch using the API key, letting the UI adapt automatically as Google updates its model catalog.
- API Model Filtering: Injected a case-insensitive check in `ApiClient.java` to only return models containing "gemini" or "gemma", filtering out noisy embedding and translation engines.
- Fully restored the clinical Mental Shield system prompt (`shieldSystemPrompt`) in its unabridged 200-line version in `GominAiChatHelper.kt` to allow maximum analytical fidelity.
- Created `defaultSystemPrompt` for Gomin AI chat — a highly detailed, professional, empathetic, and gender-neutral assistant prompt with solid guardrails against instructions leak.
- Updated `setTypingStatus` in `GominAiChatHelper.kt` to purge legacy 1.5/2.0 entries and seamlessly map modern Gemini 3.5, 3.1, and 3.0 models.
- Resolved compile errors in `ChatsHelper2.kt` by fixing an accidentally deleted closing brace in `ProfileActivityHelper.java` and adding missing imports in `GominAiChatHelper.kt`.

# OPEN PROBLEMS
None. All components build, run, and interact beautifully.

# MODIFIED FILES
- `uz.unnarsx.cherrygram.misc.Constants.kt` -> Added `GOMIN_AI_DIALOG_ID`.
- `org.telegram.messenger.MessagesController.java` -> Injected mock user lookup and local message loading interception.
- `org.telegram.messenger.SendMessagesHelper.java` -> Intercepted sending messages to local virtual dialog.
- `org.telegram.ui.ChatActivity.java` -> Removed outline Shield ActionBar icon from the main Action Bar menu and added it to the three-dots overflow menu (`headerItem`), and kept dropdown click routing for `gomin_shield_menu_item`.
- `org.telegram.ui.DialogsActivity.java` -> Injected float button `floatingButtonAi` above the stories/pencil button with the premium `lucide_sparkles` icon and updated offsets/colors.
- `TMessagesProj/src/main/res-solar/drawable/lucide_sparkles.xml` [NEW] -> Premium outline Lucide sparkles vector drawable.
- `uz.unnarsx.cherrygram.chats.gemini.GominAiHistoryManager.kt` [NEW] -> Virtual JSON history serializer.
- `uz.unnarsx.cherrygram.chats.gemini.GominAiChatHelper.kt` [NEW/OPTIMIZED] -> Restored full Mental Shield prompt, implemented detailed gender-neutral chat prompt with guardrails, and cleaned typing status model representations.
- `uz.unnarsx.cherrygram.chats.gemini.GominShieldBottomSheet.kt` [NEW/OPTIMIZED] -> Manipulation/gaslighting proﬁler bottom sheet, optimized history collection to $O(N)$, injected dates/timestamps, and fully localized titles/dialogs dynamically to match `R.string.CG_GominShield`.
- `README.md` -> Documented the whole Gomin AI ecosystem, Gomin Air Alert, and added a testing manual.
- `README.uk.md` -> Added deep Ukrainian details about Gomin AI, Gomin Shield, Gomin Air Alert, and a testing manual.
- `TMessagesProj/src/main/res-cherrygram/values-uk/cg_strings.xml` -> Added `CG_GominShield` translation ("🛡️ Ментальний щит").
- `TMessagesProj/src/main/res-cherrygram/values/cg_strings.xml` -> Added `CG_GominShield` translation ("🛡️ Mental Shield").
- `uz.unnarsx.cherrygram.helpers.ProfileActivityHelper.java` -> Disabled the debug options menu info item `injectCherryInfo` in profile activity options.
- `uz.unnarsx.cherrygram.chats.gemini.network.ApiClient.java` -> Implemented case-insensitive model filtering by checking if names contain `gemini` or `gemma`.

