# Gomin ROADMAP

*Останнє оновлення: 3 червня 2026*

---

## Коротко про проект

**Gomin** — український Telegram-клієнт на базі Cherrygram. Стартував 10 травня 2026 як ребрендинг, за 20 днів перетворився на повноцінний продукт із власним AI-двигуном, системою приватності, повітряною тривогою та Live Voice API.

**Філософія:** 100% нативність через інтеграцію в існуючі компоненти Telegram, нуль костильності, premium feel.

---

## Phase 0: Cherrygram Base (до 10 травня 2026)

Проект був звичайним форком Cherrygram (fork Telegram для Android). Комітилися стандартні оновлення:
- Telegram v11.9.1 → v12.5.1
- Cherrygram ver. 9.2.0 → 10.5.0
- Ніяких Gomin-фіч, просто "ще один мод"

**Точка входу:** `3d5766a6d` — перший коміт з rebranding.

---

## Phase 1: Rebranding (10–17 травня)

**Мета:** Перетворити Cherrygram на Gomin. Заміна назв, іконок, CI/CD.

### Що зроблено:
- Ребрендинг `ua.gomin.messenger` замість Cherrygram
- CI/CD через GitHub Actions (build + release APK)
- Іконки: кастомний векторний drawable пташки (notification + launcher)
- Сплеш-скрін з пташкою
- **Talsec Security** — встановлено, потім вимкнено (викликало краші)
- Keystore для CI збірок
- Перші build-фікси (AAPT, JNI, NDK r21)
- **Типографіка:** Nunito (спочатку), потім Geist + Playfair

### Ключові файли (створені в цю фазу):
- `.github/workflows/build.yml` — CI/CD пайплайн
- `TMessagesProj/src/main/res-cherrygram/drawable/icon_foreground_gomin*.xml` — брендовані іконки

### Проблеми:
- CI постійно падав через JNI/NDK (colorado.h, skCrypter.hpp)
- Talsec Security викликав startup crash
- Помилки AAPT при ребрендингу маніфесту

---

## Phase 2: Core Features (17–24 травня)

**Мета:** Додати ключові фічі, які виділяють Gomin з-поміж інших модів.

### 2.1 — Black Edition & Monet
- Жорстка OLED-тема (deep black, білий текст)
- Monет (Material You) адаптація
- Кастомні кольори акцентів (8 варіантів)
- Українська тема `icon_background_ukraine.xml`
- **Проблема:** Theme toggle loop, контраст, невидимі іконки в Monет

### 2.2 — Gomin Speed Engine
- Паралельний даунлоад: 12 TCP потоків по 1MB (замість 4×128KB)
- Extreme Upload: буфер 512KB
- **Shelter Mode (Укриття):** один стабільний потік 32KB для метро/укриттів

### 2.3 — Ghost Mode (Приватність)
- Unread Guard (читання без позначки "прочитано")
- Typing concealment (не показує "друкує...")
- Anonymous stories viewing
- Hidden online status
- **Anti-Delete:** збереження видалених повідомлень (out != 0, fromServer=true)
  - Візуальний маркер 🗑️
  - Логіка в `MessagesStorage.java`

### 2.4 — Кастомна типографіка
- Geist (основний) + Playfair (заголовки)
- Nunito — пробували, відмовились через неповноту
- **Проблема:** fake bold для Geist, early init protection

### 2.5 — Налаштування
- Радикальний мінімалізм у налаштуваннях (прибрали `foldersAtBottom`, рекламу, спонсорські канали)
- Злиті premium cards
- Сховані непотрібні перемикачі

### Рефакторинг тем (важливе архітектурне рішення):
**Проблема:** Тема скидалася при зміні system dark mode.
**Рішення:** Розділити day/night theme в SharedPreferences. Темні теми → `currentNightTheme`/`"nighttheme"`, світлі → `currentDayTheme`/`"theme"`.

---

## Phase 3: AI Integration (24–31 травня)

**Мета:** Вбудувати Google Gemini в Telegram-клієнт.

