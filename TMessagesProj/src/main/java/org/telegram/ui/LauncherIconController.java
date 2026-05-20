package org.telegram.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.R;

public class LauncherIconController {
    public static void tryFixLauncherIconIfNeeded() {
        try {
            for (LauncherIcon icon : LauncherIcon.values()) {
                if (isEnabled(icon)) {
                    return;
                }
            }
            setIcon(LauncherIcon.GOMIN);
        } catch (Throwable e) {
            // Safe fallback
        }
    }

    public static boolean isEnabled(LauncherIcon icon) {
        try {
            Context ctx = ApplicationLoader.applicationContext;
            int i = ctx.getPackageManager().getComponentEnabledSetting(icon.getComponentName(ctx));
            return i == PackageManager.COMPONENT_ENABLED_STATE_ENABLED || i == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && icon == LauncherIcon.GOMIN;
        } catch (Throwable e) {
            return icon == LauncherIcon.GOMIN;
        }
    }

    public static void setIcon(LauncherIcon icon) {
        Context ctx = ApplicationLoader.applicationContext;
        PackageManager pm = ctx.getPackageManager();
        for (LauncherIcon i : LauncherIcon.values()) {
            try {
                pm.setComponentEnabledSetting(i.getComponentName(ctx), i == icon ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED :
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
            } catch (Throwable e) {
                // Ignore missing/unregistered components in Standalone build
            }
        }
    }

    public enum LauncherIcon {
        GOMIN("Gomin_Icon_Main", R.mipmap.icon_launcher_cherry, 0, R.string.Default),
        DARK_CHERRY("CG_Icon_Dark", R.drawable.icon_background_dark, R.drawable.icon_foreground_gomin, R.string.AP_ChangeIcon_Dark),
        WHITE_CHERRY("CG_Icon_White_Cherry", R.drawable.icon_background_white, R.drawable.icon_foreground_gomin, R.string.AP_ChangeIcon_White),
        LAGUNA_CHERRY("CG_Icon_Laguna", R.drawable.icon_background_laguna, R.drawable.icon_foreground_gomin, R.string.AP_ChangeIcon_Laguna),
        AQUA_CHERRY("CG_Icon_Aqua", R.drawable.icon_background_aqua, R.drawable.icon_foreground_gomin, R.string.AppIconAqua),
        GREEN_CHERRY("CG_Icon_Green", R.drawable.icon_background_green, R.drawable.icon_foreground_gomin, R.string.AP_ChangeIcon_Green),
        LAVANDA_CHERRY("CG_Icon_Lavanda", R.drawable.icon_background_lavanda, R.drawable.icon_foreground_gomin, R.string.AP_ChangeIcon_Lavanda),
        VIOLET_SUNSET_CHERRY("CG_Icon_Violet_Sunset", R.drawable.icon_background_violet_sunset, R.drawable.icon_foreground_gomin, R.string.AP_ChangeIcon_Violet_Sunset),
        SUNSET_CHERRY("CG_Icon_Sunset", R.drawable.icon_background_sunset, R.drawable.icon_foreground_gomin, R.string.AP_ChangeIcon_Sunset),
        SUNRISE_CHERRY("CG_Icon_Sunrise", R.drawable.icon_background_sunrise, R.drawable.icon_foreground_gomin, R.string.AP_ChangeIcon_Sunrise),
        TURBO_CHERRY("CG_Icon_Turbo", R.drawable.icon_5_background_sa, R.drawable.icon_foreground_gomin, R.string.AppIconTurbo),
        NOX_CHERRY("CG_Icon_Night", R.mipmap.icon_2_background_sa, R.drawable.icon_foreground_gomin, R.string.AppIconNox),
        DARK_NY("CG_Icon_Dark_NY", R.drawable.icon_background_dark_ny, R.drawable.icon_foreground_gomin, R.string.AP_ChangeIcon_Cherry_NY),

        PREMIUM("PremiumIcon", R.drawable.icon_3_background_sa, R.mipmap.icon_3_foreground, R.string.AppIconPremium, true),
        TURBO("TurboIcon", R.drawable.icon_5_background_sa, R.mipmap.icon_5_foreground, R.string.AppIconTurbo, true),
        NOX("NoxIcon", R.mipmap.icon_2_background_sa, R.drawable.icon_foreground, R.string.AppIconNox, true);

        public final String key;
        public final int background;
        public final int foreground;
        public final int title;
        public final boolean premium;

        private ComponentName componentName;

        public ComponentName getComponentName(Context ctx) {
            if (componentName == null) {
                componentName = new ComponentName(ctx.getPackageName(), ctx.getPackageName() + "." + key);
            }
            return componentName;
        }

        LauncherIcon(String key, int background, int foreground, int title) {
            this(key, background, foreground, title, false);
        }

        LauncherIcon(String key, int background, int foreground, int title, boolean premium) {
            this.key = key;
            this.background = background;
            this.foreground = foreground;
            this.title = title;
            this.premium = premium;
        }
    }
}
