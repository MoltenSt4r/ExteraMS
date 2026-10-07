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
import android.view.animation.AccelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.AppIconsSelectorCell;
import org.telegram.ui.Components.Bulletin;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.LauncherIconController;

/**
 * Fullscreen App Icon picker matching Nothing Phone / exteraGram launcher icon selection screen.
 */
public class OpenExteraAppIconsActivity extends BaseFragment {

    private final List<LauncherIconController.LauncherIcon> icons = new ArrayList<>();
    private LauncherIconController.LauncherIcon selectedIcon;

    private AtmosphericBackgroundView backgroundView;
    private FrameLayout headerContainer;
    private FrameLayout headerIconContainer;
    private AppIconsSelectorCell.AdaptiveIconImageView headerIconCurrent;
    private AppIconsSelectorCell.AdaptiveIconImageView headerIconNext;

    private FrameLayout headerTextSwitcher;
    private LinearLayout textBlockCurrent;
    private LinearLayout textBlockNext;
    private TextView titleCurrent;
    private TextView subtitleCurrent;
    private TextView authorCurrent;
    private TextView titleNext;
    private TextView subtitleNext;
    private TextView authorNext;

    private FrameLayout cardContainer;
    private RecyclerListView listView;
    private IconsAdapter adapter;

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
        actionBar.setAddToContainer(false); // We use a custom floating circular back button

        FrameLayout contentView = new FrameLayout(context);

        // 1. Dynamic Atmospheric Glow Background
        int initialGlow = selectedIcon != null ? selectedIcon.glowColor : 0xFF601816;
        backgroundView = new AtmosphericBackgroundView(context, initialGlow);
        contentView.addView(backgroundView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        // 2. Header: Big Preview Icon (110dp) + Title, Description, Author
        headerContainer = new FrameLayout(context);

        // Large Preview Icon with smooth crossfade
        headerIconContainer = new FrameLayout(context);

        headerIconCurrent = new AppIconsSelectorCell.AdaptiveIconImageView(context);
        headerIconCurrent.setOuterPadding(dp(8));
        headerIconCurrent.setBackgroundOuterPadding(dp(36));
        headerIconContainer.addView(headerIconCurrent, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        headerIconNext = new AppIconsSelectorCell.AdaptiveIconImageView(context);
        headerIconNext.setOuterPadding(dp(8));
        headerIconNext.setBackgroundOuterPadding(dp(36));
        headerIconNext.setVisibility(View.GONE);
        headerIconContainer.addView(headerIconNext, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        bindIconToView(headerIconCurrent, selectedIcon);

        headerContainer.addView(headerIconContainer, LayoutHelper.createFrame(110, 110, Gravity.CENTER_HORIZONTAL | Gravity.TOP, 0, 52, 0, 0));

        // Text Switcher Container
        headerTextSwitcher = new FrameLayout(context);

        textBlockCurrent = createTextBlock(context);
        titleCurrent = (TextView) textBlockCurrent.getChildAt(0);
        subtitleCurrent = (TextView) textBlockCurrent.getChildAt(1);
        authorCurrent = (TextView) textBlockCurrent.getChildAt(2);
        headerTextSwitcher.addView(textBlockCurrent, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        textBlockNext = createTextBlock(context);
        titleNext = (TextView) textBlockNext.getChildAt(0);
        subtitleNext = (TextView) textBlockNext.getChildAt(1);
        authorNext = (TextView) textBlockNext.getChildAt(2);
        textBlockNext.setVisibility(View.GONE);
        headerTextSwitcher.addView(textBlockNext, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        bindText(titleCurrent, subtitleCurrent, authorCurrent, selectedIcon);

        headerContainer.addView(headerTextSwitcher, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP, 16, 172, 16, 0));

        contentView.addView(headerContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 246, Gravity.TOP));

        // 3. Lower Rounded Sheet / Card Container (top radius 28dp)
        cardContainer = new FrameLayout(context);
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setCornerRadii(new float[]{dp(28), dp(28), dp(28), dp(28), 0, 0, 0, 0});
        cardBg.setColor(0xFF131D1B);
        cardContainer.setBackground(cardBg);

        // 4. Grid of Icons (4 columns)
        listView = new RecyclerListView(context);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(context, 4);
        listView.setLayoutManager(gridLayoutManager);
        listView.setClipToPadding(false);
        listView.setPadding(dp(10), dp(16), dp(10), dp(96));
        adapter = new IconsAdapter();
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((view, position) -> {
            if (position < 0 || position >= icons.size()) return;
            LauncherIconController.LauncherIcon newIcon = icons.get(position);
            if (newIcon != selectedIcon) {
                switchIcon(newIcon, position);
            }
        });

        // Parallax / Collapse on scroll
        listView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            private int totalScroll = 0;

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                totalScroll += dy;
                if (totalScroll < 0) totalScroll = 0;
                float progress = Math.min(1f, totalScroll / (float) dp(120));

                headerIconContainer.setScaleX(1f - progress * 0.45f);
                headerIconContainer.setScaleY(1f - progress * 0.45f);
                headerIconContainer.setTranslationY(-progress * dp(32));

                headerTextSwitcher.setAlpha(Math.max(0f, 1f - progress * 1.6f));
                headerTextSwitcher.setTranslationY(-progress * dp(20));
            }
        });

