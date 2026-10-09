package app.exteraless.chats;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.LocaleController.getString;

import android.content.Context;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import app.exteraless.appearance.AltSeekbar;
import tw.nekomimi.nekogram.settings.BaseNekoSettingsActivity;
import tw.nekomimi.nekogram.ui.cells.HeaderCell;

/**
 * Settings screen for Text Typing Animations with an interactive live typing preview and MD3 rounded cards.
 */
public class TextAnimationSettingsActivity extends BaseNekoSettingsActivity {

    private static final int TYPE_CUSTOM_PREVIEW = 100;
    private static final int TYPE_SLIDER = 101;

    private int previewRow;
    private int infoRow;
    private int enableRow;
    private int enableShadowRow;

    private int durationRow;
    private int durationShadowRow;

    private int blurHeaderRow;
    private int blurEnableRow;
    private int blurRadiusRow;
    private int blurDurationRow;
    private int blurDelayRow;
    private int blurShadowRow;

    private int motionHeaderRow;
    private int slideEnableRow;
    private int slideDistRow;
    private int scaleEnableRow;
    private int scaleStartRow;
    private int rotateEnableRow;
    private int rotateAngleRow;
    private int motionShadowRow;

    private int particlesHeaderRow;
    private int deleteEnableRow;
    private int particleStyleRow;
    private int particleCountRow;
    private int particleSpeedRow;
    private int particleSpreadRow;
    private int particleSizeRow;
    private int particlesShadowRow;

    private int cursorHeaderRow;
    private int cursorEnableRow;
    private int cursorSpeedRow;
    private int cursorWidthRow;
    private int liquidEnableRow;
    private int liquidStretchRow;
    private int cursorShadowRow;

    private int moreHeaderRow;
    private int ignoreSpacesRow;
    private int animateAllLinesRow;
    private int moreShadowRow;

    private View previewCard;
    private EditTextBoldCursor previewEditText;

    @Override
    protected String getKey() {
        return "text_animation_settings";
    }

    @Override
    protected String getActionBarTitle() {
        return getString(R.string.OEChatsTextAnimation);
    }

