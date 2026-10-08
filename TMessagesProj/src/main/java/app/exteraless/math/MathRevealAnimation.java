package app.exteraless.math;

import android.graphics.Canvas;
import android.graphics.Color;
import android.os.SystemClock;
import android.text.Editable;
import android.text.Layout;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.CharacterStyle;
import android.text.style.UpdateAppearance;
import android.widget.TextView;

import androidx.core.graphics.ColorUtils;
import androidx.core.math.MathUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.Components.CubicBezierInterpolator;

final class MathRevealAnimation {

    private final class MaskSpan extends CharacterStyle implements UpdateAppearance {
        @Override
        public void updateDrawState(TextPaint tp) {
            if (maskSpan == this) {
                tp.setAlpha(0);
            }
        }
    }

    private final TextView view;
    private final Runnable onFinished;
    private final TextPaint paint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
    private MaskSpan maskSpan;
    private int rangeStart = -1;
    private int rangeEnd = -1;
    private long startedAt;
    private float duration = 200f;
    private float[] fromX = new float[0];
    private float[] fromY = new float[0];
    private float[] toX = new float[0];
    private float[] toY = new float[0];
    private Layout targetLayout;

    MathRevealAnimation(TextView view, Runnable onFinished) {
        this.view = view;
        this.onFinished = onFinished;
    }

    void begin(Editable text, int from, int count, float[] originX, float[] originY) {
        rangeStart = from;
        rangeEnd = from + count;
        fromX = originX;
        fromY = originY;
        toX = new float[count];
        toY = new float[count];
        targetLayout = null;
        startedAt = SystemClock.elapsedRealtime();
        duration = Math.min(420f, count * 28f + 200f);
        maskSpan = new MaskSpan();
        text.setSpan(maskSpan, rangeStart, rangeEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    boolean isRunning() {
        return rangeStart >= 0;
    }

    boolean isCaretOutside(int caret) {
        return caret < rangeStart || caret > rangeEnd;
    }

    void cancel() {
        stop(false);
    }

    private void removeMasks() {
        CharSequence text = view.getText();
        if (text instanceof Spannable) {
            Spannable spannable = (Spannable) text;
            for (MaskSpan span : spannable.getSpans(0, spannable.length(), MaskSpan.class)) {
                spannable.removeSpan(span);
            }
        }
    }

    private void stop(boolean deferred) {
        MaskSpan span = maskSpan;
        if (rangeStart < 0 && span == null) {
            return;
        }
        rangeStart = -1;
        rangeEnd = -1;
        maskSpan = null;
        targetLayout = null;
        if (span != null) {
            if (deferred) {
                AndroidUtilities.runOnUIThread(this::removeMasks);
            } else {
                removeMasks();
            }
        }
        onFinished.run();
        view.invalidate();
    }

    void draw(Canvas canvas, int accent) {
        Layout layout = view.getLayout();
        CharSequence text = view.getText();
        if (layout == null || text == null || rangeEnd > text.length()) {
            stop(true);
            return;
        }
        int count = rangeEnd - rangeStart;
        if (targetLayout != layout) {
            targetLayout = layout;
            for (int i = 0; i < count; i++) {
                int offset = rangeStart + i;
                toX[i] = layout.getPrimaryHorizontal(offset);
                toY[i] = layout.getLineBaseline(layout.getLineForOffset(offset));
            }
        }
        float elapsed = SystemClock.elapsedRealtime() - startedAt;
        float progress = MathUtils.clamp(elapsed / duration, 0f, 1f);
        float colorProgress = MathUtils.clamp(elapsed / 800f, 0f, 1f);
        int color = ColorUtils.blendARGB(accent, view.getCurrentTextColor(), CubicBezierInterpolator.EASE_BOTH.getInterpolation(colorProgress));
        int alpha = Color.alpha(color);
        float lift = view.getPaint().getTextSize() * 0.32f;
        paint.set(view.getPaint());
        paint.setColor(color);
        for (int i = 0; i < count; i++) {
            int offset = rangeStart + i;
            float t = AndroidUtilities.cascade(progress, i, count, 3.5f);
            float move = CubicBezierInterpolator.EASE_OUT_QUINT.getInterpolation(t);
            float pop = CubicBezierInterpolator.EASE_OUT_BACK.getInterpolation(t);
            float x = AndroidUtilities.lerp(fromX[i], toX[i], move);
            float y = AndroidUtilities.lerp(fromY[i], toY[i], move);
            float scale = AndroidUtilities.lerp(1.12f, 1f, pop);
            paint.setAlpha((int) (alpha * AndroidUtilities.lerp(0.4f, 1f, move)));
            int save = canvas.save();
            canvas.scale(scale, scale, x + paint.measureText(text, offset, offset + 1) / 2f, y - lift);
            canvas.drawText(text, offset, offset + 1, x, y, paint);
            canvas.restoreToCount(save);
        }
        if (progress < 1f || colorProgress < 1f) {
            view.invalidate();
        } else {
            stop(true);
        }
    }
}
