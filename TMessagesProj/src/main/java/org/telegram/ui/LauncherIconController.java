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

    public static LauncherIcon getCurrentIcon() {
        for (LauncherIcon icon : LauncherIcon.values()) {
            if (isEnabled(icon)) {
                return icon;
            }
        }
        return LauncherIcon.EXTERAMS_MD3;
    }

    public static android.graphics.drawable.Drawable createIconPreviewDrawable(Context context, LauncherIcon icon, int sizeDp) {
        if (icon == null || context == null) return null;
        int size = org.telegram.messenger.AndroidUtilities.dp(sizeDp);
        android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
        android.graphics.Path path = new android.graphics.Path();
        path.addCircle(size / 2f, size / 2f, size / 2f, android.graphics.Path.Direction.CW);
        canvas.clipPath(path);
        android.graphics.drawable.Drawable bg = androidx.core.content.ContextCompat.getDrawable(context, icon.background);
        if (bg != null) {
            bg.setBounds(0, 0, size, size);
            bg.draw(canvas);
        }
        if (icon.foreground != 0) {
            android.graphics.drawable.Drawable fg = androidx.core.content.ContextCompat.getDrawable(context, icon.foreground);
            if (fg != null) {
                int pad = org.telegram.messenger.AndroidUtilities.dp(sizeDp * 0.1f);
                fg.setBounds(-pad, -pad, size + pad, size + pad);
                fg.draw(canvas);
            }
        }
        return new android.graphics.drawable.BitmapDrawable(context.getResources(), bitmap);
    }

    public enum IconGroup {
        EXTERAMS,
        TELEGRAM
    }

    public enum LauncherIcon {
        // ExteraMS
        EXTERAMS_MD3("ExteraMSMD3Icon", R.drawable.exterams_md3_icon_background,
                R.drawable.exterams_md3_icon_foreground, R.string.AppIconExteraMSMD3, IconGroup.EXTERAMS, "It just works"),
        EXTERAMS_DOTTED("ExteraMSDottedIcon", R.drawable.exterams_dotted_icon_background,
                R.drawable.exterams_dotted_icon_foreground, R.string.AppIconExteraMSDotted, IconGroup.EXTERAMS, "@exteraGram"),
        EXTERAMS_GOOGLE("ExteraMSGoogleIcon", R.drawable.exterams_google_icon_background,
                R.drawable.exterams_google_icon_foreground, R.string.AppIconExteraMSGoogle, IconGroup.EXTERAMS, "@moltenst4r"),
        MOLTENSTAR("MoltenStarIcon", R.drawable.moltenstar_icon_background,
                R.drawable.moltenstar_icon_foreground, R.string.AppIconMoltenStar, IconGroup.EXTERAMS, "@moltenst4r"),
        MOLTENSTAR_DOTTED("MoltenStarDottedIcon", R.drawable.moltenstar_dotted_icon_background,
                R.drawable.moltenstar_dotted_icon_foreground, R.string.AppIconMoltenStarDotted, IconGroup.EXTERAMS, "@moltenst4r"),
        MOLTENSTAR_GOOGLE("MoltenStarGoogleIcon", R.drawable.moltenstar_google_icon_background,
                R.drawable.moltenstar_google_icon_foreground, R.string.AppIconMoltenStarGoogle, IconGroup.EXTERAMS, "@moltenst4r"),

        // Telegram
        TELEGRAM("TelegramIcon", R.drawable.icon_background_sa, R.mipmap.icon_foreground_sa, R.string.AppIconTelegramOriginal, IconGroup.TELEGRAM, "@telegram"),
        VINTAGE("VintageIcon", R.drawable.icon_6_background_sa, R.mipmap.icon_6_foreground_sa, R.string.AppIconVintage, IconGroup.TELEGRAM, "@telegram"),
        AQUA("AquaIcon", R.drawable.icon_4_background_sa, R.mipmap.icon_foreground_sa, R.string.AppIconAqua, IconGroup.TELEGRAM, "@telegram"),
        PREMIUM("PremiumIcon", R.drawable.icon_3_background_sa, R.mipmap.icon_3_foreground_sa, R.string.AppIconPremium, IconGroup.TELEGRAM, "@telegram"),
        TURBO("TurboIcon", R.drawable.icon_5_background_sa, R.mipmap.icon_5_foreground_sa, R.string.AppIconTurbo, IconGroup.TELEGRAM, "@telegram"),
        NOX("NoxIcon", R.mipmap.icon_2_background_sa, R.mipmap.icon_foreground_sa, R.string.AppIconNox, IconGroup.TELEGRAM, "@telegram");

        public final String key;
        public final int background;
        public final int foreground;
        public final int title;
        public final boolean premium;
        public final IconGroup group;
        public final String author;

        private ComponentName componentName;

        public ComponentName getComponentName(Context ctx) {
            if (componentName == null) {
                componentName = new ComponentName(ctx.getPackageName(), "org.telegram.messenger." + key);
            }
            return componentName;
        }

        LauncherIcon(String key, int background, int foreground, int title, IconGroup group, String author) {
            this(key, background, foreground, title, false, group, author);
        }

        LauncherIcon(String key, int background, int foreground, int title, boolean premium, IconGroup group, String author) {
            this.key = key;
            this.background = background;
            this.foreground = foreground;
            this.title = title;
            this.premium = premium;
            this.group = group;
            this.author = author;
        }

    }
}
