package app.exteraless.settings;

import static org.telegram.messenger.LocaleController.getString;

import android.content.Context;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Components.BulletinFactory;

import app.exteraless.glyph.GlyphConfig;
import app.exteraless.glyph.GlyphController;
import tw.nekomimi.nekogram.settings.BaseNekoSettingsActivity;
import tw.nekomimi.nekogram.ui.cells.HeaderCell;

/**
 * Экран «Nothing Glyph» раздела exteraless: подсветка задней панели Nothing Phone
 * на события клиента с поддержкой различных стилей анимаций для звонков,
 * голосовых сообщений и входящих уведомлений.
 */
public class OpenExteraGlyphActivity extends BaseNekoSettingsActivity {

    private int glyphHeaderRow;
    private int glyphEnableRow;
    private int glyphNewMessageRow;
    private int glyphNewMessageAnimRow;
    private int glyphCallsRow;
    private int glyphCallsAnimRow;
    private int glyphRecordingRow;
    private int glyphRecordingAnimRow;
    private int glyphScreenOffRow;
    private int glyphPreviewRow;
    private int glyphDividerRow;

    public OpenExteraGlyphActivity() {
        super();
    }

    @Override
    public boolean onFragmentCreate() {
        GlyphConfig.init();
        return super.onFragmentCreate();
    }

    @Override
    protected void updateRows() {
        super.updateRows();

        glyphHeaderRow = addRow("glyphHeader");
        glyphEnableRow = addRow(GlyphConfig.enabled.getKey());
        if (GlyphConfig.enabled()) {
            glyphNewMessageRow = addRow(GlyphConfig.onNewMessage.getKey());
            if (GlyphConfig.onNewMessage()) {
                glyphNewMessageAnimRow = addRow("glyphNewMessageAnim");
            } else {
                glyphNewMessageAnimRow = -1;
            }
            glyphCallsRow = addRow(GlyphConfig.onCall.getKey());
            if (GlyphConfig.onCall()) {
                glyphCallsAnimRow = addRow("glyphCallsAnim");
            } else {
                glyphCallsAnimRow = -1;
            }
            glyphRecordingRow = addRow(GlyphConfig.onRecording.getKey());
            if (GlyphConfig.onRecording()) {
                glyphRecordingAnimRow = addRow("glyphRecordingAnim");
            } else {
                glyphRecordingAnimRow = -1;
            }
            glyphScreenOffRow = addRow(GlyphConfig.screenOffOnly.getKey());
            glyphPreviewRow = addRow("glyphPreview");
        } else {
            glyphNewMessageRow = glyphNewMessageAnimRow = -1;
            glyphCallsRow = glyphCallsAnimRow = -1;
            glyphRecordingRow = glyphRecordingAnimRow = -1;
            glyphScreenOffRow = -1;
            glyphPreviewRow = -1;
        }
        glyphDividerRow = addRow();
    }

    private void update() {
        updateRows();
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
    }

    private CharSequence[] messageAnimationOptions() {
        return new CharSequence[]{
                getString(R.string.OEGlyphAnimDoubleFlash),
                getString(R.string.OEGlyphAnimStrobe),
                getString(R.string.OEGlyphAnimSoftPulse),
                getString(R.string.OEGlyphAnimAccentRing)
        };
    }

    private CharSequence[] callAnimationOptions() {
        return new CharSequence[]{
                getString(R.string.OEGlyphAnimPulse),
                getString(R.string.OEGlyphAnimWave),
                getString(R.string.OEGlyphAnimBreathing)
        };
    }

    private CharSequence[] recordingAnimationOptions() {
        return new CharSequence[]{
                getString(R.string.OEGlyphAnimBreathing),
                getString(R.string.OEGlyphAnimAccentRing),
                getString(R.string.OEGlyphAnimHeartbeat)
        };
    }

