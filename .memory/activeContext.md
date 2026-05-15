# Gomin Project State - 2026-05-14

## [CURRENT MISSION]: Resolve startup crashes, fix branding (icons/fonts), and build APK for Pixel 7a (arm64-v8a).

## [COMPLETED ATOMIC STEPS]
- Deployed adaptive icons from `icon_3` to `res-cherrygram` across all densities (mdpi to xxxhdpi).
- Updated `LauncherIconController.java` to use dynamic package name `ctx.getPackageName()` and renamed default icon to `Gomin_Icon_Main`.
- Fixed `FontHelper.java` to correctly map Playfair Display (headers) and Geist (messages).
- Added null check for `ApplicationLoader.applicationContext` in `FontHelper` to prevent startup crashes during early initialization.
- Set default font size to 18 in `SharedConfig.java` for better readability.
- Updated `icon_background_default.xml` to black (#000000) to align with Gomin brand.
- Performed global replacement of old package `uz.unnarsx.cherrygram` in `AndroidManifest.xml` for activity-aliases.
- Initiated clean build (`assembleAfatDebug`).

## [MODIFIED FILES]
- TMessagesProj/src/main/java/org/telegram/ui/LauncherIconController.java -> Dynamic package and branding update.
- TMessagesProj/src/main/java/uz/unnarsx/cherrygram/helpers/ui/FontHelper.java -> Font mapping and crash fix.
- TMessagesProj/src/main/java/org/telegram/messenger/SharedConfig.java -> Font size adjustment.
- TMessagesProj/src/main/res-cherrygram/drawable/icon_background_default.xml -> Background color fix.
- TMessagesProj/src/main/AndroidManifest.xml -> Package alignment for aliases.
- `Constants.kt` -> `CG_GITHUB_URL` -> Architectural consistency with new repo.
- `AboutPreferencesEntry.java` -> Removed Crowdin -> UI Cleanup.
- `MainTabsPreviewCell.java` -> UI Restoration -> Fixing "missing" previews.

## [OPEN PROBLEMS]
- Need to verify if `playfair.ttf` isn't too large for some small title labels.
- Monitor `MainTabsPreviewCell` for any layout overflows with 5+ tabs.
