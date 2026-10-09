package app.exteraless.appearance;

import static org.telegram.messenger.LocaleController.getString;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.Paint.ColorPickerBottomSheet;
import org.telegram.ui.Components.RecyclerListView;

import tw.nekomimi.nekogram.settings.BaseNekoSettingsActivity;
import tw.nekomimi.nekogram.ui.cells.HeaderCell;

/**
 * Settings screen for Material 3 Chip Folders with live real-time FoldersPreviewCell and MD3 rounded cards.
 */
public class ChipFoldersSettingsActivity extends BaseNekoSettingsActivity {

    private static final int TYPE_CUSTOM_PREVIEW = 100;
    private static final int TYPE_SLIDER = 101;

    private int previewRow;
    private int infoRow;
    private int enableRow;
    private int enableShadowRow;

    private int styleHeaderRow;
    private int styleRow;
    private int md3ColorsRow;
    private int colorActiveRow;
    private int colorInactiveRow;
    private int colorTextActiveRow;
    private int colorTextInactiveRow;
    private int shapeRow;
    private int radiusActiveRow;
    private int radiusOuterRow;
    private int radiusInnerRow;
    private int radiusAltActiveRow;
    private int radiusAltOuterRow;
    private int radiusCustomActiveRow;
    private int radiusInactiveRow;
    private int animRow;
    private int animSpeedRow;
    private int styleShadowRow;

    private int sizeHeaderRow;
    private int sizeRow;
    private int heightRow;
    private int spacingRow;
    private int spacingCustomRow;
    private int bottomPaddingRow;
    private int listTopPaddingRow;
    private int sizeShadowRow;

    private int moreHeaderRow;
    private int scrollDividerRow;
    private int scrollDividerInfoRow;

    private FoldersPreviewCell foldersPreviewCell;

    @Override
    protected String getKey() {
        return "chip_folders_settings";
    }

    @Override
    protected String getActionBarTitle() {
        return getString(R.string.OEAppearanceChipFolders);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (ChipFoldersController.getInstance().isEnabled()) {
            ChipFoldersController.getInstance().start();
        }
        if (foldersPreviewCell != null) {
            ChipFoldersController.getInstance().refreshPreview(foldersPreviewCell);
        }
    }

