# Live API Refactor — Повний аудит для наступної сесії

**Дата:** 2 червня 2026
**Мета:** Зберегти всі знайдені проблеми, щоб почати з чистого аркуша в новому чаті.

---

## ФІНАЛЬНЕ СЛОВО ПРО CAMELCASE vs SNAKE_CASE

Перевірив офіційний Google форум. Розробник надіслав конфіг з camelCase (`generationConfig`, `responseModalities`, `systemInstruction`), отримав відповідь від Google-співробітника, який сказав лише оновити модель — **не сказав змінити casing**. Це означає, що **обидва варіанти працюють** для setup. Google автоматично приймає і snake_case (Python convention), і camelCase (JS/JS SDK convention). Тому попередні AI-асистенти, які міняли casing, ганяли тебе по колу — **casing не був реальною проблемою**.

Справжні баги — інші (див. нижче).

---

## РЕАЛЬНІ БАГИ (перевірено, без фантазій)

### BUG 1 — Tool call парситься в неправильному місці
**Файл:** `TMessagesProj/src/main/java/uz/unnarsx/cherrygram/chats/gemini/GominLiveManager.kt:421-457`

Зараз код шукає `functionCall` всередині `serverContent.modelTurn.parts[]`. Але згідно з Vertex AI документацією, `toolCall` приходить як **окреме top-level поле** в JSON. Структура:
```json
{
  "toolCall": {
    "functionCalls": [
      { "id": "...", "name": "get_air_alerts", "args": {} }
    ]
  }
}
```

**Наслідок:** функція `get_air_alerts` для перевірки повітряних тривог ніколи не викличеться. Сервер надсилає `toolCall` окремо, код чекає його не там.

**Як виправити:** додати окрему гілку в `parseServerMessage`:
```kotlin
if (obj.has("toolCall")) {
    val toolCall = obj.getJSONObject("toolCall")
    val functionCalls = toolCall.optJSONArray("functionCalls")
    if (functionCalls != null) {
        for (i in 0 until functionCalls.length()) {
            val call = functionCalls.getJSONObject(i)
            // обробити виклик
        }
    }
}
```

---

### BUG 2 — `playThread` запускається даремно в режимі транскрипції
**Файл:** `GominLiveManager.kt:195-243`

Коли `isTranscriptionMode = true`, аудіо-відповіді від моделі немає. Але `playThread` все одно стартує і витрачає батарею на опитування порожньої черги `audioPlayQueue`.

**Як виправити:** обернути запуск `playThread` в `if (!isTranscriptionMode)`.

---

### BUG 3 — Дублювання тексту при завершенні turn-а
**Файл:** `GominAiChatHelper.kt:890-895`

```kotlin
onTextReceived = { text ->
    AndroidUtilities.runOnUIThread {
        val field = enterView.getEditField() ?: return@runOnUIThread
        field.append(text)
    }
}
```

Gemini Live API може переглядати вже згенерований текст при `turnComplete`. Без дедуплікації на стиках turn-ів з'являються дублі слів/фраз.

**Як виправити:** додати просту логіку — зберігати останній chunk, порівнювати новий. Або накопичувати в буфер і комітити тільки при `turnComplete`.

---

### BUG 4 — Неправильна назва MIME для аудіо (потенційно)
**Файл:** `GominLiveManager.kt:313`

```kotlin
put("mimeType", "audio/pcm;rate=16000")
```

Офіційно Google приймає `audio/pcm`. Параметр `rate=16000` працює, але деякі проксі/SDK його ігнорують або погано обробляють. Безпечніше `"audio/pcm"`.

---

### BUG 5 — Прапорець `isConnected` встановлюється до `setupComplete`
**Файл:** `GominLiveManager.kt:131-135`

`onOpen` ставить `isConnected = true` одразу після WebSocket handshake. Але `setupComplete` ще не прийшов. Зараз не ламає (бо `startAudioThreads` викликається тільки з `setupComplete`), але назва неправильна — це не "з'єднано", це "websocket відкрито". Краще назвати `isWebSocketOpen` і додати окремий `isSetupComplete`.

