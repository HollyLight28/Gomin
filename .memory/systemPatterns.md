# Gomin Architectural Patterns

### Dynamic Day/Night Theme Preference Segmentation
To prevent theme resets under system dark mode changes, always separate day and night theme storage based on the theme's properties (`themeInfo.isDark()`). If a theme is dark, it must populate `currentNightTheme` and update the `"nighttheme"` key. If it is light, it must populate `currentDayTheme` and update the `"theme"` key. This guarantees that Android's auto night switching relies on the exact theme intended by the user:
```java
if (save) {
    SharedPreferences preferences = MessagesController.getGlobalMainSettings();
    SharedPreferences.Editor editor = preferences.edit();
    if (themeInfo.isDark()) {
        currentNightTheme = themeInfo;
        editor.putString("nighttheme", themeInfo.getKey());
    } else {
        currentDayTheme = themeInfo;
        editor.putString("theme", themeInfo.getKey());
    }
    editor.apply();
}
```

### Dynamic Launcher Icons & Self-Healing
To ensure launcher icons work across different packages and standalone/debug suffix setups, always wrap PackageManager calls in try-catch structures and fall back to the default icon on exceptions:
```java
public static boolean isEnabled(LauncherIcon icon) {
    try {
        Context ctx = ApplicationLoader.applicationContext;
        int i = ctx.getPackageManager().getComponentEnabledSetting(icon.getComponentName(ctx));
        return i == PackageManager.COMPONENT_ENABLED_STATE_ENABLED || i == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && icon == LauncherIcon.GOMIN;
    } catch (Throwable e) {
        return icon == LauncherIcon.GOMIN;
    }
}
```

### Graceful Launcher Icon Component Change Restart
When changing launcher icons via `setComponentEnabledSetting`, Android OS forces a termination of the application process. To avoid this looking like an abrupt crash (perceived crash), always trigger a clean, controlled restart utilizing `AppRestartHelper.restartApp` with a safe delay (e.g., 500ms) to allow the OS and launcher threads to finish transitions:
```java
public static void setIcon(LauncherIcon icon) {
    Context ctx = ApplicationLoader.applicationContext;
    PackageManager pm = ctx.getPackageManager();
    for (LauncherIcon i : LauncherIcon.values()) {
        try {
            pm.setComponentEnabledSetting(i.getComponentName(ctx), i == icon ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED :
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        } catch (Throwable e) {
            // Ignore missing/unregistered components
        }
    }
    AndroidUtilities.runOnUIThread(() -> {
        try {
            uz.unnarsx.cherrygram.core.helpers.AppRestartHelper.restartApp(ctx);
        } catch (Throwable e) {
            System.exit(0);
        }
    }, 500);
}
```

### Elegant Monet Foreground Settings Icons
For dynamic theme systems like Monet (Black Edition) where icons are rendered natively as monochrome paths without colored circular plates, settings icons should never be colored using background-themed colors (`windowBackgroundWhiteBlueHeader` etc.) as it results in invisible elements blending with the background. Always color settings icon foreground using a specialized foreground resolver that guarantees pure white on dark themes and pure black on light themes:
```java
iconView.setColorFilter(new PorterDuffColorFilter(
    uz.unnarsx.cherrygram.helpers.ui.MonetHelper.getSettingsIconForegroundColor(iconColorTop), 
    PorterDuff.Mode.SRC_IN
));
```

### Font Mapping Strategy
For Gomin, the following font mapping is enforced in `FontHelper.java`:
- **Bold/Medium (Headers)** -> `fonts/playfair.ttf`
- **Regular (Messages)** -> `fonts/geist.ttf`
- **Early Init Protection**: Always check if `ApplicationLoader.applicationContext` is null before accessing assets.
- **Typeface Caching Pattern**: To prevent early static class loading from caching standard `Typeface.DEFAULT` permanently, bypass cache when `applicationContext` is null:
```java
public static Typeface getTypeface(String assetPath) {
    if (ApplicationLoader.applicationContext == null) {
        return FontHelper.createTypefaceFromAsset(assetPath);
    }
    return typefaceCache.computeIfAbsent(assetPath, path -> {
        // Caching loader logic...
    });
}
```

