# CURRENT MISSION
1. Fix theme resetting back to light theme under Android system night mode (Theme.java SharedPreferences persistence bug). [COMPLETED]
2. Merge Telegram Premium and Telegram FAQ cells in settings into a single visual card, and remove the "Help" section header entirely. [COMPLETED]

# COMPLETED ATOMIC STEPS
- Diagnosed theme persistence bug: SharedPreferences theme records under `MessagesController.getGlobalMainSettings()` stored dark themes exclusively in the `"theme"` (daytime) key when `nightTheme` was false, and never updated `currentNightTheme` static instance in memory. Upon app restarts/background shifts under system dark mode, Android triggered `needSwitchToTheme() == 2` (night mode) which forcefully rolled back the theme to the obsolete or missing `"nighttheme"` setting.
- Re-engineered `applyTheme` and `applyThemeInBackground` in `Theme.java`: implemented dynamic checks using `themeInfo.isDark()` to ensure all dark themes automatically populate the `currentNightTheme` instance and get stored under the `"nighttheme"` SharedPreferences key. Conversely, light themes update `currentDayTheme` and get stored under the `"theme"` key.
- Unified Telegram Premium and Telegram FAQ settings: removed `items.add(UItem.asHeader(getString(R.string.SettingsHelp)))` header and the trailing `UItem.asShadow(null)` separator inside `SettingsActivity.java`, enabling `UniversalAdapter` to merge both rows into a single premium card.
- Staged all changes and prepared commit.

# OPEN PROBLEMS
None.

# MODIFIED FILES
- `TMessagesProj/src/main/java/org/telegram/ui/ActionBar/Theme.java` -> Implemented dynamic dark theme detection using `themeInfo.isDark()` to split preferences recording between `"nighttheme"` and `"theme"` keys.
- `TMessagesProj/src/main/java/org/telegram/ui/SettingsActivity.java` -> Removed "Help" section header and dividing shadow separators.