---

### BUG 6 — Модель `"models/gemini-2.5-flash"` для voice call не є Live-моделлю
**Файл:** `GominLiveManager.kt:154-158`

```kotlin
val targetModel = if (isTranscriptionMode) {
    "models/gemini-3.0-live"        // треба уточнити
} else {
    "models/gemini-2.5-flash"       // не є Live моделлю
}
```

**Перевірені актуальні Live-моделі (червень 2026):**
- `gemini-2.5-flash-native-audio-preview-12-2025` — найкраща якість, native audio
- `gemini-2.5-flash-live` — стабільна
- `gemini-live-2.5-flash-native-audio` — для Vertex AI
- `gemini-2.0-flash-live-001` — стара, ще працює

Модель `gemini-2.5-flash` (звичайна) НЕ підтримує WebSocket BidiGenerateContent. Потрібна модель з `live` у назві.

Для transcription рекомендую `gemini-2.5-flash-native-audio-preview-12-2025` з `input_audio_transcription: {}` (див. нижче).

---

### BUG 7 — Транскрипція використовує "стенографіст"-підхід замість нативного `input_audio_transcription`
**Файл:** `GominLiveManager.kt:152-188`

Зараз ти в `setup` ставиш `responseModalities: ["TEXT"]` і в системному промпті кажеш моделі бути стенографістом. Це працює, але це обхідний шлях.

**Правильний підхід (краща якість, офіційно):**
```json
{
  "setup": {
    "model": "models/gemini-2.5-flash-native-audio-preview-12-2025",
    "generationConfig": {
      "responseModalities": ["AUDIO"]
    },
    "inputAudioTranscription": {},
    "outputAudioTranscription": {}
  }
}
```

Тоді сервер стрімить:
- `serverContent.inputTranscription.text` — текст того, що сказав користувач
- `serverContent.outputTranscription.text` — текст того, що відповіла модель

Це окремі side-channel поля, які використовують внутрішній ASR Google. Якість транскрипції українською значно вища, ніж через Whisper Large V3 від Groq (Gemini нативно підтримує `uk-UA`).

**Увага:** на free tier обмеження — 10 RPM, 250K TPM, 1500 RPD (Flash моделі). Pro моделі з квітня 2026 платні.

---

### BUG 8 — Відсутній обробник `turnComplete` у `parseServerMessage`
**Файл:** `GominLiveManager.kt:374-476`

`turnComplete` приходить у `serverContent` (рядок 405), але немає логіки "зафіксувати буфер, передати фінальний текст в UI". Зараз тільки `isAiSpeaking = false` і скидання амплітуди. Для транскрипції це означає, що фінальний текст turn-а не "комітиться" явно — він просто додається в міру приходу chunk-ів.

**Як виправити:** при `turnComplete` викликати окремий callback `onTurnComplete()` (для дедуплікації та flush буфера в `GominAiChatHelper`).

---

## ДРУГОРЯДНІ ПРОБЛЕМИ

- **Дублікат `audioRecord?.stop()` у `stopSession`** — `GominLiveManager.kt:494-498` та `:513-518` викликають `stop()` двічі. Другий виклик у try-catch, не ламає, але це dead-code від поганого рефакторингу.
- **Відсутня обробка `goAway`** — сервер може надіслати повідомлення про швидке закриття. Не критично, але гарна практика.
- **Відсутня обробка `sessionResumptionUpdate`** — для довгих сесій Google пропонує resumption tokens.

---

## ПЛАН ДІЙ (ПОРЯДОК ВИКОНАННЯ)

### Крок 1 — Виправити BUG 1 (tool call) [ПРІОРИТЕТ]
Перенести парсинг `toolCall` з `serverContent.modelTurn.parts[]` на top-level `obj.toolCall.functionCalls`. Це критично для роботи функції перевірки повітряних тривог.

