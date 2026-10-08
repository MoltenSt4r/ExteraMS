package app.exteraless.appicons;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.graphics.ColorUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.CheckBox2;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.ScaleStateListAnimator;
import org.telegram.ui.LauncherIconController.LauncherIcon;

public class AppIconCell extends LinearLayout {

    private final Theme.ResourcesProvider resourcesProvider;
    private final FrameLayout previewContainer;
    private final IconPreviewView preview;
    private final CheckBox2 checkBox;
    private final TextView title;
    private final Paint selectionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float selection;
    private ValueAnimator selectionAnimator;

    public AppIconCell(Context context, Theme.ResourcesProvider resourcesProvider) {
        super(context);
        this.resourcesProvider = resourcesProvider;
        setWillNotDraw(false);
        setClipChildren(false);
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER_HORIZONTAL);
        setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(10), AndroidUtilities.dp(4), AndroidUtilities.dp(12));

        previewContainer = new FrameLayout(context);
        previewContainer.setClipChildren(false);
        preview = new IconPreviewView(context, 5);
        previewContainer.addView(preview, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        checkBox = new CheckBox2(context, 21, new Theme.ResourcesProvider() {
            @Override
            public int getColor(int key) {
                return key == Theme.key_windowBackgroundWhite ? selectionColor() : Theme.getColor(key, resourcesProvider);
            }
        });
        checkBox.setColor(Theme.key_featuredStickers_addButton, Theme.key_windowBackgroundWhite, Theme.key_checkboxCheck);
        checkBox.setDrawUnchecked(false);
        checkBox.setDrawBackgroundAsArc(4);
        checkBox.setProgressDelegate(progress -> preview.setPreviewScale(1f - progress * 0.08f));
        previewContainer.addView(checkBox, LayoutHelper.createFrame(24, 24, Gravity.RIGHT | Gravity.BOTTOM, 0, 0, 1, 1));
        addView(previewContainer, LayoutHelper.createLinear(68, 68, Gravity.CENTER_HORIZONTAL));

        title = new TextView(context);
        title.setMaxLines(2);
        title.setGravity(Gravity.CENTER);
        title.setEllipsize(TextUtils.TruncateAt.END);
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        title.setLineSpacing(0, 0.95f);
        title.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, resourcesProvider));
        addView(title, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 8, 0, 0));

        ScaleStateListAnimator.apply(this, 0.05f, 1.2f);
    }

    public LauncherIcon getIcon() {
        return preview.getIcon();
    }

    public void set(LauncherIcon icon, boolean checked, boolean animated) {
        preview.setIcon(icon, false);
        title.setText(AppIcons.title(icon));
        checkBox.setChecked(checked, animated);
        float to = checked ? 1f : 0f;
        if (selectionAnimator != null) {
            selectionAnimator.cancel();
            selectionAnimator = null;
        }
        if (!animated) {
            selection = to;
            invalidate();
            return;
        }
        if (selection == to) {
            return;
        }
        selectionAnimator = ValueAnimator.ofFloat(selection, to).setDuration(220);
        selectionAnimator.setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT);
        selectionAnimator.addUpdateListener(a -> {
            selection = (float) a.getAnimatedValue();
            invalidate();
        });
        selectionAnimator.start();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int size = Math.max(AndroidUtilities.dp(40), Math.min(AndroidUtilities.dp(68),
                MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft() - getPaddingRight()));
        LayoutParams params = (LayoutParams) previewContainer.getLayoutParams();
        if (params.width != size) {
            params.width = params.height = size;
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private int selectionColor() {
        return ColorUtils.blendARGB(Theme.getColor(Theme.key_windowBackgroundWhite, resourcesProvider),
                Theme.getColor(Theme.key_featuredStickers_addButton, resourcesProvider), selection * 0.07f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (selection <= 0f) {
            return;
        }
        selectionPaint.setColor(selectionColor());
        AndroidUtilities.rectTmp.set(AndroidUtilities.dp(2), AndroidUtilities.dp(2),
                getWidth() - AndroidUtilities.dp(2), getHeight() - AndroidUtilities.dp(2));
        canvas.drawRoundRect(AndroidUtilities.rectTmp, AndroidUtilities.dp(16), AndroidUtilities.dp(16), selectionPaint);
    }
}
