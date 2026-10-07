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
                R.drawable.exterams_md3_icon_foreground, R.string.AppIconExteraMSMD3, IconGroup.EXTERAMS, "@moltenst4r", "It just works", 0xFF601816),
        EXTERAMS_DOTTED("ExteraMSDottedIcon", R.drawable.exterams_dotted_icon_background,
                R.drawable.exterams_dotted_icon_foreground, R.string.AppIconExteraMSDotted, IconGroup.EXTERAMS, "@exteraGram", "Dotted style", 0xFF381826),
        EXTERAMS_GOOGLE("ExteraMSGoogleIcon", R.drawable.exterams_google_icon_background,
                R.drawable.exterams_google_icon_foreground, R.string.AppIconExteraMSGoogle, IconGroup.EXTERAMS, "@moltenst4r", "Material 3 Expressive", 0xFF183048),
        MOLTENSTAR("MoltenStarIcon", R.drawable.moltenstar_icon_background,
                R.drawable.moltenstar_icon_foreground, R.string.AppIconMoltenStar, IconGroup.EXTERAMS, "@moltenst4r", "Signature star", 0xFF541814),
        MOLTENSTAR_DOTTED("MoltenStarDottedIcon", R.drawable.moltenstar_dotted_icon_background,
                R.drawable.moltenstar_dotted_icon_foreground, R.string.AppIconMoltenStarDotted, IconGroup.EXTERAMS, "@moltenst4r", "Dotted star", 0xFF381820),
        MOLTENSTAR_GOOGLE("MoltenStarGoogleIcon", R.drawable.moltenstar_google_icon_background,
                R.drawable.moltenstar_google_icon_foreground, R.string.AppIconMoltenStarGoogle, IconGroup.EXTERAMS, "@moltenst4r", "Google Pixel star", 0xFF1A3246),

        // Telegram
        TELEGRAM("TelegramIcon", R.drawable.icon_background_sa, R.mipmap.icon_foreground_sa, R.string.AppIconTelegramOriginal, IconGroup.TELEGRAM, "@telegram", "The classic paper plane", 0xFF143854),
        VINTAGE("VintageIcon", R.drawable.icon_6_background_sa, R.mipmap.icon_6_foreground_sa, R.string.AppIconVintage, IconGroup.TELEGRAM, "@telegram", "Retro nostalgic style", 0xFF442A16),
        AQUA("AquaIcon", R.drawable.icon_4_background_sa, R.mipmap.icon_foreground_sa, R.string.AppIconAqua, IconGroup.TELEGRAM, "@telegram", "Deep ocean water drop", 0xFF123C4A),
        PREMIUM("PremiumIcon", R.drawable.icon_3_background_sa, R.mipmap.icon_3_foreground_sa, R.string.AppIconPremium, IconGroup.TELEGRAM, "@telegram", "Telegram Star gradient", 0xFF36184E),
        TURBO("TurboIcon", R.drawable.icon_5_background_sa, R.mipmap.icon_5_foreground_sa, R.string.AppIconTurbo, IconGroup.TELEGRAM, "@telegram", "Speed and propulsion", 0xFF14345C),
        NOX("NoxIcon", R.mipmap.icon_2_background_sa, R.mipmap.icon_foreground_sa, R.string.AppIconNox, IconGroup.TELEGRAM, "@telegram", "Starry night sky", 0xFF161628);

        public final String key;
        public final int background;
        public final int foreground;
        public final int title;
        public final boolean premium;
        public final IconGroup group;
        public final String author;
        public final String description;
        public final int glowColor;

        private ComponentName componentName;

        public ComponentName getComponentName(Context ctx) {
            if (componentName == null) {
                componentName = new ComponentName(ctx.getPackageName(), "org.telegram.messenger." + key);
            }
            return componentName;
        }

        LauncherIcon(String key, int background, int foreground, int title, IconGroup group, String author, String description, int glowColor) {
            this(key, background, foreground, title, false, group, author, description, glowColor);
        }

        LauncherIcon(String key, int background, int foreground, int title, boolean premium, IconGroup group, String author, String description, int glowColor) {
            this.key = key;
            this.background = background;
            this.foreground = foreground;
            this.title = title;
            this.premium = premium;
            this.group = group;
            this.author = author;
            this.description = description;
            this.glowColor = glowColor;
        }

        LauncherIcon(String key, int background, int foreground, int title, IconGroup group, String author) {
            this(key, background, foreground, title, false, group, author, null, 0xFF202020);
        }

        LauncherIcon(String key, int background, int foreground, int title, boolean premium, IconGroup group, String author) {
            this(key, background, foreground, title, premium, group, author, null, 0xFF202020);
        }

    }
}
