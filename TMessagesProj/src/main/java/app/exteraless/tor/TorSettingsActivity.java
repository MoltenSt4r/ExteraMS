package app.exteraless.tor;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.text.InputType;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.browser.Browser;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextRadioCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.Switch;

import app.exteraless.appearance.AppearanceConfig;
import tw.nekomimi.nekogram.settings.BaseNekoSettingsActivity;
import tw.nekomimi.nekogram.ui.cells.HeaderCell;

/**
 * Экран настроек встроенного Tor (Orbot) в ExteraMS с Material 3 дизайном:
 * - Главная интерактивная карточка со статусом, кнопкой включения и MD3-линией прогресса.
 * - Интеграция с официальным ботом Tor Project в Telegram (@GetBridgesBot).
 * - Автоматическая вставка и парсинг мостов из буфера обмена.
 * - Выбор страны выходного узла и типа мостов.
 */
public class TorSettingsActivity extends BaseNekoSettingsActivity implements TorController.Listener {

    private static final int TYPE_HERO_CARD = 100;

    private int heroCardRow;
    private int heroShadowRow;

    private int headerRoutingRow;
    private int exitCountryRow;
    private int bridgeTypeRow;
    private int getBridgesBotRow;
    private int customBridgesRow;
    private int routingShadowRow;

    private int infoPrivacyRow;

    // Страны выхода (код, флаг + название)
    private static final String[][] COUNTRIES = new String[][]{
            {"", "🌐 Автоматически (рекомендуется)"},
            {"de", "🇩🇪 Германия"},
            {"nl", "🇳🇱 Нидерланды"},
            {"us", "🇺🇸 США"},
            {"ch", "🇨🇭 Швейцария"},
            {"fi", "🇫🇮 Финляндия"},
            {"fr", "🇫🇷 Франция"},
            {"gb", "🇬🇧 Великобритания"},
            {"se", "🇸🇪 Швеция"},
            {"at", "🇦🇹 Австрия"},
            {"ca", "🇨🇦 Канада"},
            {"jp", "🇯🇵 Япония"},
            {"is", "🇮🇸 Исландия"},
            {"pl", "🇵🇱 Польша"}
    };

    public TorSettingsActivity() {
        super();
    }

    @Override
    public boolean onFragmentCreate() {
        TorController.getInstance().addListener(this);
        return super.onFragmentCreate();
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        TorController.getInstance().removeListener(this);
    }

    @Override
    protected void updateRows() {
        super.updateRows();

        heroCardRow = addRow();
        heroShadowRow = addRow();

        headerRoutingRow = addRow();
        exitCountryRow = addRow();
        bridgeTypeRow = addRow();
        getBridgesBotRow = addRow();
        customBridgesRow = addRow();
        routingShadowRow = addRow();

        infoPrivacyRow = addRow();
    }

    @Override
    protected String getActionBarTitle() {
        return LocaleController.getString(R.string.TorSettingsTitle);
    }

    @Override
    protected BaseListAdapter createAdapter(Context context) {
        return new ListAdapter(context);
    }

    @Override
    public void onStatusChanged(int status, int progress, String message) {
        AndroidUtilities.runOnUIThread(() -> {
            if (listAdapter != null) {
                listAdapter.notifyItemChanged(heroCardRow);
                listAdapter.notifyItemChanged(exitCountryRow);
                listAdapter.notifyItemChanged(bridgeTypeRow);
                listAdapter.notifyItemChanged(customBridgesRow);
            }
        });
    }

    @Override
    public void onLog(String line) {
    }

    @Override
    protected void onItemClick(View view, int position, float x, float y) {
        TorController controller = TorController.getInstance();

        if (position == exitCountryRow) {
            showCountryPickerDialog();
        } else if (position == bridgeTypeRow) {
            showBridgePickerDialog();
        } else if (position == getBridgesBotRow) {
            openBridgesBot();
        } else if (position == customBridgesRow) {
            showCustomBridgesDialog();
        }
    }

    private void openBridgesBot() {
        try {
            getMessagesController().openByUserName("GetBridgesBot", this, 1);
        } catch (Exception e) {
            Browser.openUrl(getParentActivity(), "https://t.me/GetBridgesBot");
        }
    }

