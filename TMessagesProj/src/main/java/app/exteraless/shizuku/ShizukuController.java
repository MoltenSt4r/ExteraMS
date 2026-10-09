package app.exteraless.shizuku;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.text.TextUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.browser.Browser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Method;

import rikka.shizuku.Shizuku;

public class ShizukuController {

    private static volatile ShizukuController instance;

    public static final int REQUEST_CODE_SHIZUKU = 9001;

    public static ShizukuController getInstance() {
        if (instance == null) {
            synchronized (ShizukuController.class) {
                if (instance == null) {
                    instance = new ShizukuController();
                }
            }
        }
        return instance;
    }

    private ShizukuController() {
    }

    public boolean isAvailable() {
        try {
            return Shizuku.pingBinder();
        } catch (Throwable t) {
            return false;
        }
    }

    public boolean hasPermission() {
        try {
            if (!isAvailable()) return false;
            return Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
        } catch (Throwable t) {
            return false;
        }
    }

    public void requestPermission(int requestCode) {
        try {
            if (isAvailable()) {
                Shizuku.requestPermission(requestCode);
            }
        } catch (Throwable t) {
            FileLog.e(t);
        }
    }

    public Process exec(String[] cmd) {
        try {
            Method m = Shizuku.class.getDeclaredMethod("newProcess", String[].class, String[].class, String.class);
            m.setAccessible(true);
            return (Process) m.invoke(null, cmd, null, null);
        } catch (Throwable t) {
            FileLog.e(t);
            return null;
        }
    }

    public String runCommand(String cmd) {
        try {
            Process p = exec(new String[]{"sh", "-c", cmd});
            if (p == null) return null;
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            }
            p.waitFor();
            return sb.toString().trim();
        } catch (Throwable t) {
            FileLog.e(t);
            return null;
        }
    }

    public boolean silentInstallApk(File apkFile) {
        if (apkFile == null || !apkFile.exists()) return false;
        try {
            String tmpName = "/data/local/tmp/shizuku_install_" + System.currentTimeMillis() + ".apk";
            String shCmd = "cat > " + tmpName + " && pm install -r -d " + tmpName + "; rm -f " + tmpName;
            Process p = exec(new String[]{"sh", "-c", shCmd});
            if (p == null) return false;

            try (OutputStream os = p.getOutputStream();
                 FileInputStream fis = new FileInputStream(apkFile)) {
                byte[] buf = new byte[65536];
                int read;
                while ((read = fis.read(buf)) != -1) {
                    os.write(buf, 0, read);
                }
                os.flush();
            }

            int exitCode = p.waitFor();
            return exitCode == 0;
        } catch (Throwable t) {
            FileLog.e(t);
            return false;
        }
    }

    public boolean setUnrestrictedBackground() {
        try {
            String pkg = ApplicationLoader.applicationContext.getPackageName();
            runCommand("cmd deviceidle whitelist +" + pkg);
            runCommand("cmd appops set " + pkg + " RUN_IN_BACKGROUND allow");
            runCommand("cmd appops set " + pkg + " START_FOREGROUND allow");
            return true;
        } catch (Throwable t) {
            FileLog.e(t);
            return false;
        }
    }

    public boolean trimCaches() {
        try {
            String res = runCommand("pm trim-caches 999999999999999999");
            return res != null;
        } catch (Throwable t) {
            FileLog.e(t);
            return false;
        }
    }

    public boolean optimizeDex() {
        try {
            String pkg = ApplicationLoader.applicationContext.getPackageName();
            String res = runCommand("cmd package compile -m speed-profile " + pkg);
            return res != null;
        } catch (Throwable t) {
            FileLog.e(t);
            return false;
        }
    }

    public void openShizukuApp(Context context) {
        if (context == null) return;
        try {
            Intent intent = context.getPackageManager().getLaunchIntentForPackage("moe.shizuku.privileged.api");
            if (intent != null) {
                context.startActivity(intent);
                return;
            }
        } catch (Exception ignored) {}
        try {
            Browser.openUrl(context, "https://shizuku.rikka.app/");
        } catch (Exception ignored) {}
    }
}