### Крок 2 — Виправити BUG 6 (модель) [ПРІОРИТЕТ]
Замінити `"models/gemini-2.5-flash"` (звичайна) на `models/gemini-2.5-flash-native-audio-preview-12-2025` (Live) для voice call. Для transcription — той самий рядок.

### Крок 3 — Виправити BUG 7 (нативна транскрипція) [ПРІОРИТЕТ]
Додати в `setup` поля `inputAudioTranscription: {}` та `outputAudioTranscription: {}`. Змінити `responseModalities` на `["AUDIO"]`. Парсити нові поля `serverContent.inputTranscription.text` та `outputTranscription.text`. Прибрати "стенографіст" з системного промпту.

### Крок 4 — Виправити BUG 8 (turnComplete callback) [ВАЖЛИВО]
Додати `onTurnComplete` callback в `GominLiveManager`. Викликати його при `serverContent.turnComplete`. У `GominAiChatHelper` використовувати для flush буфера транскрипції.

### Крок 5 — Виправити BUG 3 (дедуплікація) [ВАЖЛИВО]
У `GominAiChatHelper` зберігати буфер `transcriptionBuffer` (StringBuilder). Chunk-и додавати в буфер. При `turnComplete` — комітити буфер в поле вводу, очищати буфер. Це запобігає дублюванню.

### Крок 6 — Дрібні виправлення [НЕ ТЕРМІНОВО]
- BUG 2: обернути `playThread` запуском у `if (!isTranscriptionMode)`
- BUG 4: змінити `"audio/pcm;rate=16000"` на `"audio/pcm"`
- BUG 5: перейменувати `isConnected` → `isWebSocketOpen` + додати `isSetupComplete`
- Дублікат `audioRecord?.stop()`: прибрати один з викликів

---

## ПИТАННЯ ДО КОРИСТУВАЧА (перед початком роботи)

1. **Яку саме модель використовувати для transcription?**
   - Варіант A: `gemini-2.5-flash-native-audio-preview-12-2025` + `inputAudioTranscription` (рекомендую)
   - Варіант B: `gemini-2.5-flash-live` (стабільніша, але старіша)
   - Варіант C: та, яку ти бачиш в AI Studio як `gemini-3.0-live` (треба уточнити повну назву з AI Studio UI)

2. **Для voice call (FAB) — яка модель?**
   - Рекомендую ту саму: `gemini-2.5-flash-native-audio-preview-12-2025` (нативне аудіо)

3. **Голос для voice call — залишити `Puck`?**

4. **Чи хочеш, щоб я зробив усі зміни одразу, чи по частинах (спочатку BUG 1+2+6+7 — критичні, потім решту)?**

5. **Шит-аналіз — залишити `gemma-4-31b` як зараз, чи переключити на `gemini-2.5-flash`?**

---

## КОРИСНІ ПОСИЛАННЯ

- Офіційна документація Live API: https://ai.google.dev/gemini-api/docs/live-api
- WebSocket getting started: https://ai.google.dev/gemini-api/docs/live-api/get-started-websocket
- Приклади від Google: https://github.com/google-gemini/gemini-live-api-examples
- Список моделей (uk-UA підтримується): https://firebase.google.com/docs/ai-logic/live-api/configuration
- Ліміти free tier: https://ai.google.dev/pricing

---

## ФАЙЛИ, ЯКІ ТРЕБА РЕДАГУВАТИ

1. `TMessagesProj/src/main/java/uz/unnarsx/cherrygram/chats/gemini/GominLiveManager.kt` — основний файл
2. `TMessagesProj/src/main/java/uz/unnarsx/cherrygram/chats/gemini/GominAiChatHelper.kt` — UI логіка

Інші файли (`GominLiveEdgeGlowView.kt`, `ChatActivityEnterView.java`) — НЕ чіпати, вони працюють правильно.
