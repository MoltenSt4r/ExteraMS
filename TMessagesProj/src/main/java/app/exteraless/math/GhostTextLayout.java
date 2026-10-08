package app.exteraless.math;

import android.graphics.Canvas;
import android.os.Build;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.StaticLayout;
import android.text.TextDirectionHeuristics;
import android.text.TextPaint;
import android.text.style.CharacterStyle;
import android.text.style.UpdateAppearance;
import android.widget.TextView;

import org.telegram.messenger.FileLog;

final class GhostTextLayout {

    private static final class GhostAlphaSpan extends CharacterStyle implements UpdateAppearance {
        private float alpha = 1f;

        @Override
        public void updateDrawState(TextPaint tp) {
            tp.setAlpha((int) (tp.getAlpha() * alpha));
        }
    }

    private final GhostAlphaSpan ghostAlpha = new GhostAlphaSpan();
    private StaticLayout layout;
    private int insertOffset;
    private float drawTop;
    private boolean movedText;
    private boolean detached;
    private int paragraphStart;
    private int paragraphEnd;
    private int extraHeight;
    private float cursorShiftX;
    private float cursorShiftY;

    private StaticLayout newLayout(TextView view, Layout real, CharSequence text, int width) {
        try {
            StaticLayout.Builder builder = StaticLayout.Builder.obtain(text, 0, text.length(), view.getPaint(), width)
                    .setAlignment(real.getAlignment())
                    .setLineSpacing(view.getLineSpacingExtra(), view.getLineSpacingMultiplier())
                    .setIncludePad(view.getIncludeFontPadding())
                    .setBreakStrategy(view.getBreakStrategy())
                    .setHyphenationFrequency(view.getHyphenationFrequency())
                    .setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_LTR);
            if (Build.VERSION.SDK_INT >= 26) {
                builder.setJustificationMode(view.getJustificationMode());
            }
            if (Build.VERSION.SDK_INT >= 28) {
                builder.setUseLineSpacingFromFallbacks(view.isFallbackLineSpacing());
            }
            return builder.build();
        } catch (Exception e) {
            FileLog.e(e);
            return null;
        }
    }

    private boolean moved(Layout real, StaticLayout shadow, int realOffset, int shadowOffset) {
        return Math.abs(shadow.getPrimaryHorizontal(shadowOffset) - real.getPrimaryHorizontal(realOffset)) >= 0.5f
                || Math.abs(shadow.getLineTop(shadow.getLineForOffset(shadowOffset)) - (real.getLineTop(real.getLineForOffset(realOffset)) - drawTop)) >= 0.5f;
    }

    boolean build(TextView view, Layout real, int start, int end, int caret, CharSequence insert) {
        clear();
        CharSequence text = view.getText();
        int width = real.getWidth();
        if (text == null || start < 0 || end > text.length() || caret < start || caret > end || width <= 0) {
            return false;
        }
        int offset = caret - start;
        SpannableStringBuilder builder = new SpannableStringBuilder(text, start, end);
        builder.insert(offset, insert);
        builder.setSpan(ghostAlpha, offset, offset + insert.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        StaticLayout shadow = newLayout(view, real, builder, width);
        if (shadow == null) {
            return false;
        }
        int firstLine = real.getLineForOffset(start);
        int lastLine = real.getLineForOffset(end);
        layout = shadow;
        insertOffset = offset;
        paragraphStart = start;
        paragraphEnd = end;
        drawTop = real.getLineTop(firstLine);
        extraHeight = Math.max(0, shadow.getHeight() - (real.getLineTop(lastLine + 1) - real.getLineTop(firstLine)));
        movedText = offset > 0 && moved(real, shadow, start + offset - 1, offset - 1);
        cursorShiftX = shadow.getPrimaryHorizontal(offset) - real.getPrimaryHorizontal(caret);
        cursorShiftY = shadow.getLineTop(shadow.getLineForOffset(offset)) + drawTop - real.getLineTop(real.getLineForOffset(caret));
        return true;
    }

    boolean buildDetached(TextView view, Layout real, int end, CharSequence insert) {
        clear();
        int width = real.getWidth();
        if (width <= 0) {
            return false;
        }
        SpannableStringBuilder builder = new SpannableStringBuilder(insert);
        builder.setSpan(ghostAlpha, 0, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        StaticLayout shadow = newLayout(view, real, builder, width);
        if (shadow == null) {
            return false;
        }
        layout = shadow;
        detached = true;
        drawTop = real.getLineBottom(real.getLineForOffset(end));
        extraHeight = shadow.getHeight();
        return true;
    }

    void clear() {
        layout = null;
        insertOffset = 0;
        drawTop = 0;
        movedText = false;
        detached = false;
        paragraphStart = 0;
        paragraphEnd = 0;
        extraHeight = 0;
        cursorShiftX = 0;
        cursorShiftY = 0;
    }

    void draw(Canvas canvas) {
        if (layout == null) {
            return;
        }
        canvas.save();
        canvas.translate(0, drawTop);
        layout.draw(canvas);
        canvas.restore();
    }

    void readInsertedPositions(int from, int count, float[] x, float[] y) {
        if (layout == null) {
            return;
        }
        for (int i = 0; i < count; i++) {
            int offset = insertOffset + from + i;
            x[i] = layout.getPrimaryHorizontal(offset);
            y[i] = drawTop + layout.getLineBaseline(layout.getLineForOffset(offset));
        }
    }

    void setAlpha(float alpha) {
        ghostAlpha.alpha = alpha;
    }

    boolean isEmpty() {
        return layout == null;
    }

    boolean isDetached() {
        return detached;
    }

    boolean hasMovedText() {
        return movedText;
    }

    int getExtraHeight() {
        return extraHeight;
    }

    int getParagraphStart() {
        return paragraphStart;
    }

    int getParagraphEnd() {
        return paragraphEnd;
    }

    float getCursorShiftX() {
        return cursorShiftX;
    }

    float getCursorShiftY() {
        return cursorShiftY;
    }
}
