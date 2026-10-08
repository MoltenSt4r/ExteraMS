package com.radolyn.ayugram.messages;

import android.text.TextUtils;
import android.util.SparseIntArray;

import androidx.collection.LongSparseArray;

import com.radolyn.ayugram.database.AyuData;
import com.radolyn.ayugram.database.dao.SpyDao;
import com.radolyn.ayugram.database.entities.SpyMessageContentsRead;
import com.radolyn.ayugram.database.entities.SpyMessageRead;

import org.telegram.SQLite.SQLiteCursor;
import org.telegram.SQLite.SQLiteDatabase;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.support.LongSparseIntArray;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import xyz.nextalone.nagram.NaConfig;

public final class AyuSpyController {

    private AyuSpyController() {
    }

    public static boolean isEnabled() {
        return NaConfig.INSTANCE.getSaveReadDate().Bool();
    }

    public static void onUpdatesRead(int account, LongSparseIntArray outbox, SparseIntArray encrypted, LongSparseArray<ArrayList<Integer>> contents, int contentsDate, int date) {
        if (date == 0 || !isEnabled()) {
            return;
        }
        final boolean hasOutbox = outbox != null && outbox.size() > 0;
        final boolean hasEncrypted = encrypted != null && encrypted.size() > 0;
        final boolean hasContents = contents != null && contents.size() > 0;
        if (!hasOutbox && !hasEncrypted && !hasContents) {
            return;
        }
        final long userId = UserConfig.getInstance(account).getClientUserId();
        final int readContentsDate = contentsDate != 0 ? contentsDate : date;
        final MessagesStorage storage = MessagesStorage.getInstance(account);
        storage.getStorageQueue().postRunnable(() -> {
            final ArrayList<SpyMessageRead> reads = new ArrayList<>();
            final ArrayList<SpyMessageContentsRead> contentsReads = new ArrayList<>();
            final SQLiteDatabase database = storage.getDatabase();
            if (database == null) {
                return;
            }
            if (hasOutbox) {
                for (int i = 0; i < outbox.size(); i++) {
                    final long dialogId = outbox.keyAt(i);
                    collectReads(database, reads, userId, date, String.format(Locale.US,
                            "SELECT uid, mid FROM messages_v2 WHERE uid = %d AND mid > 0 AND mid <= %d AND read_state IN(0,2) AND out = 1",
                            dialogId, outbox.valueAt(i)));
                }
            }
            if (hasEncrypted) {
                for (int i = 0; i < encrypted.size(); i++) {
                    final long dialogId = DialogObject.makeEncryptedDialogId(encrypted.keyAt(i));
                    collectReads(database, reads, userId, date, String.format(Locale.US,
                            "SELECT uid, mid FROM messages_v2 WHERE uid = %d AND date <= %d AND read_state IN(0,2) AND out = 1",
                            dialogId, encrypted.valueAt(i)));
                }
            }
            if (hasContents) {
                for (int i = 0; i < contents.size(); i++) {
                    final long dialogId = contents.keyAt(i);
                    final ArrayList<Integer> mids = contents.valueAt(i);
                    if (mids == null || mids.isEmpty()) {
                        continue;
                    }
                    final String query = dialogId == 0
                            ? String.format(Locale.US, "SELECT uid, mid FROM messages_v2 WHERE mid IN (%s) AND is_channel = 0 AND out = 1 AND (read_state & 2) = 0", TextUtils.join(",", mids))
                            : String.format(Locale.US, "SELECT uid, mid FROM messages_v2 WHERE mid IN (%s) AND uid = %d AND out = 1 AND (read_state & 2) = 0", TextUtils.join(",", mids), dialogId);
                    collectContentsReads(database, contentsReads, userId, readContentsDate, query);
                }
            }
            if (reads.isEmpty() && contentsReads.isEmpty()) {
                return;
            }
            Utilities.globalQueue.postRunnable(() -> {
                final SpyDao dao = AyuData.getSpyDao();
                if (dao == null) {
                    return;
                }
                try {
                    if (!reads.isEmpty()) {
                        dao.insertRead(reads);
                    }
                    if (!contentsReads.isEmpty()) {
                        dao.insertContentsRead(contentsReads);
                    }
                } catch (Exception e) {
                    FileLog.e(e);
                }
            });
        });
    }