    @Override
    public ActionBar createActionBar(Context context) {
        ActionBar actionBar = super.createActionBar(context);
        ActionBarMenuItem resetItem = actionBar.createMenu().addItem(1, R.drawable.msg_reset);
        resetItem.setContentDescription(getString(R.string.Reset));
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
        return actionBar;
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
        container.setBackgroundColor(getThemedColor(Theme.key_windowBackgroundWhite));
        container.setPadding(dp(16), dp(16), dp(16), dp(16));

        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(Theme.createRoundRectDrawable(dp(12), getThemedColor(Theme.key_chat_inBubble)));
        card.setPadding(dp(16), dp(12), dp(16), dp(12));

        previewEditText = new EditTextBoldCursor(context);
        previewEditText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        previewEditText.setTextColor(getThemedColor(Theme.key_chat_messageTextIn));
        previewEditText.setHintTextColor(getThemedColor(Theme.key_chat_messageTextIn) & 0x66ffffff);
        previewEditText.setHint(getString(R.string.OEChatsTextAnimationPreviewHint));
        previewEditText.setBackground(null);
        previewEditText.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        previewEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        previewEditText.setCursorColor(getThemedColor(Theme.key_chat_messageTextIn));
        previewEditText.setCursorWidth(dp(2));

        card.addView(previewEditText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        container.addView(card, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        previewCard = container;
        return previewCard;
    }

    @Override
    protected void updateRows() {
        super.updateRows();

        previewRow = addRow("preview");
        infoRow = addRow("info");
        enableRow = addRow("enable");
        enableShadowRow = addRow();

        boolean enabled = TextAnimationController.getInstance().isEnabled();
        if (enabled) {
            durationRow = addRow("duration");
            durationShadowRow = addRow();

            blurHeaderRow = addRow("blurHeader");
            blurEnableRow = addRow("blurEnable");
            boolean blur = ChatsConfig.textAnimationBlurEnabled.Bool();
            if (blur) {
                blurRadiusRow = addRow("blurRadius");
                blurDurationRow = addRow("blurDuration");
                blurDelayRow = addRow("blurDelay");
            } else {
                blurRadiusRow = -1;
                blurDurationRow = -1;
                blurDelayRow = -1;
            }
            blurShadowRow = addRow();

            motionHeaderRow = addRow("motionHeader");
            slideEnableRow = addRow("slideEnable");
            if (ChatsConfig.textAnimationSlideEnabled.Bool()) {
                slideDistRow = addRow("slideDist");
            } else {
                slideDistRow = -1;
            }
            scaleEnableRow = addRow("scaleEnable");
            if (ChatsConfig.textAnimationScaleEnabled.Bool()) {
                scaleStartRow = addRow("scaleStart");
            } else {
                scaleStartRow = -1;
            }
            rotateEnableRow = addRow("rotateEnable");
            if (ChatsConfig.textAnimationRotateEnabled.Bool()) {
                rotateAngleRow = addRow("rotateAngle");
            } else {
                rotateAngleRow = -1;
            }
            motionShadowRow = addRow();

            particlesHeaderRow = addRow("particlesHeader");
            deleteEnableRow = addRow("deleteEnable");
            if (ChatsConfig.textAnimationDeleteEnabled.Bool()) {
                particleStyleRow = addRow("particleStyle");
                particleCountRow = addRow("particleCount");
                particleSpeedRow = addRow("particleSpeed");
                particleSpreadRow = addRow("particleSpread");
                particleSizeRow = addRow("particleSize");
            } else {
                particleStyleRow = -1;
                particleCountRow = -1;
                particleSpeedRow = -1;
                particleSpreadRow = -1;
                particleSizeRow = -1;
            }
            particlesShadowRow = addRow();

            cursorHeaderRow = addRow("cursorHeader");
            cursorEnableRow = addRow("cursorEnable");
            if (ChatsConfig.textAnimationCursorEnabled.Bool()) {
                cursorSpeedRow = addRow("cursorSpeed");
                cursorWidthRow = addRow("cursorWidth");
                liquidEnableRow = addRow("liquidEnable");
                if (ChatsConfig.textAnimationLiquidCursorEnabled.Bool()) {
                    liquidStretchRow = addRow("liquidStretch");
                } else {
                    liquidStretchRow = -1;
                }
            } else {
                cursorSpeedRow = -1;
                cursorWidthRow = -1;
                liquidEnableRow = -1;
                liquidStretchRow = -1;
            }
            cursorShadowRow = addRow();

            moreHeaderRow = addRow("moreHeader");
            ignoreSpacesRow = addRow("ignoreSpaces");
            animateAllLinesRow = addRow("animateAllLines");
            moreShadowRow = addRow();
        } else {
            durationRow = -1;
            durationShadowRow = -1;
            blurHeaderRow = -1;
            blurEnableRow = -1;
            blurRadiusRow = -1;
            blurDurationRow = -1;
            blurDelayRow = -1;
            blurShadowRow = -1;
            motionHeaderRow = -1;
            slideEnableRow = -1;
            slideDistRow = -1;
            scaleEnableRow = -1;
            scaleStartRow = -1;
            rotateEnableRow = -1;
            rotateAngleRow = -1;
            motionShadowRow = -1;
            particlesHeaderRow = -1;
            deleteEnableRow = -1;
            particleStyleRow = -1;
            particleCountRow = -1;
            particleSpeedRow = -1;
            particleSpreadRow = -1;
            particleSizeRow = -1;
            particlesShadowRow = -1;
            cursorHeaderRow = -1;
            cursorEnableRow = -1;
            cursorSpeedRow = -1;
            cursorWidthRow = -1;
            liquidEnableRow = -1;
            liquidStretchRow = -1;
            cursorShadowRow = -1;
            moreHeaderRow = -1;
            ignoreSpacesRow = -1;
            animateAllLinesRow = -1;
            moreShadowRow = -1;
        }
    }

    @Override
    protected BaseListAdapter createAdapter(Context context) {
        return new ListAdapter(context);
    }

    @Override
    protected void onItemClick(View view, int position, float x, float y) {
        if (position == enableRow) {
            boolean enabled = !ChatsConfig.textAnimationEnabled.Bool();
            ChatsConfig.textAnimationEnabled.setConfigBool(enabled);
            if (enabled) {
                TextAnimationController.getInstance().start();
            } else {
                TextAnimationController.getInstance().stop();
            }
            updateRows();
            if (listAdapter != null) listAdapter.notifyDataSetChanged();
        } else if (position == blurEnableRow) {
            ChatsConfig.textAnimationBlurEnabled.setConfigBool(!ChatsConfig.textAnimationBlurEnabled.Bool());
            TextAnimationController.getInstance().updateSettings();
            updateRows();
            if (listAdapter != null) listAdapter.notifyDataSetChanged();
        } else if (position == slideEnableRow) {
            ChatsConfig.textAnimationSlideEnabled.setConfigBool(!ChatsConfig.textAnimationSlideEnabled.Bool());
            TextAnimationController.getInstance().updateSettings();
            updateRows();
            if (listAdapter != null) listAdapter.notifyDataSetChanged();
        } else if (position == scaleEnableRow) {
            ChatsConfig.textAnimationScaleEnabled.setConfigBool(!ChatsConfig.textAnimationScaleEnabled.Bool());
            TextAnimationController.getInstance().updateSettings();
            updateRows();
            if (listAdapter != null) listAdapter.notifyDataSetChanged();
        } else if (position == rotateEnableRow) {
            ChatsConfig.textAnimationRotateEnabled.setConfigBool(!ChatsConfig.textAnimationRotateEnabled.Bool());
            TextAnimationController.getInstance().updateSettings();
            updateRows();
            if (listAdapter != null) listAdapter.notifyDataSetChanged();
        } else if (position == deleteEnableRow) {
            ChatsConfig.textAnimationDeleteEnabled.setConfigBool(!ChatsConfig.textAnimationDeleteEnabled.Bool());
            TextAnimationController.getInstance().updateSettings();
            updateRows();
            if (listAdapter != null) listAdapter.notifyDataSetChanged();
        } else if (position == cursorEnableRow) {
            ChatsConfig.textAnimationCursorEnabled.setConfigBool(!ChatsConfig.textAnimationCursorEnabled.Bool());
            TextAnimationController.getInstance().updateSettings();
            updateRows();
            if (listAdapter != null) listAdapter.notifyDataSetChanged();
        } else if (position == liquidEnableRow) {
            ChatsConfig.textAnimationLiquidCursorEnabled.setConfigBool(!ChatsConfig.textAnimationLiquidCursorEnabled.Bool());
            TextAnimationController.getInstance().updateSettings();
            updateRows();
            if (listAdapter != null) listAdapter.notifyDataSetChanged();
        } else if (position == ignoreSpacesRow) {
            ChatsConfig.textAnimationIgnoreSpaces.setConfigBool(!ChatsConfig.textAnimationIgnoreSpaces.Bool());
            TextAnimationController.getInstance().updateSettings();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(ChatsConfig.textAnimationIgnoreSpaces.Bool());
            }
        } else if (position == animateAllLinesRow) {
            ChatsConfig.textAnimationAnimateAllLines.setConfigBool(!ChatsConfig.textAnimationAnimateAllLines.Bool());
            TextAnimationController.getInstance().updateSettings();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(ChatsConfig.textAnimationAnimateAllLines.Bool());
            }
        } else if (position == particleStyleRow) {
            showParticleStyleDialog();
        }
    }