    private void showCountryPickerDialog() {
        TorController controller = TorController.getInstance();
        String currentCode = controller.getExitCountry();

        int selectedIndex = 0;
        CharSequence[] items = new CharSequence[COUNTRIES.length];
        for (int i = 0; i < COUNTRIES.length; i++) {
            items[i] = COUNTRIES[i][1];
            if (COUNTRIES[i][0].equalsIgnoreCase(currentCode)) {
                selectedIndex = i;
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity(), resourcesProvider);
        builder.setTitle(LocaleController.getString(R.string.TorExitCountryTitle));
        builder.setItems(items, (dialog, which) -> {
            controller.setExitCountry(COUNTRIES[which][0]);
            dialog.dismiss();
            if (listAdapter != null) {
                listAdapter.notifyItemChanged(exitCountryRow);
            }
            BulletinFactory.of(TorSettingsActivity.this)
                    .createSimpleBulletin(R.raw.done, LocaleController.getString(R.string.TorExitCountry) + ": " + COUNTRIES[which][1])
                    .show();
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void showBridgePickerDialog() {
        Context context = getParentActivity();
        if (context == null) return;

        BottomSheet.Builder builder = new BottomSheet.Builder(context, false, resourcesProvider);
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(16), AndroidUtilities.dp(20), AndroidUtilities.dp(16));

        TextView titleView = new TextView(context);
        titleView.setText(LocaleController.getString(R.string.TorBridgeConfigTitle));
        titleView.setTextSize(20);
        titleView.setTypeface(AndroidUtilities.bold());
        titleView.setTextColor(getThemedColor(Theme.key_dialogTextBlack));
        container.addView(titleView);

        TextView subView = new TextView(context);
        subView.setText(LocaleController.getString(R.string.TorBridgeConfigSubtitle));
        subView.setTextSize(14);
        subView.setTextColor(getThemedColor(Theme.key_dialogTextGray));
        subView.setPadding(0, AndroidUtilities.dp(4), 0, AndroidUtilities.dp(16));
        container.addView(subView);

        TorController controller = TorController.getInstance();
        final int currentBridge = controller.getBridgeType();

        String[] bridgeNames = new String[]{
                LocaleController.getString(R.string.TorBridgeDirect),
                LocaleController.getString(R.string.TorBridgeSmart),
                LocaleController.getString(R.string.TorBridgeSnowflake),
                LocaleController.getString(R.string.TorBridgeSnowflakeAmp),
                LocaleController.getString(R.string.TorBridgeObfs4),
                LocaleController.getString(R.string.TorBridgeCustom)
        };

        final TextRadioCell[] cells = new TextRadioCell[bridgeNames.length];
        final int[] selected = new int[]{currentBridge};

        for (int i = 0; i < bridgeNames.length; i++) {
            final int index = i;
            TextRadioCell cell = new TextRadioCell(context, 21, true);
            cell.setTextAndCheck(bridgeNames[i], currentBridge == i, false);
            cell.setOnClickListener(v -> {
                selected[0] = index;
                for (int j = 0; j < cells.length; j++) {
                    cells[j].setChecked(j == index);
                }
            });
            cells[i] = cell;
            container.addView(cell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        }

        LinearLayout buttonLayout = new LinearLayout(context);
        buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
        buttonLayout.setGravity(Gravity.RIGHT);
        buttonLayout.setPadding(0, AndroidUtilities.dp(16), 0, 0);

        TextView cancelButton = new TextView(context);
        cancelButton.setText(LocaleController.getString(R.string.Cancel));
        cancelButton.setTextSize(14);
        cancelButton.setTypeface(AndroidUtilities.bold());
        cancelButton.setTextColor(getThemedColor(Theme.key_dialogTextBlue2));
        cancelButton.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(10), AndroidUtilities.dp(12), AndroidUtilities.dp(10));
        cancelButton.setOnClickListener(v -> builder.getDismissRunnable().run());
        buttonLayout.addView(cancelButton);

        TextView applyButton = new TextView(context);
        applyButton.setText(LocaleController.getString(R.string.OK));
        applyButton.setTextSize(14);
        applyButton.setTypeface(AndroidUtilities.bold());
        applyButton.setTextColor(getThemedColor(Theme.key_dialogTextBlue2));
        applyButton.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(10), AndroidUtilities.dp(12), AndroidUtilities.dp(10));
        applyButton.setOnClickListener(v -> {
            builder.getDismissRunnable().run();
            if (selected[0] == TorController.BRIDGE_CUSTOM) {
                showCustomBridgesDialog();
            } else {
                controller.setBridgeType(selected[0]);
                if (listAdapter != null) {
                    listAdapter.notifyItemChanged(bridgeTypeRow);
                    listAdapter.notifyItemChanged(customBridgesRow);
                }
            }
        });
        buttonLayout.addView(applyButton);

        container.addView(buttonLayout, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        builder.setCustomView(container);
        showDialog(builder.create());
    }

    private void showCustomBridgesDialog() {
        Context context = getParentActivity();
        if (context == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(context, resourcesProvider);
        builder.setTitle(LocaleController.getString(R.string.TorCustomBridgesTitle));
        builder.setMessage(LocaleController.getString(R.string.TorCustomBridgesPrompt));

        LinearLayout dialogContent = new LinearLayout(context);
        dialogContent.setOrientation(LinearLayout.VERTICAL);
        dialogContent.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(4), AndroidUtilities.dp(24), AndroidUtilities.dp(4));

        LinearLayout quickActions = new LinearLayout(context);
        quickActions.setOrientation(LinearLayout.HORIZONTAL);
        quickActions.setPadding(0, 0, 0, AndroidUtilities.dp(10));

        TextView askBotBtn = new TextView(context);
        askBotBtn.setText("🤖 @GetBridgesBot");
        askBotBtn.setTextSize(12);
        askBotBtn.setTypeface(AndroidUtilities.bold());
        askBotBtn.setTextColor(getThemedColor(Theme.key_featuredStickers_addButton));
        askBotBtn.setBackground(Theme.createSimpleSelectorRoundRectDrawable(
                AndroidUtilities.dp(8),
                Theme.multAlpha(getThemedColor(Theme.key_featuredStickers_addButton), 0.12f),
                Theme.multAlpha(getThemedColor(Theme.key_featuredStickers_addButton), 0.25f)
        ));
        askBotBtn.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(6), AndroidUtilities.dp(10), AndroidUtilities.dp(6));
        askBotBtn.setOnClickListener(v -> openBridgesBot());
        quickActions.addView(askBotBtn, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL, 0, 0, 8, 0));

        final EditText editText = new EditText(context);

        TextView pasteBtn = new TextView(context);
        pasteBtn.setText("📋 " + LocaleController.getString(R.string.TorPasteFromClipboard));
        pasteBtn.setTextSize(12);
        pasteBtn.setTypeface(AndroidUtilities.bold());
        pasteBtn.setTextColor(getThemedColor(Theme.key_featuredStickers_addButton));
        pasteBtn.setBackground(Theme.createSimpleSelectorRoundRectDrawable(
                AndroidUtilities.dp(8),
                Theme.multAlpha(getThemedColor(Theme.key_featuredStickers_addButton), 0.12f),
                Theme.multAlpha(getThemedColor(Theme.key_featuredStickers_addButton), 0.25f)
        ));
        pasteBtn.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(6), AndroidUtilities.dp(10), AndroidUtilities.dp(6));
        pasteBtn.setOnClickListener(v -> pasteBridgesFromClipboard(editText));
        quickActions.addView(pasteBtn, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL));