### 3.1 — Gomin AI Chat
**Архітектура:** Virtual Local AI Dialogue Pattern
- `dialog_id = 99999999L` — віртуальний ID
- `MessagesController.getUser()` перехоплюється, повертає mock-бота
- `SendMessagesHelper.sendMessage()` перехоплюється, редіректить на Gemini API
- Історія зберігається локально в JSON (`gomin_ai_history.json` → `GominAiHistoryManager.kt`)
- Стандартний `ChatActivity` Telegram як UI — 0% кастомного дизайну

**Перехоплення в:**
- `MessagesController.java:6608` — `user.first_name = "Gomin AI"`
- `SendMessagesHelper.java:3885` — AI history + query
- `ChatActivityEnterView.java` — Gemini кнопки

### 3.2 — Gomin Shield (Ментальний щит)
**Архітектура:** Pre-Seeded Context AI Redirection
1. Кнопка "Щит" в `ChatActivity` → `GominShieldBottomSheet.kt`
2. Викачування 1500 повідомлень (оптимізовано з 3000)
3. Gemini аналіз токсичності/газлайтингу
4. Результат в BottomSheet
5. Кнопка "💬 Почати чат про це" → передає контекст в Gomin AI Chat

**Вдосконалення:**
- Expert Mode persistence (кешування результатів)
- Smart caching (повторний аналіз того ж чату не робить новий запит)
- Message counter в UI
- Ліміт токенів збільшено до 16K

### 3.3 — Air Alert (Повітряна тривога)
**Сервер:** `alert_server_main_fixed.py` — Flask + Firebase Cloud Messaging
- Парсинг каналів з тривогами через BeautifulSoup
- Пуш-сповіщення через FCM
- Ручна перевірка статусу в налаштуваннях

**Клієнт:**
- `AirAlertController.kt` — перевірка регіону, відображення тривоги
- Червоний заголовок при тривозі (ActionBar)
- **Проблема (FIXED):** Регіон скидався кожні 10 секунд через баг парсингу

### 3.4 — Кастомні звуки
- `gomin_notif.ogg` — сповіщення
- `gomin_siren.ogg` — початок тривоги
- `gomin_cancel.ogg` — відбій тривоги
- Прив'язка через `CherrygramChatsConfig.NOTIF_SOUND_GOMIN = 3`, дефолтне значення

### 3.5 — Gomin Black Edition Activity
- `GominBlackEditionActivity.kt` — окремий екран з маніфестом
- Назва: "Гомін: Карбон"
- Філософський текст про ментальне здоров'я

### 3.6 — FAB (перша версія)
- Іконка `lucide_sparkles.xml` замість `gomin_bird`
- Кнопка 48dp, sub-button стиль
- **Проблема:** малий розмір, незручно

---

## Phase 4: Live API & Final Polish (1–3 червня)

**Мета:** Додати голосовий інтерфейс через Gemini Live API WebSocket.

### 4.1 — FAB Overhaul
- 48dp → **60dp** (`FragmentFloatingButton.java`)
- `isSubButton = false` (кругла кнопка)
- Single tap → Gomin AI Chat
- Long tap → Live Voice Session (Jarvis trigger)
- Haptic feedback on long tap

### 4.2 — Gemini Live API WebSocket
**Ключові файли:**
- `GominLiveManager.kt` — WebSocket клієнт (489 рядків)
- `GominLiveEdgeGlowView.kt` — візуальний ефект
- `LiveIndicatorView.kt` — індикатор сесії

**Архітектура:**
- OkHttp WebSocket
- OkHttpClient: connectTimeout=10s, readTimeout=0, writeTimeout=10s, pingInterval=15s
- AudioRecord (16kHz, PCM 16bit) → Base64 → WebSocket
- AudioTrack для відтворення відповіді

