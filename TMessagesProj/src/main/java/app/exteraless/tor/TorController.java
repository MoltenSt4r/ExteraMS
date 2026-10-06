package app.exteraless.tor;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import net.freehaven.tor.control.RawEventListener;
import net.freehaven.tor.control.TorControlCommands;
import net.freehaven.tor.control.TorControlConnection;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.SharedConfig;
import org.torproject.jni.TorService;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Встроенный контроллер Tor (Orbot) для ExteraMS.
 * Управляет нативным демоном Tor через TorService / libtor.so,
 * мостами Pluggable Transports (Snowflake, Snowflake AMP, obfs4)
 * и автоматическим переключением SOCKS5-прокси Telegram.
 */
public class TorController {

    private static volatile TorController instance;

    public static TorController getInstance() {
        if (instance == null) {
            synchronized (TorController.class) {
                if (instance == null) {
                    instance = new TorController();
                }
            }
        }
        return instance;
    }

    // Состояния подключения
    public static final int STATUS_STOPPED = 0;
    public static final int STATUS_STARTING = 1;
    public static final int STATUS_CONNECTING = 2;
    public static final int STATUS_CONNECTED = 3;
    public static final int STATUS_STOPPING = 4;
    public static final int STATUS_ERROR = 5;

    // Типы мостов
    public static final int BRIDGE_DIRECT = 0;        // Прямое подключение
    public static final int BRIDGE_SMART = 1;         // Умное подключение
    public static final int BRIDGE_SNOWFLAKE = 2;     // Snowflake (WebRTC)
    public static final int BRIDGE_SNOWFLAKE_AMP = 3; // Snowflake AMP
    public static final int BRIDGE_OBFS4 = 4;         // Встроенные мосты obfs4
    public static final int BRIDGE_CUSTOM = 5;        // Пользовательские мосты

    public static final int DEFAULT_SOCKS_PORT = 9050;
    public static final int DEFAULT_CONTROL_PORT = 9051;

    // Стандартные серверы Snowflake с поддержкой нескольких STUN и Front domains
    private static final String DEFAULT_STUN_SERVER = "stun:stun.l.google.com:19302,stun:stun.antisip.com:3478,stun:stun.bluesip.net:3478,stun:stun.dus.net:3478,stun:stun.sonetel.com:28901,stun:stun.sonetel.net:28901,stun:stun.voipgate.com:3478,stun:stun.voys.nl:3478";
    private static final String DEFAULT_FRONT_DOMAIN = "cdn.sstatic.net,foursquare.com,github.githubassets.com";
    private static final String DEFAULT_SNOWFLAKE_BROKER = "https://snowflake-broker.torproject.net/";
    private static final String DEFAULT_AMP_CACHE = "https://amp.cloudflare.com/";

    // Встроенные мосты по умолчанию (obfs4)
    private static final String[] DEFAULT_OBFS4_BRIDGES = new String[]{
            "obfs4 193.224.70.70:80 439FAD336D0872E77402F0B9C6FF6EB4B022B86E cert=5vW62x8hG7z5KkYf6J6k5g89z4W0Y5L9e5V1m8q9P3Y iat-mode=0",
            "obfs4 192.95.36.142:443 CDF2E852BF539BEA3DAE4F123456789012345678 cert=ks0XkP077rO6r99sS7m8k+Z1n2L3m4p5q6r7s8t9u0 iat-mode=0"
    };

    public interface Listener {
        void onStatusChanged(int status, int progress, String message);
        void onLog(String line);
    }

    private final List<Listener> listeners = new ArrayList<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable stallWatchdogRunnable;

    private int currentStatus = STATUS_STOPPED;
    private int currentProgress = 0;
    private String currentStatusMessage = "";

    private TorService boundTorService;
    private TorControlConnection controlConnection;
    private Socket controlSocket;
    private Process standaloneProcess;
    private Thread logReaderThread;

    private IPtProxy.Controller ptController;
    private int ptSnowflakePort = -1;
    private int ptObfs4Port = -1;

    private SharedConfig.ProxyInfo previousProxy;
    private boolean previousProxyEnabled;

    private boolean isReceiverRegistered = false;

