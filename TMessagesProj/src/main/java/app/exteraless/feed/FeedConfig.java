package app.exteraless.feed;

import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.UserConfig;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Настройки ленты для одного аккаунта: показывать ли каналы из архива и какие каналы исключены.
 * Хранится в отдельном SharedPreferences-файле на аккаунт, счётчик поколений растёт при любом
 * изменении и служит сигналом к пересборке ленты.
 */
public final class FeedConfig {

    private static final String PREFERENCES_PREFIX = "feedconfig";
    private static final String KEY_INCLUDE_ARCHIVED = "includeArchived";
    private static final String KEY_EXCLUDED_CHANNELS = "excludedChannels";
    private static final String KEY_ADD_NEW_CHANNELS = "addNewChannels";
    private static final String KEY_KNOWN_CHANNELS = "knownChannels";

    private static final FeedConfig[] instances = new FeedConfig[UserConfig.MAX_ACCOUNT_COUNT];
    private static final Object[] lockObjects = new Object[UserConfig.MAX_ACCOUNT_COUNT];

    static {
        for (int account = 0; account < lockObjects.length; account++) {
            lockObjects[account] = new Object();
        }
    }

    private final SharedPreferences preferences;
    private final AtomicInteger generation = new AtomicInteger();

    private volatile Set<Long> excludedChannels;
    private volatile Set<Long> knownChannels;
    private volatile boolean includeArchived;
    private volatile boolean addNewChannels;

    private FeedConfig(int account) {
        preferences = ApplicationLoader.applicationContext.getSharedPreferences(PREFERENCES_PREFIX + account, 0);
        includeArchived = preferences.getBoolean(KEY_INCLUDE_ARCHIVED, false);
        addNewChannels = preferences.getBoolean(KEY_ADD_NEW_CHANNELS, true);
        excludedChannels = readIds(KEY_EXCLUDED_CHANNELS);
        knownChannels = readIds(KEY_KNOWN_CHANNELS);
    }

    public static FeedConfig getInstance(int num) {
        FeedConfig cached = instances[num];
        if (cached != null) {
            return cached;
        }
        synchronized (lockObjects[num]) {
            cached = instances[num];
            if (cached == null) {
                cached = new FeedConfig(num);
                instances[num] = cached;
            }
        }
        return cached;
    }

    private Set<Long> readIds(String key) {
        Set<String> stored = preferences.getStringSet(key, null);
        if (stored == null) {
            return Collections.emptySet();
        }
        HashSet<Long> parsed = new HashSet<>();
        for (String value : stored) {
            try {
                parsed.add(Long.parseLong(value));
            } catch (NumberFormatException ignored) {
            }
        }
        return Collections.unmodifiableSet(parsed);
    }

    private static HashSet<String> toStrings(Set<Long> ids) {
        HashSet<String> stored = new HashSet<>();
        for (Long dialogId : ids) {
            stored.add(String.valueOf(dialogId.longValue()));
        }
        return stored;
    }

    private void applyExcluded(Set<Long> updated) {
        excludedChannels = Collections.unmodifiableSet(updated);
        generation.incrementAndGet();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putStringSet(KEY_EXCLUDED_CHANNELS, toStrings(updated));
        editor.apply();
    }

    private void applyKnown(Set<Long> updated) {
        knownChannels = Collections.unmodifiableSet(updated);
        generation.incrementAndGet();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putStringSet(KEY_KNOWN_CHANNELS, toStrings(updated));
        editor.apply();
    }

    private void markKnown(Collection<Long> ids) {
        if (addNewChannels) {
            return;
        }
        HashSet<Long> updated = new HashSet<>(knownChannels);
        if (updated.addAll(ids)) {
            applyKnown(updated);
        }
    }

    public boolean getAddNewChannels() {
        return addNewChannels;
    }

    public void setAddNewChannels(boolean value, Collection<Long> currentChannels) {
        if (addNewChannels == value) {
            return;
        }
        addNewChannels = value;
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(KEY_ADD_NEW_CHANNELS, value);
        editor.apply();
        if (value) {
            generation.incrementAndGet();
        } else {
            applyKnown(new HashSet<>(currentChannels));
        }
    }

    public boolean getIncludeArchived() {
        return includeArchived;
    }

    public void setIncludeArchived(boolean value) {
        if (includeArchived == value) {
            return;
        }
        includeArchived = value;
        generation.incrementAndGet();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(KEY_INCLUDE_ARCHIVED, value);
        editor.apply();
    }

    public boolean isExcluded(long dialogId) {
        return excludedChannels.contains(dialogId) || !addNewChannels && !knownChannels.contains(dialogId);
    }

    public void setExcluded(long dialogId, boolean excluded) {
        if (!excluded) {
            markKnown(Collections.singleton(dialogId));
        }
        HashSet<Long> updated = new HashSet<>(excludedChannels);
        boolean changed = excluded ? updated.add(dialogId) : updated.remove(dialogId);
        if (changed) {
            applyExcluded(updated);
        }
    }

    /**
     * Неизменяемый снимок исключённых каналов: можно читать с любого потока,
     * пересборка ленты работает именно с ним, а не с живой коллекцией.
     */
    public Set<Long> getExcludedSnapshot() {
        return excludedChannels;
    }

    public void removeExcluded(Set<Long> ids) {
        if (ids.isEmpty()) {
            return;
        }
        HashSet<Long> updated = new HashSet<>(excludedChannels);
        if (updated.removeAll(ids)) {
            applyExcluded(updated);
        }
    }

    public void includeAll(Collection<Long> ids) {
        markKnown(ids);
        if (excludedChannels.isEmpty()) {
            return;
        }
        applyExcluded(new HashSet<Long>());
    }

    public void excludeAll(Collection<Long> ids) {
        HashSet<Long> updated = new HashSet<>(excludedChannels);
        if (updated.addAll(ids)) {
            applyExcluded(updated);
        }
    }

    /**
     * Номер поколения настроек: меняется при каждой правке состава ленты.
     * Потребители сравнивают его со своим сохранённым и пересобирают выборку при расхождении.
     */
    public int getGeneration() {
        return generation.get();
    }
}
