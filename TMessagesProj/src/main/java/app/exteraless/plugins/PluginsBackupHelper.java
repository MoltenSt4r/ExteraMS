package app.exteraless.plugins;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.BulletinFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Резервное копирование и восстановление всех плагинов ExteraMS.
 * Упаковывает все установленные плагины, их файлы, разрешения, статус
 * и настройки SharedPreferences в переносимый ZIP-архив (.exteraplugins / .zip).
 */
public final class PluginsBackupHelper {

    private static final String MANIFEST_FILE = "manifest.json";

    private PluginsBackupHelper() {
    }

    public static void createBackup(BaseFragment fragment) {
        if (fragment == null || fragment.getParentActivity() == null) {
            return;
        }
        Activity activity = fragment.getParentActivity();
        PluginsController controller = PluginsController.getInstance();
        List<Plugin> plugins = controller.getPluginsSnapshot();
        if (plugins.isEmpty()) {
            BulletinFactory.of(fragment)
                    .createSimpleBulletin(R.raw.info, LocaleController.getString(R.string.PluginsBackupNoPlugins))
                    .show();
            return;
        }

        AlertDialog progress = new AlertDialog(activity, AlertDialog.ALERT_TYPE_SPINNER);
        progress.setMessage(LocaleController.getString(R.string.PluginsBackupCreating));
        progress.setCanCancel(false);
        progress.show();

        Utilities.globalQueue.postRunnable(() -> {
            File backupFile = null;
            try {
                String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
                File dir = new File(activity.getCacheDir(), "backups");
                dir.mkdirs();
                backupFile = new File(dir, "exterams_plugins_" + timestamp + ".exteraplugins");

                JSONObject manifest = new JSONObject();
                manifest.put("version", 1);
                manifest.put("app", "ExteraMS");
                manifest.put("timestamp", System.currentTimeMillis());

                JSONArray pluginsArr = new JSONArray();

                try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(backupFile)))) {
                    for (Plugin p : plugins) {
                        if (p.path == null) {
                            continue;
                        }
                        File src = new File(p.path);
                        if (!src.exists()) {
                            continue;
                        }

                        JSONObject pObj = new JSONObject();
                        pObj.put("id", p.id);
                        pObj.put("name", p.getDisplayName());
                        pObj.put("version", p.version);
                        pObj.put("enabled", p.enabled);
                        pObj.put("pinned", controller.isPluginPinned(p.id));
                        pObj.put("level", PluginTrustLevel.getLevel(p.id));

                        JSONArray perms = new JSONArray();
                        List<String> granted = PluginPermissions.getStored(p.id);
                        if (granted == null) {
                            granted = PluginPermissions.getEffective(p.id);
                        }
                        if (granted != null) {
                            for (String perm : granted) {
                                perms.put(perm);
                            }
                        }
                        pObj.put("permissions", perms);

                        Map<String, ?> settings = controller.getPluginSettingsPreferences(p.id);
                        if (settings != null && !settings.isEmpty()) {
                            JSONObject settingsObj = new JSONObject();
                            for (Map.Entry<String, ?> entry : settings.entrySet()) {
                                settingsObj.put(entry.getKey(), entry.getValue());
                            }
                            pObj.put("settings", settingsObj);
                        }

                        String zipEntryName = "files/" + src.getName();
                        pObj.put("fileName", src.getName());
                        pluginsArr.put(pObj);

                        zos.putNextEntry(new ZipEntry(zipEntryName));
                        try (FileInputStream fis = new FileInputStream(src)) {
                            byte[] buf = new byte[8192];
                            int read;
                            while ((read = fis.read(buf)) > 0) {
                                zos.write(buf, 0, read);
                            }
                        }
                        zos.closeEntry();
                    }

                    manifest.put("plugins", pluginsArr);

                    zos.putNextEntry(new ZipEntry(MANIFEST_FILE));
                    zos.write(manifest.toString(2).getBytes(StandardCharsets.UTF_8));
                    zos.closeEntry();
                }

                final File resultFile = backupFile;
                AndroidUtilities.runOnUIThread(() -> {
                    try {
                        progress.dismiss();
                    } catch (Throwable ignored) {
                    }
                    shareBackupFile(activity, resultFile);
                });
            } catch (Throwable t) {
                FileLog.e("PluginsBackupHelper: backup creation failed", t);
                AndroidUtilities.runOnUIThread(() -> {
                    try {
                        progress.dismiss();
                    } catch (Throwable ignored) {
                    }
                    Toast.makeText(activity, LocaleController.getString(R.string.PluginsInstallError), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private static void shareBackupFile(Activity activity, File backupFile) {
        if (activity == null || backupFile == null || !backupFile.exists()) {
            return;
        }
        try {
            Uri uri = FileProvider.getUriForFile(activity,
                    ApplicationLoader.getApplicationId() + ".provider", backupFile);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/octet-stream");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            activity.startActivity(Intent.createChooser(intent,
                    LocaleController.getString(R.string.PluginsBackupShareTitle)));
        } catch (Throwable t) {
            FileLog.e("PluginsBackupHelper: share failed", t);
        }
    }

    public static void restoreBackup(BaseFragment fragment, Uri uri, Runnable onComplete) {
        if (fragment == null || fragment.getParentActivity() == null || uri == null) {
            return;
        }
        Activity activity = fragment.getParentActivity();
        AlertDialog progress = new AlertDialog(activity, AlertDialog.ALERT_TYPE_SPINNER);
        progress.setMessage(LocaleController.getString(R.string.PluginsBackupRestoring));
        progress.setCanCancel(false);
        progress.show();

        Utilities.globalQueue.postRunnable(() -> {
            try {
                File tempZip = new File(activity.getCacheDir(), "restore_tmp.zip");
                try (InputStream in = activity.getContentResolver().openInputStream(uri);
                     FileOutputStream out = new FileOutputStream(tempZip)) {
                    if (in == null) {
                        throw new RuntimeException("Cannot open uri stream");
                    }
                    byte[] buf = new byte[8192];
                    int r;
                    while ((r = in.read(buf)) > 0) {
                        out.write(buf, 0, r);
                    }
                }

                File extractDir = new File(activity.getCacheDir(), "restore_" + System.currentTimeMillis());
                extractDir.mkdirs();

                String manifestJson = null;
                try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(tempZip)))) {
                    ZipEntry entry;
                    while ((entry = zis.getNextEntry()) != null) {
                        String name = entry.getName();
                        if (MANIFEST_FILE.equals(name)) {
                            StringBuilder sb = new StringBuilder();
                            BufferedReader reader = new BufferedReader(new InputStreamReader(zis, StandardCharsets.UTF_8));
                            String line;
                            while ((line = reader.readLine()) != null) {
                                sb.append(line).append('\n');
                            }
                            manifestJson = sb.toString();
                        } else if (name.startsWith("files/")) {
                            File outFile = new File(extractDir, name.substring("files/".length()));
                            outFile.getParentFile().mkdirs();
                            try (FileOutputStream fos = new FileOutputStream(outFile)) {
                                byte[] buf = new byte[8192];
                                int len;
                                while ((len = zis.read(buf)) > 0) {
                                    fos.write(buf, 0, len);
                                }
                            }
                        }
                        zis.closeEntry();
                    }
                }

                if (manifestJson == null) {
                    throw new RuntimeException("Manifest not found in backup");
                }

                JSONObject manifest = new JSONObject(manifestJson);
                JSONArray pluginsArr = manifest.optJSONArray("plugins");
                if (pluginsArr == null || pluginsArr.length() == 0) {
                    throw new RuntimeException("No plugins in backup manifest");
                }

                final int count = pluginsArr.length();
                AndroidUtilities.runOnUIThread(() -> {
                    try {
                        progress.dismiss();
                    } catch (Throwable ignored) {
                    }
                    new AlertDialog.Builder(activity)
                            .setTitle(LocaleController.getString(R.string.PluginsBackupTitle))
                            .setMessage(LocaleController.formatString(R.string.PluginsBackupRestoreConfirm, count))
                            .setPositiveButton(LocaleController.getString(R.string.PluginsBackupRestore), (dialog, which) -> {
                                performInstallRestoredPlugins(fragment, pluginsArr, extractDir, onComplete);
                            })
                            .setNegativeButton(LocaleController.getString(R.string.Cancel), null)
                            .show();
                });
            } catch (Throwable t) {
                FileLog.e("PluginsBackupHelper: restore inspection failed", t);
                AndroidUtilities.runOnUIThread(() -> {
                    try {
                        progress.dismiss();
                    } catch (Throwable ignored) {
                    }
                    Toast.makeText(activity, LocaleController.getString(R.string.PluginsBackupInvalid), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private static void performInstallRestoredPlugins(BaseFragment fragment, JSONArray pluginsArr,
                                                      File extractDir, Runnable onComplete) {
        if (fragment == null || fragment.getParentActivity() == null) {
            return;
        }
        Activity activity = fragment.getParentActivity();
        AlertDialog progress = new AlertDialog(activity, AlertDialog.ALERT_TYPE_SPINNER);
        progress.setMessage(LocaleController.getString(R.string.PluginsBackupRestoring));
        progress.setCanCancel(false);
        progress.show();

        Utilities.globalQueue.postRunnable(() -> {
            PluginsController controller = PluginsController.getInstance();
            int restoredCount = 0;
            for (int i = 0; i < pluginsArr.length(); i++) {
                try {
                    JSONObject pObj = pluginsArr.getJSONObject(i);
                    String id = pObj.optString("id");
                    String fileName = pObj.optString("fileName");
                    boolean enabled = pObj.optBoolean("enabled", true);
                    boolean pinned = pObj.optBoolean("pinned", false);
                    int level = pObj.optInt("level", PluginTrustLevel.GATED);

                    File file = new File(extractDir, fileName);
                    if (!file.exists()) {
                        continue;
                    }

                    // Копируем файл плагина напрямую в pluginsDir
                    File dest = new File(controller.getPluginsDir(), file.getName());
                    try (FileInputStream in = new FileInputStream(file);
                         FileOutputStream out = new FileOutputStream(dest)) {
                        byte[] buffer = new byte[8192];
                        int r;
                        while ((r = in.read(buffer)) > 0) {
                            out.write(buffer, 0, r);
                        }
                    }

                    // Восстанавливаем разрешения
                    JSONArray permsArr = pObj.optJSONArray("permissions");
                    List<String> perms = new ArrayList<>();
                    if (permsArr != null) {
                        for (int j = 0; j < permsArr.length(); j++) {
                            perms.add(permsArr.optString(j));
                        }
                    }
                    PluginPermissions.setGranted(id, perms);
                    PluginTrustLevel.setLevel(id, level);
                    controller.setPluginPinned(id, pinned);

                    // Восстанавливаем настройки плагина
                    JSONObject settings = pObj.optJSONObject("settings");
                    if (settings != null) {
                        for (Iterator<String> it = settings.keys(); it.hasNext(); ) {
                            String k = it.next();
                            Object val = settings.opt(k);
                            controller.setPluginSetting(id, k, val);
                        }
                    }

                    if (controller.getPreferences() != null) {
                        controller.getPreferences().edit()
                                .putBoolean(PluginsConstants.KEY_PLUGIN_ENABLED_PREFIX + id, enabled)
                                .apply();
                    }

                    restoredCount++;
                } catch (Throwable t) {
                    FileLog.e("PluginsBackupHelper: error restoring plugin", t);
                }
            }

            final int finalRestored = restoredCount;
            AndroidUtilities.runOnUIThread(() -> {
                try {
                    progress.dismiss();
                } catch (Throwable ignored) {
                }
                controller.rescanPlugins(() -> {
                    if (fragment.getParentActivity() != null) {
                        BulletinFactory.of(fragment)
                                .createSimpleBulletin(R.raw.contact_check,
                                        LocaleController.formatString(R.string.PluginsBackupRestored, finalRestored))
                                .show();
                    }
                    if (onComplete != null) {
                        onComplete.run();
                    }
                });
            });
        });
    }
}
