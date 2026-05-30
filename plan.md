# Технічне завдання: Виправлення багів інтерфейсу Gomin AI

Даний документ містить інструкції для виправлення трьох критичних багів у віртуальному чаті Gomin AI.

---

## 1. Масштабування іконки Gomin AI (пташки)
**Файл:** `TMessagesProj/src/main/java/org/telegram/ui/Components/AvatarDrawable.java`

**Проблема:** Векторний логотип пташки завеликий для кола аватара.

**Інструкція:**
У методі `draw(Canvas canvas)` знайти блок, де малюється `avatarType == AVATAR_TYPE_GOMIN_AI`. Потрібно змінити масштаб іконки перед її малюванням.
Приклад логіки:
```java
if (drawable != null) {
    float currentScale = scaleSize;
    if (avatarType == AVATAR_TYPE_GOMIN_AI) {
        // Зменшуємо іконку відносно розміру контейнера
        currentScale *= (size / (float) dp(108)) * 0.85f; 
    }
    final int w = (int) (drawable.getIntrinsicWidth() * currentScale);
    final int h = (int) (drawable.getIntrinsicHeight() * currentScale);
    // ... далі стандартний код встановлення меж (bounds) та малювання
}
```

---

## 2. Блокування переходу на чорний екран (Профіль)
**Файл:** `TMessagesProj/src/main/java/org/telegram/ui/Components/ChatAvatarContainer.java`

**Проблема:** При натисканні на хедер чату відкривається пустий профіль, оскільки ID `99999999L` не існує в системі Telegram.

**Інструкція:**
У методі `openProfile(boolean byAvatar, boolean fromChatAnimation, boolean removeLast)` додати перевірку на самому початку:
```java
if (user != null && user.id == uz.unnarsx.cherrygram.misc.Constants.GOMIN_AI_DIALOG_ID) {
    return; // Профілю для віртуального асистента не існує, ігноруємо клік
}
```

---

## 3. Виправлення порядку повідомлень (Знизу вгору)
**Файл:** `TMessagesProj/src/main/java/org/telegram/ui/ChatActivity.java`

**Проблема:** Чат відображається "задом наперед" — нові повідомлення зверху, старі знизу.

**Інструкція:**
В `ChatActivity.java` всередині `onFragmentCreate()` при ініціалізації параметрів для діалогу Gomin AI примусово вимкнути інверсію:
```java
if (dialog_id == uz.unnarsx.cherrygram.misc.Constants.GOMIN_AI_DIALOG_ID) {
    reversed = false; 
}
```
Також переконайтеся, що в `GominAiHistoryManager.kt` метод `loadMessages` повертає список повідомлень у правильному хронологічному порядку для адаптера Telegram.

---

## 4. Оновлення статусу "пише..." у хедері
**Файл:** `TMessagesProj/src/main/java/uz/unnarsx/cherrygram/chats/gemini/GominAiChatHelper.kt`

**Проблема:** Субтитри в хедері можуть не оновлюватися або конфліктувати з системним ActionBar.

**Інструкція:**
У методі `setTypingStatus(isTyping: Boolean)` замінити звернення до `actionBar` на `avatarContainer`:
```kotlin
if (isTyping) {
    activity.avatarContainer?.setSubtitle("пише...")
} else {
    activity.avatarContainer?.setSubtitle("$friendlyModelName • онлайн")
}
```

---

## Резюме файлів для редагування:
1. `org.telegram.ui.Components.AvatarDrawable.java`
2. `org.telegram.ui.Components.ChatAvatarContainer.java`
3. `org.telegram.ui.ChatActivity.java`
4. `uz.unnarsx.cherrygram.chats.gemini.GominAiChatHelper.kt`
5. `uz.unnarsx.cherrygram.chats.gemini.GominAiHistoryManager.kt` (перевірка порядку масиву)