**Вирішені проблеми:**
1. **Casing:** Вихідні (client→server) — snake_case (`generation_config`, `response_modalities`), вхідні (server→client) — camelCase (`serverContent`, `toolCall`)
2. **Model:** `models/gemini-2.5-flash-native-audio-preview-12-2025` для voice, `models/gemini-3.1-flash-live-preview` для transcription
3. **setupComplete:** Очікування `setupComplete: {}` перед `startAudioThreads()`
4. **Mutex:** `synchronized(audioLock)` для thread-safe stopSession
5. **ToolCall:** Парсинг на top-level `obj.toolCall.functionCalls` (не всередині `serverContent.modelTurn.parts`)
6. **Transcription:** Нативний `inputAudioTranscription: {}` + `serverContent.inputTranscription.text` (замість "стенографіста")
7. **Deduplication:** Буферизація в StringBuilder + commit при `turnComplete`
8. **Audio format:** `"audio/pcm"` (без `;rate=16000`)
9. **CPU protection:** Перевірка `AudioTrack.STATE_INITIALIZED` перед циклом

**Тести:**
- `GominAiChatHelperDedupTest.kt` — тест дедуплікації

### 4.3 — UI Інтеграція
- `ChatActivityEnterView.java`: long-press на скріпку → старт транскрипції, червона крапка замість скріпки
- `DialogsActivity.java`: LiveIndicatorView, FAB 60dp
- `ChatActivity.java`: Gemini buttons, Shield, model selector

### 4.4 — Останні правки (3 червня)
- Виправлення контрасту Black Edition
- AirAlertController UI реорганізація
- Виправлення дзвінка (VoIP decline red button)
- Фікс MediaPlayer NPE
- Відновлення gomin_notif замість gomin_test

---

## Поточний стан проекту (3 червня 2026)

### Що працює:
- ✅ Повний ре Branding (Gomin замість Cherrygram)
- ✅ Black Edition тема + Monет адаптація
- ✅ Gomin Speed Engine (Boost + Shelter)
- ✅ Ghost Mode (Unread Guard, typing, stories, online, anti-delete)
- ✅ Gomin AI Chat (Gemini SDK)
- ✅ Gomin Shield (аналіз токсичності з кешуванням)
- ✅ Air Alert (Flask бекенд + FCM + UI)
- ✅ Кастомні звуки (siren, notif, cancel)
- ✅ FAB 60dp (single tap → AI, long tap → Live)
- ✅ Gemini Live API (voice call + transcription)
- ✅ OTA Auto-Updater (GitHub Releases)

### Що в процесі / планується:
- 🔄 **Gomin Drive** — хмарне сховище на базі Saved Messages + MediaActivity
- 🔄 **Gomin Jarvis** — Google Search web retrieval + читання Telegram каналів
- 🔄 Подальша стабілізація Live API (BufferedInput, cummulative dedup)

### Відомі проблеми:
- Live API потребує подальшої стабілізації (баги 1-8 з Refactor.md)
- Немає обробки `goAway` та `sessionResumptionUpdate` в WebSocket
- Потенційно неправильна модель для transcription (`gemini-3.1-flash-live-preview` — треба уточнити)

---

## Карта архітектурних рішень (що і чому)

| Рішення | Причина | Коли |
|---------|---------|------|
| **Virtual AI Dialogue (99999999L)** | Не створювати Telegram-бота, не мати сервер | Phase 3 |
| **Pre-Seeded Context** | Передача контексту з Shield в AI Chat | Phase 3 |
| **Dynamic Theme Split** | Android system dark mode скидав тему | Phase 2 |
| **Resource Overlay (res-cherrygram)** | Чистий merge з upstream Telegram | Phase 1 |
| **Snake_case outgoing, camelCase incoming** | Google Live API так приймає/віддає | Phase 4 |
| **setupComplete lock** | Аудіо до готовності сервера — dead session | Phase 4 |
| **Mutex для stopSession** | Race condition при закритті WebSocket | Phase 4 |
| **16kHz PCM audio** | Google Live API вимагає саме цей формат | Phase 4 |
| **Native inputAudioTranscription** | Якість розпізнавання української вища за Whisper | Phase 4 |

---

## Як читати цей файл

Це живий документ. Оновлюй при кожній значній зміні проекту. Якщо додається нова фаза — вставляй перед "Поточний стан". Якщо фіча дойшла до релізу — перенеси в "Що працює".

**Файли рядом:**
- `.memory/META_PROMPT.md` — системна інструкція для AI
- `.memory/TASKS/` — технічні завдання
- `.memory/activeContext.md` — поточна сесія
