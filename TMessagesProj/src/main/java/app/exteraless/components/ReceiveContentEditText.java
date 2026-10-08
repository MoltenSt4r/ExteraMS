package app.exteraless.components;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Rect;
import android.os.Build;
import android.util.AttributeSet;
import android.text.Editable;
import android.text.Selection;
import android.view.DragEvent;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.EditText;

import androidx.core.view.ContentInfoCompat;
import androidx.core.view.OnReceiveContentViewBehavior;
import androidx.core.view.ViewCompat;
import androidx.core.view.inputmethod.EditorInfoCompat;
import androidx.core.view.inputmethod.InputConnectionCompat;
import androidx.core.widget.TextViewOnReceiveContentListener;

import app.exteraless.math.InlineMathController;

/**
 *
 * Повторяет приёмник контента из AppCompatEditText, но без зависимости от appcompat:
 * поле само реализует {@link OnReceiveContentViewBehavior}, поэтому любой
 * {@code ViewCompat.setOnReceiveContentListener} на нём начинает работать сразу для трёх
 * источников — клавиатуры (commitContent), меню «Вставить» и drag&drop. Без этого класса
 * androidx-приёмник молчит: платформенный TextView умеет отдавать ему контент только с API 31.
 */
@SuppressLint({"RestrictedApi", "AppCompatCustomView"})
public abstract class ReceiveContentEditText extends EditText implements OnReceiveContentViewBehavior {

    private final TextViewOnReceiveContentListener defaultOnReceiveContentListener;
    private InlineMathController inlineMath;

    public ReceiveContentEditText(Context context) {
        super(context);
        defaultOnReceiveContentListener = new TextViewOnReceiveContentListener();
    }

    public ReceiveContentEditText(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        defaultOnReceiveContentListener = new TextViewOnReceiveContentListener();
    }

    /** Приём drop'а на API 24..30, где платформа этого не делает. */
    public static final class OnDropApi24Impl {
        public static boolean onDropForTextView(DragEvent dragEvent, ReceiveContentEditText view, Activity activity) {
            activity.requestDragAndDropPermissions(dragEvent);
            final int offset = view.getOffsetForPosition(dragEvent.getX(), dragEvent.getY());
            view.beginBatchEdit();
            try {
                // курсор ставится туда, куда бросили, и уже в эту позицию вставляется содержимое
                Selection.setSelection(view.getText(), offset);
                ViewCompat.performReceiveContent(view, new ContentInfoCompat.Builder(dragEvent.getClipData(), ContentInfoCompat.SOURCE_DRAG_AND_DROP).build());
                return true;
            } finally {
                view.endBatchEdit();
            }
        }
    }

    private Activity findActivity() {
        for (Context context = getContext(); context instanceof ContextWrapper; context = ((ContextWrapper) context).getBaseContext()) {
            if (context instanceof Activity) {
                return (Activity) context;
            }
        }
        return null;
    }

    private boolean handleDragEventViaReceiveContent(DragEvent dragEvent) {
        // С API 31 drop разбирает сама платформа
        if (Build.VERSION.SDK_INT >= 31 || dragEvent.getLocalState() != null || ViewCompat.getOnReceiveContentMimeTypes(this) == null) {
            return false;
        }
        if (dragEvent.getAction() != DragEvent.ACTION_DROP) {
            return false;
        }
        final Activity activity = findActivity();
        if (activity == null) {
            return false;
        }
        return OnDropApi24Impl.onDropForTextView(dragEvent, this, activity);
    }

    private boolean handleMenuActionViaReceiveContent(int id) {
        // Paste и pasteAsPlainText уходят в приёмник целиком,
        // включая ClipData с URI, которые обычный TextView просто проигнорировал бы
        if (ViewCompat.getOnReceiveContentMimeTypes(this) == null || (id != android.R.id.paste && id != android.R.id.pasteAsPlainText)) {
            return false;
        }
        final ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        final ClipData clip = clipboard == null ? null : clipboard.getPrimaryClip();
        if (clip != null && clip.getItemCount() > 0) {
            ViewCompat.performReceiveContent(this, new ContentInfoCompat.Builder(clip, ContentInfoCompat.SOURCE_CLIPBOARD)
                    .setFlags(id == android.R.id.paste ? 0 : ContentInfoCompat.FLAG_CONVERT_TO_PLAIN_TEXT)
                    .build());
        }
        return true;
    }

    @Override
    public Editable getText() {
        // до API 28 EditText.getText() падает кастом, если setText ещё не звали (как в AppCompatEditText)
        if (Build.VERSION.SDK_INT >= 28) {
            return super.getText();
        }
        return super.getEditableText();
    }

    public void setInlineMath(InlineMathController controller) {
        inlineMath = controller;
        requestLayout();
    }

    public InlineMathController getInlineMath() {
        return inlineMath;
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo editorInfo) {
        final InputConnection ic = createReceivingInputConnection(editorInfo);
        return ic == null || inlineMath == null ? ic : inlineMath.wrap(ic);
    }

    private InputConnection createReceivingInputConnection(EditorInfo editorInfo) {
        final InputConnection ic = super.onCreateInputConnection(editorInfo);
        if (ic == null) {
            return null;
        }
        final String[] mimeTypes = ViewCompat.getOnReceiveContentMimeTypes(this);
        if (mimeTypes == null) {
            return ic;
        }
        EditorInfoCompat.setContentMimeTypes(editorInfo, mimeTypes);
        if (Build.VERSION.SDK_INT > 30) {
            return ic;
        }
        return InputConnectionCompat.createWrapper(this, ic, editorInfo);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (inlineMath != null && inlineMath.onKeyEvent(event)) {
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public int getCompoundPaddingBottom() {
        return super.getCompoundPaddingBottom() + (inlineMath != null ? inlineMath.getExtraBottom() : 0);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (inlineMath != null && inlineMath.updateOnMeasure()) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    @Override
    protected void onSelectionChanged(int selStart, int selEnd) {
        super.onSelectionChanged(selStart, selEnd);
        if (inlineMath != null) {
            inlineMath.invalidateState();
        }
    }

    @Override
    protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter);
        if (inlineMath != null) {
            inlineMath.onTextChanged();
        }
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
        if (inlineMath != null) {
            inlineMath.onFocusChanged(focused);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (inlineMath != null && event.getAction() == MotionEvent.ACTION_DOWN) {
            inlineMath.onTouchDown();
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (inlineMath != null) {
            inlineMath.cancel();
        }
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        super.setText(text, type);
        if (inlineMath != null) {
            inlineMath.cancel();
        }
    }

    @Override
    public boolean onDragEvent(DragEvent dragEvent) {
        if (handleDragEventViaReceiveContent(dragEvent)) {
            return true;
        }
        return super.onDragEvent(dragEvent);
    }

    @Override
    public ContentInfoCompat onReceiveContent(ContentInfoCompat payload) {
        // то, что приёмник не забрал (обычный текст), вставляет штатное поведение TextView
        return defaultOnReceiveContentListener.onReceiveContent(this, payload);
    }

    @Override
    public boolean onTextContextMenuItem(int id) {
        if (handleMenuActionViaReceiveContent(id)) {
            return true;
        }
        return super.onTextContextMenuItem(id);
    }
}
