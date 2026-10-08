package app.exteraless.appicons;

import android.content.Context;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.LinearLayout;

import androidx.core.graphics.ColorUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AnimatedTextView;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.LauncherIconController.LauncherIcon;

public class AppIconHeroView extends LinearLayout {

    public static final int PREVIEW_DP = 128;
    private static final int COLLAPSED_DP = 40;

    private final Theme.ResourcesProvider resourcesProvider;
    private final IconPreviewView preview;
    private final AnimatedTextView title;
    private final AnimatedTextView subtitle;
    private LauncherIcon icon;
    private float collapse;

    public AppIconHeroView(Context context, Theme.ResourcesProvider resourcesProvider) {
        super(context);
        this.resourcesProvider = resourcesProvider;
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER_HORIZONTAL);
        setClipChildren(false);
        setClipToPadding(false);

        preview = new IconPreviewView(context, 0);
        addView(preview, LayoutHelper.createLinear(PREVIEW_DP, PREVIEW_DP, Gravity.CENTER_HORIZONTAL));
        title = addText(22, true, 28, 18);
        subtitle = AppIcons.hasDescriptions() ? addText(14, false, 20, 6) : null;
        updateColors();
    }

    private AnimatedTextView addText(int sizeDp, boolean bold, int heightDp, int topDp) {
        AnimatedTextView view = new AnimatedTextView(getContext(), true, true, false);
        view.setGravity(Gravity.CENTER);
        view.setTypeface(bold ? AndroidUtilities.bold() : null);
        view.setTextSize(AndroidUtilities.dp(sizeDp));
        view.setIncludeFontPadding(false);
        view.setAllowCancel(true);
        view.setAnimationProperties(0.35f, 0, 260, CubicBezierInterpolator.EASE_OUT_QUINT);
        addView(view, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, heightDp, Gravity.CENTER_HORIZONTAL, 24, topDp, 24, 0));
        return view;
    }

    public void updateColors() {
        int color = Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, resourcesProvider);
        title.setTextColor(color);
        if (subtitle != null) {
            subtitle.setTextColor(ColorUtils.setAlphaComponent(color, 179));
        }
    }

    public void set(LauncherIcon icon) {
        if (this.icon == icon) {
            return;
        }
        boolean animated = this.icon != null;
        this.icon = icon;
        preview.setIcon(icon, animated);
        title.setText(AppIcons.title(icon), animated);
        if (subtitle != null) {
            CharSequence description = AppIcons.description(icon);
            subtitle.setText(TextUtils.isEmpty(description) ? "" : description, animated);
        }
    }

    public void setPreviewSizeDp(int dp) {
        LayoutParams params = (LayoutParams) preview.getLayoutParams();
        int size = AndroidUtilities.dp(dp);
        if (params.width != size) {
            params.width = params.height = size;
            preview.requestLayout();
        }
    }

    public int getTextBlockHeight() {
        return AndroidUtilities.dp(subtitle != null ? 46 + 26 : 46);
    }

    public float previewCenterY() {
        return getTop() + preview.getTop() + preview.getHeight() / 2f;
    }

    public void setCollapse(float collapse) {
        if (this.collapse != collapse) {
            this.collapse = collapse;
            applyCollapse();
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        applyCollapse();
    }

    private void applyCollapse() {
        float text = Math.min(1f, collapse / 0.35f);
        title.setAlpha(1f - text);
        title.setTranslationY(-AndroidUtilities.dp(12) * text);
        if (subtitle != null) {
            subtitle.setAlpha(1f - text);
            subtitle.setTranslationY(-AndroidUtilities.dp(12) * text);
        }
        int height = preview.getMeasuredHeight();
        if (height <= 0) {
            return;
        }
        float scale = AndroidUtilities.lerp(1f, AndroidUtilities.dp(COLLAPSED_DP) / (float) height, collapse);
        preview.setPivotX(preview.getMeasuredWidth() / 2f);
        preview.setPivotY(height / 2f);
        preview.setScaleX(scale);
        preview.setScaleY(scale);
        float target = AndroidUtilities.statusBarHeight + ActionBar.getCurrentActionBarHeight() / 2f - getTop();
        preview.setTranslationY((target - (preview.getTop() + height / 2f)) * collapse);
    }

    public void invalidateShape() {
        preview.invalidateShape();
    }
}