    public static class SliderCell extends FrameLayout {
        private AltSeekbar altSeekbar;

        public SliderCell(Context context) {
            super(context);
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        }

        public void set(String title, int min, int max, int current, AltSeekbar.OnDrag onDrag) {
            if (altSeekbar != null) {
                removeView(altSeekbar);
            }
            altSeekbar = new AltSeekbar(getContext(), onDrag, min, max, title, String.valueOf(min), String.valueOf(max));
            altSeekbar.setProgress(current);
            addView(altSeekbar, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        }
    }

    private class ListAdapter extends BaseListAdapter {

        public ListAdapter(Context context) {
            super(context);
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int type = holder.getItemViewType();
            return type == TYPE_CHECK || type == TYPE_SETTINGS || type == TYPE_SLIDER;
        }

        @Override
        protected boolean isSectionContent(int viewType) {
            if (viewType == TYPE_CUSTOM_PREVIEW || viewType == TYPE_SLIDER) {
                return true;
            }
            return super.isSectionContent(viewType);
        }

        @Override
        public int getItemViewType(int position) {
            if (position == previewRow) {
                return TYPE_CUSTOM_PREVIEW;
            } else if (position == infoRow) {
                return TYPE_INFO_PRIVACY;
            } else if (position == blurHeaderRow || position == motionHeaderRow || position == particlesHeaderRow
                    || position == cursorHeaderRow || position == moreHeaderRow) {
                return TYPE_HEADER;
            } else if (position == enableRow || position == blurEnableRow || position == slideEnableRow
                    || position == scaleEnableRow || position == rotateEnableRow || position == deleteEnableRow
                    || position == cursorEnableRow || position == liquidEnableRow || position == ignoreSpacesRow
                    || position == animateAllLinesRow) {
                return TYPE_CHECK;
            } else if (position == particleStyleRow) {
                return TYPE_SETTINGS;
            } else if (position == durationRow || position == blurRadiusRow || position == blurDurationRow
                    || position == blurDelayRow || position == slideDistRow || position == scaleStartRow
                    || position == rotateAngleRow || position == particleCountRow || position == particleSpeedRow
                    || position == particleSpreadRow || position == particleSizeRow || position == cursorSpeedRow
                    || position == cursorWidthRow || position == liquidStretchRow) {
                return TYPE_SLIDER;
            } else if (position == enableShadowRow || position == durationShadowRow || position == blurShadowRow
                    || position == motionShadowRow || position == particlesShadowRow || position == cursorShadowRow
                    || position == moreShadowRow) {
                return TYPE_SHADOW;
            }
            return TYPE_SETTINGS;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == TYPE_CUSTOM_PREVIEW) {
                View card = createPreviewCard(mContext);
                card.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                return new RecyclerListView.Holder(card);
            } else if (viewType == TYPE_SLIDER) {
                SliderCell cell = new SliderCell(mContext);
                cell.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                return new RecyclerListView.Holder(cell);
            }
            return super.onCreateViewHolder(parent, viewType);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, boolean partial) {
            int type = holder.getItemViewType();
            if (type == TYPE_HEADER) {
                HeaderCell cell = (HeaderCell) holder.itemView;
                if (position == blurHeaderRow) {
                    cell.setText(getString(R.string.OEChatsTextAnimationBlur));
                } else if (position == motionHeaderRow) {
                    cell.setText(getString(R.string.OEChatsTextAnimationMotion));
                } else if (position == particlesHeaderRow) {
                    cell.setText(getString(R.string.OEChatsTextAnimationParticles));
                } else if (position == cursorHeaderRow) {
                    cell.setText(getString(R.string.OEChatsTextAnimationCursor));
                } else if (position == moreHeaderRow) {
                    cell.setText(getString(R.string.More));
                }
            } else if (type == TYPE_CHECK) {
                TextCheckCell cell = (TextCheckCell) holder.itemView;
                if (position == enableRow) {
                    cell.setTextAndCheck(getString(R.string.OEChatsTextAnimationEnable), TextAnimationController.getInstance().isEnabled(), false);
                } else if (position == blurEnableRow) {
                    cell.setTextAndCheck(getString(R.string.OEChatsTextAnimationBlur), ChatsConfig.textAnimationBlurEnabled.Bool(), true);
                } else if (position == slideEnableRow) {
                    cell.setTextAndCheck(getString(R.string.OEChatsTextAnimationSlide), ChatsConfig.textAnimationSlideEnabled.Bool(), true);
                } else if (position == scaleEnableRow) {
                    cell.setTextAndCheck(getString(R.string.OEChatsTextAnimationScale), ChatsConfig.textAnimationScaleEnabled.Bool(), true);
                } else if (position == rotateEnableRow) {
                    cell.setTextAndCheck(getString(R.string.OEChatsTextAnimationRotate), ChatsConfig.textAnimationRotateEnabled.Bool(), true);
                } else if (position == deleteEnableRow) {
                    cell.setTextAndCheck(getString(R.string.OEChatsTextAnimationParticles), ChatsConfig.textAnimationDeleteEnabled.Bool(), true);
                } else if (position == cursorEnableRow) {
                    cell.setTextAndCheck(getString(R.string.OEChatsTextAnimationCursorFluid), ChatsConfig.textAnimationCursorEnabled.Bool(), true);
                } else if (position == liquidEnableRow) {
                    cell.setTextAndCheck(getString(R.string.OEChatsTextAnimationLiquidCursor), ChatsConfig.textAnimationLiquidCursorEnabled.Bool(), true);
                } else if (position == ignoreSpacesRow) {
                    cell.setTextAndCheck(getString(R.string.OEChatsTextAnimationIgnoreSpaces), ChatsConfig.textAnimationIgnoreSpaces.Bool(), true);
                } else if (position == animateAllLinesRow) {
                    cell.setTextAndCheck(getString(R.string.OEChatsTextAnimationAnimateAllLines), ChatsConfig.textAnimationAnimateAllLines.Bool(), false);
                }
            } else if (type == TYPE_SETTINGS) {
                TextSettingsCell cell = (TextSettingsCell) holder.itemView;
                cell.setIcon(0);
                if (position == particleStyleRow) {
                    cell.setTextAndValue(getString(R.string.OEChatsTextAnimationParticlesStyle), getParticleStyleName(ChatsConfig.textAnimationParticleStyle.Int()), true);
                }
            } else if (type == TYPE_SLIDER) {
                SliderCell cell = (SliderCell) holder.itemView;
                if (position == durationRow) {
                    cell.set(getString(R.string.OEChatsTextAnimationDuration), 100, 800, ChatsConfig.textAnimationDuration.Int(),
                            val -> {
                                ChatsConfig.textAnimationDuration.setConfigInt(Math.round(val));
                                TextAnimationController.getInstance().updateSettings();
                            });
                } else if (position == blurRadiusRow) {
                    cell.set(getString(R.string.OEChatsTextAnimationBlurRadius), 1, 30, ChatsConfig.textAnimationBlurRadius.Int(),
                            val -> {
                                ChatsConfig.textAnimationBlurRadius.setConfigInt(Math.round(val));
                                TextAnimationController.getInstance().updateSettings();
                            });
                } else if (position == blurDurationRow) {
                    cell.set(getString(R.string.OEChatsTextAnimationBlurDuration), 100, 800, ChatsConfig.textAnimationBlurDuration.Int(),
                            val -> {
                                ChatsConfig.textAnimationBlurDuration.setConfigInt(Math.round(val));
                                TextAnimationController.getInstance().updateSettings();
                            });
                } else if (position == blurDelayRow) {
                    cell.set(getString(R.string.OEChatsTextAnimationBlurDelay), 0, 50, ChatsConfig.textAnimationBlurTextDelay.Int(),
                            val -> {
                                ChatsConfig.textAnimationBlurTextDelay.setConfigInt(Math.round(val));
                                TextAnimationController.getInstance().updateSettings();
                            });
                } else if (position == slideDistRow) {
                    cell.set(getString(R.string.OEChatsTextAnimationSlideDist), 5, 50, ChatsConfig.textAnimationSlideDist.Int(),
                            val -> {
                                ChatsConfig.textAnimationSlideDist.setConfigInt(Math.round(val));
                                TextAnimationController.getInstance().updateSettings();
                            });
                } else if (position == scaleStartRow) {
                    cell.set(getString(R.string.OEChatsTextAnimationScaleStart), 0, 90, ChatsConfig.textAnimationScaleStart.Int(),
                            val -> {
                                ChatsConfig.textAnimationScaleStart.setConfigInt(Math.round(val));
                                TextAnimationController.getInstance().updateSettings();
                            });
                } else if (position == rotateAngleRow) {
                    cell.set(getString(R.string.OEChatsTextAnimationRotateAngle), -45, 45, ChatsConfig.textAnimationRotateAngle.Int(),
                            val -> {
                                ChatsConfig.textAnimationRotateAngle.setConfigInt(Math.round(val));
                                TextAnimationController.getInstance().updateSettings();
                            });
                } else if (position == particleCountRow) {
                    cell.set(getString(R.string.OEChatsTextAnimationParticleCount), 1, 20, ChatsConfig.textAnimationParticleCount.Int(),
                            val -> {
                                ChatsConfig.textAnimationParticleCount.setConfigInt(Math.round(val));
                                TextAnimationController.getInstance().updateSettings();
                            });
                } else if (position == particleSpeedRow) {
                    cell.set(getString(R.string.OEChatsTextAnimationParticleSpeed), 10, 100, ChatsConfig.textAnimationParticleSpeed.Int(),
                            val -> {
                                ChatsConfig.textAnimationParticleSpeed.setConfigInt(Math.round(val));
                                TextAnimationController.getInstance().updateSettings();
                            });
                } else if (position == particleSpreadRow) {
                    cell.set(getString(R.string.OEChatsTextAnimationParticleSpread), 10, 100, ChatsConfig.textAnimationParticleSpread.Int(),
                            val -> {
                                ChatsConfig.textAnimationParticleSpread.setConfigInt(Math.round(val));
                                TextAnimationController.getInstance().updateSettings();
                            });
                } else if (position == particleSizeRow) {
                    cell.set(getString(R.string.OEChatsTextAnimationParticleSize), 10, 100, ChatsConfig.textAnimationParticleSize.Int(),
                            val -> {
                                ChatsConfig.textAnimationParticleSize.setConfigInt(Math.round(val));
                                TextAnimationController.getInstance().updateSettings();
                            });
                } else if (position == cursorSpeedRow) {
                    cell.set(getString(R.string.OEChatsTextAnimationCursorSpeed), 10, 100, ChatsConfig.textAnimationCursorSpeed.Int(),
                            val -> {
                                ChatsConfig.textAnimationCursorSpeed.setConfigInt(Math.round(val));
                                TextAnimationController.getInstance().updateSettings();
                            });
                } else if (position == cursorWidthRow) {
                    cell.set(getString(R.string.OEChatsTextAnimationCursorWidth), 1, 15, ChatsConfig.textAnimationCursorWidth.Int(),
                            val -> {
                                ChatsConfig.textAnimationCursorWidth.setConfigInt(Math.round(val));
                                TextAnimationController.getInstance().updateSettings();
                            });
                } else if (position == liquidStretchRow) {
                    cell.set(getString(R.string.OEChatsTextAnimationLiquidStretch), 10, 100, ChatsConfig.textAnimationLiquidStretch.Int(),
                            val -> {
                                ChatsConfig.textAnimationLiquidStretch.setConfigInt(Math.round(val));
                                TextAnimationController.getInstance().updateSettings();
                            });
                }
            } else if (type == TYPE_INFO_PRIVACY) {
                TextInfoPrivacyCell cell = (TextInfoPrivacyCell) holder.itemView;
                if (position == infoRow) {
                    cell.setText(getString(R.string.OEChatsTextAnimationInfo));
                }
            }
        }
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
            if (listAdapter != null) listAdapter.notifyItemChanged(particleStyleRow);
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
            updateRows();
            if (listAdapter != null) listAdapter.notifyDataSetChanged();
        });
        builder.setNegativeButton(getString(R.string.Cancel), null);
        showDialog(builder.create());
    }
}