### Resource Overlay
Custom branding should be placed in `src/main/res-cherrygram/` or `src/main/res-solar/` to override standard resources without modifying the base `res/` directory. This allows for cleaner merges with upstream Telegram updates.

### Safe Asynchronous Form Preference State Synchronization
When building dynamic forms using TextWatcher (e.g. `GeminiPreferencesEntry.java`), always ensure that downstream views are null-safe. TextWatcher fires on `setText()` during initial `fillItems()` before downstream views are constructed.
```java
private void doOnDone() {
    if (geminiApiKeyField != null && geminiApiKeyField.getEditText() != null) {
        CherrygramMessagesConfig.INSTANCE.setGeminiApiKey(geminiApiKeyField.getEditText().getText().toString().trim());
    }
}
```

### Virtual Local AI Dialogue Pattern (Intercept & Bypass)
To construct custom local chat experiences (like the Gomin AI assistant) inside a native Telegram codebase without setting up a physical bot backend or triggering MTProto server errors, intercept dialog requests targeting a predefined virtual ID (`99999999L`):
1. **Mock User**: Intercept `MessagesController.getUser(Long id)` to dynamically construct and return a mock `TLRPC.User` bot profile.
2. **Local Load Intercept**: Intercept `MessagesController.loadMessagesInternal` for the virtual ID, loading them from local JSON (`gomin_ai_history.json`) and notifying the system using a custom `messagesDidLoad` notification instead of requesting server data.
3. **Send Intercept**: Intercept `SendMessagesHelper.sendMessage` inside the sending pipeline. Save user inputs locally in JSON, refresh the native chat adapter instantly, and launch an asynchronous thread targeting the Gemini SDK while showing standard "typing..." status.

### Pre-Seeded Context AI Redirection
When bridging analysis tools (such as the Gomin Shield Manipulation Analyzer) and chat interfaces, utilize a global thread-safe state variable to carry over structured context (e.g., analyzed transcript and system instructions) directly into the first turns of a new local virtual conversation session:
1. Extract transcript messages (up to 1000) from the target peer.
2. Launch a background task utilizing the Gemini Generative SDK to run analysis.
3. Upon analysis completion, user clicks "💬 Почати чат про це". Store the transcript history and the generated analysis in `activeShieldContext` / `activeShieldHistory`.
4. Launch `ChatActivity` with `Constants.GOMIN_AI_DIALOG_ID`. On startup, if pre-seeded context is present, feed it as the initial invisible chat turns to the model, ensuring the AI assistant is fully briefed on the conversation before the user types a single word.

### Premium Outline Icon Customization Pattern (Vector Graphics)
When adding custom action items or floating buttons for custom feature sets (e.g. AI systems), always construct or reference premium, thin-bordered (typically `strokeWidth="1.8"` or `2.0`) outline icons (such as Lucide design guidelines) inside `res-solar/drawable` or `res-cherrygram/drawable`. This maintains visual consistency with premium custom typography and OLED contrast themes. Never use thick, saturated default images.

### Gemini WebSocket Live API Casing & Synchronization Pattern
When building custom real-time audio/video communication features with Gemini Multimodal Live API via raw WebSockets:
1. **Input Payloads (Casing)**: Outgoing messages (sent by the client), such as the initial `setup` or the interruption `client_content`, MUST use strict **snake_case** keys (`generation_config`, `response_modalities`, `speech_config`, `voice_config`, `prebuilt_voice_config`, `voice_name`, `google_search`, `function_declarations`, `client_content`, `turn_complete`).
2. **Output Payloads (Casing)**: Incoming messages (sent by the server), such as server content or tool calls, arrive in **camelCase** (`serverContent`, `toolCall`, `functionCalls`).
3. **Teardown Mutex**: Always wrap session termination sequence in a thread-safe `synchronized` lock check (`if (!isSessionActive) return; isSessionActive = false`) to ensure system audio resources (`AudioRecord`, `AudioTrack`) and callback handlers are released exactly once, avoiding duplicate postings to the UI main thread and race crashes.
4. **Playback Loop CPU Protection**: Before polling or feeding PCM blocks to `AudioTrack`, always explicitly assert that the track initialized successfully (`AudioTrack.STATE_INITIALIZED`). If initialization fails, terminate the playback thread and set active flags to false to prevent infinite loops and 100% CPU drain.