        cardContainer.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        // Bottom gradient for smooth scrolling under pill button
        View bottomGradient = new View(context);
        GradientDrawable gradient = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.TRANSPARENT, 0xFF131D1B}
        );
        bottomGradient.setBackground(gradient);
        bottomGradient.setClickable(false);
        cardContainer.addView(bottomGradient, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 96, Gravity.BOTTOM));

        contentView.addView(cardContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.TOP, 0, 238, 0, 0));

        // 5. Floating Circular Back Button
        ImageView backButton = new ImageView(context);
        backButton.setImageResource(R.drawable.ic_ab_back);
        backButton.setColorFilter(Color.WHITE);
        backButton.setScaleType(ImageView.ScaleType.CENTER);
        backButton.setBackground(Theme.createSimpleSelectorCircleDrawable(dp(40), 0x2AFFFFFF, 0x4DFFFFFF));
        backButton.setOnClickListener(v -> finishFragment());
        contentView.addView(backButton, LayoutHelper.createFrame(40, 40, Gravity.LEFT | Gravity.TOP, 16, 12, 0, 0));

        // 6. Bottom Floating Pill Button ("Оставить такую" / "Выбрать")
        selectButton = new TextView(context);
        selectButton.setGravity(Gravity.CENTER);
        selectButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        selectButton.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        selectButton.setTextColor(0xFF002A24); // High contrast dark teal on bright cyan
        selectButton.setBackground(Theme.createSimpleSelectorRoundRectDrawable(
                dp(24),
                0xFF00E5BF,
                0xFF00C7A5
        ));
        updateButtonState();

        selectButton.setOnClickListener(v -> {
            LauncherIconController.LauncherIcon current = LauncherIconController.getCurrentIcon();
            if (selectedIcon != current) {
                LauncherIconController.setIcon(selectedIcon);
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.showBulletin, Bulletin.TYPE_APP_ICON, selectedIcon);
                updateButtonState();
                AndroidUtilities.vibrateCursor(selectButton);
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
            } else {
                finishFragment();
            }
        });

        contentView.addView(selectButton, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 16, 0, 16, 16));

        fragmentView = contentView;
        return fragmentView;
    }

    private LinearLayout createTextBlock(Context context) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView title = new TextView(context);
        title.setTextColor(Color.WHITE);
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 22);
        title.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        title.setGravity(Gravity.CENTER);
        layout.addView(title, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL));

        TextView subtitle = new TextView(context);
        subtitle.setTextColor(0xB3FFFFFF);
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        subtitle.setGravity(Gravity.CENTER);
        layout.addView(subtitle, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 4, 0, 0));

        TextView author = new TextView(context);
        author.setTextColor(0x80FFFFFF);
        author.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        author.setGravity(Gravity.CENTER);
        layout.addView(author, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 2, 0, 0));

        return layout;
    }

    private void bindText(TextView title, TextView subtitle, TextView author, LauncherIconController.LauncherIcon icon) {
        if (icon == null) return;
        title.setText(getString(icon.title));

        String desc = icon.description;
        String auth = icon.author;

        if (!TextUtils.isEmpty(desc)) {
            subtitle.setText(desc);
            subtitle.setVisibility(View.VISIBLE);

            if (!TextUtils.isEmpty(auth)) {
                author.setText(auth);
                author.setVisibility(View.VISIBLE);
            } else {
                author.setVisibility(View.GONE);
            }
        } else if (!TextUtils.isEmpty(auth)) {
            subtitle.setText(auth);
            subtitle.setVisibility(View.VISIBLE);
            author.setVisibility(View.GONE);
        } else {
            subtitle.setVisibility(View.GONE);
            author.setVisibility(View.GONE);
        }
    }

    private void bindIconToView(AppIconsSelectorCell.AdaptiveIconImageView view, LauncherIconController.LauncherIcon icon) {
        if (view == null || icon == null) return;
        view.setImageResource(icon.background);
        view.setForeground(icon.foreground);
        view.setAdaptiveIconMode(icon.group != LauncherIconController.IconGroup.TELEGRAM);
    }

    private void switchIcon(LauncherIconController.LauncherIcon newIcon, int newPosition) {
        LauncherIconController.LauncherIcon oldIcon = selectedIcon;
        selectedIcon = newIcon;

        // 1. Animate Atmospheric Glow
        if (backgroundView != null) {
            backgroundView.animateGlowTo(newIcon.glowColor);
        }

        // 2. Animate Big Icon Crossfade & Spring
        if (headerIconContainer != null) {
            bindIconToView(headerIconNext, newIcon);
            headerIconNext.setAlpha(0f);
            headerIconNext.setScaleX(0.86f);
            headerIconNext.setScaleY(0.86f);
            headerIconNext.setVisibility(View.VISIBLE);

            headerIconNext.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(240)
                    .setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT)
                    .start();

            headerIconCurrent.animate()
                    .alpha(0f)
                    .scaleX(0.86f)
                    .scaleY(0.86f)
                    .setDuration(240)
                    .setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT)
                    .withEndAction(() -> {
                        headerIconCurrent.setVisibility(View.GONE);
                        AppIconsSelectorCell.AdaptiveIconImageView temp = headerIconCurrent;
                        headerIconCurrent = headerIconNext;
                        headerIconNext = temp;
                    })
                    .start();
        }

        // 3. Animate Text Crossfade & Slide
        if (headerTextSwitcher != null) {
            bindText(titleNext, subtitleNext, authorNext, newIcon);
            textBlockNext.setAlpha(0f);
            textBlockNext.setTranslationY(dp(6));
            textBlockNext.setVisibility(View.VISIBLE);

            textBlockNext.animate()
                    .alpha(1f)
                    .translationY(0)
                    .setDuration(220)
                    .setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT)
                    .start();

            textBlockCurrent.animate()
                    .alpha(0f)
                    .translationY(-dp(6))
                    .setDuration(220)
                    .setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT)
                    .withEndAction(() -> {
                        textBlockCurrent.setVisibility(View.GONE);
                        LinearLayout temp = textBlockCurrent;
                        textBlockCurrent = textBlockNext;
                        textBlockNext = temp;

                        TextView tT = titleCurrent; titleCurrent = titleNext; titleNext = tT;
                        TextView sT = subtitleCurrent; subtitleCurrent = subtitleNext; subtitleNext = sT;
                        TextView aT = authorCurrent; authorCurrent = authorNext; authorNext = aT;
                    })
                    .start();
        }

        // 4. Animate Grid Selection on visible cells
        int oldIndex = icons.indexOf(oldIcon);
        if (listView != null) {
            for (int i = 0; i < listView.getChildCount(); i++) {
                View child = listView.getChildAt(i);
                if (child instanceof IconGridCell) {
                    IconGridCell cell = (IconGridCell) child;
                    int pos = listView.getChildAdapterPosition(child);
                    if (pos == newPosition) {
                        cell.setSelected(true, true);
                    } else if (pos == oldIndex) {
                        cell.setSelected(false, true);
                    }
                }
            }
        }

        // 5. Update Bottom Pill Button
        updateButtonState();
    }

    private void updateButtonState() {
        if (selectButton == null) return;
        boolean isCurrent = (selectedIcon == LauncherIconController.getCurrentIcon());
        selectButton.setText(isCurrent
                ? getString(R.string.OEAppearanceAppIconKeep)
                : getString(R.string.OEAppearanceAppIconSelect));
    }

    private class IconsAdapter extends RecyclerListView.SelectionAdapter {

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return true;
        }

        @Override
        public int getItemCount() {
            return icons.size();
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new RecyclerListView.Holder(new IconGridCell(parent.getContext()));
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            IconGridCell cell = (IconGridCell) holder.itemView;
            LauncherIconController.LauncherIcon icon = icons.get(position);
            cell.bind(icon, icon == selectedIcon);
        }
    }

    private static class IconGridCell extends FrameLayout {

        private final View selectionBackground;
        private final AppIconsSelectorCell.AdaptiveIconImageView iconView;
        private final CheckmarkBadgeView checkmarkBadge;
        private final TextView titleView;
        private boolean isSelected = false;

        public IconGridCell(@NonNull Context context) {
            super(context);

            // 1. Selection Rounded Rectangle (radius 16dp)
            selectionBackground = new View(context);
            GradientDrawable selBg = new GradientDrawable();
            selBg.setCornerRadius(dp(16));
            selBg.setColor(0x2200D8B4); // Subtle translucent accent highlight
            selectionBackground.setBackground(selBg);
            selectionBackground.setAlpha(0f);
            addView(selectionBackground, LayoutHelper.createFrame(70, 84, Gravity.CENTER_HORIZONTAL | Gravity.TOP, 0, 4, 0, 0));

            // 2. Icon Container with 56dp Icon + 20dp Checkmark Badge
            FrameLayout iconContainer = new FrameLayout(context);

            iconView = new AppIconsSelectorCell.AdaptiveIconImageView(context);
            iconView.setOuterPadding(dp(4));
            iconView.setBackgroundOuterPadding(dp(28));
            iconContainer.addView(iconView, LayoutHelper.createFrame(56, 56, Gravity.CENTER));

            checkmarkBadge = new CheckmarkBadgeView(context);
            checkmarkBadge.setVisibility(GONE);
            iconContainer.addView(checkmarkBadge, LayoutHelper.createFrame(20, 20, Gravity.BOTTOM | Gravity.RIGHT, 0, 0, dp(1), dp(1)));

            addView(iconContainer, LayoutHelper.createFrame(62, 62, Gravity.CENTER_HORIZONTAL | Gravity.TOP, 0, 8, 0, 0));

            // 3. Label below Icon
            titleView = new TextView(context);
            titleView.setGravity(Gravity.CENTER);
            titleView.setMaxLines(1);
            titleView.setSingleLine(true);
            titleView.setEllipsize(TextUtils.TruncateAt.END);
            titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            titleView.setTextColor(0xFFE0E0E0);
            titleView.setIncludeFontPadding(true);
            addView(titleView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 20, Gravity.CENTER_HORIZONTAL | Gravity.TOP, 2, 70, 2, 0));
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(dp(96), MeasureSpec.EXACTLY));
        }

        public void bind(LauncherIconController.LauncherIcon icon, boolean selected) {
            iconView.setImageResource(icon.background);
            iconView.setForeground(icon.foreground);
            iconView.setAdaptiveIconMode(icon.group != LauncherIconController.IconGroup.TELEGRAM);

            titleView.setText(getString(icon.title));
            setSelected(selected, false);
        }

        public void setSelected(boolean selected, boolean animate) {
            this.isSelected = selected;
            float targetAlpha = selected ? 1f : 0f;

            if (animate) {
                if (selected) {
                    checkmarkBadge.setVisibility(VISIBLE);
                    checkmarkBadge.setScaleX(0f);
                    checkmarkBadge.setScaleY(0f);
                    checkmarkBadge.setAlpha(0f);
                    checkmarkBadge.animate()
                            .scaleX(1f).scaleY(1f).alpha(1f)
                            .setDuration(240)
                            .setInterpolator(new OvershootInterpolator(1.5f))
                            .start();

                    selectionBackground.animate()
                            .alpha(1f)
                            .setDuration(200)
                            .start();
                } else {
                    checkmarkBadge.animate()
                            .scaleX(0f).scaleY(0f).alpha(0f)
                            .setDuration(180)
                            .setInterpolator(new AccelerateInterpolator())
                            .withEndAction(() -> checkmarkBadge.setVisibility(GONE))
                            .start();

                    selectionBackground.animate()
                            .alpha(0f)
                            .setDuration(180)
                            .start();
                }
            } else {
                checkmarkBadge.setVisibility(selected ? VISIBLE : GONE);
                checkmarkBadge.setScaleX(targetAlpha);
                checkmarkBadge.setScaleY(targetAlpha);
                checkmarkBadge.setAlpha(targetAlpha);
                selectionBackground.setAlpha(targetAlpha);
            }
        }
    }

    private static class CheckmarkBadgeView extends FrameLayout {
        public CheckmarkBadgeView(@NonNull Context context) {
            super(context);
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.OVAL);
            bg.setColor(0xFF00E5BF);
            setBackground(bg);

            ImageView checkIcon = new ImageView(context);
            checkIcon.setImageResource(R.drawable.round_check2);
            checkIcon.setColorFilter(0xFF002A24);
            addView(checkIcon, LayoutHelper.createFrame(14, 14, Gravity.CENTER));
        }
    }

    private static class AtmosphericBackgroundView extends View {
        private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private int currentGlowColor;
        private ValueAnimator glowAnimator;

        public AtmosphericBackgroundView(Context context, int initialGlowColor) {
            super(context);
            this.currentGlowColor = initialGlowColor;
        }

        public void animateGlowTo(int targetGlowColor) {
            if (currentGlowColor == targetGlowColor) return;
            if (glowAnimator != null) {
                glowAnimator.cancel();
            }
            glowAnimator = ValueAnimator.ofObject(new ArgbEvaluator(), currentGlowColor, targetGlowColor);
            glowAnimator.setDuration(280);
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
            float cy = dp(107); // Centered directly behind the 110dp preview icon
            float radius = w * 0.9f;

            glowPaint.setShader(new RadialGradient(
                    cx, cy, radius,
                    new int[]{currentGlowColor, 0x00000000},
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
            canvas.drawColor(0xFF0E1514); // Dark base matching target video
            canvas.drawRect(0, 0, getWidth(), getHeight(), glowPaint);
        }
    }
}
