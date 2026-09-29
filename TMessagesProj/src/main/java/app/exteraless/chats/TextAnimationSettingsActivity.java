package app.exteraless.chats;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.LocaleController.getString;

import android.content.Context;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;

import java.util.ArrayList;

import app.exteraless.appearance.AltSeekbar;

/**
 * Settings screen for Text Typing Animations with an interactive live typing preview.
 */
public class TextAnimationSettingsActivity extends BaseFragment {

    private static final int ID_PREVIEW = 1;
    private static final int ID_ENABLE = 2;
    private static final int ID_DURATION = 3;

    private static final int ID_BLUR_ENABLE = 10;
    private static final int ID_BLUR_DURATION = 11;
    private static final int ID_BLUR_RADIUS = 12;
    private static final int ID_BLUR_DELAY = 13;

    private static final int ID_SLIDE_ENABLE = 20;
    private static final int ID_SLIDE_DIST = 21;
    private static final int ID_SCALE_ENABLE = 22;
    private static final int ID_SCALE_START = 23;
    private static final int ID_ROTATE_ENABLE = 24;
    private static final int ID_ROTATE_ANGLE = 25;

    private static final int ID_DELETE_ENABLE = 30;
    private static final int ID_PARTICLE_STYLE = 31;
    private static final int ID_PARTICLE_COUNT = 32;
    private static final int ID_PARTICLE_SPEED = 33;
    private static final int ID_PARTICLE_SPREAD = 34;
    private static final int ID_PARTICLE_SIZE = 35;

    private static final int ID_CURSOR_ENABLE = 40;
    private static final int ID_CURSOR_SPEED = 41;
    private static final int ID_CURSOR_WIDTH = 42;
    private static final int ID_LIQUID_ENABLE = 43;
    private static final int ID_LIQUID_STRETCH = 44;

    private static final int ID_IGNORE_SPACES = 50;
    private static final int ID_ANIMATE_ALL_LINES = 51;

