# Gomin Architectural Patterns

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
