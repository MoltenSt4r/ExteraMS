package app.exteraless.player;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.LocaleController.getString;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.media.audiofx.AudioEffect;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.Switch;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import app.exteraless.appearance.AppearanceConfig;

public class PlayerSettingsActivity extends BaseFragment {

    private static final int ID_MD3_PLAYER = 1;
    private static final int ID_MD3_MINI_PLAYER = 2;
    private static final int ID_BG_STYLE = 3;
    private static final int ID_SEEKBAR_STYLE = 4;
    private static final int ID_COLOR_STYLE = 5;
    private static final int ID_SWIPE_TRACK = 6;
    private static final int ID_CROP_COVER = 7;
    private static final int ID_KEEP_SCREEN_ON = 8;

    private static final int ID_LYRICS_EXPERIMENTAL = 20;
    private static final int ID_LYRICS_SOURCES = 21;
    private static final int ID_LYRICS_PRIORITY = 22;
    private static final int ID_LYRICS_ROMANIZE = 23;
    private static final int ID_LYRICS_TRANSLATE = 24;
    private static final int ID_LYRICS_ALIGNMENT = 25;
    private static final int ID_LYRICS_ROLES = 26;
    private static final int ID_LYRICS_TAP_SEEK = 27;
    private static final int ID_LYRICS_AUTOSCROLL = 28;
    private static final int ID_LYRICS_HIDE_STATUS_BAR = 29;
    private static final int ID_LYRICS_TEXT_SIZE = 30;
    private static final int ID_LYRICS_BLUR = 31;

    private static final int ID_SLEEP_TIMER = 40;
    private static final int ID_PAUSE_ON_MUTE = 41;
    private static final int ID_RESUME_ON_BLUETOOTH = 42;
    private static final int ID_SYSTEM_EQUALIZER = 43;

    private UniversalRecyclerView listView;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(getString(R.string.OEAppearancePlayerSettings));
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

        listView = new UniversalRecyclerView(this, this::fillItems, this::onItemClick, null);
        contentView.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        actionBar.setAdaptiveBackground(listView);