    private void showChoice(String title, CharSequence[] options, int selected,
                            Utilities.Callback<Integer> onSelected) {
        if (getParentActivity() == null) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(title);
        builder.setItems(options, (dialog, which) -> onSelected.run(which));
        builder.setNegativeButton(getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    @Override
    protected String getActionBarTitle() {
        return getString(R.string.OEGlyphTitle);
    }

    @Override
    public int getSearchGuid() {
        return 25000;
    }

    @Override
    public int getSearchIcon() {
        return R.drawable.deproko_baseline_lamp_filled_24;
    }

    @Override
    public String getSearchPrefix() {
        return "OEGlyph";
    }

    @Override
    protected String getKey() {
        return "exteraless_glyph";
    }

    @Override
    protected BaseListAdapter createAdapter(Context context) {
        return new ListAdapter(context);
    }

    @Override
    protected void onItemClick(View view, int position, float x, float y) {
        if (position == glyphEnableRow) {
            boolean enabled = GlyphConfig.enabled.toggleConfigBool();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(enabled);
            }
            if (enabled) {
                GlyphController.getInstance().init();
            } else {
                GlyphController.getInstance().shutdown();
            }
            update();
        } else if (position == glyphNewMessageRow) {
            boolean enabled = GlyphConfig.onNewMessage.toggleConfigBool();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(enabled);
            }
            update();
        } else if (position == glyphNewMessageAnimRow) {
            CharSequence[] options = messageAnimationOptions();
            showChoice(getString(R.string.OEGlyphNewMessageAnim), options,
                    GlyphConfig.messageAnimationStyle(), which -> {
                        GlyphConfig.messageAnimationStyle.setConfigInt(which);
                        update();
                        GlyphController.getInstance().previewMessage();
                    });
        } else if (position == glyphCallsRow) {
            boolean enabled = GlyphConfig.onCall.toggleConfigBool();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(enabled);
            }
            update();
        } else if (position == glyphCallsAnimRow) {
            CharSequence[] options = callAnimationOptions();
            showChoice(getString(R.string.OEGlyphCallsAnim), options,
                    GlyphConfig.callAnimationStyle(), which -> {
                        GlyphConfig.callAnimationStyle.setConfigInt(which);
                        update();
                        GlyphController.getInstance().previewCall();
                    });
        } else if (position == glyphRecordingRow) {
            boolean enabled = GlyphConfig.onRecording.toggleConfigBool();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(enabled);
            }
            update();
        } else if (position == glyphRecordingAnimRow) {
            CharSequence[] options = recordingAnimationOptions();
            showChoice(getString(R.string.OEGlyphRecordingAnim), options,
                    GlyphConfig.recordingAnimationStyle(), which -> {
                        GlyphConfig.recordingAnimationStyle.setConfigInt(which);
                        update();
                        GlyphController.getInstance().previewRecording();
                    });
        } else if (position == glyphScreenOffRow) {
            boolean enabled = GlyphConfig.screenOffOnly.toggleConfigBool();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(enabled);
            }
        } else if (position == glyphPreviewRow) {
            if (!GlyphController.getInstance().isSupported()) {
                BulletinFactory.of(this)
                        .createSimpleBulletin(R.raw.info, getString(R.string.OEGlyphUnsupported))
                        .show();
                return;
            }
            GlyphController.getInstance().preview();
        }
    }

    private class ListAdapter extends BaseListAdapter {

        public ListAdapter(Context context) {
            super(context);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position, boolean partial) {
            switch (holder.getItemViewType()) {
                case TYPE_HEADER: {
                    HeaderCell cell = (HeaderCell) holder.itemView;
                    if (position == glyphHeaderRow) {
                        cell.setText(getString(R.string.OEGlyphTitle));
                    }
                    break;
                }
                case TYPE_CHECK: {
                    TextCheckCell cell = (TextCheckCell) holder.itemView;
                    if (position == glyphEnableRow) {
                        cell.setTextAndCheck(getString(R.string.OEGlyphEnable),
                                GlyphConfig.enabled(), true);
                    } else if (position == glyphNewMessageRow) {
                        cell.setTextAndCheck(getString(R.string.OEGlyphNewMessage),
                                GlyphConfig.onNewMessage(), glyphNewMessageAnimRow != -1);
                    } else if (position == glyphCallsRow) {
                        cell.setTextAndCheck(getString(R.string.OEGlyphCalls),
                                GlyphConfig.onCall(), glyphCallsAnimRow != -1);
                    } else if (position == glyphRecordingRow) {
                        cell.setTextAndCheck(getString(R.string.OEGlyphRecording),
                                GlyphConfig.onRecording(), glyphRecordingAnimRow != -1);
                    } else if (position == glyphScreenOffRow) {
                        cell.setTextAndCheck(getString(R.string.OEGlyphScreenOff),
                                GlyphConfig.screenOffOnly(), false);
                    }
                    break;
                }
                case TYPE_TEXT: {
                    TextCell cell = (TextCell) holder.itemView;
                    if (position == glyphPreviewRow) {
                        cell.setColors(Theme.key_windowBackgroundWhiteGrayIcon,
                                Theme.key_windowBackgroundWhiteBlackText);
                        cell.setTextAndIcon(getString(R.string.OEGlyphPreview),
                                R.drawable.deproko_baseline_lamp_filled_24, false);
                    } else if (position == glyphNewMessageAnimRow) {
                        CharSequence[] options = messageAnimationOptions();
                        int idx = GlyphConfig.messageAnimationStyle();
                        cell.setTextAndValue(getString(R.string.OEGlyphNewMessageAnim),
                                idx >= 0 && idx < options.length ? options[idx].toString() : "", true);
                    } else if (position == glyphCallsAnimRow) {
                        CharSequence[] options = callAnimationOptions();
                        int idx = GlyphConfig.callAnimationStyle();
                        cell.setTextAndValue(getString(R.string.OEGlyphCallsAnim),
                                idx >= 0 && idx < options.length ? options[idx].toString() : "", true);
                    } else if (position == glyphRecordingAnimRow) {
                        CharSequence[] options = recordingAnimationOptions();
                        int idx = GlyphConfig.recordingAnimationStyle();
                        cell.setTextAndValue(getString(R.string.OEGlyphRecordingAnim),
                                idx >= 0 && idx < options.length ? options[idx].toString() : "", true);
                    }
                    break;
                }
                case TYPE_INFO_PRIVACY: {
                    TextInfoPrivacyCell cell = (TextInfoPrivacyCell) holder.itemView;
                    if (position == glyphDividerRow) {
                        cell.setText(getString(R.string.OEGlyphInfo));
                        cell.setBackground(Theme.getThemedDrawable(mContext,
                                R.drawable.greydivider_bottom, Theme.key_windowBackgroundGrayShadow));
                    }
                    break;
                }
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == glyphHeaderRow) {
                return TYPE_HEADER;
            } else if (position == glyphPreviewRow || position == glyphNewMessageAnimRow || position == glyphCallsAnimRow || position == glyphRecordingAnimRow) {
                return TYPE_TEXT;
            } else if (position == glyphDividerRow) {
                return TYPE_INFO_PRIVACY;
            }
            return TYPE_CHECK;
        }
    }
}
