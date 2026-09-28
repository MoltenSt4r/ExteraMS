package org.telegram.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.R;

public class LauncherIconController {
    public static void tryFixLauncherIconIfNeeded() {
        for (LauncherIcon icon : LauncherIcon.values()) {
            if (isEnabled(icon)) {
                return;
            }
        }

        setIcon(LauncherIcon.EXTERAMS_MD3);
    }

    public static boolean isEnabled(LauncherIcon icon) {
        Context ctx = ApplicationLoader.applicationContext;
        int i = ctx.getPackageManager().getComponentEnabledSetting(icon.getComponentName(ctx));
        // Пока пользователь ничего не выбирал, включённой считается наша иконка:
        // именно она стоит у <application> в манифесте, и переключатель должен
        // показывать выбранным то, что человек видит на рабочем столе.
        return i == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                || i == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && icon == LauncherIcon.EXTERAMS_MD3;
    }

    public static void setIcon(LauncherIcon icon) {
        Context ctx = ApplicationLoader.applicationContext;
        PackageManager pm = ctx.getPackageManager();
        for (LauncherIcon i : LauncherIcon.values()) {
            pm.setComponentEnabledSetting(i.getComponentName(ctx), i == icon ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED :
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        }
    }

    public enum IconGroup {
        EXTERAMS,
        TELEGRAM
    }

    public enum LauncherIcon {
        // ExteraMS
        EXTERAMS_MD3("ExteraMSMD3Icon", R.drawable.exterams_md3_icon_background,
                R.drawable.exterams_md3_icon_foreground, R.string.AppIconExteraMSMD3, IconGroup.EXTERAMS),
        EXTERAMS_DOTTED("ExteraMSDottedIcon", R.drawable.exterams_dotted_icon_background,
                R.drawable.exterams_dotted_icon_foreground, R.string.AppIconExteraMSDotted, IconGroup.EXTERAMS),
        MOLTENSTAR("MoltenStarIcon", R.drawable.moltenstar_icon_background,
                R.drawable.moltenstar_icon_foreground, R.string.AppIconMoltenStar, IconGroup.EXTERAMS),
        MOLTENSTAR_DOTTED("MoltenStarDottedIcon", R.drawable.moltenstar_dotted_icon_background,
                R.drawable.moltenstar_dotted_icon_foreground, R.string.AppIconMoltenStarDotted, IconGroup.EXTERAMS),

        // Telegram
        TELEGRAM("TelegramIcon", R.drawable.icon_background_sa, R.mipmap.icon_foreground_sa, R.string.AppIconTelegramOriginal, IconGroup.TELEGRAM),
        VINTAGE("VintageIcon", R.drawable.icon_6_background_sa, R.mipmap.icon_6_foreground_sa, R.string.AppIconVintage, IconGroup.TELEGRAM),
        AQUA("AquaIcon", R.drawable.icon_4_background_sa, R.mipmap.icon_foreground_sa, R.string.AppIconAqua, IconGroup.TELEGRAM),
        PREMIUM("PremiumIcon", R.drawable.icon_3_background_sa, R.mipmap.icon_3_foreground_sa, R.string.AppIconPremium, IconGroup.TELEGRAM),
        TURBO("TurboIcon", R.drawable.icon_5_background_sa, R.mipmap.icon_5_foreground_sa, R.string.AppIconTurbo, IconGroup.TELEGRAM),
        NOX("NoxIcon", R.mipmap.icon_2_background_sa, R.mipmap.icon_foreground_sa, R.string.AppIconNox, IconGroup.TELEGRAM);

        public final String key;
        public final int background;
        public final int foreground;
        public final int title;
        public final boolean premium;
        public final IconGroup group;

        private ComponentName componentName;

        public ComponentName getComponentName(Context ctx) {
            if (componentName == null) {
                componentName = new ComponentName(ctx.getPackageName(), "org.telegram.messenger." + key);
            }
            return componentName;
        }

        LauncherIcon(String key, int background, int foreground, int title, IconGroup group) {
            this(key, background, foreground, title, false, group);
        }

        LauncherIcon(String key, int background, int foreground, int title, boolean premium, IconGroup group) {
            this.key = key;
            this.background = background;
            this.foreground = foreground;
            this.title = title;
            this.premium = premium;
            this.group = group;
        }

    }
}
