package app.exteraless.chats;

import android.content.Context;
import android.text.TextUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class UserLookup {

    private static final String BOT_USERNAME = "usinfobot";
    private static final int START_RETRIES = 4;
    private static final long START_RETRY_DELAY = 1500;

    private static final Pattern ID_LINE = Pattern.compile("^[^\\p{L}\\d-]*(-?\\d{1,19})$");
    private static final Pattern USERNAME_LINE = Pattern.compile("^[^\\p{L}\\d@]*@([A-Za-z\\d_]{4,32})");

    public interface Callback {
        void run(TLRPC.User user, boolean needsStart);
    }

    public static final class Info {
        public final String username;
        public final TLRPC.User user;
        public final boolean needsStart;

        private Info(String username, TLRPC.User user, boolean needsStart) {
            this.username = username;
            this.user = user;
            this.needsStart = needsStart;
        }
    }

    private UserLookup() {
    }

    public static Info parse(TLRPC.messages_BotResults results, long userId) {
        if (results == null) {
            return null;
        }
        boolean found = false;
        String username = null;
        for (TLRPC.BotInlineResult result : results.results) {
            if (result == null || result.send_message == null || TextUtils.isEmpty(result.send_message.message)) {
                continue;
            }
            long id = 0;
            String name = null;
            for (String raw : result.send_message.message.split("\n")) {
                String line = raw.replaceAll("\\p{C}", "").trim();
                Matcher matcher = ID_LINE.matcher(line);
                if (id == 0 && matcher.matches()) {
                    id = toLong(matcher.group(1));
                    continue;
                }
                matcher = USERNAME_LINE.matcher(line);
                if (name == null && matcher.find()) {
                    name = matcher.group(1);
                }
            }
            if (id == userId) {
                found = true;
                username = name;
                break;
            }
        }
        TLRPC.User user = null;
        for (TLRPC.User candidate : results.users) {
            if (candidate != null && candidate.id == userId && !candidate.min && candidate.access_hash != 0) {
                user = candidate;
            }
        }
        if (found || user != null) {
            return new Info(username, user, false);
        }
        if (results.switch_pm != null) {
            return new Info(null, null, true);
        }
        return null;
    }

    private static long toLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static Runnable find(int account, long userId, Callback callback) {
        Lookup lookup = new Lookup(account, userId, callback);
        lookup.start();
        return lookup::cancel;
    }

    public static void show(Context context, int account, Theme.ResourcesProvider resourcesProvider, long userId, Utilities.Callback<TLRPC.User> onResult) {
        if (context == null) {
            return;
        }
        TLRPC.User known = userId > 0 ? MessagesController.getInstance(account).getUser(userId) : null;
        if (known != null) {
            onResult.run(known);
            return;
        }
        new Session(context, account, resourcesProvider, userId, onResult).run(0);
    }

    private static final class Lookup {

        private final int account;
        private final long userId;
        private final Callback callback;
        private boolean done;
        private int reqId;
        private Runnable resolving;

        Lookup(int account, long userId, Callback callback) {
            this.account = account;
            this.userId = userId;
            this.callback = callback;
        }

        void start() {
            if (userId <= 0) {
                finish(null, false);
                return;
            }
            MessagesController controller = MessagesController.getInstance(account);
            TLRPC.User user = controller.getUser(userId);
            if (user != null) {
                finish(user, false);
                return;
            }
            MessagesStorage storage = MessagesStorage.getInstance(account);
            storage.getStorageQueue().postRunnable(() -> {
                TLRPC.User stored = storage.getUser(userId);
                AndroidUtilities.runOnUIThread(() -> {
                    if (done) {
                        return;
                    }
                    if (stored != null) {
                        controller.putUser(stored, true);
                        finish(stored, false);
                        return;
                    }
                    withBot();
                });
            });
        }

        private void withBot() {
            MessagesController controller = MessagesController.getInstance(account);
            TLObject bot = controller.getUserOrChat(BOT_USERNAME);
            if (bot instanceof TLRPC.User) {
                query((TLRPC.User) bot);
                return;
            }
            resolving = controller.getUserNameResolver().resolve(BOT_USERNAME, peerId -> {
                resolving = null;
                if (done) {
                    return;
                }
                TLRPC.User resolved = peerId != null && peerId > 0 && peerId != Long.MAX_VALUE ? controller.getUser(peerId) : null;
                if (resolved == null) {
                    finish(null, false);
                    return;
                }
                query(resolved);
            });
        }

        private void query(TLRPC.User bot) {
            MessagesController controller = MessagesController.getInstance(account);
            TLRPC.TL_messages_getInlineBotResults req = new TLRPC.TL_messages_getInlineBotResults();
            req.bot = controller.getInputUser(bot);
            req.peer = new TLRPC.TL_inputPeerEmpty();
            req.query = String.valueOf(userId);
            req.offset = "";
            reqId = ConnectionsManager.getInstance(account).sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
                reqId = 0;
                if (done) {
                    return;
                }
                Info info = response instanceof TLRPC.messages_BotResults ? parse((TLRPC.messages_BotResults) response, userId) : null;
                if (info == null) {
                    finish(null, false);
                    return;
                }
                if (info.needsStart) {
                    finish(null, true);
                    return;
                }
                if (info.user != null) {
                    ArrayList<TLRPC.User> users = new ArrayList<>();
                    users.add(info.user);
                    controller.putUsers(users, false);
                    MessagesStorage.getInstance(account).putUsersAndChats(users, null, true, true);
                    finish(controller.getUser(userId), false);
                    return;
                }
                if (TextUtils.isEmpty(info.username)) {
                    finish(null, false);
                    return;
                }
                resolving = controller.getUserNameResolver().resolve(info.username, peerId -> {
                    resolving = null;
                    if (done) {
                        return;
                    }
                    finish(peerId != null && peerId == userId ? controller.getUser(userId) : null, false);
                });
            }), ConnectionsManager.RequestFlagFailOnServerErrors);
        }

        private void finish(TLRPC.User user, boolean needsStart) {
            if (done) {
                return;
            }
            done = true;
            callback.run(user, needsStart);
        }

        void cancel() {
            if (done) {
                return;
            }
            done = true;
            if (reqId != 0) {
                ConnectionsManager.getInstance(account).cancelRequest(reqId, true);
                reqId = 0;
            }
            if (resolving != null) {
                resolving.run();
                resolving = null;
            }
        }
    }

    private static final class Session {

        private final Context context;
        private final int account;
        private final Theme.ResourcesProvider resourcesProvider;
        private final long userId;
        private final Utilities.Callback<TLRPC.User> onResult;
        private AlertDialog progress;
        private Runnable cancel;
        private boolean cancelled;

        Session(Context context, int account, Theme.ResourcesProvider resourcesProvider, long userId, Utilities.Callback<TLRPC.User> onResult) {
            this.context = context;
            this.account = account;
            this.resourcesProvider = resourcesProvider;
            this.userId = userId;
            this.onResult = onResult;
        }

        void run(int attempt) {
            if (cancelled) {
                return;
            }
            showProgress(300);
            cancel = find(account, userId, (user, needsStart) -> {
                if (cancelled) {
                    return;
                }
                if (needsStart && attempt == 0) {
                    dismissProgress();
                    askToStart();
                    return;
                }
                if (needsStart && attempt < START_RETRIES) {
                    AndroidUtilities.runOnUIThread(() -> run(attempt + 1), START_RETRY_DELAY);
                    return;
                }
                dismissProgress();
                onResult.run(user);
            });
        }

        private void showProgress(long delay) {
            if (progress != null) {
                return;
            }
            progress = new AlertDialog(context, AlertDialog.ALERT_TYPE_SPINNER, resourcesProvider);
            progress.setOnCancelListener(dialog -> {
                cancelled = true;
                if (cancel != null) {
                    cancel.run();
                }
            });
            if (delay > 0) {
                progress.showDelayed(delay);
            } else {
                progress.show();
            }
        }

        private void dismissProgress() {
            if (progress != null) {
                progress.dismiss();
                progress = null;
            }
        }

        private void askToStart() {
            new AlertDialog.Builder(context, resourcesProvider)
                    .setTitle(LocaleController.getString(R.string.OEUserLookupTitle))
                    .setMessage(AndroidUtilities.replaceTags(LocaleController.getString(R.string.OEUserLookupStart)))
                    .setPositiveButton(LocaleController.getString(R.string.OEUserLookupStartButton), (dialog, which) -> startBot())
                    .setNegativeButton(LocaleController.getString(R.string.Cancel), null)
                    .show();
        }

        private void startBot() {
            MessagesController controller = MessagesController.getInstance(account);
            TLObject bot = controller.getUserOrChat(BOT_USERNAME);
            if (!(bot instanceof TLRPC.User)) {
                onResult.run(null);
                return;
            }
            controller.sendBotStart((TLRPC.User) bot, "start");
            showProgress(0);
            AndroidUtilities.runOnUIThread(() -> run(1), START_RETRY_DELAY);
        }
    }
}
