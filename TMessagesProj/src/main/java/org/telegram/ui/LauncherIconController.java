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

        setIcon(LauncherIcon.MOLTENGRAM);
    }

    public static boolean isEnabled(LauncherIcon icon) {
        Context ctx = ApplicationLoader.applicationContext;
        int i = ctx.getPackageManager().getComponentEnabledSetting(icon.getComponentName(ctx));
        // Пока пользователь ничего не выбирал, включённой считается наша иконка:
        // именно она стоит у <application> в манифесте, и переключатель должен
        // показывать выбранным то, что человек видит на рабочем столе.
        return i == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                || i == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && icon == LauncherIcon.MOLTENGRAM;
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
        return LauncherIcon.MOLTENGRAM;
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
        MOLTENGRAM,
        TELEGRAM
    }

    public enum LauncherIcon {
        // MoltenGram
        MOLTENGRAM("MoltenGramIcon", R.drawable.moltengram_icon_background,
                R.drawable.moltengram_icon_foreground, R.string.AppIconMoltenGramDefault, IconGroup.MOLTENGRAM, "@moltenst4r", "Signature obsidian silhouette", 0xFF601816),
        MOLTENGRAM_DOTTED("MoltenGramDottedIcon", R.drawable.moltengram_dotted_icon_background,
                0, R.string.AppIconMoltenGramDotted, IconGroup.MOLTENGRAM, "@moltenst4r", "Nothing Glyph dot-matrix", 0xFF381826),
        MOLTENGRAM_NEON("MoltenGramNeonIcon", R.drawable.moltengram_neon_icon_background,
                0, R.string.AppIconMoltenGramNeon, IconGroup.MOLTENGRAM, "@moltenst4r", "Layered translucent neon glass", 0xFF5D0597),
        MOLTENGRAM_LAVA("MoltenGramLavaIcon", R.drawable.moltengram_lava_icon_background,
                0, R.string.AppIconMoltenGramLava, IconGroup.MOLTENGRAM, "@moltenst4r", "Glowing molten magma core", 0xFF8A3C14),
        MOLTENGRAM_AERO("MoltenGramAeroIcon", R.drawable.moltengram_aero_icon_background,
                0, R.string.AppIconMoltenGramAero, IconGroup.MOLTENGRAM, "@moltenst4r", "Frutiger Aero 3D glass aesthetic", 0xFF1B6A56),
        MOLTENGRAM_PIXEL("MoltenGramPixelIcon", R.drawable.moltengram_pixel_icon_background,
                0, R.string.AppIconMoltenGramPixel, IconGroup.MOLTENGRAM, "@moltenst4r", "Retro 8-bit cyber art", 0xFF20465E),
        MOLTENGRAM_MINT("MoltenGramMintIcon", R.drawable.moltengram_mint_icon_background,
                0, R.string.AppIconMoltenGramMint, IconGroup.MOLTENGRAM, "@moltenst4r", "Organic pastel waves", 0xFF2E6B4A),
        MOLTENGRAM_MOCHA("MoltenGramMochaIcon", R.drawable.moltengram_mocha_icon_background,
                0, R.string.AppIconMoltenGramMocha, IconGroup.MOLTENGRAM, "@moltenst4r", "Warm marble mocha coffee", 0xFF4A3428),
        MOLTENGRAM_SUNSET("MoltenGramSunsetIcon", R.drawable.moltengram_sunset_icon_background,
                0, R.string.AppIconMoltenGramSunset, IconGroup.MOLTENGRAM, "@moltenst4r", "Soft twilight sunset glow", 0xFF6D2F3C),
        MOLTENGRAM_AZURE("MoltenGramAzureIcon", R.drawable.moltengram_azure_icon_background,
                0, R.string.AppIconMoltenGramAzure, IconGroup.MOLTENGRAM, "@moltenst4r", "Vibrant electric cyan-blue gradient", 0xFF007A9E),

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