        fragmentView = contentView;
        return fragmentView;
    }

    private void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        // --- Секция: Внешний вид плеера ---
        items.add(UItem.asHeader(getString(R.string.OEAppearancePlayerAppearanceHeader)));
        items.add(UItem.asCheck(ID_MD3_PLAYER, getString(R.string.OEAppearanceMd3Player))
                .setChecked(AppearanceConfig.md3Player()));
        items.add(UItem.asCheck(ID_MD3_MINI_PLAYER, getString(R.string.OEAppearanceMd3MiniPlayer))
                .setChecked(AppearanceConfig.md3MiniPlayer()));

        String bgStyleName;
        int bgStyle = AppearanceConfig.playerBackgroundStyle();
        if (bgStyle == 1) {
            bgStyleName = getString(R.string.OEAppearancePlayerBackgroundStyleBlur);
        } else if (bgStyle == 2) {
            bgStyleName = getString(R.string.OEAppearancePlayerBackgroundStyleGradient);
        } else {
            bgStyleName = getString(R.string.OEAppearancePlayerBackgroundStyleTheme);
        }
        items.add(UItem.asButton(ID_BG_STYLE, getString(R.string.OEAppearancePlayerBackgroundStyle), bgStyleName));

        int seekbarStyle = AppearanceConfig.playerSeekbarStyle();
        String seekbarName;
        if (seekbarStyle == 0) {
            seekbarName = getString(R.string.OEAppearancePlayerSeekbarDefault);
        } else if (seekbarStyle == 1) {
            seekbarName = getString(R.string.OEAppearancePlayerSeekbarWavy);
        } else if (seekbarStyle == 2) {
            seekbarName = getString(R.string.OEAppearancePlayerSeekbarSlim);
        } else {
            seekbarName = getString(R.string.OEAppearancePlayerSeekbarSquiggly);
        }
        items.add(UItem.asButton(ID_SEEKBAR_STYLE, getString(R.string.OEAppearancePlayerSeekbarStyle), seekbarName));

        int colorStyle = AppearanceConfig.playerColorStyle();
        String colorName = colorStyle == 1 ? getString(R.string.OEAppearancePlayerColorStyleTheme) : getString(R.string.OEAppearancePlayerColorStyleCover);
        items.add(UItem.asButton(ID_COLOR_STYLE, getString(R.string.OEAppearancePlayerColorStyle), colorName));

        items.add(UItem.asCheck(ID_SWIPE_TRACK, getString(R.string.OEAppearancePlayerSwipeTrack))
                .setChecked(AppearanceConfig.playerSwipeTrack()));
        items.add(UItem.asCheck(ID_CROP_COVER, getString(R.string.OEAppearancePlayerCropCover))
                .setChecked(AppearanceConfig.playerCropCover()));
        items.add(UItem.asCheck(ID_KEEP_SCREEN_ON, getString(R.string.OEAppearancePlayerKeepScreenOn))
                .setChecked(AppearanceConfig.playerKeepScreenOn()));
        items.add(UItem.asShadow(null));

        // --- Секция: Текст песни (MetroList) ---
        items.add(UItem.asHeader(getString(R.string.OEAppearancePlayerLyricsHeader)));

        items.add(UItem.asCheck(ID_LYRICS_EXPERIMENTAL, getString(R.string.OEAppearancePlayerLyricsExperimental))
                .setChecked(AppearanceConfig.playerLyricsExperimental()));

        items.add(UItem.asButton(ID_LYRICS_SOURCES, getString(R.string.OEAppearancePlayerLyricsSources), getString(R.string.OEAppearancePlayerLyricsSourcesDesc)));
        items.add(UItem.asButton(ID_LYRICS_PRIORITY, getString(R.string.OEAppearancePlayerLyricsPriority), getString(R.string.OEAppearancePlayerLyricsPriorityDesc)));

        items.add(UItem.asCheck(ID_LYRICS_ROMANIZE, getString(R.string.OEAppearancePlayerLyricsRomanize))
                .setChecked(AppearanceConfig.playerLyricsRomanize()));

        items.add(UItem.asCheck(ID_LYRICS_TRANSLATE, getString(R.string.OEAppearancePlayerLyricsTranslate), getString(R.string.OEAppearancePlayerLyricsTranslateDesc))
                .setChecked(AppearanceConfig.playerLyricsTranslate()));

        int alignMode = AppearanceConfig.playerLyricsAlignment();
        String alignName = alignMode == 0 ? getString(R.string.OEAppearancePlayerLyricsAlignmentCenter) : getString(R.string.OEAppearancePlayerLyricsAlignmentLeft);
        items.add(UItem.asButton(ID_LYRICS_ALIGNMENT, getString(R.string.OEAppearancePlayerLyricsAlignment), alignName));

        items.add(UItem.asCheck(ID_LYRICS_ROLES, getString(R.string.OEAppearancePlayerLyricsSplitRoles))
                .setChecked(AppearanceConfig.playerLyricsSplitRoles()));

        items.add(UItem.asCheck(ID_LYRICS_TAP_SEEK, getString(R.string.OEAppearancePlayerLyricsTapToSeek))
                .setChecked(AppearanceConfig.playerLyricsTapToSeek()));

        items.add(UItem.asCheck(ID_LYRICS_AUTOSCROLL, getString(R.string.OEAppearancePlayerLyricsAutoScroll))
                .setChecked(AppearanceConfig.playerLyricsAutoScroll()));

        items.add(UItem.asCheck(ID_LYRICS_HIDE_STATUS_BAR, getString(R.string.OEAppearancePlayerLyricsHideStatusBar))
                .setChecked(AppearanceConfig.playerLyricsHideStatusBar()));

        int textSizeMode = AppearanceConfig.playerLyricsTextSize();
        String textSizeName;
        if (textSizeMode == 0) {
            textSizeName = getString(R.string.OEAppearancePlayerLyricsTextSizeNormal);
        } else if (textSizeMode == 2) {
            textSizeName = getString(R.string.OEAppearancePlayerLyricsTextSizeHuge);
        } else {
            textSizeName = getString(R.string.OEAppearancePlayerLyricsTextSizeLarge);
        }
        items.add(UItem.asButton(ID_LYRICS_TEXT_SIZE, getString(R.string.OEAppearancePlayerLyricsTextSize), textSizeName));

        items.add(UItem.asCheck(ID_LYRICS_BLUR, getString(R.string.OEAppearancePlayerLyricsBlur))
                .setChecked(AppearanceConfig.playerLyricsBlur()));
        items.add(UItem.asShadow(null));

        // --- Секция: Воспроизведение и аудио ---
        items.add(UItem.asHeader(getString(R.string.OEAppearancePlayerAudioHeader)));

        SleepTimer timer = SleepTimer.getInstance();
        String timerStatus = timer.isRunning() ? timer.getFormattedRemaining() : getString(R.string.OEAppearancePlayerSleepTimerOff);
        items.add(UItem.asButton(ID_SLEEP_TIMER, getString(R.string.OEAppearancePlayerSleepTimer), timerStatus));

        items.add(UItem.asCheck(ID_PAUSE_ON_MUTE, getString(R.string.OEAppearancePlayerPauseOnMute))
                .setChecked(AppearanceConfig.playerPauseOnMute()));
        items.add(UItem.asCheck(ID_RESUME_ON_BLUETOOTH, getString(R.string.OEAppearancePlayerResumeOnBluetooth))
                .setChecked(AppearanceConfig.playerResumeOnBluetooth()));
        items.add(UItem.asButton(ID_SYSTEM_EQUALIZER, getString(R.string.OEAppearancePlayerEqualizer)));
        items.add(UItem.asShadow(null));
    }

    private void onItemClick(UItem item, View view, int position, float x, float y) {
        if (item.id == ID_MD3_PLAYER) {
            AppearanceConfig.md3Player.setConfigBool(!AppearanceConfig.md3Player());
            updateList();
        } else if (item.id == ID_MD3_MINI_PLAYER) {
            AppearanceConfig.md3MiniPlayer.setConfigBool(!AppearanceConfig.md3MiniPlayer());
            updateList();
        } else if (item.id == ID_BG_STYLE) {
            CharSequence[] options = {
                    getString(R.string.OEAppearancePlayerBackgroundStyleTheme),
                    getString(R.string.OEAppearancePlayerBackgroundStyleBlur),
                    getString(R.string.OEAppearancePlayerBackgroundStyleGradient)
            };
            showSelector(getString(R.string.OEAppearancePlayerBackgroundStyle), options, which -> {
                AppearanceConfig.playerBackgroundStyle.setConfigInt(which);
            });
        } else if (item.id == ID_SEEKBAR_STYLE) {
            new SeekbarStyleDialog(getContext(), which -> updateList()).show();
        } else if (item.id == ID_COLOR_STYLE) {
            CharSequence[] options = {
                    getString(R.string.OEAppearancePlayerColorStyleCover),
                    getString(R.string.OEAppearancePlayerColorStyleTheme)
            };
            showSelector(getString(R.string.OEAppearancePlayerColorStyle), options, which -> {
                AppearanceConfig.playerColorStyle.setConfigInt(which);
            });
        } else if (item.id == ID_SWIPE_TRACK) {
            AppearanceConfig.playerSwipeTrack.setConfigBool(!AppearanceConfig.playerSwipeTrack());
            updateList();
        } else if (item.id == ID_CROP_COVER) {
            AppearanceConfig.playerCropCover.setConfigBool(!AppearanceConfig.playerCropCover());
            updateList();
        } else if (item.id == ID_KEEP_SCREEN_ON) {
            AppearanceConfig.playerKeepScreenOn.setConfigBool(!AppearanceConfig.playerKeepScreenOn());
            updateList();
        } else if (item.id == ID_LYRICS_EXPERIMENTAL) {
            AppearanceConfig.playerLyricsExperimental.setConfigBool(!AppearanceConfig.playerLyricsExperimental());
            updateList();
        } else if (item.id == ID_LYRICS_SOURCES) {
            showLyricsSourcesDialog();
        } else if (item.id == ID_LYRICS_PRIORITY) {
            showLyricsPriorityDialog();
        } else if (item.id == ID_LYRICS_ROMANIZE) {
            AppearanceConfig.playerLyricsRomanize.setConfigBool(!AppearanceConfig.playerLyricsRomanize());
            updateList();
        } else if (item.id == ID_LYRICS_TRANSLATE) {
            AppearanceConfig.playerLyricsTranslate.setConfigBool(!AppearanceConfig.playerLyricsTranslate());
            updateList();
        } else if (item.id == ID_LYRICS_ALIGNMENT) {
            CharSequence[] options = {
                    getString(R.string.OEAppearancePlayerLyricsAlignmentCenter),
                    getString(R.string.OEAppearancePlayerLyricsAlignmentLeft)
            };
            showSelector(getString(R.string.OEAppearancePlayerLyricsAlignment), options, which -> {
                AppearanceConfig.playerLyricsAlignment.setConfigInt(which);
            });
        } else if (item.id == ID_LYRICS_ROLES) {
            AppearanceConfig.playerLyricsSplitRoles.setConfigBool(!AppearanceConfig.playerLyricsSplitRoles());
            updateList();
        } else if (item.id == ID_LYRICS_TAP_SEEK) {
            AppearanceConfig.playerLyricsTapToSeek.setConfigBool(!AppearanceConfig.playerLyricsTapToSeek());
            updateList();
        } else if (item.id == ID_LYRICS_AUTOSCROLL) {
            AppearanceConfig.playerLyricsAutoScroll.setConfigBool(!AppearanceConfig.playerLyricsAutoScroll());
            updateList();
        } else if (item.id == ID_LYRICS_HIDE_STATUS_BAR) {
            AppearanceConfig.playerLyricsHideStatusBar.setConfigBool(!AppearanceConfig.playerLyricsHideStatusBar());
            updateList();
        } else if (item.id == ID_LYRICS_TEXT_SIZE) {
            CharSequence[] options = {
                    getString(R.string.OEAppearancePlayerLyricsTextSizeNormal),
                    getString(R.string.OEAppearancePlayerLyricsTextSizeLarge),
                    getString(R.string.OEAppearancePlayerLyricsTextSizeHuge)
            };
            showSelector(getString(R.string.OEAppearancePlayerLyricsTextSize), options, which -> {
                AppearanceConfig.playerLyricsTextSize.setConfigInt(which);
            });
        } else if (item.id == ID_LYRICS_BLUR) {
            AppearanceConfig.playerLyricsBlur.setConfigBool(!AppearanceConfig.playerLyricsBlur());
            updateList();
        } else if (item.id == ID_SLEEP_TIMER) {
            SleepTimer.showDialog(getContext(), getResourceProvider());
        } else if (item.id == ID_PAUSE_ON_MUTE) {
            AppearanceConfig.playerPauseOnMute.setConfigBool(!AppearanceConfig.playerPauseOnMute());
            updateList();
        } else if (item.id == ID_RESUME_ON_BLUETOOTH) {
            AppearanceConfig.playerResumeOnBluetooth.setConfigBool(!AppearanceConfig.playerResumeOnBluetooth());
            updateList();
        } else if (item.id == ID_SYSTEM_EQUALIZER) {
            try {
                Intent eqIntent = new Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);
                eqIntent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, getContext().getPackageName());
                eqIntent.putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC);
                if (getParentActivity() != null && eqIntent.resolveActivity(getParentActivity().getPackageManager()) != null) {
                    getParentActivity().startActivity(eqIntent);
                } else {
                    BulletinFactory.global().createSimpleBulletin(R.drawable.baseline_volume_up_24, getString(R.string.OEAppearancePlayerEqualizerNotFound)).show();
                }
            } catch (Throwable t) {
                FileLog.e(t);
            }
        }
    }

    private void showLyricsSourcesDialog() {
        if (getContext() == null) return;
        android.app.Dialog dialog = new android.app.Dialog(getContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout root = new LinearLayout(getContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(20), dp(20), dp(16));

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(24));
        bg.setColor(Theme.getColor(Theme.key_dialogBackground));
        root.setBackground(bg);

        TextView title = new TextView(getContext());
        title.setText(getString(R.string.OEAppearancePlayerLyricsSources));
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        title.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        title.setTypeface(AndroidUtilities.bold());
        title.setPadding(0, 0, 0, dp(14));
        root.addView(title);

        ScrollView scroll = new ScrollView(getContext());
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout container = new LinearLayout(getContext());
        container.setOrientation(LinearLayout.VERTICAL);

        addSourceToggle(container, getString(R.string.OEAppearancePlayerLyricsProviderLrcLib), getString(R.string.OEAppearancePlayerLyricsProviderLrcLibDesc), AppearanceConfig.playerLyricsLrcLib(), val -> {
            AppearanceConfig.playerLyricsLrcLib.setConfigBool(val);
        });

        addSourceToggle(container, getString(R.string.OEAppearancePlayerLyricsProviderKuGou), getString(R.string.OEAppearancePlayerLyricsProviderKuGouDesc), AppearanceConfig.playerLyricsKuGou(), val -> {
            AppearanceConfig.playerLyricsKuGou.setConfigBool(val);
        });

        addSourceToggle(container, getString(R.string.OEAppearancePlayerLyricsProviderBetterLyrics), getString(R.string.OEAppearancePlayerLyricsProviderBetterLyricsDesc), AppearanceConfig.playerLyricsBetterLyrics(), val -> {
            AppearanceConfig.playerLyricsBetterLyrics.setConfigBool(val);
        });

        addSourceToggle(container, getString(R.string.OEAppearancePlayerLyricsProviderPaxsenix), getString(R.string.OEAppearancePlayerLyricsProviderPaxsenixDesc), AppearanceConfig.playerLyricsPaxsenix(), val -> {
            AppearanceConfig.playerLyricsPaxsenix.setConfigBool(val);
        });

        addSourceToggle(container, getString(R.string.OEAppearancePlayerLyricsProviderLyricsPlus), getString(R.string.OEAppearancePlayerLyricsProviderLyricsPlusDesc), AppearanceConfig.playerLyricsLyricsPlus(), val -> {
            AppearanceConfig.playerLyricsLyricsPlus.setConfigBool(val);
        });

        addSourceToggle(container, getString(R.string.OEAppearancePlayerLyricsProviderZemer), getString(R.string.OEAppearancePlayerLyricsProviderZemerDesc), AppearanceConfig.playerLyricsZemer(), val -> {
            AppearanceConfig.playerLyricsZemer.setConfigBool(val);
        });

        TextView note = new TextView(getContext());
        note.setText(getString(R.string.OEAppearancePlayerLyricsNote));
        note.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        note.setTextColor(Theme.getColor(Theme.key_dialogTextGray2));
        note.setPadding(0, dp(14), 0, dp(14));
        container.addView(note);

        scroll.addView(container);
        root.addView(scroll, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        TextView closeBtn = new TextView(getContext());
        closeBtn.setText(getString(R.string.Close));
        closeBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        int accent = Theme.getColor(Theme.key_featuredStickers_addButton);
        closeBtn.setTextColor(accent != 0 ? accent : 0xff00d2b4);
        closeBtn.setGravity(Gravity.RIGHT);
        closeBtn.setPadding(dp(16), dp(12), dp(12), dp(8));
        closeBtn.setOnClickListener(v -> dialog.dismiss());
        root.addView(closeBtn, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        dialog.setContentView(root);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int dialogWidth = Math.min(AndroidUtilities.displaySize.x - dp(32), dp(350));
            dialog.getWindow().setLayout(dialogWidth, dp(500));
        }
        dialog.show();
    }

    private void addSourceToggle(LinearLayout parent, String name, String desc, boolean checked, Utilities.Callback<Boolean> onToggle) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(10), dp(4), dp(10));

        LinearLayout textCol = new LinearLayout(getContext());
        textCol.setOrientation(LinearLayout.VERTICAL);

        TextView nameView = new TextView(getContext());
        nameView.setText(name);
        nameView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        nameView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        textCol.addView(nameView);

        TextView descView = new TextView(getContext());
        descView.setText(desc);
        descView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        descView.setTextColor(Theme.getColor(Theme.key_dialogTextGray2));
        textCol.addView(descView);

        row.addView(textCol, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        Switch sw = new Switch(getContext());
        sw.setChecked(checked, false);
        LinearLayout.LayoutParams swLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        swLp.leftMargin = dp(14);
        swLp.rightMargin = dp(4);
        row.addView(sw, swLp);

        row.setOnClickListener(v -> {
            boolean next = !sw.isChecked();
            sw.setChecked(next, true);
            onToggle.run(next);
        });

        parent.addView(row);
    }

    private void showLyricsPriorityDialog() {
        if (getContext() == null) return;
        android.app.Dialog dialog = new android.app.Dialog(getContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout root = new LinearLayout(getContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(20), dp(20), dp(16));

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(24));
        bg.setColor(Theme.getColor(Theme.key_dialogBackground));
        root.setBackground(bg);

        TextView title = new TextView(getContext());
        title.setText(getString(R.string.OEAppearancePlayerLyricsPriority));
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        title.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        title.setTypeface(AndroidUtilities.bold());
        root.addView(title);

        TextView desc = new TextView(getContext());
        desc.setText(getString(R.string.OEAppearancePlayerLyricsPriorityDesc));
        desc.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        desc.setTextColor(Theme.getColor(Theme.key_dialogTextGray2));
        desc.setPadding(0, dp(4), 0, dp(12));
        root.addView(desc);

        String currentOrder = AppearanceConfig.playerLyricsProviderOrder();
        final List<String> list = new ArrayList<>();
        for (String s : currentOrder.split(",")) {
            String trimmed = s.trim();
            if (!trimmed.isEmpty()) {
                list.add(trimmed);
            }
        }

        RecyclerView recycler = new RecyclerView(getContext());
        recycler.setLayoutManager(new LinearLayoutManager(getContext()));
        recycler.setOverScrollMode(View.OVER_SCROLL_NEVER);

        class PriorityViewHolder extends RecyclerView.ViewHolder {
            final TextView number;
            final TextView name;
            final ImageView handle;

            PriorityViewHolder(View itemView) {
                super(itemView);
                number = itemView.findViewById(1);
                name = itemView.findViewById(2);
                handle = itemView.findViewById(3);
            }
        }

        final ItemTouchHelper[] touchHelperRef = new ItemTouchHelper[1];

        RecyclerView.Adapter<PriorityViewHolder> adapter = new RecyclerView.Adapter<PriorityViewHolder>() {
            @NonNull
            @Override
            public PriorityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                LinearLayout row = new LinearLayout(getContext());
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(dp(12), dp(10), dp(12), dp(10));

                GradientDrawable rowBg = new GradientDrawable();
                rowBg.setCornerRadius(dp(12));
                rowBg.setColor(Theme.getColor(Theme.key_dialogButtonCorner));
                row.setBackground(rowBg);

                TextView numView = new TextView(getContext());
                numView.setId(1);
                numView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
                numView.setTypeface(AndroidUtilities.bold());
                numView.setTextColor(Theme.getColor(Theme.key_dialogTextGray2));
                numView.setGravity(Gravity.CENTER);
                row.addView(numView, new LinearLayout.LayoutParams(dp(24), LinearLayout.LayoutParams.WRAP_CONTENT));

                TextView nameView = new TextView(getContext());
                nameView.setId(2);
                nameView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
                nameView.setTypeface(AndroidUtilities.bold());
                nameView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
                LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                nameLp.leftMargin = dp(8);
                nameLp.rightMargin = dp(8);
                row.addView(nameView, nameLp);

                ImageView handleView = new ImageView(getContext());
                handleView.setId(3);
                handleView.setImageResource(R.drawable.msg_reorder);
                handleView.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_dialogTextGray2), PorterDuff.Mode.SRC_IN));
                handleView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                handleView.setPadding(dp(4), dp(4), dp(4), dp(4));
                row.addView(handleView, new LinearLayout.LayoutParams(dp(32), dp(32)));

                RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.bottomMargin = dp(6);
                row.setLayoutParams(lp);

                return new PriorityViewHolder(row);
            }

            @Override
            public void onBindViewHolder(@NonNull PriorityViewHolder holder, int position) {
                holder.number.setText(String.valueOf(position + 1));
                String providerName = list.get(position);
                holder.name.setText(providerName);
                holder.handle.setOnTouchListener((v, event) -> {
                    if (event.getActionMasked() == MotionEvent.ACTION_DOWN && touchHelperRef[0] != null) {
                        touchHelperRef[0].startDrag(holder);
                    }
                    return false;
                });
            }

            @Override
            public int getItemCount() {
                return list.size();
            }
        };

        ItemTouchHelper touchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                int from = viewHolder.getAdapterPosition();
                int to = target.getAdapterPosition();
                if (from >= 0 && to >= 0 && from < list.size() && to < list.size() && from != to) {
                    Collections.swap(list, from, to);
                    adapter.notifyItemMoved(from, to);
                    adapter.notifyItemChanged(from);
                    adapter.notifyItemChanged(to);
                    return true;
                }
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            }

            @Override
            public boolean isLongPressDragEnabled() {
                return true;
            }
        });
        touchHelperRef[0] = touchHelper;
        touchHelper.attachToRecyclerView(recycler);
        recycler.setAdapter(adapter);

        root.addView(recycler, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        TextView doneBtn = new TextView(getContext());
        doneBtn.setText(getString(R.string.Done));
        doneBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        doneBtn.setTypeface(AndroidUtilities.bold());
        int accent = Theme.getColor(Theme.key_featuredStickers_addButton);
        doneBtn.setTextColor(accent != 0 ? accent : 0xff00d2b4);
        doneBtn.setGravity(Gravity.RIGHT);
        doneBtn.setPadding(dp(16), dp(12), dp(8), dp(4));
        doneBtn.setOnClickListener(v -> {
            String newOrder = String.join(",", list);
            AppearanceConfig.playerLyricsProviderOrder.setConfigString(newOrder);
            dialog.dismiss();
            updateList();
            BulletinFactory.of(PlayerSettingsActivity.this).createSimpleBulletin(R.drawable.msg_filled_data_music, getString(R.string.Done)).show();
        });
        root.addView(doneBtn, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        dialog.setContentView(root);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int dialogWidth = Math.min(AndroidUtilities.displaySize.x - dp(32), dp(350));
            dialog.getWindow().setLayout(dialogWidth, dp(440));
        }
        dialog.show();
    }

    private void showSelector(String title, CharSequence[] options, Utilities.Callback<Integer> onSelected) {
        if (getContext() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), getResourceProvider());
        builder.setTitle(title);
        builder.setItems(options, (dialog, which) -> {
            onSelected.run(which);
            updateList();
        });
        builder.setNegativeButton(getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void updateList() {
        if (listView != null && listView.adapter != null) {
            listView.adapter.update(true);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateList();
    }
}