    @Override
    protected void updateRows() {
        super.updateRows();

        previewRow = addRow("preview");
        infoRow = addRow("info");
        enableRow = addRow("enable");
        enableShadowRow = addRow();

        boolean enabled = ChipFoldersController.getInstance().isEnabled();
        if (enabled) {
            styleHeaderRow = addRow("styleHeader");
            styleRow = addRow("style");
            md3ColorsRow = addRow("md3Colors");

            boolean md3Colors = AppearanceConfig.chipFoldersMd3Colors.Bool();
            if (!md3Colors) {
                colorActiveRow = addRow("colorActive");
                if (AppearanceConfig.chipFoldersStyle.Int() != 1) {
                    colorInactiveRow = addRow("colorInactive");
                } else {
                    colorInactiveRow = -1;
                }
                colorTextActiveRow = addRow("colorTextActive");
                colorTextInactiveRow = addRow("colorTextInactive");
            } else {
                colorActiveRow = -1;
                colorInactiveRow = -1;
                colorTextActiveRow = -1;
                colorTextInactiveRow = -1;
            }

            shapeRow = addRow("shape");
            int shape = AppearanceConfig.chipFoldersShape.Int();
            if (shape == 5) {
                radiusActiveRow = addRow("radiusActive");
                radiusOuterRow = addRow("radiusOuter");
                radiusInnerRow = addRow("radiusInner");
                radiusAltActiveRow = -1;
                radiusAltOuterRow = -1;
                radiusCustomActiveRow = -1;
                radiusInactiveRow = -1;
            } else if (shape == 6) {
                radiusActiveRow = -1;
                radiusOuterRow = -1;
                radiusInnerRow = -1;
                radiusAltActiveRow = addRow("radiusAltActive");
                radiusAltOuterRow = addRow("radiusAltOuter");
                radiusCustomActiveRow = -1;
                radiusInactiveRow = -1;
            } else if (shape == 7) {
                radiusActiveRow = -1;
                radiusOuterRow = -1;
                radiusInnerRow = -1;
                radiusAltActiveRow = -1;
                radiusAltOuterRow = -1;
                radiusCustomActiveRow = addRow("radiusCustomActive");
                radiusInactiveRow = addRow("radiusInactive");
            } else {
                radiusActiveRow = -1;
                radiusOuterRow = -1;
                radiusInnerRow = -1;
                radiusAltActiveRow = -1;
                radiusAltOuterRow = -1;
                radiusCustomActiveRow = -1;
                radiusInactiveRow = -1;
            }

            animRow = addRow("anim");
            if (AppearanceConfig.chipFoldersAnim.Int() != 0) {
                animSpeedRow = addRow("animSpeed");
            } else {
                animSpeedRow = -1;
            }
            styleShadowRow = addRow();

            sizeHeaderRow = addRow("sizeHeader");
            sizeRow = addRow("size");
            if (AppearanceConfig.chipFoldersSize.Int() == 3) {
                heightRow = addRow("height");
            } else {
                heightRow = -1;
            }
            spacingRow = addRow("spacing");
            if (AppearanceConfig.chipFoldersSpacing.Int() == 3) {
                spacingCustomRow = addRow("spacingCustom");
            } else {
                spacingCustomRow = -1;
            }
            bottomPaddingRow = addRow("bottomPadding");
            listTopPaddingRow = addRow("listTopPadding");
            sizeShadowRow = addRow();

            moreHeaderRow = addRow("moreHeader");
            scrollDividerRow = addRow("scrollDivider");
            scrollDividerInfoRow = addRow("scrollDividerInfo");
        } else {
            styleHeaderRow = -1;
            styleRow = -1;
            md3ColorsRow = -1;
            colorActiveRow = -1;
            colorInactiveRow = -1;
            colorTextActiveRow = -1;
            colorTextInactiveRow = -1;
            shapeRow = -1;
            radiusActiveRow = -1;
            radiusOuterRow = -1;
            radiusInnerRow = -1;
            radiusAltActiveRow = -1;
            radiusAltOuterRow = -1;
            radiusCustomActiveRow = -1;
            radiusInactiveRow = -1;
            animRow = -1;
            animSpeedRow = -1;
            styleShadowRow = -1;
            sizeHeaderRow = -1;
            sizeRow = -1;
            heightRow = -1;
            spacingRow = -1;
            spacingCustomRow = -1;
            bottomPaddingRow = -1;
            listTopPaddingRow = -1;
            sizeShadowRow = -1;
            moreHeaderRow = -1;
            scrollDividerRow = -1;
            scrollDividerInfoRow = -1;
        }
    }

    @Override
    protected BaseListAdapter createAdapter(Context context) {
        return new ListAdapter(context);
    }

    private View createPreview(Context context) {
        if (foldersPreviewCell == null) {
            foldersPreviewCell = new FoldersPreviewCell(context, getResourceProvider());
        }
        ChipFoldersController.getInstance().refreshPreview(foldersPreviewCell);
        return foldersPreviewCell;
    }

    private void onSettingChanged(Runnable action) {
        action.run();
        ChipFoldersController.getInstance().updateSettings();
        if (foldersPreviewCell != null) {
            ChipFoldersController.getInstance().refreshPreview(foldersPreviewCell);
        }
    }

