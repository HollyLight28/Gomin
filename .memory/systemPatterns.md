# System Patterns - Gomin (Cherrygram Edition)

## API Configuration Pattern
In Telegram/Cherrygram, API credentials are typically defined in `BuildVars.java`.

## Resource Management
- Icons: `TMessagesProj/src/main/res/mipmap-*`
- Layouts: `TMessagesProj/src/main/res/layout`
- Fonts: Typically handled in `Typeface` logic or `assets/fonts`.

## Build Lock Recovery Pattern (Windows)
When Gradle fails with `IllegalStateException: Could not find EOCD`, it indicates a corrupted APK or file lock by Gradle Daemon.
Recovery steps:
1. Kill all Java processes: `taskkill /F /IM java.exe`
2. Manually delete the build directory: `rm -Recurse -Force <module>/build`
3. Perform a clean build: `./gradlew clean` (if possible) followed by `assemble`

## Font Management Pattern
Custom fonts (Geist, Playfair Display) are injected by replacing standard Roboto constants in `AndroidUtilities.java`:
- `TYPEFACE_ROBOTO_REGULAR` -> `fonts/geist.ttf`
- `TYPEFACE_ROBOTO_MEDIUM` -> `fonts/playfair.ttf`

## Package Rebranding Pattern
All package-level constants and configurations should point to `ua.gomin.messenger`.
