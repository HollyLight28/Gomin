# Gomin Project State - 2026-05-14

## [CURRENT MISSION]
Rebrand Cherrygram to Gomin, fix UI previews, and implement premium typography.

## [COMPLETED ATOMIC STEPS]
1. Changed `CG_GITHUB_URL` in `Constants.kt` to `https://github.com/HollyLight28/Gomin`.
2. Removed `CGP_Crowdin` (Help with translation) from `AboutPreferencesEntry.java`.
3. Implemented font forcing in `FontHelper.java`:
   - `geist.ttf` as regular (messages).
   - `playfair.ttf` as medium/bold (titles).
4. Restored `MainTabsPreviewCell.java` logic to show bottom tabs organizer preview.

## [MODIFIED FILES]
- `Constants.kt` -> `CG_GITHUB_URL` -> Architectural consistency with new repo.
- `AboutPreferencesEntry.java` -> Removed Crowdin -> UI Cleanup.
- `FontHelper.java` -> Global Font Swap -> Premium Branding.
- `MainTabsPreviewCell.java` -> UI Restoration -> Fixing "missing" previews.

## [OPEN PROBLEMS]
- Need to verify if `playfair.ttf` isn't too large for some small title labels.
- Monitor `MainTabsPreviewCell` for any layout overflows with 5+ tabs.
