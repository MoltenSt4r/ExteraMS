package app.exteraless.player;

import static org.telegram.messenger.LocaleController.getString;

import android.content.Context;
import android.content.Intent;
import android.media.audiofx.AudioEffect;
import android.view.View;
import android.widget.FrameLayout;

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
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;

import java.util.ArrayList;

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

    private static final int ID_LYRICS_LRCLIB = 20;
    private static final int ID_LYRICS_TEXT_SIZE = 21;
    private static final int ID_LYRICS_BLUR = 22;
    private static final int ID_LYRICS_AUTOSCROLL = 23;

    private static final int ID_SLEEP_TIMER = 30;
    private static final int ID_PAUSE_ON_MUTE = 31;
    private static final int ID_RESUME_ON_BLUETOOTH = 32;
    private static final int ID_SYSTEM_EQUALIZER = 33;

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
        String seekbarName = seekbarStyle == 1 ? getString(R.string.OEAppearancePlayerSeekbarStraight) : getString(R.string.OEAppearancePlayerSeekbarWavy);
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

        // --- Секция: Текст и караоке ---
        items.add(UItem.asHeader(getString(R.string.OEAppearancePlayerLyricsHeader)));
        items.add(UItem.asCheck(ID_LYRICS_LRCLIB, getString(R.string.OEPlayerFindLyrics))
                .setChecked(AppearanceConfig.lrclibAllowed.Bool()));

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
        items.add(UItem.asCheck(ID_LYRICS_AUTOSCROLL, getString(R.string.OEAppearancePlayerLyricsAutoScroll))
                .setChecked(AppearanceConfig.playerLyricsAutoScroll()));
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
            boolean next = !AppearanceConfig.md3Player();
            AppearanceConfig.md3Player.setConfigBool(next);
            updateList();
        } else if (item.id == ID_MD3_MINI_PLAYER) {
            boolean next = !AppearanceConfig.md3MiniPlayer();
            AppearanceConfig.md3MiniPlayer.setConfigBool(next);
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
            CharSequence[] options = {
                    getString(R.string.OEAppearancePlayerSeekbarWavy),
                    getString(R.string.OEAppearancePlayerSeekbarStraight)
            };
            showSelector(getString(R.string.OEAppearancePlayerSeekbarStyle), options, which -> {
                AppearanceConfig.playerSeekbarStyle.setConfigInt(which);
            });
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
        } else if (item.id == ID_LYRICS_LRCLIB) {
            AppearanceConfig.lrclibAllowed.setConfigBool(!AppearanceConfig.lrclibAllowed.Bool());
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
        } else if (item.id == ID_LYRICS_AUTOSCROLL) {
            AppearanceConfig.playerLyricsAutoScroll.setConfigBool(!AppearanceConfig.playerLyricsAutoScroll());
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
