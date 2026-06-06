# CURRENT MISSION
1. Fix bottom tabs misalignment and layout wrapping when search button is enabled in main tabs. [IN PROGRESS]
2. Fix glass border (white outline) drawing on dark theme (resolving double dp() scaling which caused outline to draw 5mm outside the background capsule). [IN PROGRESS]
3. Compile and verify the local build. [PENDING]

# COMPLETED ATOMIC STEPS
- Analyzed git history and identified that the recent commit 77944c08c reverted MAIN_TABS_MARGIN to dp(6) which introduced double-scaling dp(dp(6)) bugs.
- Found that tabsContainer width MATCH_PARENT with weight 1.0f on tabsView caused tabsView to expand but tabs inside stayed left-aligned.
- Formulated layout fix using wrap_content for tabsContainer and fixed dimensions for tabsView within it.

# OPEN PROBLEMS
- Bottom tabs bar and search button overlapping and misaligned (currently left-aligned and distorted).
- Dark theme glass border drawing outside the tab capsule due to MAIN_TABS_MARGIN being set to dp(6) static variable causing double dp() scale calculations.

# MODIFIED FILES
- `TMessagesProj/src/main/java/org/telegram/ui/DialogsActivity.java` -> Modified MAIN_TABS_MARGIN to raw value 6 to prevent double dp() calculations.
- `TMessagesProj/src/main/java/org/telegram/ui/MainTabsActivity.java` -> Wrapped MAIN_TABS_MARGIN in dp() in drawing functions, added maxWidth constraint in anonymous LinearLayout tabsContainer, and updated layouts to WRAP_CONTENT and center gravity.

