# Gomin UA — Детальний покроковий план рефакторингу

Цей документ містить вичерпний покроковий план рефакторингу системи **Gomin Shield (Ментальний щит)** та **Gomin Air Alert (Повітряна тривога)**. 

Кожен крок розписаний максимально детально, з конкретними назвами файлів, імпортами, структурами коду «ДО» та «ПІСЛЯ» та інструкціями для інтеграції. Це дозволить впроваджувати зміни поступово, крок за кроком, без ризику зламати стабільність проекту.

---

## Зміст кроків
- [Крок 1: Ментальний щит — Виправлення скролу в Bottom Sheet](#крок-1-ментальний-щит--виправлення-скролу-в-bottom-sheet)
- [Крок 2: Air Alert — Створення тихого каналу та системного звуку відбою](#крок-2-air-alert--створення-тихого-каналу-та-системного-звуку-відбою)
- [Крок 3: Air Alert — Рання ініціалізація каналів сповіщень](#крок-3-air-alert--рання-ініціалізація-каналів-сповіщень)
- [Крок 4: Air Alert — Спрощення контролера та ізоляція MediaPlayer](#крок-4-air-alert--спрощення-контролера-та-ізоляція-mediaplayer)
- [Крок 5: Air Alert — Оновлення ресивера зупинки сирени](#крок-5-air-alert--оновлення-ресивера-зупинки-сирени)
- [Крок 6: Air Alert — Усунення дублювання сповіщень у FCM Listener](#крок-6-air-alert--усунення-дублювання-сповіщень-у-fcm-listener)
- [Крок 7: Air Alert — Виправлення поведінки при вимкненні екрана](#крок-7-air-alert--виправлення-поведінки-при-вимкненні-екрана)
- [Крок 8: Air Alert — Очищення невикористовуваного налаштування API Key](#крок-8-air-alert--очищення-невикористовуваного-налаштування-api-key)
- [Крок 9: Чистка проекту — Видалення застарілих серверних скриптів](#крок-9-чистка-проекту--видалення-застарілих-серверних-скриптів)

---

### Крок 1: Ментальний щит — Виправлення скролу в Bottom Sheet

**Мета**: Замінити стандартний `ScrollView` на `NestedScrollView` та зв'язати його з `nestedScrollChild` класу `BottomSheet`. Це змусить жест свайпу вниз для закриття шторки спрацьовувати лише тоді, коли користувач прокрутив весь текст аналізу до самого верху.

* **Файл**: `TMessagesProj/src/main/java/uz/unnarsx/cherrygram/chats/gemini/GominShieldBottomSheet.kt`

#### Що конкретно зробити:

1. **Імпорти**:
   Замінити рядок:
   ```kotlin
   import android.widget.ScrollView
   ```
   На рядок:
   ```kotlin
   import androidx.core.widget.NestedScrollView
   ```

2. **Оголошення поля класу**:
   Змінити тип змінної `scrollView` з:
   ```kotlin
   private val scrollView: ScrollView
   ```
   На:
   ```kotlin
   private val scrollView: NestedScrollView
   ```

3. **Ініціалізація у блоці `init`**:
   Замінити створення об'єкта (лінії 123-126):
   ```kotlin
   scrollView = ScrollView(context).apply {
       overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
       isVerticalScrollBarEnabled = true
   }
   ```
   На:
   ```kotlin
   scrollView = NestedScrollView(context).apply {
       overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
       isVerticalScrollBarEnabled = true
   }
   ```

4. **Параметр Dismiss**:
   Залишити `setCanDismissWithSwipe(true)` на лінії 67, але оскільки тепер `nestedScrollChild = scrollView` вказує на `NestedScrollView`, шторка більше не буде закриватися при звичайному скролі тексту вгору/назад.

---

### Крок 2: Air Alert — Створення тихого каналу та системного звуку відбою

**Мета**: 
1. Створити тихий канал (`air_alert_silent`) з низьким пріоритетом (без звуку та вібрації) для перепостингу сповіщення під час натискання кнопки «Зупинити сирену».
2. Перевести відбій тривоги на системний рівень, прописавши звук `gomin_cancel.mp3` безпосередньо в канал відбою (`air_alert_info`).

* **Файл**: `TMessagesProj/src/main/java/uz/unnarsx/cherrygram/alerts/AirAlertNotificationHelper.kt`

#### Що конкретно зробити:

1. **Додати константу тихого каналу**:
   ```kotlin
   private const val CHANNEL_SILENT_ID = "air_alert_silent"
   ```

2. **Оновити метод `createNotificationChannels`**:
   Додати звук до каналу відбою та створити третій тихий канал.
   
   *Код ДО:*
   ```kotlin
   // Канал для ВІДБОЮ (Звичайний)
   val infoChannel = NotificationChannel(
       CHANNEL_INFO_ID,
       "Повітряна тривога (Інфо)",
       NotificationManager.IMPORTANCE_DEFAULT
   ).apply {
       description = "Сповіщення про відбій тривоги"
       enableVibration(true)
   }
   ```
   
   *Код ПІСЛЯ:*
   ```kotlin
   // Канал для ВІДБОЮ (Звичайний зі звуком відбою)
   val cancelUri = Uri.parse("android.resource://${context.packageName}/${R.raw.gomin_cancel}")
   val infoChannel = NotificationChannel(
       CHANNEL_INFO_ID,
       "Повітряна тривога (Інфо)",
       NotificationManager.IMPORTANCE_HIGH // Піднімаємо пріоритет для відтворення звуку
   ).apply {
       description = "Сповіщення про відбій тривоги"
       setSound(cancelUri, AudioAttributes.Builder()
           .setUsage(AudioAttributes.USAGE_NOTIFICATION)
           .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
           .build())
       enableVibration(true)
       lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
   }

   // Канал для ТИХОЇ ТРИВОГИ (Коли сирену зупинили, але статус висить)
   val silentChannel = NotificationChannel(
       CHANNEL_SILENT_ID,
       "Повітряна тривога (Без звуку)",
       NotificationManager.IMPORTANCE_LOW
   ).apply {
       description = "Активна тривога з вимкненим звуком сирени"
       setSound(null, null)
       enableVibration(false)
       lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
   }
   
   notificationManager.createNotificationChannel(criticalChannel)
   notificationManager.createNotificationChannel(infoChannel)
   notificationManager.createNotificationChannel(silentChannel) // Не забути зареєструвати!
   ```

3. **Створити метод `showSilentNotification`**:
   Додати новий метод до об'єкта `AirAlertNotificationHelper`:
   ```kotlin
   fun showSilentNotification(context: Context, title: String, body: String) {
       val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
       
       val contentIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.let {
           PendingIntent.getActivity(context, 0, it, PendingIntent.FLAG_IMMUTABLE)
       }

       val builder = NotificationCompat.Builder(context, CHANNEL_SILENT_ID)
           .setSmallIcon(R.mipmap.icon_launcher_cherry)
           .setContentTitle(title)
           .setContentText(body)
           .setPriority(NotificationCompat.PRIORITY_LOW) // Низький пріоритет
           .setAutoCancel(false)
           .setOngoing(true)
           .setContentIntent(contentIntent)
           .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

       notificationManager.notify(NOTIFICATION_ID, builder.build()) // Перезаписує старе сповіщення 1001
   }
   ```

---

### Крок 3: Air Alert — Рання ініціалізація каналів сповіщень

**Мета**: Перенести ініціалізацію каналів сповіщень у `ApplicationLoader.java` для того, щоб канали були створені в системі до обробки першого FCM пуша (коли додаток запущений «холодним» способом у фоні).

* **Файли**: 
  1. `TMessagesProj/src/main/java/org/telegram/messenger/ApplicationLoader.java`
  2. `TMessagesProj/src/main/java/org/telegram/ui/LaunchActivity.java`

#### Що конкретно зробити:

1. **В `ApplicationLoader.java`**:
   Додати імпорт:
   ```java
   import uz.unnarsx.cherrygram.alerts.AirAlertNotificationHelper;
   ```
   Вставити виклик створення каналів всередину методу `onCreate()` після ініціалізації `applicationContext` (наприклад, перед `LauncherIconController.tryFixLauncherIconIfNeeded()`):
   ```java
   AirAlertNotificationHelper.INSTANCE.createNotificationChannels(applicationContext);
   ```

2. **В `LaunchActivity.java`**:
   Видалити старий виклик (орієнтовно лінія 9083):
   ```java
   uz.unnarsx.cherrygram.alerts.AirAlertNotificationHelper.INSTANCE.createNotificationChannels(this);
   ```

---

### Крок 4: Air Alert — Спрощення контролера та ізоляція MediaPlayer

**Мета**: Видалити ручний `MediaPlayer` для реальних тривог. Зберегти `MediaPlayer` **виключно** для тестового режиму в налаштуваннях. Додати підтримку динамічних текстів пуша (`title`/`body`) у контролер.

* **Файл**: `TMessagesProj/src/main/java/uz/unnarsx/cherrygram/alerts/AirAlertController.kt`

#### Що конкретно зробити:

1. **Додати змінні збереження стану останнього пуша**:
   ```kotlin
   @Volatile
   private var lastAlertTitle: String? = null
   @Volatile
   private var lastAlertBody: String? = null
   ```

2. **Модифікувати `handlePushStatus` та `setAlertStatus`**:
   Додати параметри `title` та `body` та передавати їх у сповіщення.
   
   *Код ДО:*
   ```kotlin
   fun handlePushStatus(alert: Boolean) { ... }
   private fun setAlertStatus(active: Boolean) { ... }
   ```
   
   *Код ПІСЛЯ:*
   ```kotlin
   fun handlePushStatus(alert: Boolean, title: String? = null, body: String? = null) {
       AndroidUtilities.runOnUIThread {
           if (isTesting) {
               pendingAlertStatus = alert
           } else {
               setAlertStatus(alert, title, body)
           }
       }
   }

   private fun setAlertStatus(active: Boolean, title: String? = null, body: String? = null) {
       if (isTesting) {
           pendingAlertStatus = active
           return
       }
       
       val changed = isAlertActive != active
       if (changed) {
           isAlertActive = active
           CherrygramCoreConfig.airAlertLastActive = active
           
           val context = org.telegram.messenger.ApplicationLoader.applicationContext
           val regionName = CherrygramCoreConfig.airAlertRegionName.ifEmpty { "Ваша область" }
           
           if (isAlertActive) {
               // Зберігаємо останній текст тривоги
               lastAlertTitle = title ?: "🚨 ПОВІТРЯНА ТРИВОГА"
               lastAlertBody = body ?: regionName
               
               AirAlertNotificationHelper.showStartNotification(
                   context,
                   lastAlertTitle!!,
                   lastAlertBody!!
               )
               
               safetyStopRunnable?.let { AndroidUtilities.cancelRunOnUIThread(it) }
               val runnable = Runnable {
                   setAlertStatus(false) // Авто-відбій через 12 годин
               }
               safetyStopRunnable = runnable
               AndroidUtilities.runOnUIThread(runnable, SAFETY_TIMEOUT_MS)
           } else {
               safetyStopRunnable?.let { AndroidUtilities.cancelRunOnUIThread(it) }
               safetyStopRunnable = null
               
               // Прибираємо сирену тривоги та показуємо відбій
               val endTitle = title ?: "✅ ВІДБІЙ ТРИВОГИ"
               val endBody = body ?: regionName
               AirAlertNotificationHelper.showEndNotification(
                   context,
                   endTitle,
                   endBody
               )
           }
           NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.cgAirAlertStatusChanged)
       }
   }
   ```

3. **Модифікувати `playSound`**:
   Повністю відключити `playSound` для реальних тривог, залишаючи запуск плеєра тільки під час тестування (`isTesting == true`).
   
   *Код ПІСЛЯ:*
   ```kotlin
   private fun playSound(isStart: Boolean) {
       // ДЛЯ РЕАЛЬНИХ ТРИВОГ ЗВУК ГРАЄ СИСТЕМА ЧЕРЕЗ КАНАЛ СПОВІЩЕНЬ!
       if (!isTesting) {
           return
       }
       
       try {
           mediaPlayer?.setOnCompletionListener(null)
           mediaPlayer?.stop()
           mediaPlayer?.release()
           mediaPlayer = null

           if (isStart) {
               val soundRes = org.telegram.messenger.R.raw.gomin_siren
               val player = MediaPlayer.create(org.telegram.messenger.ApplicationLoader.applicationContext, soundRes)
               if (player != null) {
                   mediaPlayer = player
                   player.start()
               }
           }
       } catch (e: Exception) {
           FileLog.e(e)
           mediaPlayer?.release()
           mediaPlayer = null
       }
   }
   ```

4. **Оновити метод `stopSiren`**:
   Цей метод викликається кнопкою «Зупинити сирену» в шторці. Замість зупинки плеєра він має перевести активну нотифікацію в тихий канал.
   
   *Код ПІСЛЯ:*
   ```kotlin
   fun stopSiren() {
       AndroidUtilities.runOnUIThread {
           if (isTesting) {
               stopTest()
               return@runOnUIThread
           }
           
           // Якщо тривога активна — переводимо сповіщення на тихий канал
           if (isAlertActive) {
               val context = org.telegram.messenger.ApplicationLoader.applicationContext
               val title = lastAlertTitle ?: "🚨 ПОВІТРЯНА ТРИВОГА"
               val body = lastAlertBody ?: "Звук сирени вимкнено"
               AirAlertNotificationHelper.showSilentNotification(context, title, body)
           } else {
               // Якщо відбій або тривога неактивна — прибираємо все
               val context = org.telegram.messenger.ApplicationLoader.applicationContext
               AirAlertNotificationHelper.cancelAll(context)
           }
       }
   }
   ```

---

### Крок 5: Air Alert — Оновлення ресивера зупинки сирени

**Мета**: Переконатися, що ресивер коректно перенаправляє подію натискання кнопки в шторці до `AirAlertController.stopSiren()`.

* **Файл**: `TMessagesProj/src/main/java/uz/unnarsx/cherrygram/alerts/AirAlertStopReceiver.kt`

#### Що конкретно зробити:

Перевірити та за потреби спростити метод `onReceive`:
```kotlin
class AirAlertStopReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "STOP_SIREN") {
            AirAlertController.stopSiren() // Перенаправляємо в контролер
        }
    }
}
```

---

### Крок 6: Air Alert — Усунення дублювання сповіщень у FCM Listener

**Мета**: Запобігти створенню сповіщень напряму з `GcmPushListenerService.java`. Тепер сервіс лише передає дані пуша (зокрема, тексти `title` та `body`) до `AirAlertController`, який самостійно створює одне правильне сповіщення.

* **Файл**: `TMessagesProj/src/main/java/org/telegram/messenger/GcmPushListenerService.java`

#### Що конкретно зробити:

*Код ДО:*
```java
if (data.containsKey("action")) {
    String action = data.get("action");
    String title = data.get("title");
    String body = data.get("body");
    boolean isAlert = "alert_on".equals(action);

    if (title != null && body != null) {
        if (isAlert) {
            uz.unnarsx.cherrygram.alerts.AirAlertNotificationHelper.INSTANCE.showStartNotification(getApplicationContext(), title, body);
        } else {
            uz.unnarsx.cherrygram.alerts.AirAlertNotificationHelper.INSTANCE.showEndNotification(getApplicationContext(), title, body);
        }
    }

    if ("alert_on".equals(action)) {
        uz.unnarsx.cherrygram.alerts.AirAlertController.INSTANCE.handlePushStatus(true);
    } else if ("alert_off".equals(action)) {
        uz.unnarsx.cherrygram.alerts.AirAlertController.INSTANCE.handlePushStatus(false);
    }
}
```

*Код ПІСЛЯ:*
```java
if (data.containsKey("action")) {
    String action = data.get("action");
    String title = data.get("title");
    String body = data.get("body");

    // Видалено прямий виклик AirAlertNotificationHelper.
    // Передаємо параметри у AirAlertController для централізованої обробки.
    if ("alert_on".equals(action)) {
        uz.unnarsx.cherrygram.alerts.AirAlertController.INSTANCE.handlePushStatus(true, title, body);
    } else if ("alert_off".equals(action)) {
        uz.unnarsx.cherrygram.alerts.AirAlertController.INSTANCE.handlePushStatus(false, title, body);
    }
}
```

---

### Крок 7: Air Alert — Виправлення поведінки при вимкненні екрана

**Мета**: Прибрати застаріле вимкнення сирени при переході екрана у сплячий режим (`ACTION_SCREEN_OFF`). Сирена має грати свій час, навіть якщо телефон лежить у кишені з вимкненим екраном.

* **Файл**: `TMessagesProj/src/main/java/org/telegram/messenger/ScreenReceiver.java`

#### Що конкретно зробити:

*Код ДО:*
```java
if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
    if (BuildVars.LOGS_ENABLED) {
        FileLog.d("screen off");
    }
    ConnectionsManager.getInstance(UserConfig.selectedAccount).setAppPaused(true, true);
    ApplicationLoader.isScreenOn = false;
    uz.unnarsx.cherrygram.alerts.AirAlertController.INSTANCE.stopSiren(); // << ЦЕЙ РЯДОК ГЛУШИВ ТРИВОГУ В КИШЕНІ
}
```

*Код ПІСЛЯ:*
```java
if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
    if (BuildVars.LOGS_ENABLED) {
        FileLog.d("screen off");
    }
    ConnectionsManager.getInstance(UserConfig.selectedAccount).setAppPaused(true, true);
    ApplicationLoader.isScreenOn = false;
    // Виклик stopSiren() видалено. Сирена більше не затихає при згасанні екрана.
}
```

---

### Крок 8: Air Alert — Очищення невикористовуваного налаштування API Key

**Мета**: Оскільки система працює через FCM пуші від нашого власного бекенда, користувачеві не потрібно вводити API-ключ від `alerts.in.ua`. Видаляємо це поле з конфігурації та коду налаштувань для чистоти інтерфейсу.

* **Файли**:
  1. `TMessagesProj/src/main/java/uz/unnarsx/cherrygram/core/configs/CherrygramCoreConfig.kt`
  2. `TMessagesProj/src/main/java/uz/unnarsx/cherrygram/preferences/CGPreferencesEntry.java`

#### Що конкретно зробити:

1. **В `CherrygramCoreConfig.kt`**:
   Видалити рядок 98:
   ```kotlin
   var airAlertApiKey by sharedPreferences.string("CP_AirAlert_ApiKey", "")
   ```

2. **В `CGPreferencesEntry.java`**:
   * Видалити оголошення константи (лінія 121):
     ```java
     private final int airAlertApiKeyRow = 71;
     ```
   * Видалити метод `showAirAlertApiKeyDialog()` (лінії 607-627).
   * Видалити обробник зміни у методі `onInputDone` (лінії 629-633):
     ```java
     protected void onInputDone(int id, String text) {
         if (id == airAlertApiKeyRow) {
             CherrygramCoreConfig.INSTANCE.setAirAlertApiKey(text);
         }
     }
     ```

---

### Крок 9: Чистка проекту — Видалення застарілих серверних скриптів

**Мета**: Видалити непотрібні копії скриптів парсингу з кореня проекту, які не використовуються клієнтським додатком.

* **Дія**: Повністю видалити такі файли з кореневого каталогу проекту:
  - `alert_server_main_utf8.py`
  - `alert_server_main_fixed.py`