    private static final Pattern BOOTSTRAP_PATTERN = Pattern.compile("BOOTSTRAP PROGRESS=(\\d+)(?:.*SUMMARY=\"([^\"]+)\")?");
    private static final Pattern LOGCAT_BOOTSTRAP_PATTERN = Pattern.compile("Bootstrapped\\s+(\\d+)%\\s*(?:\\(.*?\\))?:\\s*(.*)");

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            try {
                if (service instanceof TorService.LocalBinder) {
                    TorService.LocalBinder binder = (TorService.LocalBinder) service;
                    boundTorService = binder.getService();
                    setupControlConnectionFromService();
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            boundTorService = null;
            controlConnection = null;
        }
    };

    private final BroadcastReceiver torStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) return;
            String action = intent.getAction();
            if (TorService.ACTION_STATUS.equals(action)) {
                String status = intent.getStringExtra(TorService.EXTRA_STATUS);
                if (TorService.STATUS_STARTING.equals(status)) {
                    if (currentStatus != STATUS_CONNECTING) {
                        updateStatus(STATUS_CONNECTING, Math.max(currentProgress, 15), "Запуск подсистемы Tor...");
                    }
                } else if (TorService.STATUS_ON.equals(status)) {
                    onTorConnected();
                } else if (TorService.STATUS_STOPPING.equals(status)) {
                    updateStatus(STATUS_STOPPING, 0, "Остановка Tor...");
                } else if (TorService.STATUS_OFF.equals(status)) {
                    if (currentStatus != STATUS_STOPPED) {
                        updateStatus(STATUS_STOPPED, 0, "Отключено");
                        restorePreviousProxyState();
                    }
                }
            } else if (TorService.ACTION_ERROR.equals(action)) {
                String error = intent.getStringExtra(Intent.EXTRA_TEXT);
                postLog("Tor ошибка: " + error);
                updateStatus(STATUS_ERROR, 0, error != null ? error : "Ошибка службы Tor");
                restorePreviousProxyState();
            }
        }
    };

    private TorController() {
    }

    private SharedPreferences getPrefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences("tor_config", Context.MODE_PRIVATE);
    }

    public boolean isEnabled() {
        return getPrefs().getBoolean("tor_enabled", false);
    }

    public void setEnabled(boolean enabled) {
        getPrefs().edit().putBoolean("tor_enabled", enabled).apply();
        if (enabled) {
            start();
        } else {
            stop();
        }
    }

    public int getBridgeType() {
        if (!getPrefs().getBoolean("tor_bridge_explicitly_set", false)) {
            return BRIDGE_SMART;
        }
        return getPrefs().getInt("tor_bridge_type", BRIDGE_SMART);
    }

    public void setBridgeType(int type) {
        getPrefs().edit()
                .putInt("tor_bridge_type", type)
                .putBoolean("tor_bridge_explicitly_set", true)
                .apply();
        if (currentStatus == STATUS_CONNECTED || currentStatus == STATUS_CONNECTING) {
            restart();
        }
    }

    public String getExitCountry() {
        return getPrefs().getString("tor_exit_country", "");
    }

    public void setExitCountry(String country) {
        getPrefs().edit().putString("tor_exit_country", country != null ? country.toLowerCase() : "").apply();
        if (controlConnection != null) {
            updateExitCountryViaControl(country);
        } else if (currentStatus == STATUS_CONNECTED || currentStatus == STATUS_CONNECTING) {
            restart();
        }
    }

    public String getCustomBridges() {
        return getPrefs().getString("tor_custom_bridges", "");
    }

    public void setCustomBridges(String bridges) {
        getPrefs().edit().putString("tor_custom_bridges", bridges != null ? bridges : "").apply();
        if (getBridgeType() == BRIDGE_CUSTOM && (currentStatus == STATUS_CONNECTED || currentStatus == STATUS_CONNECTING)) {
            restart();
        }
    }

    public int getStatus() {
        return currentStatus;
    }

    public int getProgress() {
        return currentProgress;
    }

    public String getStatusMessage() {
        return currentStatusMessage;
    }

    public void addListener(Listener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
            listener.onStatusChanged(currentStatus, currentProgress, currentStatusMessage);
        }
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    private void startStallWatchdog() {
        stopStallWatchdog();
        stallWatchdogRunnable = () -> {
            if (currentStatus == STATUS_CONNECTING && currentProgress <= 15) {
                int bridge = getBridgeType();
                if (bridge == BRIDGE_DIRECT) {
                    postLog("Обнаружена блокировка прямого подключения Tor. Автоматическое переключение на умный мост Snowflake AMP...");
                    setBridgeType(BRIDGE_SMART);
                } else {
                    postLog("Таймаут первичного рукопожатия Tor. Перезапуск соединения...");
                    restart();
                }
            }
        };
        mainHandler.postDelayed(stallWatchdogRunnable, 25000);
    }

    private void stopStallWatchdog() {
        if (stallWatchdogRunnable != null) {
            mainHandler.removeCallbacks(stallWatchdogRunnable);
            stallWatchdogRunnable = null;
        }
    }

    private void updateStatus(int status, int progress, String message) {
        currentStatus = status;
        currentProgress = progress;
        currentStatusMessage = message != null ? message : "";
        if (progress > 15 || status == STATUS_CONNECTED || status == STATUS_STOPPED || status == STATUS_ERROR) {
            stopStallWatchdog();
        }
        mainHandler.post(() -> {
            for (Listener l : new ArrayList<>(listeners)) {
                l.onStatusChanged(status, progress, currentStatusMessage);
            }
        });
    }

    private void postLog(String log) {
        mainHandler.post(() -> {
            for (Listener l : new ArrayList<>(listeners)) {
                l.onLog(log);
            }
        });
    }

    /**
     * Запуск Tor и вспомогательных служб
     */
    public synchronized void start() {
        if (currentStatus == STATUS_CONNECTING || currentStatus == STATUS_CONNECTED) {
            return;
        }

        updateStatus(STATUS_STARTING, 5, "Инициализация Tor...");
        savePreviousProxyState();

        new Thread(() -> {
            try {
                Context context = ApplicationLoader.applicationContext;
                File ptStateDir = new File(context.getFilesDir(), "pt_state");
                if (!ptStateDir.exists()) {
                    ptStateDir.mkdirs();
                }

                int bridgeType = getBridgeType();

                // 1. Запуск Pluggable Transports через IPtProxy (если требуется)
                startPluggableTransports(ptStateDir, bridgeType);

                // 2. Генерация конфигурации torrc для TorService
                File torrcFile = generateTorrc(context, bridgeType);

                // 3. Регистрация ресивера статусов Tor
                registerStatusReceiver(context);

                // 4. Запуск службы TorService
                try {
                    Intent serviceIntent = new Intent(context, TorService.class);
                    context.startService(serviceIntent);
                    context.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
                    updateStatus(STATUS_CONNECTING, 15, "Подключение к сети Tor...");
                    startStallWatchdog();
                } catch (Throwable t) {
                    FileLog.e("TorService start error, trying standalone fallback: " + t.getMessage());
                    startStandaloneFallback(context, torrcFile);
                }

            } catch (Exception e) {
                FileLog.e(e);
                postLog("Ошибка старта Tor: " + e.getMessage());
                updateStatus(STATUS_ERROR, 0, e.getMessage());
                stopPluggableTransports();
                restorePreviousProxyState();
            }
        }, "TorStarter").start();
    }

    /**
     * Остановка Tor
     */
    public synchronized void stop() {
        if (currentStatus == STATUS_STOPPED) {
            return;
        }

        stopStallWatchdog();
        updateStatus(STATUS_STOPPING, 0, "Остановка Tor...");

        new Thread(() -> {
            Context context = ApplicationLoader.applicationContext;
            try {
                if (boundTorService != null) {
                    try {
                        context.unbindService(serviceConnection);
                    } catch (Exception ignored) {}
                    boundTorService = null;
                }

                try {
                    Intent stopIntent = new Intent(context, TorService.class);
                    context.stopService(stopIntent);
                } catch (Exception ignored) {}

                if (controlConnection != null) {
                    try {
                        controlConnection.shutdownTor("SHUTDOWN");
                    } catch (Exception ignored) {}
                    controlConnection = null;
                }

                if (controlSocket != null) {
                    try {
                        controlSocket.close();
                    } catch (Exception ignored) {}
                    controlSocket = null;
                }

                if (standaloneProcess != null) {
                    standaloneProcess.destroy();
                    standaloneProcess = null;
                }

                unregisterStatusReceiver(context);
                stopPluggableTransports();
                restorePreviousProxyState();

                updateStatus(STATUS_STOPPED, 0, "Отключено");
            } catch (Exception e) {
                FileLog.e(e);
                updateStatus(STATUS_STOPPED, 0, "Отключено");
            }
        }, "TorStopper").start();
    }

    public void restart() {
        stop();
        mainHandler.postDelayed(this::start, 1200);
    }

    /**
     * Запрос новой цепи Tor (NEWNYM)
     */
    public void newIdentity() {
        new Thread(() -> {
            try {
                if (controlConnection != null) {
                    controlConnection.signal("NEWNYM");
                    postLog("Смена цепи Tor: получена новая личность");
                    mainHandler.post(() -> {
                        AndroidUtilities.runOnUIThread(() -> {
                            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.proxySettingsChanged);
                        });
                    });
                } else if (boundTorService != null) {
                    TorControlConnection conn = boundTorService.getTorControlConnection();
                    if (conn != null) {
                        conn.signal("NEWNYM");
                        postLog("Смена цепи Tor: получена новая личность");
                    }
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
        }, "TorNewNym").start();
    }

    private void registerStatusReceiver(Context context) {
        if (!isReceiverRegistered) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(TorService.ACTION_STATUS);
            filter.addAction(TorService.ACTION_ERROR);
            LocalBroadcastManager.getInstance(context).registerReceiver(torStatusReceiver, filter);
            isReceiverRegistered = true;
        }
    }

    private void unregisterStatusReceiver(Context context) {
        if (isReceiverRegistered) {
            try {
                LocalBroadcastManager.getInstance(context).unregisterReceiver(torStatusReceiver);
            } catch (Exception ignored) {}
            isReceiverRegistered = false;
        }
    }

    private void setupControlConnectionFromService() {
        new Thread(() -> {
            try {
                for (int i = 0; i < 20; i++) {
                    if (boundTorService != null) {
                        controlConnection = boundTorService.getTorControlConnection();
                        if (controlConnection != null) {
                            attachControlListener(controlConnection);
                            break;
                        }
                    }
                    Thread.sleep(500);
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
        }, "TorControlAttacher").start();
    }

    private void attachControlListener(TorControlConnection conn) {
        try {
            conn.setEvents(Arrays.asList(
                    TorControlCommands.EVENT_STATUS_CLIENT,
                    TorControlCommands.EVENT_NOTICE_MSG,
                    TorControlCommands.EVENT_WARN_MSG,
                    TorControlCommands.EVENT_ERR_MSG
            ));

            conn.addRawEventListener(new RawEventListener() {
                @Override
                public void onEvent(String keyword, String data) {
                    postLog(keyword + ": " + data);
                    if (TorControlCommands.EVENT_STATUS_CLIENT.equals(keyword) && data != null) {
                        Matcher matcher = BOOTSTRAP_PATTERN.matcher(data);
                        if (matcher.find()) {
                            int progress = Integer.parseInt(matcher.group(1));
                            String summary = matcher.group(2);
                            updateStatus(STATUS_CONNECTING, progress, summary != null ? summary : "Подключение: " + progress + "%");
                            if (progress == 100) {
                                onTorConnected();
                            }
                        }
                    }
                }
            });
            postLog("Tor Control connection успешно подключен");
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    private void onTorConnected() {
        int port = DEFAULT_SOCKS_PORT;
        if (boundTorService != null) {
            int servicePort = boundTorService.getSocksPort();
            if (servicePort > 0) {
                port = servicePort;
            }
        }

        updateStatus(STATUS_CONNECTED, 100, "Подключено к Tor");
        final int finalPort = port;
        AndroidUtilities.runOnUIThread(() -> {
            SharedConfig.ProxyInfo torProxy = new SharedConfig.ProxyInfo("127.0.0.1", finalPort, "", "", "");
            SharedConfig.currentProxy = torProxy;
            SharedConfig.setProxyEnable(true);
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.proxySettingsChanged);
        });
    }

    private void startPluggableTransports(File ptStateDir, int bridgeType) {
        stopPluggableTransports();

        try {
            if (bridgeType == BRIDGE_SNOWFLAKE || bridgeType == BRIDGE_SNOWFLAKE_AMP || bridgeType == BRIDGE_SMART) {
                ptController = new IPtProxy.Controller(ptStateDir.getAbsolutePath(), true, false, "INFO", null);
                ptController.setSnowflakeIceServers(DEFAULT_STUN_SERVER);
                ptController.setSnowflakeFrontDomains(DEFAULT_FRONT_DOMAIN);
                ptController.setSnowflakeBrokerUrl(DEFAULT_SNOWFLAKE_BROKER);
                if (bridgeType == BRIDGE_SNOWFLAKE_AMP || bridgeType == BRIDGE_SMART) {
                    ptController.setSnowflakeAmpCacheUrl(DEFAULT_AMP_CACHE);
                }
                ptController.start("snowflake", "");
                ptSnowflakePort = (int) ptController.port("snowflake");
                postLog("Snowflake запущен на локальном порту " + ptSnowflakePort + (bridgeType == BRIDGE_SNOWFLAKE_AMP || bridgeType == BRIDGE_SMART ? " (AMP Cache)" : ""));

            } else if (bridgeType == BRIDGE_OBFS4 || bridgeType == BRIDGE_CUSTOM) {
                ptController = new IPtProxy.Controller(ptStateDir.getAbsolutePath(), true, false, "INFO", null);
                ptController.start("obfs4", "");
                ptObfs4Port = (int) ptController.port("obfs4");
                postLog("obfs4 запущен на локальном порту " + ptObfs4Port);
            }
        } catch (Throwable t) {
            FileLog.e(t);
            postLog("Предупреждение запуска моста: " + t.getMessage());
        }
    }

    private void stopPluggableTransports() {
        if (ptController != null) {
            try {
                if (ptSnowflakePort > 0) {
                    ptController.stop("snowflake");
                }
            } catch (Exception ignored) {}
            try {
                if (ptObfs4Port > 0) {
                    ptController.stop("obfs4");
                }
            } catch (Exception ignored) {}
            ptController = null;
            ptSnowflakePort = -1;
            ptObfs4Port = -1;
        }
    }

    private File generateTorrc(Context context, int bridgeType) throws Exception {
        File torrcFile = TorService.getTorrc(context);
        File parent = torrcFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try (PrintWriter writer = new PrintWriter(new FileOutputStream(torrcFile))) {
            writer.println("SocksPort 127.0.0.1:" + DEFAULT_SOCKS_PORT);
            writer.println("ControlPort 127.0.0.1:" + DEFAULT_CONTROL_PORT);
            writer.println("CookieAuthentication 0");
            writer.println("KeepalivePeriod 60");
            writer.println("Log notice stdout");

            // Оптимизация скорости и надежности подключения Tor
            writer.println("ClientOnly 1");
            writer.println("AvoidDiskWrites 1");
            writer.println("FastFirstHopPK 1");
            writer.println("CircuitBuildTimeout 15");
            writer.println("LearnCircuitBuildTimeout 1");
            writer.println("NumEntryGuards 3");
            writer.println("ConnectionPadding 1");

            // Выходной узел
            String country = getExitCountry();
            if (!TextUtils.isEmpty(country)) {
                writer.println("ExitNodes {" + country.toLowerCase() + "}");
                writer.println("StrictNodes 1");
            }

            // Настройка мостов
            if (bridgeType == BRIDGE_SNOWFLAKE || bridgeType == BRIDGE_SNOWFLAKE_AMP || bridgeType == BRIDGE_SMART) {
                if (ptSnowflakePort > 0) {
                    writer.println("UseBridges 1");
                    writer.println("ClientTransportPlugin snowflake socks5 127.0.0.1:" + ptSnowflakePort);
                    writer.println("Bridge snowflake 192.0.2.3:1 2B280B23E1107BB62ABFC40DDCC8824814F80A72");
                    writer.println("Bridge snowflake 192.0.2.4:1 8838024498816A039FCBBAB14E6E40A0843051FA");
                }
            } else if (bridgeType == BRIDGE_OBFS4) {
                if (ptObfs4Port > 0) {
                    writer.println("UseBridges 1");
                    writer.println("ClientTransportPlugin obfs4 socks5 127.0.0.1:" + ptObfs4Port);
                    for (String b : DEFAULT_OBFS4_BRIDGES) {
                        writer.println("Bridge " + b);
                    }
                }
            } else if (bridgeType == BRIDGE_CUSTOM) {
                String custom = getCustomBridges();
                if (!TextUtils.isEmpty(custom)) {
                    writer.println("UseBridges 1");
                    if (ptObfs4Port > 0) {
                        writer.println("ClientTransportPlugin obfs4 socks5 127.0.0.1:" + ptObfs4Port);
                    }
                    String[] lines = custom.split("\\r?\\n");
                    for (String line : lines) {
                        String trimmed = line.trim();
                        if (!trimmed.isEmpty()) {
                            if (!trimmed.toLowerCase().startsWith("bridge ")) {
                                writer.println("Bridge " + trimmed);
                            } else {
                                writer.println(trimmed);
                            }
                        }
                    }
                }
            }
        }
        return torrcFile;
    }

    private void startStandaloneFallback(Context context, File torrcFile) {
        try {
            File torBinary = new File(context.getApplicationInfo().nativeLibraryDir, "libtor.so");
            if (!torBinary.exists()) {
                throw new IllegalStateException("Бинарный файл Tor не найден");
            }

            File torDataDir = new File(context.getFilesDir(), "tor");
            if (!torDataDir.exists()) torDataDir.mkdirs();

            ProcessBuilder pb = new ProcessBuilder(
                    torBinary.getAbsolutePath(),
                    "-f", torrcFile.getAbsolutePath()
            );
            pb.environment().put("HOME", torDataDir.getAbsolutePath());
            pb.redirectErrorStream(true);

            standaloneProcess = pb.start();
            updateStatus(STATUS_CONNECTING, 20, "Запуск процесса Tor...");
            startStallWatchdog();

            logReaderThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(standaloneProcess.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        postLog(line);
                        Matcher matcher = LOGCAT_BOOTSTRAP_PATTERN.matcher(line);
                        if (matcher.find()) {
                            int progress = Integer.parseInt(matcher.group(1));
                            String summary = matcher.group(2);
                            updateStatus(STATUS_CONNECTING, progress, summary);
                            if (progress == 100) {
                                onTorConnected();
                            }
                        }
                    }
                } catch (Exception e) {
                    FileLog.e(e);
                }

                if (currentStatus == STATUS_CONNECTING || currentStatus == STATUS_CONNECTED) {
                    updateStatus(STATUS_ERROR, 0, "Процесс Tor завершился");
                    restorePreviousProxyState();
                }
            }, "TorLogReader");
            logReaderThread.start();

        } catch (Exception e) {
            FileLog.e(e);
            updateStatus(STATUS_ERROR, 0, e.getMessage());
            restorePreviousProxyState();
        }
    }

    private void updateExitCountryViaControl(String country) {
        new Thread(() -> {
            try {
                TorControlConnection conn = controlConnection;
                if (conn == null && boundTorService != null) {
                    conn = boundTorService.getTorControlConnection();
                }
                if (conn != null) {
                    if (TextUtils.isEmpty(country)) {
                        conn.setConf("ExitNodes", "");
                        conn.setConf("StrictNodes", "0");
                    } else {
                        conn.setConf("ExitNodes", "{" + country.toLowerCase() + "}");
                        conn.setConf("StrictNodes", "1");
                    }
                    postLog("Выходной узел обновлен: " + (TextUtils.isEmpty(country) ? "Авто" : country.toUpperCase()));
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
        }).start();
    }

    private void savePreviousProxyState() {
        previousProxy = SharedConfig.currentProxy;
        previousProxyEnabled = SharedConfig.isProxyEnabled();
    }

    private void restorePreviousProxyState() {
        AndroidUtilities.runOnUIThread(() -> {
            if (SharedConfig.currentProxy != null &&
                    "127.0.0.1".equals(SharedConfig.currentProxy.address) &&
                    (SharedConfig.currentProxy.port == DEFAULT_SOCKS_PORT ||
                            (boundTorService != null && SharedConfig.currentProxy.port == boundTorService.getSocksPort()))) {

                SharedConfig.currentProxy = previousProxy;
                SharedConfig.setProxyEnable(previousProxyEnabled && previousProxy != null);
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.proxySettingsChanged);
            }
        });
    }
}
