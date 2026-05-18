# Gomin Architectural Patterns

### Dynamic Launcher Icons
To ensure launcher icons work across different packages (e.g., when rebranding), always use `ctx.getPackageName()` instead of hardcoded strings in `LauncherIconController`:
```java
public ComponentName getComponentName(Context ctx) {
    if (componentName == null) {
        componentName = new ComponentName(ctx.getPackageName(), ctx.getPackageName() + "." + key);
    }
    return componentName;
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
Custom branding should be placed in `src/main/res-cherrygram/` (or equivalent) to override standard resources without modifying the base `res/` directory. This allows for cleaner merges with upstream Telegram updates.
