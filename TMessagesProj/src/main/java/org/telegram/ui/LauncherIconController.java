package org.telegram.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

import org.telegram.messenger.AndroidUtilities;
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
        // Clean restart after 500ms to apply icon change smoothly and avoid abrupt system crash
        AndroidUtilities.runOnUIThread(() -> {
            try {
                uz.unnarsx.cherrygram.core.helpers.AppRestartHelper.restartApp(ctx);
            } catch (Throwable e) {
                System.exit(0);
            }
        }, 500);
    }

    public enum LauncherIcon {
        GOMIN("Gomin_Icon_Main", R.drawable.icon_background_dark, R.drawable.icon_foreground_gomin_dark, R.string.Default),
        DARK_CHERRY("CG_Icon_Dark", R.drawable.icon_background_default, R.drawable.icon_foreground_gomin_default, R.string.AP_ChangeIcon_Classic),
        WHITE_CHERRY("CG_Icon_White_Cherry", R.drawable.icon_background_white, R.drawable.icon_foreground_gomin_white, R.string.AP_ChangeIcon_White),
        AQUA_CHERRY("CG_Icon_Aqua", R.drawable.icon_background_aqua, R.drawable.icon_foreground_gomin_aqua, R.string.AppIconAqua),
        LAVANDA_CHERRY("CG_Icon_Lavanda", R.drawable.icon_background_lavanda, R.drawable.icon_foreground_gomin_lavanda, R.string.AP_ChangeIcon_Lavanda),
        VIOLET_SUNSET_CHERRY("CG_Icon_Violet_Sunset", R.drawable.icon_background_black_red, R.drawable.icon_foreground_gomin_sunset, R.string.AP_ChangeIcon_Violet_Sunset),
        UKRAINE_CHERRY("CG_Icon_Ukraine", R.drawable.icon_background_ukraine, R.drawable.icon_foreground_gomin_yellow, R.string.AP_ChangeIcon_Ukraine),
        PREMIUM("PremiumIcon", R.drawable.icon_3_background_sa, R.mipmap.icon_3_foreground, R.string.AppIconPremium, true);

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
