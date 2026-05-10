# Active Context - Gomin (Cherrygram Edition)

## [CURRENT MISSION]
- [CURRENT MISSION]: Виправити стабілізацію збірки Gomin в GitHub Actions та локально.

## [COMPLETED ATOMIC STEPS]
- [COMPLETED ATOMIC STEPS]:
    - Проаналізовано JNI код (TgNetWrapper.cpp) та знайдено заглушки для security.
    - Перевірено локальну збірку (працює успішно, C++ компілюється).
    - Виявлено помилку в build.gradle: `desktopPath` дорівнює `null` на Linux, що валить конфігурацію Gradle в CI.

## [OPEN PROBLEMS]
- [OPEN PROBLEMS]:
    - GitHub Actions не має встановленого NDK r21.
    - GitHub Actions падає через `into null` в тасці `copyFiles`.
- Need to locate exact paths for `BuildVars.java`.
- Need to locate font assets.
- Need to verify build environment (JDK, SDK, NDK).

## [MODIFIED FILES]
- None yet.
