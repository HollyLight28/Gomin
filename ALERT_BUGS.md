# Air Alert System — Bug Report & Refactor Plan

## Контекст

Система тривоги в Gomin UA Telegram отримує пуши від Firebase (FCM) від бекенда (`alert_backend/main.py`), який використовує `api.alerts.in.ua`.

Коли приходить тривога:
1. FCM push → `GcmPushListenerService`
2. Показується системна нотифікація з `gomin_siren.mp3`
3. Через 15с звук нотифікації закінчується
4. В шторці висить сповіщення "🚨 ПОВІТРЯНА ТРИВОГА"
5. В додатку заголовок стає **червоним**

Коли відбій:
1. FCM push → нотифікація оновлюється на "✅ ВІДБІЙ ТРИВОГИ" з `gomin_cancel.mp3`
2. Червоний заголовок зникає

---

## Головна проблема: MediaPlayer дублює системну нотифікацію

**Файл:** `TMessagesProj/src/main/java/uz/unnarsx/cherrygram/alerts/AirAlertController.kt`

Зараз при тривозі звук сирени грає одночасно з **двох джерел**:
1. **Системна нотифікація** (NotificationChannel) — грає `gomin_siren.mp3`
2. **MediaPlayer** в коді — грає той самий `gomin_siren.mp3`

MediaPlayer — це **непотрібний дубль**, який створює всі критичні баги.

### Що видалити з `AirAlertController.kt`:

| Що | Змінна/метод | Рядки |
|----|--------------|-------|
| Програвач | `mediaPlayer` | 20 |
| Флаг "сирена вже грала" | `sirenPlayedForCurrentAlert` | 18, 34, 111, 131 |
| Runnable автостопа | `sirenStopRunnable` | 22, 164-171, 211-212 |
| Runnable безпеки (12г) | `safetyStopRunnable` | 23, 124-129, 132-133, 229, 258-268 |
| Флаг тесту | `isTesting` | 25, 100-103, 213-215, 221-271 |
| Збережений стан тесту | `savedAlertState` | 26 |
| Відкладений статус | `pendingAlertStatus` | 28, 101, 249-252, 290 |
| Весь метод | `playSound()` | 150-195 |
| Весь метод | `stopSiren()` | 209-219 |
| Весь метод | `stopSirenOnly()` | 197-207 |
| Весь метод | `testAlert()` | 221-240 |
| Весь метод | `stopTest()` | 242-271 |
| Всі виклики `playSound()` | в `setAlertStatus()` | 112, 134 |
| Всі виклики `NotificationCenter` | тестові | 233, 256, 265 |

### Що залишити в `AirAlertController.kt`:

| Що | Для чого |
|----|----------|
| `isAlertActive` | Стан тривоги (true/false) |
| `handlePushStatus()` | Прийом FCM пушів |
| `checkAlertStatus()` | Polling як fallback (кожні 60с) |
| `setAlertStatus()` | Логіка зміни стану (без звуку) |
| `postNotificationName(cgAirAlertStatusChanged)` | Оновлення червоного заголовку |
| `startMonitoring()` / `stopMonitoring()` | Запуск/зупинка polling |
| `init()` | Ініціалізація при старті |
| `fetchRegions()` | Список областей для налаштувань |

### Як зміниться `setAlertStatus()`:

**До:**
```
при зміні стану:
  якщо тривога:
    playSound(true)              ← MediaPlayer + нотифікація
    showStartNotification()      ← нотифікація
  якщо відбій:
    playSound(false)             ← MediaPlayer
    showEndNotification()        ← нотифікація
```

**Після:**
```
при зміні стану:
  якщо тривога:
    showStartNotification()      ← тільки нотифікація (вона сама грає звук)
  якщо відбій:
    showEndNotification()         ← тільки нотифікація (вона сама грає звук)
```

---

## Похідні баги, які зникнуть після видалення MediaPlayer

| Баг | Файл | Чому зникне |
|-----|------|-------------|
| Подвійна сирена | `AirAlertController.kt` | Нотифікація грає один раз |
| Бокова кнопка не вимикає звук | `ScreenReceiver.java` | Нема MediaPlayer, нотифікація грає ~15с і стихає |
| Сирена глохне при вимкненому екрані | — | Нотифікація системна, не залежить від сну CPU |
| Потрібен WakeLock | — | Не потрібен для системної нотифікації |
| Потрібен AudioFocus | — | Не потрібен для системної нотифікації |
| Сирена 15с і все | — | Нотифікація грає один раз ~15с, це нормально |
| Перезапуск додатку = сирена не грає | `init()` | Не грає — і правильно, нотифікація висить в шторці |