    private UniversalRecyclerView listView;
    private EditTextBoldCursor previewEditText;
    private View previewCard;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(getString(R.string.OEChatsTextAnimation));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == 1) {
                    showResetDialog();
                }
            }
        });

        ActionBarMenuItem resetItem = actionBar.createMenu().addItem(1, R.drawable.msg_reset);
        resetItem.setContentDescription(getString(R.string.Reset));

        FrameLayout contentView = new FrameLayout(context);
        contentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        listView = new UniversalRecyclerView(this, this::fillItems, this::onItemClick, null);
        contentView.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        actionBar.setAdaptiveBackground(listView);

        fragmentView = contentView;
        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (TextAnimationController.getInstance().isEnabled()) {
            TextAnimationController.getInstance().start();
        }
    }

    private View createPreviewCard(Context context) {
        if (previewCard != null) {
            return previewCard;
        }
        FrameLayout container = new FrameLayout(context);
        container.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        container.setPadding(dp(16), dp(16), dp(16), dp(16));

        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(Theme.createRoundRectDrawable(dp(12), Theme.getColor(Theme.key_chat_inBubble)));
        card.setPadding(dp(16), dp(12), dp(16), dp(12));

        previewEditText = new EditTextBoldCursor(context);
        previewEditText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        previewEditText.setTextColor(Theme.getColor(Theme.key_chat_messageTextIn));
        previewEditText.setHintTextColor(Theme.getColor(Theme.key_chat_messageTextIn) & 0x66ffffff);
        previewEditText.setHint(getString(R.string.OEChatsTextAnimationPreviewHint));
        previewEditText.setBackground(null);
        previewEditText.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        previewEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        previewEditText.setCursorColor(Theme.getColor(Theme.key_chat_messageTextIn));
        previewEditText.setCursorWidth(dp(2));

        card.addView(previewEditText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        container.addView(card, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        previewCard = container;
        return previewCard;
    }

    private View createSlider(String title, int min, int max, int current, AltSeekbar.OnDrag onDrag) {
        Context context = getContext();
        AltSeekbar bar = new AltSeekbar(context, onDrag, min, max, title, String.valueOf(min), String.valueOf(max));
        bar.setProgress(current);
        bar.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        return bar;
    }

    private void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asCustom(ID_PREVIEW, createPreviewCard(getContext())));
        items.add(UItem.asShadow(getString(R.string.OEChatsTextAnimationInfo)));

        boolean enabled = TextAnimationController.getInstance().isEnabled();
        items.add(UItem.asCheck(ID_ENABLE, getString(R.string.OEChatsTextAnimationEnable)).setChecked(enabled));

        if (!enabled) {
            items.add(UItem.asShadow(null));
            return;
        }

        items.add(UItem.asCustom(ID_DURATION, createSlider(
                getString(R.string.OEChatsTextAnimationDuration), 100, 800,
                ChatsConfig.textAnimationDuration.Int(),
                val -> {
                    ChatsConfig.textAnimationDuration.setConfigInt(Math.round(val));
                    TextAnimationController.getInstance().updateSettings();
                }
        )));
        items.add(UItem.asShadow(null));

        // Blur
        items.add(UItem.asHeader(getString(R.string.OEChatsTextAnimationBlur)));
        boolean blurEnabled = ChatsConfig.textAnimationBlurEnabled.Bool();
        items.add(UItem.asCheck(ID_BLUR_ENABLE, getString(R.string.OEChatsTextAnimationBlur)).setChecked(blurEnabled));
        if (blurEnabled) {
            items.add(UItem.asCustom(ID_BLUR_RADIUS, createSlider(
                    getString(R.string.OEChatsTextAnimationBlurRadius), 1, 30,
                    ChatsConfig.textAnimationBlurRadius.Int(),
                    val -> {
                        ChatsConfig.textAnimationBlurRadius.setConfigInt(Math.round(val));
                        TextAnimationController.getInstance().updateSettings();
                    }
            )));
            items.add(UItem.asCustom(ID_BLUR_DURATION, createSlider(
                    getString(R.string.OEChatsTextAnimationBlurDuration), 100, 800,
                    ChatsConfig.textAnimationBlurDuration.Int(),
                    val -> {
                        ChatsConfig.textAnimationBlurDuration.setConfigInt(Math.round(val));
                        TextAnimationController.getInstance().updateSettings();
                    }
            )));
            items.add(UItem.asCustom(ID_BLUR_DELAY, createSlider(
                    getString(R.string.OEChatsTextAnimationBlurDelay), 0, 50,
                    ChatsConfig.textAnimationBlurTextDelay.Int(),
                    val -> {
                        ChatsConfig.textAnimationBlurTextDelay.setConfigInt(Math.round(val));
                        TextAnimationController.getInstance().updateSettings();
                    }
            )));
        }
        items.add(UItem.asShadow(null));

        // Motion & Scale
        items.add(UItem.asHeader(getString(R.string.OEChatsTextAnimationMotion)));
        boolean slideEnabled = ChatsConfig.textAnimationSlideEnabled.Bool();
        items.add(UItem.asCheck(ID_SLIDE_ENABLE, getString(R.string.OEChatsTextAnimationSlide)).setChecked(slideEnabled));
        if (slideEnabled) {
            items.add(UItem.asCustom(ID_SLIDE_DIST, createSlider(
                    getString(R.string.OEChatsTextAnimationSlideDist), 5, 50,
                    ChatsConfig.textAnimationSlideDist.Int(),
                    val -> {
                        ChatsConfig.textAnimationSlideDist.setConfigInt(Math.round(val));
                        TextAnimationController.getInstance().updateSettings();
                    }
            )));
        }

        boolean scaleEnabled = ChatsConfig.textAnimationScaleEnabled.Bool();
        items.add(UItem.asCheck(ID_SCALE_ENABLE, getString(R.string.OEChatsTextAnimationScale)).setChecked(scaleEnabled));
        if (scaleEnabled) {
            items.add(UItem.asCustom(ID_SCALE_START, createSlider(
                    getString(R.string.OEChatsTextAnimationScaleStart), 0, 90,
                    ChatsConfig.textAnimationScaleStart.Int(),
                    val -> {
                        ChatsConfig.textAnimationScaleStart.setConfigInt(Math.round(val));
                        TextAnimationController.getInstance().updateSettings();
                    }
            )));
        }

        boolean rotateEnabled = ChatsConfig.textAnimationRotateEnabled.Bool();
        items.add(UItem.asCheck(ID_ROTATE_ENABLE, getString(R.string.OEChatsTextAnimationRotate)).setChecked(rotateEnabled));
        if (rotateEnabled) {
            items.add(UItem.asCustom(ID_ROTATE_ANGLE, createSlider(
                    getString(R.string.OEChatsTextAnimationRotateAngle), -45, 45,
                    ChatsConfig.textAnimationRotateAngle.Int(),
                    val -> {
                        ChatsConfig.textAnimationRotateAngle.setConfigInt(Math.round(val));
                        TextAnimationController.getInstance().updateSettings();
                    }
            )));
        }
        items.add(UItem.asShadow(null));

        // Particles
        items.add(UItem.asHeader(getString(R.string.OEChatsTextAnimationParticles)));
        boolean deleteEnabled = ChatsConfig.textAnimationDeleteEnabled.Bool();
        items.add(UItem.asCheck(ID_DELETE_ENABLE, getString(R.string.OEChatsTextAnimationParticles)).setChecked(deleteEnabled));
        if (deleteEnabled) {
            items.add(UItem.asButton(ID_PARTICLE_STYLE, getString(R.string.OEChatsTextAnimationParticlesStyle),
                    getParticleStyleName(ChatsConfig.textAnimationParticleStyle.Int())));
            items.add(UItem.asCustom(ID_PARTICLE_COUNT, createSlider(
                    getString(R.string.OEChatsTextAnimationParticleCount), 1, 20,
                    ChatsConfig.textAnimationParticleCount.Int(),
                    val -> {
                        ChatsConfig.textAnimationParticleCount.setConfigInt(Math.round(val));
                        TextAnimationController.getInstance().updateSettings();
                    }
            )));
            items.add(UItem.asCustom(ID_PARTICLE_SPEED, createSlider(
                    getString(R.string.OEChatsTextAnimationParticleSpeed), 10, 100,
                    ChatsConfig.textAnimationParticleSpeed.Int(),
                    val -> {
                        ChatsConfig.textAnimationParticleSpeed.setConfigInt(Math.round(val));
                        TextAnimationController.getInstance().updateSettings();
                    }
            )));
            items.add(UItem.asCustom(ID_PARTICLE_SPREAD, createSlider(
                    getString(R.string.OEChatsTextAnimationParticleSpread), 10, 100,
                    ChatsConfig.textAnimationParticleSpread.Int(),
                    val -> {
                        ChatsConfig.textAnimationParticleSpread.setConfigInt(Math.round(val));
                        TextAnimationController.getInstance().updateSettings();
                    }
            )));
            items.add(UItem.asCustom(ID_PARTICLE_SIZE, createSlider(
                    getString(R.string.OEChatsTextAnimationParticleSize), 10, 100,
                    ChatsConfig.textAnimationParticleSize.Int(),
                    val -> {
                        ChatsConfig.textAnimationParticleSize.setConfigInt(Math.round(val));
                        TextAnimationController.getInstance().updateSettings();
                    }
            )));
        }
        items.add(UItem.asShadow(null));

        // Cursor
        items.add(UItem.asHeader(getString(R.string.OEChatsTextAnimationCursor)));
        boolean cursorEnabled = ChatsConfig.textAnimationCursorEnabled.Bool();
        items.add(UItem.asCheck(ID_CURSOR_ENABLE, getString(R.string.OEChatsTextAnimationCursorFluid)).setChecked(cursorEnabled));
        if (cursorEnabled) {
            items.add(UItem.asCustom(ID_CURSOR_SPEED, createSlider(
                    getString(R.string.OEChatsTextAnimationCursorSpeed), 10, 100,
                    ChatsConfig.textAnimationCursorSpeed.Int(),
                    val -> {
                        ChatsConfig.textAnimationCursorSpeed.setConfigInt(Math.round(val));
                        TextAnimationController.getInstance().updateSettings();
                    }
            )));
            items.add(UItem.asCustom(ID_CURSOR_WIDTH, createSlider(
                    getString(R.string.OEChatsTextAnimationCursorWidth), 1, 15,
                    ChatsConfig.textAnimationCursorWidth.Int(),
                    val -> {
                        ChatsConfig.textAnimationCursorWidth.setConfigInt(Math.round(val));
                        TextAnimationController.getInstance().updateSettings();
                    }
            )));

            boolean liquidEnabled = ChatsConfig.textAnimationLiquidCursorEnabled.Bool();
            items.add(UItem.asCheck(ID_LIQUID_ENABLE, getString(R.string.OEChatsTextAnimationLiquidCursor)).setChecked(liquidEnabled));
            if (liquidEnabled) {
                items.add(UItem.asCustom(ID_LIQUID_STRETCH, createSlider(
                        getString(R.string.OEChatsTextAnimationLiquidStretch), 10, 100,
                        ChatsConfig.textAnimationLiquidStretch.Int(),
                        val -> {
                            ChatsConfig.textAnimationLiquidStretch.setConfigInt(Math.round(val));
                            TextAnimationController.getInstance().updateSettings();
                        }
                )));
            }
        }
        items.add(UItem.asShadow(null));

        // More
        items.add(UItem.asHeader(getString(R.string.More)));
        items.add(UItem.asCheck(ID_IGNORE_SPACES, getString(R.string.OEChatsTextAnimationIgnoreSpaces))
                .setChecked(ChatsConfig.textAnimationIgnoreSpaces.Bool()));
        items.add(UItem.asCheck(ID_ANIMATE_ALL_LINES, getString(R.string.OEChatsTextAnimationAnimateAllLines))
                .setChecked(ChatsConfig.textAnimationAnimateAllLines.Bool()));
        items.add(UItem.asShadow(null));
    }

    private String getParticleStyleName(int style) {
        switch (style) {
            case 1:
                return getString(R.string.OEChatsTextAnimationParticlesSparks);
            case 2:
                return getString(R.string.OEChatsTextAnimationParticlesSnow);
            case 3:
                return getString(R.string.OEChatsTextAnimationParticlesPetals);
            case 4:
                return getString(R.string.OEChatsTextAnimationParticlesLetters);
            case 0:
            default:
                return getString(R.string.OEChatsTextAnimationParticlesDust);
        }
    }

    private void onItemClick(UItem item, View view, int position, float x, float y) {
        if (item.id == ID_ENABLE) {
            boolean enabled = !ChatsConfig.textAnimationEnabled.Bool();
            ChatsConfig.textAnimationEnabled.setConfigBool(enabled);
            if (enabled) {
                TextAnimationController.getInstance().start();
            } else {
                TextAnimationController.getInstance().stop();
            }
            listView.adapter.update(true);
        } else if (item.id == ID_BLUR_ENABLE) {
            ChatsConfig.textAnimationBlurEnabled.setConfigBool(!ChatsConfig.textAnimationBlurEnabled.Bool());
            TextAnimationController.getInstance().updateSettings();
            listView.adapter.update(true);
        } else if (item.id == ID_SLIDE_ENABLE) {
            ChatsConfig.textAnimationSlideEnabled.setConfigBool(!ChatsConfig.textAnimationSlideEnabled.Bool());
            TextAnimationController.getInstance().updateSettings();
            listView.adapter.update(true);
        } else if (item.id == ID_SCALE_ENABLE) {
            ChatsConfig.textAnimationScaleEnabled.setConfigBool(!ChatsConfig.textAnimationScaleEnabled.Bool());
            TextAnimationController.getInstance().updateSettings();
            listView.adapter.update(true);
        } else if (item.id == ID_ROTATE_ENABLE) {
            ChatsConfig.textAnimationRotateEnabled.setConfigBool(!ChatsConfig.textAnimationRotateEnabled.Bool());
            TextAnimationController.getInstance().updateSettings();
            listView.adapter.update(true);
        } else if (item.id == ID_DELETE_ENABLE) {
            ChatsConfig.textAnimationDeleteEnabled.setConfigBool(!ChatsConfig.textAnimationDeleteEnabled.Bool());
            TextAnimationController.getInstance().updateSettings();
            listView.adapter.update(true);
        } else if (item.id == ID_CURSOR_ENABLE) {
            ChatsConfig.textAnimationCursorEnabled.setConfigBool(!ChatsConfig.textAnimationCursorEnabled.Bool());
            TextAnimationController.getInstance().updateSettings();
            listView.adapter.update(true);
        } else if (item.id == ID_LIQUID_ENABLE) {
            ChatsConfig.textAnimationLiquidCursorEnabled.setConfigBool(!ChatsConfig.textAnimationLiquidCursorEnabled.Bool());
            TextAnimationController.getInstance().updateSettings();
            listView.adapter.update(true);
        } else if (item.id == ID_IGNORE_SPACES) {
            ChatsConfig.textAnimationIgnoreSpaces.setConfigBool(!ChatsConfig.textAnimationIgnoreSpaces.Bool());
            TextAnimationController.getInstance().updateSettings();
            listView.adapter.update(true);
        } else if (item.id == ID_ANIMATE_ALL_LINES) {
            ChatsConfig.textAnimationAnimateAllLines.setConfigBool(!ChatsConfig.textAnimationAnimateAllLines.Bool());
            TextAnimationController.getInstance().updateSettings();
            listView.adapter.update(true);
        } else if (item.id == ID_PARTICLE_STYLE) {
            showParticleStyleDialog();
        }
    }

    private void showParticleStyleDialog() {
        CharSequence[] items = new CharSequence[]{
                getString(R.string.OEChatsTextAnimationParticlesDust),
                getString(R.string.OEChatsTextAnimationParticlesSparks),
                getString(R.string.OEChatsTextAnimationParticlesSnow),
                getString(R.string.OEChatsTextAnimationParticlesPetals),
                getString(R.string.OEChatsTextAnimationParticlesLetters)
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), getResourceProvider());
        builder.setTitle(getString(R.string.OEChatsTextAnimationParticlesStyle));
        builder.setItems(items, (dialog, which) -> {
            ChatsConfig.textAnimationParticleStyle.setConfigInt(which);
            TextAnimationController.getInstance().updateSettings();
            dialog.dismiss();
            listView.adapter.update(true);
        });
        builder.setNegativeButton(getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void showResetDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), getResourceProvider());
        builder.setTitle(getString(R.string.OEChatsTextAnimationReset));
        builder.setMessage(getString(R.string.OEChatsTextAnimationResetConfirm));
        builder.setPositiveButton(getString(R.string.Reset), (dialog, which) -> {
            TextAnimationController.getInstance().resetToDefaults();
            listView.adapter.update(true);
        });
        builder.setNegativeButton(getString(R.string.Cancel), null);
        showDialog(builder.create());
    }
}
