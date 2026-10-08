package app.exteraless.math;

import android.text.TextUtils;
import android.util.LruCache;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MessagesController;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class CalcmulaCurrency {

    private static final String BOT_USERNAME = "calcmulabot";
    private static final String FAILED = "";

    private static final Set<String> CODES = new HashSet<>(Arrays.asList(
            "usd", "eur", "rub", "uah", "kzt", "byn", "gbp", "jpy", "cny", "try", "inr", "brl", "cad",
            "aud", "chf", "pln", "czk", "sek", "nok", "dkk", "huf", "ron", "bgn", "gel", "amd", "azn",
            "uzs", "kgs", "tjs", "mdl", "ils", "aed", "sar", "krw", "hkd", "sgd", "thb", "vnd", "idr",
            "myr", "php", "mxn", "ars", "clp", "cop", "pen", "nzd", "zar", "egp", "rsd", "isk",
            "ton", "btc", "eth", "usdt", "usdc", "sol", "bnb", "trx", "ltc", "xrp", "doge", "not", "dogs"
    ));

    private static final String SYMBOLS = "$€₽£¥₴₸₺₹₿₩";

    private static final LruCache<String, String> RESULTS = new LruCache<>(64);
    private static final Set<String> PENDING = new HashSet<>();

    private CalcmulaCurrency() {
    }

    private static boolean isCurrencySymbol(char c) {
        return SYMBOLS.indexOf(c) >= 0;
    }

    private static boolean isQueryChar(char c) {
        return Character.isLetterOrDigit(c) || c == '.' || c == ',' || MathExpression.isBlank(c)
                || MathExpression.isSymbolChar(c) || isCurrencySymbol(c);
    }

    private static boolean mentionsCurrency(String query) {
        boolean digit = false;
        boolean currency = false;
        int wordStart = -1;
        for (int i = 0; i <= query.length(); i++) {
            char c = i < query.length() ? query.charAt(i) : ' ';
            if (Character.isDigit(c)) {
                digit = true;
            }
            if (isCurrencySymbol(c)) {
                currency = true;
            }
            if (Character.isLetter(c)) {
                if (wordStart < 0) {
                    wordStart = i;
                }
            } else if (wordStart >= 0) {
                if (CODES.contains(query.substring(wordStart, i).toLowerCase(Locale.ROOT))) {
                    currency = true;
                }
                wordStart = -1;
            }
        }
        return digit && currency;
    }

    public static String queryAt(CharSequence text, int caret) {
        int equalsIndex = MathExpression.equalsIndexAt(text, caret);
        if (equalsIndex < 0) {
            return null;
        }
        int limit = Math.max(0, equalsIndex - 64);
        int start = equalsIndex - 1;
        while (start >= limit && isQueryChar(text.charAt(start))) {
            start--;
        }
        start++;
        if (start > 0 && Character.isLetterOrDigit(text.charAt(start - 1))) {
            return null;
        }
        while (start >= 0 && start < equalsIndex) {
            String query = text.subSequence(start, equalsIndex).toString().trim();
            if (!query.isEmpty()) {
                char first = query.charAt(0);
                if ((Character.isDigit(first) || isCurrencySymbol(first)) && mentionsCurrency(query)) {
                    return query;
                }
            }
            start = MathExpression.nextCandidate(text, start, equalsIndex);
        }
        return null;
    }

    public static MathExpression.Suggestion suggestionAt(CharSequence text, int caret, String query) {
        String value = RESULTS.get(query);
        if (TextUtils.isEmpty(value)) {
            return null;
        }
        return new MathExpression.Suggestion(caret, MathExpression.insertTextFor(text, caret, value), value);
    }

    public static String resultFor(String query) {
        String value = query == null ? null : RESULTS.get(query);
        return TextUtils.isEmpty(value) ? null : value;
    }

    public static boolean isKnown(String query) {
        return RESULTS.get(query) != null;
    }

    public static void request(int account, String query, Runnable onResult) {
        if (query == null || isKnown(query) || !PENDING.add(query)) {
            return;
        }
        MessagesController controller = MessagesController.getInstance(account);
        TLObject bot = controller.getUserOrChat(BOT_USERNAME);
        if (bot instanceof TLRPC.User) {
            query(account, (TLRPC.User) bot, query, onResult);
            return;
        }
        TLRPC.TL_contacts_resolveUsername req = new TLRPC.TL_contacts_resolveUsername();
        req.username = BOT_USERNAME;
        ConnectionsManager.getInstance(account).sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
            TLRPC.User user = null;
            if (response instanceof TLRPC.TL_contacts_resolvedPeer) {
                TLRPC.TL_contacts_resolvedPeer resolved = (TLRPC.TL_contacts_resolvedPeer) response;
                controller.putUsers(resolved.users, false);
                controller.putChats(resolved.chats, false);
                for (TLRPC.User candidate : resolved.users) {
                    if (candidate != null && BOT_USERNAME.equalsIgnoreCase(candidate.username)) {
                        user = candidate;
                    }
                }
            }
            if (user == null) {
                PENDING.remove(query);
                RESULTS.put(query, FAILED);
                return;
            }
            query(account, user, query, onResult);
        }));
    }

    private static void query(int account, TLRPC.User bot, String query, Runnable onResult) {
        TLRPC.TL_messages_getInlineBotResults req = new TLRPC.TL_messages_getInlineBotResults();
        req.bot = MessagesController.getInstance(account).getInputUser(bot);
        req.peer = new TLRPC.TL_inputPeerSelf();
        req.query = query;
        req.offset = "";
        ConnectionsManager.getInstance(account).sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
            PENDING.remove(query);
            String value = FAILED;
            if (error == null && response instanceof TLRPC.messages_BotResults) {
                TLRPC.messages_BotResults results = (TLRPC.messages_BotResults) response;
                if (!results.results.isEmpty()) {
                    String title = results.results.get(0).title;
                    if (title != null && title.startsWith("=")) {
                        value = title.substring(1).trim();
                    }
                }
            }
            RESULTS.put(query, value);
            if (onResult != null && !value.isEmpty()) {
                onResult.run();
            }
        }));
    }
}
