package app.exteraless.plugins;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.Bulletin;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.LaunchActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import app.exteraless.plugins.ui.PluginPermissionsActivity;
import app.exteraless.plugins.ui.PluginsActivity;

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
    private static final Set<String> OVERLAY_SHOWN = ConcurrentHashMap.newKeySet();
    private static final Queue<String> PENDING = new ConcurrentLinkedQueue<>();
    private static final Set<String> UNCONSENTED = ConcurrentHashMap.newKeySet();
    private static final Runnable SHOW_UNCONSENTED = PluginDenialNotice::showUnconsented;
    private static final NotificationCenter.NotificationCenterDelegate AFTER_PASSCODE = new NotificationCenter.NotificationCenterDelegate() {
        @Override
        public void didReceivedNotification(int id, int account, Object... args) {
            NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.passcodeDismissed);
            scheduleUnconsented();
        }
    };
    private static final int FLUSH_DELAY = 700;
    private static final int UNCONSENTED_DELAY = 3000;
    private static final int UNCONSENTED_NAMES = 3;

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

    public static void noteOverlay(String pluginId) {
        if (TextUtils.isEmpty(pluginId) || Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }
        AndroidUtilities.runOnUIThread(() -> showOverlay(pluginId));
    }

    private static void showOverlay(String pluginId) {
        BaseFragment fragment = LaunchActivity.getSafeLastFragment();
        Activity activity = fragment != null ? fragment.getParentActivity() : null;
        if (activity == null || Settings.canDrawOverlays(activity) || !OVERLAY_SHOWN.add(pluginId)) {
            return;
        }
        Plugin plugin = PluginsController.getInstance().getPlugin(pluginId);
        String name = plugin != null ? plugin.getDisplayName() : pluginId;
        fragment.showDialog(new AlertDialog.Builder(activity, fragment.getResourceProvider())
                .setTitle(LocaleController.getString(R.string.PluginOverlayTitle))
                .setMessage(LocaleController.formatString(R.string.PluginOverlayText, name))
                .setPositiveButton(LocaleController.getString(R.string.PluginOverlayOpen), (d, w) -> {
                    try {
                        activity.startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:" + activity.getPackageName())));
                    } catch (Exception e) {
                        FileLog.e(e);
                    }
                })
                .setNegativeButton(LocaleController.getString(R.string.Cancel), null)
                .create());
    }

    public static void noteUnconsented(String pluginId) {
        if (TextUtils.isEmpty(pluginId) || !UNCONSENTED.add(pluginId)) {
            return;
        }
        scheduleUnconsented();
    }

    private static void scheduleUnconsented() {
        AndroidUtilities.cancelRunOnUIThread(SHOW_UNCONSENTED);
        AndroidUtilities.runOnUIThread(SHOW_UNCONSENTED, UNCONSENTED_DELAY);
    }

    private static void showUnconsented() {
        if (UNCONSENTED.isEmpty()) {
            return;
        }
        BaseFragment fragment = LaunchActivity.getSafeLastFragment();
        if (fragment == null || fragment.getParentActivity() == null) {
            return;
        }
        if (SharedConfig.isWaitingForPasscodeEnter) {
            NotificationCenter.getGlobalInstance().addObserver(AFTER_PASSCODE, NotificationCenter.passcodeDismissed);
            return;
        }
        List<String> ids = new ArrayList<>(UNCONSENTED);
        UNCONSENTED.removeAll(ids);
        PluginsController controller = PluginsController.getInstance();
        List<String> present = new ArrayList<>();
        List<String> names = new ArrayList<>();
        for (String id : ids) {
            Plugin plugin = controller.getPlugin(id);
            if (plugin == null) {
                continue;
            }
            present.add(id);
            if (names.size() < UNCONSENTED_NAMES) {
                names.add(plugin.getDisplayName());
            }
        }
        if (present.isEmpty()) {
            return;
        }
        CharSequence list = TextUtils.join(", ", names);
        if (present.size() > names.size()) {
            list = LocaleController.formatString(R.string.PluginsUnconsentedMore, list, present.size() - names.size());
        }
        final String single = present.size() == 1 ? present.get(0) : null;
        BulletinFactory.of(fragment)
                .createSimpleBulletin(R.raw.info,
                        LocaleController.getString(single != null ? R.string.PluginUnconsentedTitle : R.string.PluginsUnconsentedTitle),
                        list,
                        LocaleController.getString(R.string.PluginsUnconsentedAction),
                        () -> fragment.presentFragment(single != null
                                ? new PluginPermissionsActivity(single)
                                : new PluginsActivity()))
                .setDuration(Bulletin.DURATION_PROLONG)
                .show();
    }

    public static void flush() {
        if (!UNCONSENTED.isEmpty()) {
            scheduleUnconsented();
        }
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
        OVERLAY_SHOWN.remove(pluginId);
        PENDING.removeIf(mark -> mark.startsWith(pluginId + "|"));
    }
}
