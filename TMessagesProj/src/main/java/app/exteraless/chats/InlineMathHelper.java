package app.exteraless.chats;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InlineMathHelper {

    private static final Pattern MATH_PATTERN = Pattern.compile("(?:^|[\\s,;:\\n])([0-9.\\s+\\-*/^()%]+)=\\s*$");

    public static void attach(EditText editText) {
        if (editText == null) return;
        editText.addTextChangedListener(new TextWatcher() {
            private boolean selfChange = false;
            private boolean spaceJustTyped = false;
            private int typedStart = -1;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (selfChange) return;
                spaceJustTyped = (count == 0 && after == 1);
                typedStart = start;
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (selfChange || !spaceJustTyped) return;
                if (start >= 0 && start < s.length() && s.charAt(start) == ' ') {
                    spaceJustTyped = true;
                } else {
                    spaceJustTyped = false;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (selfChange) return;
                if (!spaceJustTyped || typedStart < 0) return;
                if (!ChatsConfig.mathResults.Bool()) return;

                String textBefore = s.subSequence(0, typedStart).toString();
                Matcher matcher = MATH_PATTERN.matcher(textBefore);
                if (matcher.find()) {
                    String expr = matcher.group(1);
                    String result = evaluate(expr);
                    if (result != null) {
                        selfChange = true;
                        try {
                            s.insert(typedStart, result);
                        } finally {
                            selfChange = false;
                        }
                    }
                }
            }
        });
    }

    public static String evaluate(String expr) {
        if (expr == null) return null;
        expr = expr.trim();
        if (expr.isEmpty()) return null;

        boolean hasOperator = false;
        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);
            if (c == '+' || c == '-' || c == '*' || c == '/' || c == '^' || c == '%') {
                hasOperator = true;
                break;
            }
        }
        if (!hasOperator) return null;

        try {
            Double val = parseExpression(expr);
            if (val == null || Double.isNaN(val) || Double.isInfinite(val)) return null;
            if (Math.abs(val) > 1e14) return null;

            if (val == Math.floor(val) && !Double.isInfinite(val)) {
                long longVal = val.longValue();
                return String.valueOf(longVal);
            } else {
                String formatted = String.format(Locale.US, "%.6f", val);
                while (formatted.endsWith("0")) {
                    formatted = formatted.substring(0, formatted.length() - 1);
                }
                if (formatted.endsWith(".")) {
                    formatted = formatted.substring(0, formatted.length() - 1);
                }
                return formatted;
            }
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Double parseExpression(final String str) {
        return new Object() {
            int pos = -1, ch;

            void nextChar() {
                ch = (++pos < str.length()) ? str.charAt(pos) : -1;
            }

            boolean eat(int charToEat) {
                while (ch == ' ') nextChar();
                if (ch == charToEat) {
                    nextChar();
                    return true;
                }
                return false;
            }

            Double parse() {
                nextChar();
                double x = parseExpr();
                while (ch == ' ') nextChar();
                if (pos < str.length()) return null;
                return x;
            }

            double parseExpr() {
                double x = parseTerm();
                for (;;) {
                    if      (eat('+')) x += parseTerm();
                    else if (eat('-')) x -= parseTerm();
                    else return x;
                }
            }

            double parseTerm() {
                double x = parseFactor();
                for (;;) {
                    if      (eat('*')) x *= parseFactor();
                    else if (eat('/')) {
                        double d = parseFactor();
                        if (d == 0) throw new ArithmeticException("div0");
                        x /= d;
                    }
                    else if (eat('%')) {
                        double d = parseFactor();
                        if (d == 0) throw new ArithmeticException("div0");
                        x %= d;
                    }
                    else return x;
                }
            }

            double parseFactor() {
                if (eat('+')) return parseFactor();
                if (eat('-')) return -parseFactor();

                double x;
                int startPos = this.pos;
                if (eat('(')) {
                    x = parseExpr();
                    if (!eat(')')) throw new RuntimeException("missing )");
                } else if ((ch >= '0' && ch <= '9') || ch == '.') {
                    while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                    x = Double.parseDouble(str.substring(startPos, this.pos));
                } else {
                    throw new RuntimeException("unexpected " + (char) ch);
                }

                if (eat('^')) x = Math.pow(x, parseFactor());

                return x;
            }
        }.parse();
    }
}
