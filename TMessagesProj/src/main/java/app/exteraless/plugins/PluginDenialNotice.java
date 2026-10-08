package app.exteraless.plugins;

import android.text.TextUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.LaunchActivity;

import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import app.exteraless.plugins.ui.PluginPermissionsActivity;

/**
 * Сообщить пользователю, что плагину чего-то не хватило.
 *
 * Отказ не роняет плагин: команда просто ничего не делает, и со стороны это
 * неотличимо от поломки — «нажал, и тишина». Здесь появляется единственный
 * ответ на вопрос «почему не работает»: короткое сообщение с именем плагина и
 * тем, чего ему не дали.
 *
 * Показываем не чаще одного раза на пару (плагин, разрешение) за запуск: хуки
 * срабатывают часто, и повторять одно и то же на каждый вызов — верный способ
 * сделать сообщение фоновым шумом, который перестают читать.
 */
public final class PluginDenialNotice {

    private static final Set<String> SHOWN = ConcurrentHashMap.newKeySet();
    private static final Queue<String> PENDING = new ConcurrentLinkedQueue<>();
    private static final int FLUSH_DELAY = 700;

    private PluginDenialNotice() {
    }

    public static void note(String pluginId, String permission) {
        if (TextUtils.isEmpty(pluginId) || TextUtils.isEmpty(permission)) {
            return;
        }
        // ui выдан всегда; отказ по нему означал бы, что плагина нет в реестре —
        // сообщать пользователю тут нечего.
        if (PluginPermissions.UI.equals(permission)) {
            return;
        }
        final String mark = pluginId + "|" + permission;
        if (!SHOWN.add(mark)) {
            return;
        }
        AndroidUtilities.runOnUIThread(() -> {
            if (!show(pluginId, permission)) {
                PENDING.add(mark);
            }
        });
    }

    public static void flush() {
        if (PENDING.isEmpty()) {
            return;
        }
        AndroidUtilities.runOnUIThread(() -> {
            String mark = PENDING.peek();
            if (mark == null) {
                return;
            }
            int sep = mark.indexOf('|');
            if (sep <= 0 || sep == mark.length() - 1) {
                PENDING.poll();
                return;
            }
            if (show(mark.substring(0, sep), mark.substring(sep + 1))) {
                PENDING.poll();
            }
        }, FLUSH_DELAY);
    }

    private static boolean show(String pluginId, String permission) {
        BaseFragment fragment = LaunchActivity.getSafeLastFragment();
        if (fragment == null || fragment.getParentActivity() == null) {
            return false;
        }
        Plugin plugin = PluginsController.getInstance().getPlugin(pluginId);
        String name = plugin != null ? plugin.getDisplayName() : pluginId;
        CharSequence text = LocaleController.formatString(R.string.PluginDeniedNotice,
                name, PluginPermissionsActivity.titleOf(permission));
        final boolean grantable = PluginPermissions.isKnown(permission);
        BulletinFactory.of(fragment)
                .createSimpleBulletin(R.raw.error, text,
                        LocaleController.getString(grantable ? R.string.PluginDeniedGrant : R.string.PluginDeniedOpen),
                        () -> {
                            if (grantable) {
                                grant(fragment, pluginId, name, permission);
                            } else {
                                fragment.presentFragment(new PluginPermissionsActivity(pluginId));
                            }
                        })
                .show();
        return true;
    }

    private static void grant(BaseFragment fragment, String pluginId, String name, String permission) {
        if (PluginTrustLevel.allows(pluginId, permission)) {
            applyGrant(fragment, pluginId, name, permission, PluginTrustLevel.getLevel(pluginId));
            return;
        }
        final int level = PluginPermissions.isDangerous(permission) ? PluginTrustLevel.TRUSTED : PluginTrustLevel.GATED;
        if (level != PluginTrustLevel.TRUSTED || fragment.getParentActivity() == null) {
            applyGrant(fragment, pluginId, name, permission, level);
            return;
        }
        new AlertDialog.Builder(fragment.getParentActivity(), fragment.getResourceProvider())
                .setTitle(PluginPermissionsActivity.titleOf(permission))
                .setMessage(LocaleController.formatString(R.string.PluginGrantTrustedConfirm, name))
                .setPositiveButton(LocaleController.getString(R.string.PluginDeniedGrant),
                        (d, w) -> applyGrant(fragment, pluginId, name, permission, level))
                .setNegativeButton(LocaleController.getString(R.string.Cancel), null)
                .show();
    }

    private static void applyGrant(BaseFragment fragment, String pluginId, String name, String permission, int level) {
        if (PluginTrustLevel.getLevel(pluginId) < level) {
            PluginTrustLevel.setLevel(pluginId, level);
        }
        PluginPermissions.grant(pluginId, permission);
        BulletinFactory.of(fragment)
                .createSimpleBulletin(R.raw.contact_check,
                        LocaleController.formatString(R.string.PluginGrantedRestart, name),
                        LocaleController.getString(R.string.PluginGrantedRestartAction),
                        () -> PluginsController.getInstance().reloadPlugin(pluginId))
                .show();
    }

    /** Забыть показанное (удаление плагина, смена разрешений). */
    public static void reset(String pluginId) {
        if (TextUtils.isEmpty(pluginId)) {
            return;
        }
        SHOWN.removeIf(mark -> mark.startsWith(pluginId + "|"));
        PENDING.removeIf(mark -> mark.startsWith(pluginId + "|"));
    }
}