---

## Side button behavior (за бажанням)

**Поточний код:** `ScreenReceiver.java:27` викликає `stopSiren()` при `ACTION_SCREEN_OFF`. Після видалення MediaPlayer цей рядок не має сенсу. Можна:
1. **Просто прибрати** — нотифікація грає ~15с сама і стихає
2. **Додати логіку** — при `ACTION_SCREEN_OFF` скасовувати нотифікацію тривоги і показувати її ж, але без звуку:

```java
// ScreenReceiver.java — опціонально
if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
    // Переностимо нотифікацію без звуку, щоб не глушити
    AirAlertController.INSTANCE.silenceNotificationOnScreenOff();
}
```

Рішення за тобою. Найпростіше — просто прибрати виклик `stopSiren()` з `ScreenReceiver`.

---

## Другорядні баги та покращення

### 1. `FLAG_INSISTENT` — прибрати

**Файл:** `AirAlertNotificationHelper.kt:86`

```kotlin
notification.flags = notification.flags or Notification.FLAG_INSISTENT
```

Deprecated з Android 8 (API 26). Не впливає на поведінку на сучасних версіях. Просто видалити.

### 2. HTTP → HTTPS в polling URL

**Файл:** `AirAlertController.kt:74`

```kotlin
val url = URL("http://204.168.201.148:5000/status?region_id=$regionId")
```

Потрібен HTTPS на сервері, потім змінити в коді додатка.

### 3. `airAlertApiKey` — прибрати з налаштувань

**Файли:** `CherrygramCoreConfig.kt:98`, `CGPreferencesEntry.java`

Поле `airAlertApiKey` зберігається але НІДЕ не використовується. Додаток отримує тривоги через FCM push, який приходить від сервера — сервер має свій токен `alerts.in.ua`. Користувачу не потрібен ніякий API ключ.

**Зробити:**
- Видалити `airAlertApiKey` з `CherrygramCoreConfig.kt`
- Прибрати поле вводу API ключа з `CGPreferencesEntry.java`

### 4. Notification channel creation — перенести раніше

**Файл:** `LaunchActivity.java` → перенести в `ApplicationLoader.java`

`AirAlertNotificationHelper.createNotificationChannels()` викликається в `LaunchActivity.onCreate()`. Якщо додаток запущений FCM push-ом (а не через іконку), канали можуть не існувати.

**Зробити:** викликати `createNotificationChannels()` в `ApplicationLoader.onCreate()` або в `CherrygramCoreConfig.init()`.

### 5. `alert_server_main_*.py` — видалити

**Файли:** `alert_server_main_utf8.py`, `alert_server_main_fixed.py` в корені проекту

Це старі копії серверного парсера Telegram каналу. Не використовуються (сервер працює через `alerts.in.ua` API). Видалити.

---

## Що НЕ треба чіпати

### Huawei Push Listener
HuaweiListener не підтримує тривоги — **не виправляти**, нашо ціль тільки Google Play / GCM.

### ActionBar / червоний заголовок
Робить те що треба: 
- Стежить за `isAlertActive`
- Оновлюється через `NotificationCenter.cgAirAlertStatusChanged`
- Синхронізується при `onAttachedToWindow`
- Багів не знайдено

### Polling (60с)
Працює як fallback. Можна залишити.

---

## Фічі на майбутнє (НЕ в цьому рефакторингу)

### Карта/список тривог в меню (три крапки)
На головному екрані, в меню з трьома крапками — список всіх областей з кольоровою індикацією:
- 🟢 спокійно
- 🔴 тривога
- Список з підписами областей

### Зміна звуку сирени
Додати в налаштування AI Alert вибір звуку. Але це дрочня — `gomin_siren.mp3` норм.

---

## Порядок дій (що робити)

```
1. AirAlertController.kt — видалити весь MediaPlayer код
2. AirAlertNotificationHelper.kt — прибрати FLAG_INSISTENT
3. ScreenReceiver.java — прибрати виклик stopSiren() 
4. CherrygramCoreConfig.kt — прибрати airAlertApiKey
5. CGPreferencesEntry.java — прибрати поле API ключа
6. ApplicationLoader.java — перенести createNotificationChannels()
7. AlertController.kt — змінити http на https (коли буде HTTPS на сервері)
8. Видалити alert_server_main_*.py з кореня
```

Після цих змін тривога грає тільки через системну нотифікацію. Жодних багів з подвійним звуком, боковою кнопкою, сном і т.д.
