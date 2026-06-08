# TASK-004: AI Chat — фікс відправки фото та очищення історії

**Статус:** COMPLETED
**Дата:** 2026-06-03

## Опис

Два баги в Gomin AI Chat (dialog_id = 99999999L):
1. Фото/документи не відправляються — мовчки дропаються
2. Очищення історії не оновлює UI — чат пустіє тільки після перезаходу

---

## БАГ 1: Фото не відправляються в AI Chat

### Root Cause

**Файл:** `SendMessagesHelper.java:3882-3899`

Інтерсепція для GOMIN_AI_DIALOG_ID обробляє тільки текст:

```java
if (peer == Constants.GOMIN_AI_DIALOG_ID) {
    final String text = message != null ? message : (caption != null ? caption : "");
    if (!TextUtils.isEmpty(text)) {
        // зберігає текст в історію AI
        GominAiHistoryManager.INSTANCE.addMessage("user", text);
        // ...
        GominAiChatHelper.INSTANCE.queryAi(currentAccount, text);
    }
    return;  // <-- UNCONDITIONAL RETURN: photo/document/location/poll ІГНОРУЮТЬСЯ
}
```

**Наслідок:**
- Фото без caption → `text = ""` → нічого не зберігається, фото мовчки зникає
- Фото з caption → caption текст зберігається, але фото губиться
- Документи, відео, location — те саме

### Фікс

Додано перевірку на наявність медіа. Якщо користувач відправляє фото/документ без тексту — AI-чат ігнорує повідомлення (поки що немає підтримки inline image data в Gemini SDK інтеграції). Якщо з текстом — текст обробляється, медіа ігнорується з повідомленням.

---

## БАГ 2: Очищення історії не оновлює UI

### Root Cause

**Файл:** `GominAiChatHelper.kt:768-773`

```kotlin
builder.setPositiveButton("Очистити") { dialog, _ ->
    GominAiHistoryManager.clearHistory()    // Видаляє JSON ✅
    activeShieldContext = null
    activeShieldHistory = null
    notifyChatUpdated(UserConfig.selectedAccount)  // ❌ НЕ ПРАЦЮЄ
    dialog.dismiss()
}
```

`notifyChatUpdated()` постить `messagesDidLoad` з `loadIndex = 0`:

```kotlin
NotificationCenter.getInstance(currentAccount).postNotificationName(
    NotificationCenter.messagesDidLoad,
    ...,    // args[0-10]
    0,      // args[11]: loadIndex = 0  ❌
    ...
)
```

**Чому не працює:** В `ChatActivity.java:20241`:
```java
int index = waitingForLoad.indexOf(queryLoadIndex);  // index = waitingForLoad.indexOf(0)
```
`waitingForLoad` ніколи не містить `0` (індекси починаються з 1+). Тому:
```java
if (index == -1) {
    return;  // ВИХІД без обробки — UI не оновлюється
}
```

**Наслідок:** JSON файл видалено, але ChatActivity не знає про це — старі повідомлення залишаються в пам'яті (`messages`, `messagesByDays` etc.). Тільки при перезаході в чат `MessagesController.loadMessagesInternal()` завантажує пустий JSON і показує пустий чат.

### Фікс

Два кроки:

**Крок 1:** `ChatActivity.java:24634` — змінити `clearHistory` з `private` на `public`:

```java
// Було:
private void clearHistory(boolean overwrite, TL_updates_channelDifferenceTooLong differenceTooLong) {
// Стало:
public void clearHistory(boolean overwrite, TL_updates_channelDifferenceTooLong differenceTooLong) {
```

**Крок 2:** `GominAiChatHelper.kt:768-773` — замінити `notifyChatUpdated()` на прямий виклик:

```kotlin
builder.setPositiveButton("Очистити") { dialog, _ ->
    GominAiHistoryManager.clearHistory()
    activeShieldContext = null
    activeShieldHistory = null
    dialog.dismiss()
    activity.clearHistory(false, null)  // Прямий виклик — очищає messages, adapter, UI
}
```

## Acceptance Criteria

- [ ] Фото з caption: caption текст потрапляє в AI, фото ігнорується (не крашить)
- [ ] Фото без caption: нічого не відбувається (не крашить, не дропає мовчки)
- [ ] Текст без фото: працює як раніше
- [ ] Очищення історії: UI миттєво показує пустий чат (без перезаходу)
- [ ] Build проходить