    @Override
    protected void onItemClick(View view, int position, float x, float y) {
        if (position == enableRow) {
            boolean enabled = !AppearanceConfig.chipFoldersEnabled.Bool();
            AppearanceConfig.chipFoldersEnabled.setConfigBool(enabled);
            if (enabled) {
                ChipFoldersController.getInstance().start();
                if (foldersPreviewCell != null) {
                    ChipFoldersController.getInstance().refreshPreview(foldersPreviewCell);
                }
            } else {
                ChipFoldersController.getInstance().stop();
            }
            getNotificationCenter().postNotificationName(NotificationCenter.dialogFiltersUpdated);
            updateRows();
            if (listAdapter != null) {
                listAdapter.notifyDataSetChanged();
            }
        } else if (position == md3ColorsRow) {
            AppearanceConfig.chipFoldersMd3Colors.setConfigBool(!AppearanceConfig.chipFoldersMd3Colors.Bool());
            ChipFoldersController.getInstance().updateSettings();
            if (foldersPreviewCell != null) {
                ChipFoldersController.getInstance().refreshPreview(foldersPreviewCell);
            }
            updateRows();
            if (listAdapter != null) {
                listAdapter.notifyDataSetChanged();
            }
        } else if (position == scrollDividerRow) {
            AppearanceConfig.chipFoldersScrollDivider.setConfigBool(!AppearanceConfig.chipFoldersScrollDivider.Bool());
            ChipFoldersController.getInstance().updateSettings();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(AppearanceConfig.chipFoldersScrollDivider.Bool());
            }
        } else if (position == styleRow) {
            showStyleDialog();
        } else if (position == shapeRow) {
            showShapeDialog();
        } else if (position == animRow) {
            showAnimDialog();
        } else if (position == sizeRow) {
            showSizeDialog();
        } else if (position == spacingRow) {
            showSpacingDialog();
        } else if (position == colorActiveRow) {
            showColorPicker(AppearanceConfig.chipFoldersColorActive);
        } else if (position == colorInactiveRow) {
            showColorPicker(AppearanceConfig.chipFoldersColorInactive);
        } else if (position == colorTextActiveRow) {
            showColorPicker(AppearanceConfig.chipFoldersColorTextActive);
        } else if (position == colorTextInactiveRow) {
            showColorPicker(AppearanceConfig.chipFoldersColorTextInactive);
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
            } else if (position == infoRow || position == scrollDividerInfoRow) {
                return TYPE_INFO_PRIVACY;
            } else if (position == styleHeaderRow || position == sizeHeaderRow || position == moreHeaderRow) {
                return TYPE_HEADER;
            } else if (position == enableRow || position == md3ColorsRow || position == scrollDividerRow) {
                return TYPE_CHECK;
            } else if (position == styleRow || position == shapeRow || position == animRow || position == sizeRow
                    || position == spacingRow || position == colorActiveRow || position == colorInactiveRow
                    || position == colorTextActiveRow || position == colorTextInactiveRow) {
                return TYPE_SETTINGS;
            } else if (position == radiusActiveRow || position == radiusOuterRow || position == radiusInnerRow
                    || position == radiusAltActiveRow || position == radiusAltOuterRow || position == radiusCustomActiveRow
                    || position == radiusInactiveRow || position == animSpeedRow || position == heightRow
                    || position == spacingCustomRow || position == bottomPaddingRow || position == listTopPaddingRow) {
                return TYPE_SLIDER;
            } else if (position == enableShadowRow || position == styleShadowRow || position == sizeShadowRow) {
                return TYPE_SHADOW;
            }
            return TYPE_SETTINGS;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == TYPE_CUSTOM_PREVIEW) {
                FrameLayout container = new FrameLayout(mContext);
                container.setBackgroundColor(getThemedColor(Theme.key_windowBackgroundWhite));
                container.setPadding(0, AndroidUtilities.dp(8), 0, AndroidUtilities.dp(8));
                container.addView(createPreview(mContext), LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
                container.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                return new RecyclerListView.Holder(container);
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
                if (position == styleHeaderRow) {
                    cell.setText(getString(R.string.OEAppearanceChipFoldersStyle));
                } else if (position == sizeHeaderRow) {
                    cell.setText(getString(R.string.OEAppearanceChipFoldersSize));
                } else if (position == moreHeaderRow) {
                    cell.setText(getString(R.string.More));
                }
            } else if (type == TYPE_CHECK) {
                TextCheckCell cell = (TextCheckCell) holder.itemView;
                if (position == enableRow) {
                    cell.setTextAndCheck(getString(R.string.OEAppearanceChipFoldersEnable), ChipFoldersController.getInstance().isEnabled(), false);
                } else if (position == md3ColorsRow) {
                    cell.setTextAndCheck(getString(R.string.OEAppearanceChipFoldersMd3Colors), AppearanceConfig.chipFoldersMd3Colors.Bool(), true);
                } else if (position == scrollDividerRow) {
                    cell.setTextAndCheck(getString(R.string.OEAppearanceChipFoldersScrollDivider), AppearanceConfig.chipFoldersScrollDivider.Bool(), false);
                }
            } else if (type == TYPE_SETTINGS) {
                TextSettingsCell cell = (TextSettingsCell) holder.itemView;
                cell.setIcon(0);
                if (position == styleRow) {
                    cell.setTextAndValue(getString(R.string.OEAppearanceChipFoldersStyle), getStyleName(AppearanceConfig.chipFoldersStyle.Int()), true);
                } else if (position == colorActiveRow) {
                    cell.setText(getString(R.string.OEAppearanceChipFoldersColorActive), true);
                } else if (position == colorInactiveRow) {
                    cell.setText(getString(R.string.OEAppearanceChipFoldersColorInactive), true);
                } else if (position == colorTextActiveRow) {
                    cell.setText(getString(R.string.OEAppearanceChipFoldersColorTextActive), true);
                } else if (position == colorTextInactiveRow) {
                    cell.setText(getString(R.string.OEAppearanceChipFoldersColorTextInactive), true);
                } else if (position == shapeRow) {
                    cell.setTextAndValue(getString(R.string.OEAppearanceChipFoldersShape), getShapeName(AppearanceConfig.chipFoldersShape.Int()), true);
                } else if (position == animRow) {
                    cell.setTextAndValue(getString(R.string.OEAppearanceChipFoldersAnim), getAnimName(AppearanceConfig.chipFoldersAnim.Int()), AppearanceConfig.chipFoldersAnim.Int() != 0);
                } else if (position == sizeRow) {
                    cell.setTextAndValue(getString(R.string.OEAppearanceChipFoldersSize), getSizeName(AppearanceConfig.chipFoldersSize.Int()), true);
                } else if (position == spacingRow) {
                    cell.setTextAndValue(getString(R.string.OEAppearanceChipFoldersSpacing), getSpacingName(AppearanceConfig.chipFoldersSpacing.Int()), true);
                }
            } else if (type == TYPE_SLIDER) {
                SliderCell cell = (SliderCell) holder.itemView;
                if (position == radiusActiveRow) {
                    cell.set(getString(R.string.OEAppearanceChipFoldersRadiusActive), 0, 24, AppearanceConfig.chipFoldersRadiusActive.Int(),
                            val -> onSettingChanged(() -> AppearanceConfig.chipFoldersRadiusActive.setConfigInt(Math.round(val))));
                } else if (position == radiusOuterRow) {
                    cell.set(getString(R.string.OEAppearanceChipFoldersRadiusOuter), 0, 24, AppearanceConfig.chipFoldersRadiusOuter.Int(),
                            val -> onSettingChanged(() -> AppearanceConfig.chipFoldersRadiusOuter.setConfigInt(Math.round(val))));
                } else if (position == radiusInnerRow) {
                    cell.set(getString(R.string.OEAppearanceChipFoldersRadiusInner), 0, 24, AppearanceConfig.chipFoldersRadiusInner.Int(),
                            val -> onSettingChanged(() -> AppearanceConfig.chipFoldersRadiusInner.setConfigInt(Math.round(val))));
                } else if (position == radiusAltActiveRow) {
                    cell.set(getString(R.string.OEAppearanceChipFoldersRadiusActive), 0, 24, AppearanceConfig.chipFoldersRadiusAltActive.Int(),
                            val -> onSettingChanged(() -> AppearanceConfig.chipFoldersRadiusAltActive.setConfigInt(Math.round(val))));
                } else if (position == radiusAltOuterRow) {
                    cell.set(getString(R.string.OEAppearanceChipFoldersRadiusOuter), 0, 24, AppearanceConfig.chipFoldersRadiusAltOuter.Int(),
                            val -> onSettingChanged(() -> AppearanceConfig.chipFoldersRadiusAltOuter.setConfigInt(Math.round(val))));
                } else if (position == radiusCustomActiveRow) {
                    cell.set(getString(R.string.OEAppearanceChipFoldersRadiusActive), 0, 24, AppearanceConfig.chipFoldersRadiusCustomActive.Int(),
                            val -> onSettingChanged(() -> AppearanceConfig.chipFoldersRadiusCustomActive.setConfigInt(Math.round(val))));
                } else if (position == radiusInactiveRow) {
                    cell.set(getString(R.string.OEAppearanceChipFoldersRadiusInactive), 0, 24, AppearanceConfig.chipFoldersRadiusInactive.Int(),
                            val -> onSettingChanged(() -> AppearanceConfig.chipFoldersRadiusInactive.setConfigInt(Math.round(val))));
                } else if (position == animSpeedRow) {
                    cell.set(getString(R.string.OEAppearanceChipFoldersAnimSpeed), 50, 200, AppearanceConfig.chipFoldersAnimSpeed.Int(),
                            val -> onSettingChanged(() -> AppearanceConfig.chipFoldersAnimSpeed.setConfigInt(Math.round(val))));
                } else if (position == heightRow) {
                    cell.set(getString(R.string.OEAppearanceChipFoldersHeight), 36, 64, AppearanceConfig.chipFoldersCustomHeight.Int(),
                            val -> onSettingChanged(() -> AppearanceConfig.chipFoldersCustomHeight.setConfigInt(Math.round(val))));
                } else if (position == spacingCustomRow) {
                    cell.set(getString(R.string.OEAppearanceChipFoldersCustomSpacing), 0, 24, AppearanceConfig.chipFoldersCustomSpacing.Int(),
                            val -> onSettingChanged(() -> AppearanceConfig.chipFoldersCustomSpacing.setConfigInt(Math.round(val))));
                } else if (position == bottomPaddingRow) {
                    cell.set(getString(R.string.OEAppearanceChipFoldersBottomPadding), 0, 24, AppearanceConfig.chipFoldersBarBottomPadding.Int(),
                            val -> onSettingChanged(() -> AppearanceConfig.chipFoldersBarBottomPadding.setConfigInt(Math.round(val))));
                } else if (position == listTopPaddingRow) {
                    cell.set(getString(R.string.OEAppearanceChipFoldersListTopPadding), 0, 24, AppearanceConfig.chipFoldersListTopPadding.Int(),
                            val -> onSettingChanged(() -> AppearanceConfig.chipFoldersListTopPadding.setConfigInt(Math.round(val))));
                }
            } else if (type == TYPE_INFO_PRIVACY) {
                TextInfoPrivacyCell cell = (TextInfoPrivacyCell) holder.itemView;
                if (position == infoRow) {
                    cell.setText(getString(R.string.OEAppearanceChipFoldersInfo));
                } else if (position == scrollDividerInfoRow) {
                    cell.setText(getString(R.string.OEAppearanceChipFoldersScrollDividerInfo));
                }
            }
        }
    }

    private String getStyleName(int style) {
        switch (style) {
            case 1:
                return getString(R.string.OEAppearanceChipFoldersStyleOutlined);
            case 2:
                return getString(R.string.OEAppearanceChipFoldersStyleGhost);
            case 0:
            default:
                return getString(R.string.OEAppearanceChipFoldersStyleFilled);
        }
    }

    private String getShapeName(int shape) {
        switch (shape) {
            case 0:
                return getString(R.string.OEAppearanceChipFoldersShapeCapsule);
            case 1:
                return getString(R.string.OEAppearanceChipFoldersShapeRounded);
            case 2:
                return getString(R.string.OEAppearanceChipFoldersShapeSoft);
            case 3:
                return getString(R.string.OEAppearanceChipFoldersShapeSquare);
            case 4:
                return getString(R.string.OEAppearanceChipFoldersShapeDynamic);
            case 5:
                return getString(R.string.OEAppearanceChipFoldersShapeExpressive);
            case 6:
                return getString(R.string.OEAppearanceChipFoldersShapeAlternative);
            case 7:
            default:
                return getString(R.string.OEAppearanceChipFoldersShapeCustom);
        }
    }

    private String getAnimName(int anim) {
        switch (anim) {
            case 1:
                return getString(R.string.OEAppearanceChipFoldersAnimJelly);
            case 2:
                return getString(R.string.OEAppearanceChipFoldersAnimSqueeze);
            case 3:
                return getString(R.string.OEAppearanceChipFoldersAnimPress);
            case 4:
                return getString(R.string.OEAppearanceChipFoldersAnimSpring);
            case 0:
            default:
                return getString(R.string.OEAppearanceChipFoldersAnimNone);
        }
    }

    private String getSizeName(int size) {
        switch (size) {
            case 0:
                return getString(R.string.OEAppearanceChipFoldersSizeCompact);
            case 2:
                return getString(R.string.OEAppearanceChipFoldersSizeLarge);
            case 3:
                return getString(R.string.OEAppearanceChipFoldersSizeCustom);
            case 1:
            default:
                return getString(R.string.OEAppearanceChipFoldersSizeDefault);
        }
    }

    private String getSpacingName(int spacing) {
        switch (spacing) {
            case 0:
                return getString(R.string.OEAppearanceChipFoldersSpacingTight);
            case 2:
                return getString(R.string.OEAppearanceChipFoldersSpacingWide);
            case 3:
                return getString(R.string.OEAppearanceChipFoldersSpacingCustom);
            case 1:
            default:
                return getString(R.string.OEAppearanceChipFoldersSpacingDefault);
        }
    }

    private void showStyleDialog() {
        CharSequence[] items = new CharSequence[]{
                getString(R.string.OEAppearanceChipFoldersStyleFilled),
                getString(R.string.OEAppearanceChipFoldersStyleOutlined),
                getString(R.string.OEAppearanceChipFoldersStyleGhost)
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), getResourceProvider());
        builder.setTitle(getString(R.string.OEAppearanceChipFoldersStyle));
        builder.setItems(items, (dialog, which) -> {
            AppearanceConfig.chipFoldersStyle.setConfigInt(which);
            ChipFoldersController.getInstance().updateSettings();
            if (foldersPreviewCell != null) {
                ChipFoldersController.getInstance().refreshPreview(foldersPreviewCell);
            }
            dialog.dismiss();
            updateRows();
            if (listAdapter != null) listAdapter.notifyDataSetChanged();
        });
        builder.setNegativeButton(getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void showShapeDialog() {
        CharSequence[] items = new CharSequence[]{
                getString(R.string.OEAppearanceChipFoldersShapeCapsule),
                getString(R.string.OEAppearanceChipFoldersShapeRounded),
                getString(R.string.OEAppearanceChipFoldersShapeSoft),
                getString(R.string.OEAppearanceChipFoldersShapeSquare),
                getString(R.string.OEAppearanceChipFoldersShapeDynamic),
                getString(R.string.OEAppearanceChipFoldersShapeExpressive),
                getString(R.string.OEAppearanceChipFoldersShapeAlternative),
                getString(R.string.OEAppearanceChipFoldersShapeCustom)
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), getResourceProvider());
        builder.setTitle(getString(R.string.OEAppearanceChipFoldersShape));
        builder.setItems(items, (dialog, which) -> {
            AppearanceConfig.chipFoldersShape.setConfigInt(which);
            ChipFoldersController.getInstance().updateSettings();
            if (foldersPreviewCell != null) {
                ChipFoldersController.getInstance().refreshPreview(foldersPreviewCell);
            }
            dialog.dismiss();
            updateRows();
            if (listAdapter != null) listAdapter.notifyDataSetChanged();
        });
        builder.setNegativeButton(getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void showAnimDialog() {
        CharSequence[] items = new CharSequence[]{
                getString(R.string.OEAppearanceChipFoldersAnimNone),
                getString(R.string.OEAppearanceChipFoldersAnimJelly),
                getString(R.string.OEAppearanceChipFoldersAnimSqueeze),
                getString(R.string.OEAppearanceChipFoldersAnimPress),
                getString(R.string.OEAppearanceChipFoldersAnimSpring)
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), getResourceProvider());
        builder.setTitle(getString(R.string.OEAppearanceChipFoldersAnim));
        builder.setItems(items, (dialog, which) -> {
            AppearanceConfig.chipFoldersAnim.setConfigInt(which);
            ChipFoldersController.getInstance().updateSettings();
            if (foldersPreviewCell != null) {
                ChipFoldersController.getInstance().refreshPreview(foldersPreviewCell);
            }
            dialog.dismiss();
            updateRows();
            if (listAdapter != null) listAdapter.notifyDataSetChanged();
        });
        builder.setNegativeButton(getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void showSizeDialog() {
        CharSequence[] items = new CharSequence[]{
                getString(R.string.OEAppearanceChipFoldersSizeCompact),
                getString(R.string.OEAppearanceChipFoldersSizeDefault),
                getString(R.string.OEAppearanceChipFoldersSizeLarge),
                getString(R.string.OEAppearanceChipFoldersSizeCustom)
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), getResourceProvider());
        builder.setTitle(getString(R.string.OEAppearanceChipFoldersSize));
        builder.setItems(items, (dialog, which) -> {
            AppearanceConfig.chipFoldersSize.setConfigInt(which);
            ChipFoldersController.getInstance().updateSettings();
            if (foldersPreviewCell != null) {
                ChipFoldersController.getInstance().refreshPreview(foldersPreviewCell);
            }
            dialog.dismiss();
            updateRows();
            if (listAdapter != null) listAdapter.notifyDataSetChanged();
        });
        builder.setNegativeButton(getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void showSpacingDialog() {
        CharSequence[] items = new CharSequence[]{
                getString(R.string.OEAppearanceChipFoldersSpacingTight),
                getString(R.string.OEAppearanceChipFoldersSpacingDefault),
                getString(R.string.OEAppearanceChipFoldersSpacingWide),
                getString(R.string.OEAppearanceChipFoldersSpacingCustom)
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), getResourceProvider());
        builder.setTitle(getString(R.string.OEAppearanceChipFoldersSpacing));
        builder.setItems(items, (dialog, which) -> {
            AppearanceConfig.chipFoldersSpacing.setConfigInt(which);
            ChipFoldersController.getInstance().updateSettings();
            if (foldersPreviewCell != null) {
                ChipFoldersController.getInstance().refreshPreview(foldersPreviewCell);
            }
            dialog.dismiss();
            updateRows();
            if (listAdapter != null) listAdapter.notifyDataSetChanged();
        });
        builder.setNegativeButton(getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void showColorPicker(tw.nekomimi.nekogram.config.ConfigItem item) {
        int color = item.Int();
        if (color == 0) {
            color = Theme.getColor(Theme.key_windowBackgroundWhiteBlueText);
        }
        ColorPickerBottomSheet sheet = new ColorPickerBottomSheet(getContext(), getResourceProvider());
        sheet.setColor(color);
        sheet.setColorListener(newColor -> {
            item.setConfigInt(newColor);
            ChipFoldersController.getInstance().updateSettings();
            if (foldersPreviewCell != null) {
                ChipFoldersController.getInstance().refreshPreview(foldersPreviewCell);
            }
            if (listAdapter != null) listAdapter.notifyDataSetChanged();
        });
        sheet.show();
    }
}