        dialogContent.addView(quickActions, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        editText.setTextSize(14);
        editText.setText(TorController.getInstance().getCustomBridges());
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        editText.setLines(4);
        editText.setMaxLines(8);
        editText.setTextColor(getThemedColor(Theme.key_dialogTextBlack));
        editText.setHintTextColor(getThemedColor(Theme.key_dialogTextGray));
        editText.setHint("obfs4 IP:PORT FINGERPRINT cert=... iat-mode=0");

        dialogContent.addView(editText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        builder.setView(dialogContent);

        builder.setPositiveButton(LocaleController.getString(R.string.TorCustomBridgesSave), (dialog, which) -> {
            TorController.getInstance().setCustomBridges(editText.getText().toString().trim());
            TorController.getInstance().setBridgeType(TorController.BRIDGE_CUSTOM);
            if (listAdapter != null) {
                listAdapter.notifyItemChanged(bridgeTypeRow);
                listAdapter.notifyItemChanged(customBridgesRow);
            }
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void pasteBridgesFromClipboard(EditText editText) {
        try {
            ClipboardManager clipboard = (ClipboardManager) ApplicationLoader.applicationContext.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard == null || !clipboard.hasPrimaryClip()) {
                BulletinFactory.of(this).createSimpleBulletin(R.raw.info, LocaleController.getString(R.string.TorNoBridgesInClipboard)).show();
                return;
            }
            ClipData clip = clipboard.getPrimaryClip();
            if (clip == null || clip.getItemCount() == 0) {
                BulletinFactory.of(this).createSimpleBulletin(R.raw.info, LocaleController.getString(R.string.TorNoBridgesInClipboard)).show();
                return;
            }
            CharSequence text = clip.getItemAt(0).getText();
            if (TextUtils.isEmpty(text)) {
                BulletinFactory.of(this).createSimpleBulletin(R.raw.info, LocaleController.getString(R.string.TorNoBridgesInClipboard)).show();
                return;
            }

            String[] lines = text.toString().split("\n");
            StringBuilder sb = new StringBuilder();
            int count = 0;
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.startsWith("Bridge ")) {
                    trimmed = trimmed.substring(7).trim();
                }
                if (trimmed.startsWith("obfs4 ") || trimmed.startsWith("snowflake ") || trimmed.startsWith("webtunnel ")) {
                    if (sb.length() > 0) {
                        sb.append("\n");
                    }
                    sb.append(trimmed);
                    count++;
                }
            }

            if (count > 0) {
                String current = editText.getText().toString().trim();
                if (TextUtils.isEmpty(current)) {
                    editText.setText(sb.toString());
                } else {
                    editText.setText(current + "\n" + sb.toString());
                }
                editText.setSelection(editText.getText().length());
                BulletinFactory.of(this).createSimpleBulletin(R.raw.done, LocaleController.formatString(R.string.TorBridgesPasted, count)).show();
            } else {
                BulletinFactory.of(this).createSimpleBulletin(R.raw.info, LocaleController.getString(R.string.TorNoBridgesInClipboard)).show();
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    private String getCountryNameByCode(String code) {
        if (code == null || code.isEmpty()) {
            return "Автоматически";
        }
        for (String[] c : COUNTRIES) {
            if (c[0].equalsIgnoreCase(code)) {
                return c[1];
            }
        }
        return code.toUpperCase();
    }

    private String getBridgeName(int type) {
        switch (type) {
            case TorController.BRIDGE_SMART:
                return LocaleController.getString(R.string.TorBridgeSmart);
            case TorController.BRIDGE_SNOWFLAKE:
                return "Snowflake (WebRTC)";
            case TorController.BRIDGE_SNOWFLAKE_AMP:
                return "Snowflake AMP";
            case TorController.BRIDGE_OBFS4:
                return LocaleController.getString(R.string.TorBridgeObfs4);
            case TorController.BRIDGE_CUSTOM:
                return LocaleController.getString(R.string.TorBridgeCustom);
            case TorController.BRIDGE_DIRECT:
            default:
                return LocaleController.getString(R.string.TorBridgeDirect);
        }
    }

    /**
     * MD3 линия прогресса для Bootstrap
     */
    public static class TorProgressBar extends View {
        private float progress = 0f;
        private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();
        private final Theme.ResourcesProvider resourcesProvider;

        public TorProgressBar(Context context, Theme.ResourcesProvider resourcesProvider) {
            super(context);
            this.resourcesProvider = resourcesProvider;
            trackPaint.setStyle(Paint.Style.FILL);
            progressPaint.setStyle(Paint.Style.FILL);
        }

        public void setProgress(float p) {
            this.progress = Math.max(0f, Math.min(1f, p));
            invalidate();
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int width = MeasureSpec.getSize(widthMeasureSpec);
            int height = MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY
                    ? MeasureSpec.getSize(heightMeasureSpec)
                    : AndroidUtilities.dp(4);
            setMeasuredDimension(width, height);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) return;
            float r = h / 2f;

            trackPaint.setColor(Theme.getColor(Theme.key_switch2Track, resourcesProvider));
            rect.set(0, 0, w, h);
            canvas.drawRoundRect(rect, r, r, trackPaint);

            if (progress > 0) {
                progressPaint.setColor(Theme.getColor(Theme.key_featuredStickers_addButton, resourcesProvider));
                rect.set(0, 0, Math.max(h, w * progress), h);
                canvas.drawRoundRect(rect, r, r, progressPaint);
            }
        }
    }

    /**
     * Главная Hero-карточка управления Tor с тумблером, иконкой Orbot и MD3-прогрессом
     */
    public class TorHeroCard extends FrameLayout {
        private final FrameLayout iconContainer;
        private final ImageView iconView;
        private final TextView titleView;
        private final TextView subtitleView;
        private final Switch switchView;
        private final LinearLayout progressContainer;
        private final TextView progressTextView;
        private final TorProgressBar progressBar;
        private final TextView newIdentityButton;
        private boolean isToggling = false;

        public TorHeroCard(Context context, Theme.ResourcesProvider resourcesProvider) {
            super(context);
            setTag(RecyclerListView.TAG_NOT_SECTION);

            int radius = AndroidUtilities.dp(Math.max(14, AppearanceConfig.sectionRadius()));
            setBackground(Theme.createRoundRectDrawable(radius, getThemedColor(Theme.key_windowBackgroundWhite)));

            LinearLayout mainLayout = new LinearLayout(context);
            mainLayout.setOrientation(LinearLayout.VERTICAL);
            mainLayout.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16));
            addView(mainLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            // Верхняя плашка: Иконка + Заголовок/Статус + Переключатель
            LinearLayout headerRow = new LinearLayout(context);
            headerRow.setOrientation(LinearLayout.HORIZONTAL);
            headerRow.setGravity(Gravity.CENTER_VERTICAL);
            headerRow.setBackground(Theme.createSimpleSelectorRoundRectDrawable(
                    AndroidUtilities.dp(8), 0, Theme.multAlpha(getThemedColor(Theme.key_listSelector), 0.5f)
            ));
            headerRow.setOnClickListener(v -> toggleTorState());

            iconContainer = new FrameLayout(context);
            iconView = new ImageView(context);
            iconView.setImageResource(R.drawable.ic_orbot);
            iconView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            iconContainer.addView(iconView, LayoutHelper.createFrame(26, 26, Gravity.CENTER));
            headerRow.addView(iconContainer, LayoutHelper.createLinear(48, 48, Gravity.CENTER_VERTICAL));

            LinearLayout textCol = new LinearLayout(context);
            textCol.setOrientation(LinearLayout.VERTICAL);

            titleView = new TextView(context);
            titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            titleView.setTypeface(AndroidUtilities.bold());
            titleView.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlackText));
            textCol.addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            subtitleView = new TextView(context);
            subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            subtitleView.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteGrayText));
            textCol.addView(subtitleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 3, 0, 0));

            headerRow.addView(textCol, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f, Gravity.CENTER_VERTICAL, 14, 0, 10, 0));

            switchView = new Switch(context, resourcesProvider);
            switchView.setColors(Theme.key_switch2Track, Theme.key_switch2TrackChecked, Theme.key_windowBackgroundWhite, Theme.key_windowBackgroundWhite);
            switchView.setFocusable(false);
            switchView.setClickable(false);
            headerRow.addView(switchView, LayoutHelper.createLinear(37, 24, Gravity.CENTER_VERTICAL));

            mainLayout.addView(headerRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            // MD3 Прогресс подключения
            progressContainer = new LinearLayout(context);
            progressContainer.setOrientation(LinearLayout.VERTICAL);
            progressContainer.setPadding(0, AndroidUtilities.dp(12), 0, 0);

            progressTextView = new TextView(context);
            progressTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            progressTextView.setTypeface(AndroidUtilities.bold());
            progressTextView.setTextColor(getThemedColor(Theme.key_featuredStickers_addButton));
            progressContainer.addView(progressTextView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            progressBar = new TorProgressBar(context, resourcesProvider);
            progressContainer.addView(progressBar, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 4, 0, 6, 0, 0));

            mainLayout.addView(progressContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            // Кнопка «Сменить цепь (Новая личность)» при подключенном состоянии
            newIdentityButton = new TextView(context);
            newIdentityButton.setText(LocaleController.getString(R.string.TorNewIdentity));
            newIdentityButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            newIdentityButton.setTypeface(AndroidUtilities.bold());
            newIdentityButton.setTextColor(getThemedColor(Theme.key_featuredStickers_addButton));
            newIdentityButton.setGravity(Gravity.CENTER);
            newIdentityButton.setBackground(Theme.createSimpleSelectorRoundRectDrawable(
                    AndroidUtilities.dp(8),
                    Theme.multAlpha(getThemedColor(Theme.key_featuredStickers_addButton), 0.12f),
                    Theme.multAlpha(getThemedColor(Theme.key_featuredStickers_addButton), 0.25f)
            ));
            Drawable refreshIcon = ContextCompat.getDrawable(context, R.drawable.msg_retry_solar);
            if (refreshIcon != null) {
                refreshIcon = refreshIcon.mutate();
                refreshIcon.setColorFilter(new PorterDuffColorFilter(getThemedColor(Theme.key_featuredStickers_addButton), PorterDuff.Mode.SRC_IN));
                refreshIcon.setBounds(0, 0, AndroidUtilities.dp(18), AndroidUtilities.dp(18));
                newIdentityButton.setCompoundDrawables(refreshIcon, null, null, null);
                newIdentityButton.setCompoundDrawablePadding(AndroidUtilities.dp(8));
            }
            newIdentityButton.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(8), AndroidUtilities.dp(14), AndroidUtilities.dp(8));
            newIdentityButton.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                TorController.getInstance().newIdentity();
                BulletinFactory.of(TorSettingsActivity.this)
                        .createSimpleBulletin(R.raw.done, LocaleController.getString(R.string.TorNewIdentitySuccess))
                        .show();
            });

            mainLayout.addView(newIdentityButton, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 38, 0, 12, 0, 0));

            update();
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(
                    MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
            );
        }

        private void toggleTorState() {
            if (isToggling) return;
            isToggling = true;
            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            TorController controller = TorController.getInstance();
            boolean newState = !controller.isEnabled();
            controller.setEnabled(newState);
            update();
            postDelayed(() -> isToggling = false, 300);
        }

        public void update() {
            TorController controller = TorController.getInstance();
            int status = controller.getStatus();
            boolean enabled = controller.isEnabled();

            switchView.setChecked(enabled, true);

            if (status == TorController.STATUS_CONNECTED) {
                iconContainer.setBackground(Theme.createCircleDrawable(AndroidUtilities.dp(48), 0x224CAF50));
                iconView.setColorFilter(new PorterDuffColorFilter(0xFF4CAF50, PorterDuff.Mode.MULTIPLY));
                titleView.setText(LocaleController.getString(R.string.TorConnected));
                subtitleView.setText(LocaleController.getString(R.string.TorProtectedDetail));
                progressContainer.setVisibility(View.GONE);
                newIdentityButton.setVisibility(View.VISIBLE);
            } else if (status == TorController.STATUS_CONNECTING || status == TorController.STATUS_STARTING) {
                iconContainer.setBackground(Theme.createCircleDrawable(AndroidUtilities.dp(48), 0x24FF9800));
                iconView.setColorFilter(new PorterDuffColorFilter(0xFFFF9800, PorterDuff.Mode.MULTIPLY));
                titleView.setText(status == TorController.STATUS_STARTING
                        ? LocaleController.getString(R.string.TorStarting)
                        : LocaleController.formatString(R.string.TorConnecting, controller.getProgress()));
                subtitleView.setText(TextUtils.isEmpty(controller.getStatusMessage())
                        ? "Установление анонимного соединения..."
                        : controller.getStatusMessage());
                progressContainer.setVisibility(View.VISIBLE);
                progressBar.setProgress(controller.getProgress() / 100.0f);
                progressTextView.setText(controller.getProgress() + "% • " + (TextUtils.isEmpty(controller.getStatusMessage()) ? "Подключение к узлам" : controller.getStatusMessage()));
                newIdentityButton.setVisibility(View.GONE);
            } else if (status == TorController.STATUS_ERROR) {
                iconContainer.setBackground(Theme.createCircleDrawable(AndroidUtilities.dp(48), 0x24F44336));
                iconView.setColorFilter(new PorterDuffColorFilter(0xFFF44336, PorterDuff.Mode.MULTIPLY));
                titleView.setText(LocaleController.getString(R.string.TorError));
                subtitleView.setText(controller.getStatusMessage());
                progressContainer.setVisibility(View.GONE);
                newIdentityButton.setVisibility(View.GONE);
            } else {
                // STATUS_STOPPED или STATUS_STOPPING
                iconContainer.setBackground(Theme.createCircleDrawable(AndroidUtilities.dp(48), 0x147F7F7F));
                iconView.setColorFilter(new PorterDuffColorFilter(getThemedColor(Theme.key_windowBackgroundWhiteGrayIcon), PorterDuff.Mode.MULTIPLY));
                titleView.setText(LocaleController.getString(R.string.TorSettingsTitle));
                subtitleView.setText(LocaleController.getString(R.string.TorDisabledDetail));
                progressContainer.setVisibility(View.GONE);
                newIdentityButton.setVisibility(View.GONE);
            }
        }
    }

    private class ListAdapter extends BaseListAdapter {

        public ListAdapter(Context context) {
            super(context);
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int position = holder.getAdapterPosition();
            return position == exitCountryRow || position == bridgeTypeRow || position == getBridgesBotRow || position == customBridgesRow;
        }

        @Override
        public int getItemViewType(int position) {
            if (position == heroCardRow) {
                return TYPE_HERO_CARD;
            } else if (position == heroShadowRow || position == routingShadowRow) {
                return TYPE_SHADOW;
            } else if (position == headerRoutingRow) {
                return TYPE_HEADER;
            } else if (position == exitCountryRow || position == bridgeTypeRow || position == customBridgesRow) {
                return TYPE_SETTINGS;
            } else if (position == getBridgesBotRow) {
                return TYPE_TEXT;
            } else if (position == infoPrivacyRow) {
                return TYPE_INFO_PRIVACY;
            }
            return TYPE_SETTINGS;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == TYPE_HERO_CARD) {
                TorHeroCard heroCard = new TorHeroCard(mContext, resourcesProvider);
                RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.setMargins(AndroidUtilities.dp(12), AndroidUtilities.dp(8), AndroidUtilities.dp(12), AndroidUtilities.dp(8));
                heroCard.setLayoutParams(lp);
                return new RecyclerListView.Holder(heroCard);
            }
            return super.onCreateViewHolder(parent, viewType);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, boolean partial) {
            TorController controller = TorController.getInstance();

            if (position == heroCardRow) {
                if (holder.itemView instanceof TorHeroCard) {
                    ((TorHeroCard) holder.itemView).update();
                }
            } else if (position == headerRoutingRow) {
                HeaderCell cell = (HeaderCell) holder.itemView;
                cell.setText(LocaleController.getString(R.string.TorRoutingHeader));
            } else if (position == exitCountryRow) {
                TextSettingsCell cell = (TextSettingsCell) holder.itemView;
                cell.setIcon(0);
                cell.setTextAndValue(LocaleController.getString(R.string.TorExitCountry), getCountryNameByCode(controller.getExitCountry()), true);
            } else if (position == bridgeTypeRow) {
                TextSettingsCell cell = (TextSettingsCell) holder.itemView;
                cell.setIcon(0);
                cell.setTextAndValue(LocaleController.getString(R.string.TorBridgeType), getBridgeName(controller.getBridgeType()), true);
            } else if (position == getBridgesBotRow) {
                TextCell cell = (TextCell) holder.itemView;
                cell.setTextAndValueAndIcon(LocaleController.getString(R.string.TorGetBridgesTelegramBot), "@GetBridgesBot", R.drawable.msg_bots_solar, true);
            } else if (position == customBridgesRow) {
                TextSettingsCell cell = (TextSettingsCell) holder.itemView;
                cell.setIcon(0);
                boolean hasBridges = !TextUtils.isEmpty(controller.getCustomBridges());
                cell.setTextAndValue(LocaleController.getString(R.string.TorCustomBridgesTitle), hasBridges ? "Настроены" : "Не заданы", false);
            } else if (position == infoPrivacyRow) {
                TextInfoPrivacyCell cell = (TextInfoPrivacyCell) holder.itemView;
                cell.setText(LocaleController.getString(R.string.TorInfoPrivacy));
            }
        }
    }
}
