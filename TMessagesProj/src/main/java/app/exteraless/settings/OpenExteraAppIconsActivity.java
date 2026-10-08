package app.exteraless.settings;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.LocaleController.getString;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.AppIconsSelectorCell;
import org.telegram.ui.Components.Bulletin;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.LauncherIconController;

/**
 * Fullscreen App Icon picker matching Nothing OS / exteraGram styling
 * with smooth transitions, atmospheric glow, and full theme integration.
 */
public class OpenExteraAppIconsActivity extends BaseFragment {

    private final List<LauncherIconController.LauncherIcon> icons = new ArrayList<>();
    private LauncherIconController.LauncherIcon selectedIcon;

    private RecyclerListView listView;
    private IconsAdapter adapter;

    private AtmosphericHeaderLayout headerLayout;
    private AppIconsSelectorCell.AdaptiveIconImageView headerIconView;
    private TextView headerTitleView;
    private TextView headerAuthorView;

    private TextView selectButton;

    @Override
    public boolean onFragmentCreate() {
        icons.clear();
        for (LauncherIconController.LauncherIcon icon : LauncherIconController.LauncherIcon.values()) {
            icons.add(icon);
        }
        selectedIcon = LauncherIconController.getCurrentIcon();
        return super.onFragmentCreate();
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(getString(R.string.OEAppearanceAppIcon));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        FrameLayout contentView = new FrameLayout(context);
        contentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        // Header view with atmospheric radial glow behind the large preview
        headerLayout = new AtmosphericHeaderLayout(context);
        headerLayout.setOrientation(LinearLayout.VERTICAL);
        headerLayout.setGravity(Gravity.CENTER_HORIZONTAL);
        headerLayout.setPadding(0, dp(20), 0, dp(18));

        headerIconView = new AppIconsSelectorCell.AdaptiveIconImageView(context);
        headerIconView.setOuterPadding(dp(8));
        headerIconView.setBackgroundOuterPadding(0);
        headerLayout.addView(headerIconView, LayoutHelper.createLinear(110, 110, Gravity.CENTER_HORIZONTAL));

        headerTitleView = new TextView(context);
        headerTitleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        headerTitleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 22);
        headerTitleView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        headerTitleView.setGravity(Gravity.CENTER);
        headerLayout.addView(headerTitleView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 12, 0, 0));

        headerAuthorView = new TextView(context);
        headerAuthorView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
        headerAuthorView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        headerAuthorView.setGravity(Gravity.CENTER);
        headerLayout.addView(headerAuthorView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 4, 0, 0));

        updateHeader(selectedIcon, false);

        // RecyclerView with 4 columns
        listView = new RecyclerListView(context);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(context, 4);
        gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return position == 0 ? 4 : 1;
            }
        });
        listView.setLayoutManager(gridLayoutManager);
        listView.setClipToPadding(false);
        listView.setPadding(dp(12), dp(4), dp(12), dp(96));
        adapter = new IconsAdapter();
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((view, position) -> {
            if (position <= 0 || position > icons.size()) return;
            LauncherIconController.LauncherIcon icon = icons.get(position - 1);
            if (selectedIcon != icon) {
                selectedIcon = icon;
                updateHeader(selectedIcon, true);
                updateButtonState();
                adapter.notifyDataSetChanged();
            }
        });

        contentView.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        // Bottom gradient to smooth list scrolling under button
        View bottomGradient = new View(context);
        GradientDrawable gradient = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.TRANSPARENT, Theme.getColor(Theme.key_windowBackgroundGray)}
        );
        bottomGradient.setBackground(gradient);
        contentView.addView(bottomGradient, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 88, Gravity.BOTTOM));

        // Bottom floating pill button
        selectButton = new TextView(context);
        selectButton.setGravity(Gravity.CENTER);
        selectButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        selectButton.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        selectButton.setTextColor(Theme.getColor(Theme.key_featuredStickers_buttonText));
        selectButton.setBackground(Theme.createSimpleSelectorRoundRectDrawable(
                dp(24),
                Theme.getColor(Theme.key_featuredStickers_addButton),
                Theme.getColor(Theme.key_featuredStickers_addButton)
        ));
        updateButtonState();

        selectButton.setOnClickListener(v -> {
            LauncherIconController.LauncherIcon current = LauncherIconController.getCurrentIcon();
            if (selectedIcon != current) {
                LauncherIconController.setIcon(selectedIcon);
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.showBulletin, Bulletin.TYPE_APP_ICON, selectedIcon);
                updateButtonState();
                adapter.notifyDataSetChanged();
            } else {
                finishFragment();
            }
        });

        contentView.addView(selectButton, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 16, 0, 16, 16));

        fragmentView = contentView;
        return fragmentView;
    }

    private void updateHeader(LauncherIconController.LauncherIcon icon, boolean animate) {
        if (icon == null || headerIconView == null) return;
        headerIconView.setImageResource(icon.background);
        headerIconView.setIsNekoXIcon(icon.group == LauncherIconController.IconGroup.MOLTENGRAM);
        headerIconView.setForeground(icon.foreground);
        headerIconView.setAdaptiveIconMode(icon.group != LauncherIconController.IconGroup.TELEGRAM);

        if (animate) {
            headerIconView.setScaleX(0.88f);
            headerIconView.setScaleY(0.88f);
            headerIconView.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(240)
                    .setInterpolator(new OvershootInterpolator(1.3f))
                    .start();
        }

        headerTitleView.setText(getString(icon.title));
        headerAuthorView.setText(icon.author != null ? icon.author : "");

        if (headerLayout != null) {
            headerLayout.animateGlowTo(icon.glowColor);
        }
    }

    private void updateButtonState() {
        if (selectButton == null) return;
        boolean isCurrent = (selectedIcon == LauncherIconController.getCurrentIcon());
        selectButton.setText(isCurrent
                ? getString(R.string.OEAppearanceAppIconKeep)
                : getString(R.string.OEAppearanceAppIconSelect));
    }

    private class IconsAdapter extends RecyclerListView.SelectionAdapter {

        private static final int TYPE_HEADER = 0;
        private static final int TYPE_ICON = 1;

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return holder.getItemViewType() == TYPE_ICON;
        }

        @Override
        public int getItemCount() {
            return 1 + icons.size();
        }

        @Override
        public int getItemViewType(int position) {
            return position == 0 ? TYPE_HEADER : TYPE_ICON;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == TYPE_HEADER) {
                if (headerLayout.getParent() != null) {
                    ((ViewGroup) headerLayout.getParent()).removeView(headerLayout);
                }
                headerLayout.setLayoutParams(new RecyclerView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                ));
                return new RecyclerListView.Holder(headerLayout);
            } else {
                return new RecyclerListView.Holder(new IconGridCell(parent.getContext()));
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder.getItemViewType() == TYPE_ICON) {
                IconGridCell cell = (IconGridCell) holder.itemView;
                LauncherIconController.LauncherIcon icon = icons.get(position - 1);
                cell.bind(icon, icon == selectedIcon);
            }
        }
    }

    private static class IconGridCell extends FrameLayout {

        private final AppIconsSelectorCell.AdaptiveIconImageView iconView;
        private final ImageView checkmarkBadge;
        private final TextView titleView;
        private final View selectionBackground;

        public IconGridCell(@NonNull Context context) {
            super(context);
            setPadding(dp(4), dp(4), dp(4), dp(4));

            selectionBackground = new View(context);
            addView(selectionBackground, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

            FrameLayout iconContainer = new FrameLayout(context);

            iconView = new AppIconsSelectorCell.AdaptiveIconImageView(context);
            iconView.setOuterPadding(dp(5));
            iconView.setBackgroundOuterPadding(0);
            iconContainer.addView(iconView, LayoutHelper.createFrame(56, 56, Gravity.CENTER));

            checkmarkBadge = new ImageView(context);
            checkmarkBadge.setImageResource(R.drawable.round_check2);
            checkmarkBadge.setColorFilter(Theme.getColor(Theme.key_featuredStickers_addButton));
            iconContainer.addView(checkmarkBadge, LayoutHelper.createFrame(20, 20, Gravity.BOTTOM | Gravity.RIGHT, 0, 0, dp(2), dp(2)));

            addView(iconContainer, LayoutHelper.createFrame(62, 62, Gravity.CENTER_HORIZONTAL | Gravity.TOP, 0, 8, 0, 0));

            titleView = new TextView(context);
            titleView.setGravity(Gravity.CENTER);
            titleView.setMaxLines(1);
            titleView.setSingleLine(true);
            titleView.setEllipsize(TextUtils.TruncateAt.END);
            titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            titleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            addView(titleView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL | Gravity.TOP, 2, 74, 2, 8));
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(dp(100), MeasureSpec.EXACTLY));
        }

        public void bind(LauncherIconController.LauncherIcon icon, boolean isSelected) {
            iconView.setImageResource(icon.background);
            iconView.setIsNekoXIcon(icon.group == LauncherIconController.IconGroup.MOLTENGRAM);
            iconView.setForeground(icon.foreground);
            iconView.setAdaptiveIconMode(icon.group != LauncherIconController.IconGroup.TELEGRAM);

            titleView.setText(getString(icon.title));

            if (isSelected) {
                int accent = Theme.getColor(Theme.key_featuredStickers_addButton);
                int bgTint = Theme.multAlpha(accent, 0.16f);
                selectionBackground.setBackground(Theme.createRoundRectDrawable(dp(16), bgTint));
                checkmarkBadge.setVisibility(VISIBLE);
            } else {
                selectionBackground.setBackground(Theme.createSimpleSelectorRoundRectDrawable(
                        dp(16),
                        Color.TRANSPARENT,
                        Theme.getColor(Theme.key_listSelector)
                ));
                checkmarkBadge.setVisibility(GONE);
            }
        }
    }

    private static class AtmosphericHeaderLayout extends LinearLayout {
        private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private int currentGlowColor = 0;
        private ValueAnimator glowAnimator;

        public AtmosphericHeaderLayout(Context context) {
            super(context);
            setWillNotDraw(false);
        }

        public void animateGlowTo(int targetGlowColor) {
            int alpha = Theme.isCurrentThemeDark() ? 0x48 : 0x22;
            int tintedTarget = ColorUtils.setAlphaComponent(targetGlowColor, alpha);
            if (currentGlowColor == 0) {
                currentGlowColor = tintedTarget;
                updateShader();
                invalidate();
                return;
            }
            if (glowAnimator != null) {
                glowAnimator.cancel();
            }
            glowAnimator = ValueAnimator.ofObject(new ArgbEvaluator(), currentGlowColor, tintedTarget);
            glowAnimator.setDuration(260);
            glowAnimator.addUpdateListener(animation -> {
                currentGlowColor = (int) animation.getAnimatedValue();
                updateShader();
                invalidate();
            });
            glowAnimator.start();
        }

        private void updateShader() {
            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) return;

            float cx = w / 2f;
            float cy = dp(75);
            float radius = w * 0.75f;

            glowPaint.setShader(new RadialGradient(
                    cx, cy, radius,
                    new int[]{currentGlowColor, Color.TRANSPARENT},
                    new float[]{0f, 1f},
                    Shader.TileMode.CLAMP
            ));
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            updateShader();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (currentGlowColor != 0) {
                canvas.drawRect(0, 0, getWidth(), getHeight(), glowPaint);
            }
        }
    }
}
