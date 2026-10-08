package app.exteraless.ghost;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public final class OwnLastSeen {

    private OwnLastSeen() {
    }

    public static int seconds(int account, TLRPC.User user) {
        if (user == null || !(user.status instanceof TLRPC.TL_userStatusOffline)) {
            return 0;
        }
        int expires = user.status.expires;
        if (expires <= 0 || expires > ConnectionsManager.getInstance(account).getCurrentTime()) {
            return 0;
        }
        return expires;
    }

    public static String time(int seconds) {
        Locale locale = LocaleController.getInstance().getCurrentLocale();
        return DateFormat.getTimeInstance(DateFormat.MEDIUM, locale == null ? Locale.getDefault() : locale)
                .format(new Date(seconds * 1000L));
    }

    public static boolean isToday(int seconds) {
        Calendar now = Calendar.getInstance();
        Calendar then = Calendar.getInstance();
        then.setTimeInMillis(seconds * 1000L);
        return now.get(Calendar.YEAR) == then.get(Calendar.YEAR)
                && now.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR);
    }

    public static String format(int seconds) {
        Calendar now = Calendar.getInstance();
        Calendar then = Calendar.getInstance();
        then.setTimeInMillis(seconds * 1000L);
        String time = time(seconds);
        if (isToday(seconds)) {
            return LocaleController.formatString(R.string.TodayAtFormattedWithToday, time);
        }
        now.add(Calendar.DAY_OF_YEAR, -1);
        if (now.get(Calendar.YEAR) == then.get(Calendar.YEAR)
                && now.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)) {
            return LocaleController.formatString(R.string.YesterdayAtFormatted, time);
        }
        LocaleController locale = LocaleController.getInstance();
        Date date = new Date(seconds * 1000L);
        String day = then.get(Calendar.YEAR) == Calendar.getInstance().get(Calendar.YEAR)
                ? locale.getFormatterDayMonth().format(date) : locale.getFormatterYear().format(date);
        return LocaleController.formatString(R.string.formatDateAtTime, day, time);
    }
}