    private static void collectReads(SQLiteDatabase database, ArrayList<SpyMessageRead> out, long userId, int date, String query) {
        SQLiteCursor cursor = null;
        try {
            cursor = database.queryFinalized(query);
            while (cursor.next()) {
                final SpyMessageRead read = new SpyMessageRead();
                read.userId = userId;
                read.dialogId = cursor.longValue(0);
                read.messageId = cursor.intValue(1);
                read.date = date;
                out.add(read);
            }
        } catch (Exception e) {
            FileLog.e(e);
        } finally {
            if (cursor != null) {
                cursor.dispose();
            }
        }
    }

    private static void collectContentsReads(SQLiteDatabase database, ArrayList<SpyMessageContentsRead> out, long userId, int date, String query) {
        SQLiteCursor cursor = null;
        try {
            cursor = database.queryFinalized(query);
            while (cursor.next()) {
                final SpyMessageContentsRead read = new SpyMessageContentsRead();
                read.userId = userId;
                read.dialogId = cursor.longValue(0);
                read.messageId = cursor.intValue(1);
                read.date = date;
                out.add(read);
            }
        } catch (Exception e) {
            FileLog.e(e);
        } finally {
            if (cursor != null) {
                cursor.dispose();
            }
        }
    }

    public static int getReadDate(MessageObject messageObject) {
        if (messageObject == null || !isEnabled()) {
            return 0;
        }
        final SpyDao dao = AyuData.getSpyDao();
        if (dao == null) {
            return 0;
        }
        try {
            final Integer date = dao.getReadDate(UserConfig.getInstance(messageObject.currentAccount).getClientUserId(), messageObject.getDialogId(), messageObject.getId());
            return date == null ? 0 : date;
        } catch (Exception e) {
            FileLog.e(e);
            return 0;
        }
    }

    public static int getContentsReadDate(MessageObject messageObject) {
        if (messageObject == null || !isEnabled()) {
            return 0;
        }
        final SpyDao dao = AyuData.getSpyDao();
        if (dao == null) {
            return 0;
        }
        try {
            final Integer date = dao.getContentsReadDate(UserConfig.getInstance(messageObject.currentAccount).getClientUserId(), messageObject.getDialogId(), messageObject.getId());
            return date == null ? 0 : date;
        } catch (Exception e) {
            FileLog.e(e);
            return 0;
        }
    }

    public static String formatPlayedDate(int seconds) {
        final long date = seconds * 1000L;
        final Calendar calendar = Calendar.getInstance();
        final int day = calendar.get(Calendar.DAY_OF_YEAR);
        final int year = calendar.get(Calendar.YEAR);
        calendar.setTimeInMillis(date);
        final int dateDay = calendar.get(Calendar.DAY_OF_YEAR);
        final int dateYear = calendar.get(Calendar.YEAR);
        final LocaleController locale = LocaleController.getInstance();
        final String time = locale.getFormatterDay().format(new Date(date));
        if (dateDay == day && dateYear == year) {
            return LocaleController.formatString(R.string.OEAyuPlayedTodayAt, time);
        } else if (dateDay + 1 == day && dateYear == year) {
            return LocaleController.formatString(R.string.OEAyuPlayedYesterdayAt, time);
        } else if (Math.abs(System.currentTimeMillis() - date) < 31536000000L) {
            return LocaleController.formatString(R.string.OEAyuPlayedDateTimeAt, locale.getFormatterDayMonth().format(new Date(date)), time);
        }
        return LocaleController.formatString(R.string.OEAyuPlayedDateTimeAt, locale.getFormatterYear().format(new Date(date)), time);
    }
}
