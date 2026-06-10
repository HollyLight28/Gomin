# План виправлення Speed Engine Card та іконок

## Знайдені проблеми

### 1. 🐛 CRITICAL: Switch має 0×0 розмір
`org.telegram.ui.Components.Switch` extends `View` і **НЕ має `onMeasure()`**. 
Коли додається з `WRAP_CONTENT, WRAP_CONTENT` — міряється як **0×0 пікселів**.
Тому перемикачі (upload, slow network) — невидимі.

**Файл:** `CGPreferencesEntry.java`
**Рядки:** 358 (uploadSwitch) та 402 (slowSwitch)
**Фікс:** `LayoutHelper.createLinear(WRAP_CONTENT, WRAP_CONTENT)` → `LayoutHelper.createLinear(39, 40)`
(39×40 dp — стандартний розмір Switch у Telegram, як в TextCheckCell, TextCell)

### 2. 🐛 HIGH: Іконка камери — вишенька
`R.drawable.camera_icon_cherrygram` — це не камера, а вишня (Cherrygram брендинг).

**Файл:** `CGPreferencesEntry.java`, рядок 193
**Фікс:** `R.drawable.camera_icon_cherrygram` → `R.drawable.camera_icon_telegram`
(знайдено в `res-cherrygram/drawable/camera_icon_telegram.xml` — нормальна іконка камери)

### 3. 🐛 HIGH: Іконка "Theme/Інше" — сонце замість шестерні
`R.drawable.msg_settings_solar` — сонце/шестерня в solar-стилі.

**Файл:** `CGPreferencesEntry.java`, рядок 226
**Фікс:** `R.drawable.msg_settings_solar` → `R.drawable.msg_settings`
(знайдено в `SolarIconReplace.kt` — стандартна шестерня налаштувань)

## Зміни
- 1 файл: `CGPreferencesEntry.java`, 3 зміни
- Жодних змін в ресурсах чи інших файлах
