package app.exteraless.backup;

import static org.telegram.messenger.LocaleController.getString;

import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import androidx.sqlite.db.SupportSQLiteDatabase;

import com.radolyn.ayugram.database.AyuData;
import com.radolyn.ayugram.database.AyuDatabase;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.BulletinFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import tw.nekomimi.nekogram.utils.AlertUtil;

public final class AyuDatabaseImport {

    public static final String AYUGRAM_EXPORT_NAME = "database-export.sqlite";

    private static final String DELETED = "DeletedMessage";
    private static final String EDITED = "EditedMessage";
    private static final String REACTIONS = "DeletedMessageReaction";
    private static final String DELETED_DIALOGS = "DeletedDialog";
    private static final String READS = "SpyMessageRead";
    private static final String CONTENTS_READS = "SpyMessageContentsRead";
    private static final String LAST_SEEN = "LastSeenEntity";
    private static final String AYUGRAM_LAST_SEEN = "SpyLastSeen";
    private static final Map<String, String> READ_ALIASES = Collections.singletonMap("date", "entityCreateDate");

    private AyuDatabaseImport() {
    }

    public static boolean isAyuGramExport(String name) {
        return AYUGRAM_EXPORT_NAME.equals(name);
    }

    public static void importFromUri(BaseFragment fragment, Uri uri) {
        File file = new File(AndroidUtilities.getCacheDir(), "ayu-import-" + System.currentTimeMillis() + ".sqlite");
        try (InputStream input = ApplicationLoader.applicationContext.getContentResolver().openInputStream(uri)) {
            if (input == null) {
                return;
            }
            try (OutputStream output = new FileOutputStream(file)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    output.write(buffer, 0, read);
                }
            }
        } catch (Exception e) {
            FileLog.e(e);
            file.delete();
            BulletinFactory.of(fragment).createSimpleBulletin(R.raw.error, getString(R.string.OEAyuDatabaseImportFailed)).show();
            return;
        }
        confirm(fragment, file, true);
    }

    public static void confirm(BaseFragment fragment, File file) {
        confirm(fragment, file, false);
    }

    private static void confirm(BaseFragment fragment, File file, boolean temporary) {
        Activity activity = fragment.getParentActivity();
        if (activity == null) {
            return;
        }
        AlertUtil.showConfirm(activity,
                getString(R.string.OEAyuDatabaseImport),
                getString(R.string.OEAyuDatabaseImportInfo),
                R.drawable.msg_download,
                getString(R.string.Import),
                false,
                () -> run(fragment, file, temporary));
    }

    private static void run(BaseFragment fragment, File file, boolean temporary) {
        Activity activity = fragment.getParentActivity();
        if (activity == null) {
            return;
        }
        AlertDialog progressDialog = new AlertDialog(activity, AlertDialog.ALERT_TYPE_SPINNER, fragment.getResourceProvider());
        progressDialog.setCanCancel(false);
        progressDialog.show();
        Utilities.globalQueue.postRunnable(() -> {
            int added;
            try {
                added = merge(file);
            } catch (Exception e) {
                FileLog.e(e);
                added = -1;
            }
            if (temporary) {
                file.delete();
            }
            final int result = added;
            AndroidUtilities.runOnUIThread(() -> {
                progressDialog.dismiss();
                if (result < 0) {
                    BulletinFactory.of(fragment).createSimpleBulletin(R.raw.error, getString(R.string.OEAyuDatabaseImportFailed)).show();
                } else {
                    BulletinFactory.of(fragment).createSimpleBulletin(R.raw.done, LocaleController.formatString(R.string.OEAyuDatabaseImportDone, result)).show();
                }
            });
        });
    }

    private static int merge(File file) throws IOException {
        File copy = new File(AndroidUtilities.getCacheDir(), "ayu-merge-" + System.currentTimeMillis() + ".sqlite");
        if (!AndroidUtilities.copyFile(file, copy)) {
            throw new IOException("Failed to stage database");
        }
        try (SQLiteDatabase source = SQLiteDatabase.openDatabase(copy.getAbsolutePath(), null, SQLiteDatabase.OPEN_READWRITE)) {
            if (!source.isDatabaseIntegrityOk()) {
                throw new IOException("Database integrity check failed");
            }
            if (!hasTable(source, DELETED) && !hasTable(source, EDITED)) {
                throw new IOException("Not a message database");
            }
            AyuDatabase database = AyuData.getDatabase();
            if (database == null) {
                throw new IOException("Message database is closed");
            }
            SupportSQLiteDatabase target = database.getOpenHelper().getWritableDatabase();
            target.beginTransaction();
            try {
                HashMap<Long, Long> deletedIds = new HashMap<>();
                int added = copyDeleted(source, target, deletedIds);
                copyReactions(source, target, deletedIds);
                added += copyEdited(source, target);
                copyAll(source, target, DELETED_DIALOGS, DELETED_DIALOGS, Collections.emptyMap());
                copyAll(source, target, READS, READS, READ_ALIASES);
                copyAll(source, target, CONTENTS_READS, CONTENTS_READS, READ_ALIASES);
                copyLastSeen(source, target);
                target.setTransactionSuccessful();
                return added;
            } finally {
                target.endTransaction();
            }
        } finally {
            SQLiteDatabase.deleteDatabase(copy);
        }
    }

    private static int copyDeleted(SQLiteDatabase source, SupportSQLiteDatabase target, HashMap<Long, Long> ids) {
        if (!hasTable(source, DELETED)) {
            return 0;
        }
        Map<String, Boolean> columns = columns(target.query("PRAGMA table_info(`" + DELETED + "`)"));
        int added = 0;
        try (Cursor row = source.rawQuery("SELECT * FROM `" + DELETED + "`", null)) {
            int fakeId = row.getColumnIndex("fakeId");
            int userId = row.getColumnIndex("userId");
            int dialogId = row.getColumnIndex("dialogId");
            int topicId = row.getColumnIndex("topicId");
            int messageId = row.getColumnIndex("messageId");
            if (userId < 0 || dialogId < 0 || messageId < 0) {
                return 0;
            }
            while (row.moveToNext()) {
                if (exists(target, "SELECT 1 FROM `" + DELETED + "` WHERE userId = ? AND dialogId = ? AND topicId = ? AND messageId = ? LIMIT 1",
                        row.getLong(userId), row.getLong(dialogId), topicId < 0 ? 0 : row.getLong(topicId), row.getLong(messageId))) {
                    continue;
                }
                long id = target.insert(DELETED, SQLiteDatabase.CONFLICT_ABORT, values(row, columns, Collections.emptyMap(), "fakeId"));
                if (id != -1) {
                    added++;
                    if (fakeId >= 0) {
                        ids.put(row.getLong(fakeId), id);
                    }
                }
            }
        }
        return added;
    }

    private static void copyReactions(SQLiteDatabase source, SupportSQLiteDatabase target, HashMap<Long, Long> ids) {
        if (ids.isEmpty() || !hasTable(source, REACTIONS)) {
            return;
        }
        Map<String, Boolean> columns = columns(target.query("PRAGMA table_info(`" + REACTIONS + "`)"));
        try (Cursor row = source.rawQuery("SELECT * FROM `" + REACTIONS + "`", null)) {
            int deletedMessageId = row.getColumnIndex("deletedMessageId");
            if (deletedMessageId < 0) {
                return;
            }
            while (row.moveToNext()) {
                Long mapped = ids.get(row.getLong(deletedMessageId));
                if (mapped == null) {
                    continue;
                }
                ContentValues values = values(row, columns, Collections.emptyMap(), "fakeReactionId");
                values.put("deletedMessageId", mapped);
                target.insert(REACTIONS, SQLiteDatabase.CONFLICT_IGNORE, values);
            }
        }
    }

    private static int copyEdited(SQLiteDatabase source, SupportSQLiteDatabase target) {
        if (!hasTable(source, EDITED)) {
            return 0;
        }
        Map<String, Boolean> columns = columns(target.query("PRAGMA table_info(`" + EDITED + "`)"));
        int added = 0;
        try (Cursor row = source.rawQuery("SELECT * FROM `" + EDITED + "`", null)) {
            int userId = row.getColumnIndex("userId");
            int dialogId = row.getColumnIndex("dialogId");
            int messageId = row.getColumnIndex("messageId");
            int entityCreateDate = row.getColumnIndex("entityCreateDate");
            int editDate = row.getColumnIndex("editDate");
            if (userId < 0 || dialogId < 0 || messageId < 0 || entityCreateDate < 0 || editDate < 0) {
                return 0;
            }
            while (row.moveToNext()) {
                if (exists(target, "SELECT 1 FROM `" + EDITED + "` WHERE userId = ? AND dialogId = ? AND messageId = ? AND entityCreateDate = ? AND editDate = ? LIMIT 1",
                        row.getLong(userId), row.getLong(dialogId), row.getLong(messageId), row.getLong(entityCreateDate), row.getLong(editDate))) {
                    continue;
                }
                if (target.insert(EDITED, SQLiteDatabase.CONFLICT_ABORT, values(row, columns, Collections.emptyMap(), "fakeId")) != -1) {
                    added++;
                }
            }
        }
        return added;
    }

    private static void copyAll(SQLiteDatabase source, SupportSQLiteDatabase target, String sourceTable, String targetTable, Map<String, String> aliases) {
        if (!hasTable(source, sourceTable)) {
            return;
        }
        Map<String, Boolean> columns = columns(target.query("PRAGMA table_info(`" + targetTable + "`)"));
        try (Cursor row = source.rawQuery("SELECT * FROM `" + sourceTable + "`", null)) {
            while (row.moveToNext()) {
                target.insert(targetTable, SQLiteDatabase.CONFLICT_IGNORE, values(row, columns, aliases, "fakeId"));
            }
        }
    }

    private static void copyLastSeen(SQLiteDatabase source, SupportSQLiteDatabase target) {
        String table;
        String column;
        if (hasTable(source, LAST_SEEN)) {
            table = LAST_SEEN;
            column = "lastSeen";
        } else if (hasTable(source, AYUGRAM_LAST_SEEN)) {
            table = AYUGRAM_LAST_SEEN;
            column = "lastSeenDate";
        } else {
            return;
        }
        try (Cursor row = source.rawQuery("SELECT userId, `" + column + "` FROM `" + table + "`", null)) {
            while (row.moveToNext()) {
                long userId = row.getLong(0);
                long lastSeen = row.getLong(1);
                ContentValues values = new ContentValues();
                values.put("userId", userId);
                values.put("lastSeen", lastSeen);
                if (target.insert(LAST_SEEN, SQLiteDatabase.CONFLICT_IGNORE, values) == -1) {
                    target.execSQL("UPDATE `" + LAST_SEEN + "` SET lastSeen = ? WHERE userId = ? AND lastSeen < ?", new Object[]{lastSeen, userId, lastSeen});
                }
            }
        }
    }

    private static ContentValues values(Cursor row, Map<String, Boolean> columns, Map<String, String> aliases, String skip) {
        ContentValues values = new ContentValues();
        for (Map.Entry<String, Boolean> column : columns.entrySet()) {
            String name = column.getKey();
            if (name.equals(skip)) {
                continue;
            }
            int index = row.getColumnIndex(name);
            if (index < 0 && aliases.containsKey(name)) {
                index = row.getColumnIndex(aliases.get(name));
            }
            if (index < 0 || row.isNull(index)) {
                if (column.getValue()) {
                    values.put(name, 0);
                }
                continue;
            }
            switch (row.getType(index)) {
                case Cursor.FIELD_TYPE_INTEGER:
                    values.put(name, row.getLong(index));
                    break;
                case Cursor.FIELD_TYPE_FLOAT:
                    values.put(name, row.getDouble(index));
                    break;
                case Cursor.FIELD_TYPE_BLOB:
                    values.put(name, row.getBlob(index));
                    break;
                default:
                    values.put(name, row.getString(index));
                    break;
            }
        }
        return values;
    }

    private static Map<String, Boolean> columns(Cursor info) {
        LinkedHashMap<String, Boolean> columns = new LinkedHashMap<>();
        try (info) {
            int name = info.getColumnIndex("name");
            int notNull = info.getColumnIndex("notnull");
            while (info.moveToNext()) {
                columns.put(info.getString(name), info.getInt(notNull) != 0);
            }
        }
        return columns;
    }

    private static boolean hasTable(SQLiteDatabase database, String table) {
        try (Cursor cursor = database.rawQuery("SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?", new String[]{table})) {
            return cursor.moveToFirst();
        }
    }

    private static boolean exists(SupportSQLiteDatabase database, String sql, Object... args) {
        try (Cursor cursor = database.query(sql, args)) {
            return cursor.moveToFirst();
        }
    }
}
