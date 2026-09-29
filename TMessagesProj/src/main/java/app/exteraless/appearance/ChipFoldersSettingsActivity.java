package app.exteraless.appearance;

import static org.telegram.messenger.LocaleController.getString;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;

import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.Paint.ColorPickerBottomSheet;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;

import java.util.ArrayList;

/**
 * Settings screen for Material 3 Chip Folders with live real-time FoldersPreviewCell.
 */
public class ChipFoldersSettingsActivity extends BaseFragment {

    private static final int ID_PREVIEW = 1;
    private static final int ID_ENABLE = 2;

    private static final int ID_STYLE = 10;
    private static final int ID_MD3_COLORS = 11;
    private static final int ID_COLOR_ACTIVE = 12;
    private static final int ID_COLOR_INACTIVE = 13;
    private static final int ID_COLOR_TEXT_ACTIVE = 14;
    private static final int ID_COLOR_TEXT_INACTIVE = 15;

    private static final int ID_SHAPE = 20;
    private static final int ID_RADIUS_ACTIVE = 21;
    private static final int ID_RADIUS_OUTER = 22;
    private static final int ID_RADIUS_INNER = 23;
    private static final int ID_RADIUS_INACTIVE = 24;
    private static final int ID_RADIUS_ALT_ACTIVE = 25;
    private static final int ID_RADIUS_ALT_OUTER = 26;

    private static final int ID_ANIM = 30;
    private static final int ID_ANIM_SPEED = 31;

    private static final int ID_SIZE = 40;
    private static final int ID_HEIGHT = 41;
    private static final int ID_SPACING = 42;
    private static final int ID_SPACING_CUSTOM = 43;
    private static final int ID_BOTTOM_PADDING = 44;
    private static final int ID_LIST_TOP_PADDING = 45;

    private static final int ID_SCROLL_DIVIDER = 50;

    private UniversalRecyclerView listView;
    private FoldersPreviewCell foldersPreviewCell;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(getString(R.string.OEAppearanceChipFolders));
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

    private View createPreview(Context context) {
        if (foldersPreviewCell == null) {
            foldersPreviewCell = new FoldersPreviewCell(context, getResourceProvider());
        }
        ChipFoldersController.getInstance().refreshPreview(foldersPreviewCell);
        return foldersPreviewCell;
    }

    private View createSlider(String title, int min, int max, int current, AltSeekbar.OnDrag onDrag) {
        Context context = getContext();
        AltSeekbar bar = new AltSeekbar(context, onDrag, min, max, title, String.valueOf(min), String.valueOf(max));
        bar.setProgress(current);
        bar.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        return bar;
    }

    private void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asCustom(ID_PREVIEW, createPreview(getContext())));
        items.add(UItem.asShadow(getString(R.string.OEAppearanceChipFoldersInfo)));

        boolean enabled = ChipFoldersController.getInstance().isEnabled();
        items.add(UItem.asCheck(ID_ENABLE, getString(R.string.OEAppearanceChipFoldersEnable)).setChecked(enabled));

        if (!enabled) {
            items.add(UItem.asShadow(null));
            return;
        }

        // Style
        items.add(UItem.asHeader(getString(R.string.OEAppearanceChipFoldersStyle)));
        items.add(UItem.asButton(ID_STYLE, getString(R.string.OEAppearanceChipFoldersStyle),
                getStyleName(AppearanceConfig.chipFoldersStyle.Int())));

        boolean md3Colors = AppearanceConfig.chipFoldersMd3Colors.Bool();
        items.add(UItem.asCheck(ID_MD3_COLORS, getString(R.string.OEAppearanceChipFoldersMd3Colors))
                .setChecked(md3Colors));

        if (!md3Colors) {
            items.add(UItem.asButton(ID_COLOR_ACTIVE, getString(R.string.OEAppearanceChipFoldersColorActive)));
            if (AppearanceConfig.chipFoldersStyle.Int() != 1) { // not outlined
                items.add(UItem.asButton(ID_COLOR_INACTIVE, getString(R.string.OEAppearanceChipFoldersColorInactive)));
            }
            items.add(UItem.asButton(ID_COLOR_TEXT_ACTIVE, getString(R.string.OEAppearanceChipFoldersColorTextActive)));
            items.add(UItem.asButton(ID_COLOR_TEXT_INACTIVE, getString(R.string.OEAppearanceChipFoldersColorTextInactive)));
        }

        // Shape
        items.add(UItem.asButton(ID_SHAPE, getString(R.string.OEAppearanceChipFoldersShape),
                getShapeName(AppearanceConfig.chipFoldersShape.Int())));

        int shape = AppearanceConfig.chipFoldersShape.Int();
        if (shape == 5) { // Expressive
            items.add(UItem.asCustom(ID_RADIUS_ACTIVE, createSlider(
                    getString(R.string.OEAppearanceChipFoldersRadiusActive), 0, 24,
                    AppearanceConfig.chipFoldersRadiusActive.Int(),
                    val -> onSettingChanged(() -> AppearanceConfig.chipFoldersRadiusActive.setConfigInt(Math.round(val)))
            )));
            items.add(UItem.asCustom(ID_RADIUS_OUTER, createSlider(
                    getString(R.string.OEAppearanceChipFoldersRadiusOuter), 0, 24,
                    AppearanceConfig.chipFoldersRadiusOuter.Int(),
                    val -> onSettingChanged(() -> AppearanceConfig.chipFoldersRadiusOuter.setConfigInt(Math.round(val)))
            )));
            items.add(UItem.asCustom(ID_RADIUS_INNER, createSlider(
                    getString(R.string.OEAppearanceChipFoldersRadiusInner), 0, 24,
                    AppearanceConfig.chipFoldersRadiusInner.Int(),
                    val -> onSettingChanged(() -> AppearanceConfig.chipFoldersRadiusInner.setConfigInt(Math.round(val)))
            )));
        } else if (shape == 6) { // Alternative
            items.add(UItem.asCustom(ID_RADIUS_ALT_ACTIVE, createSlider(
                    getString(R.string.OEAppearanceChipFoldersRadiusActive), 0, 24,
                    AppearanceConfig.chipFoldersRadiusAltActive.Int(),
                    val -> onSettingChanged(() -> AppearanceConfig.chipFoldersRadiusAltActive.setConfigInt(Math.round(val)))
            )));
            items.add(UItem.asCustom(ID_RADIUS_ALT_OUTER, createSlider(
                    getString(R.string.OEAppearanceChipFoldersRadiusOuter), 0, 24,
                    AppearanceConfig.chipFoldersRadiusAltOuter.Int(),
                    val -> onSettingChanged(() -> AppearanceConfig.chipFoldersRadiusAltOuter.setConfigInt(Math.round(val)))
            )));
        } else if (shape == 7) { // Custom
            items.add(UItem.asCustom(ID_RADIUS_ACTIVE, createSlider(
                    getString(R.string.OEAppearanceChipFoldersRadiusActive), 0, 24,
                    AppearanceConfig.chipFoldersRadiusCustomActive.Int(),
                    val -> onSettingChanged(() -> AppearanceConfig.chipFoldersRadiusCustomActive.setConfigInt(Math.round(val)))
            )));
            items.add(UItem.asCustom(ID_RADIUS_INACTIVE, createSlider(
                    getString(R.string.OEAppearanceChipFoldersRadiusInactive), 0, 24,
                    AppearanceConfig.chipFoldersRadiusInactive.Int(),
                    val -> onSettingChanged(() -> AppearanceConfig.chipFoldersRadiusInactive.setConfigInt(Math.round(val)))
            )));
        }

        // Animation
        items.add(UItem.asButton(ID_ANIM, getString(R.string.OEAppearanceChipFoldersAnim),
                getAnimName(AppearanceConfig.chipFoldersAnim.Int())));
        if (AppearanceConfig.chipFoldersAnim.Int() != 0) {
            items.add(UItem.asCustom(ID_ANIM_SPEED, createSlider(
                    getString(R.string.OEAppearanceChipFoldersAnimSpeed), 50, 200,
                    AppearanceConfig.chipFoldersAnimSpeed.Int(),
                    val -> onSettingChanged(() -> AppearanceConfig.chipFoldersAnimSpeed.setConfigInt(Math.round(val)))
            )));
        }
        items.add(UItem.asShadow(null));

        // Bar Size & Spacing
        items.add(UItem.asHeader(getString(R.string.OEAppearanceChipFoldersSize)));
        items.add(UItem.asButton(ID_SIZE, getString(R.string.OEAppearanceChipFoldersSize),
                getSizeName(AppearanceConfig.chipFoldersSize.Int())));
        if (AppearanceConfig.chipFoldersSize.Int() == 3) { // Custom
            items.add(UItem.asCustom(ID_HEIGHT, createSlider(
                    getString(R.string.OEAppearanceChipFoldersHeight), 36, 64,
                    AppearanceConfig.chipFoldersCustomHeight.Int(),
                    val -> onSettingChanged(() -> AppearanceConfig.chipFoldersCustomHeight.setConfigInt(Math.round(val)))
            )));
        }

        items.add(UItem.asButton(ID_SPACING, getString(R.string.OEAppearanceChipFoldersSpacing),
                getSpacingName(AppearanceConfig.chipFoldersSpacing.Int())));
        if (AppearanceConfig.chipFoldersSpacing.Int() == 3) { // Custom
            items.add(UItem.asCustom(ID_SPACING_CUSTOM, createSlider(
                    getString(R.string.OEAppearanceChipFoldersCustomSpacing), 0, 24,
                    AppearanceConfig.chipFoldersCustomSpacing.Int(),
                    val -> onSettingChanged(() -> AppearanceConfig.chipFoldersCustomSpacing.setConfigInt(Math.round(val)))
            )));
        }

        items.add(UItem.asCustom(ID_BOTTOM_PADDING, createSlider(
                getString(R.string.OEAppearanceChipFoldersBottomPadding), 0, 24,
                AppearanceConfig.chipFoldersBarBottomPadding.Int(),
                val -> onSettingChanged(() -> AppearanceConfig.chipFoldersBarBottomPadding.setConfigInt(Math.round(val)))
        )));

        items.add(UItem.asCustom(ID_LIST_TOP_PADDING, createSlider(
                getString(R.string.OEAppearanceChipFoldersListTopPadding), 0, 24,
                AppearanceConfig.chipFoldersListTopPadding.Int(),
                val -> onSettingChanged(() -> AppearanceConfig.chipFoldersListTopPadding.setConfigInt(Math.round(val)))
        )));
        items.add(UItem.asShadow(null));

        // More
        items.add(UItem.asHeader(getString(R.string.More)));
        items.add(UItem.asCheck(ID_SCROLL_DIVIDER, getString(R.string.OEAppearanceChipFoldersScrollDivider))
                .setChecked(AppearanceConfig.chipFoldersScrollDivider.Bool()));
        items.add(UItem.asShadow(getString(R.string.OEAppearanceChipFoldersScrollDividerInfo)));
    }

    private void onSettingChanged(Runnable action) {
        action.run();
        ChipFoldersController.getInstance().updateSettings();
        if (foldersPreviewCell != null) {
            ChipFoldersController.getInstance().refreshPreview(foldersPreviewCell);
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

    private void onItemClick(UItem item, View view, int position, float x, float y) {
        if (item.id == ID_ENABLE) {
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
            listView.adapter.update(true);
        } else if (item.id == ID_MD3_COLORS) {
            AppearanceConfig.chipFoldersMd3Colors.setConfigBool(!AppearanceConfig.chipFoldersMd3Colors.Bool());
            ChipFoldersController.getInstance().updateSettings();
            if (foldersPreviewCell != null) {
                ChipFoldersController.getInstance().refreshPreview(foldersPreviewCell);
            }
            listView.adapter.update(true);
        } else if (item.id == ID_SCROLL_DIVIDER) {
            AppearanceConfig.chipFoldersScrollDivider.setConfigBool(!AppearanceConfig.chipFoldersScrollDivider.Bool());
            ChipFoldersController.getInstance().updateSettings();
            listView.adapter.update(true);
        } else if (item.id == ID_STYLE) {
            showStyleDialog();
        } else if (item.id == ID_SHAPE) {
            showShapeDialog();
        } else if (item.id == ID_ANIM) {
            showAnimDialog();
        } else if (item.id == ID_SIZE) {
            showSizeDialog();
        } else if (item.id == ID_SPACING) {
            showSpacingDialog();
        } else if (item.id == ID_COLOR_ACTIVE) {
            showColorPicker(AppearanceConfig.chipFoldersColorActive);
        } else if (item.id == ID_COLOR_INACTIVE) {
            showColorPicker(AppearanceConfig.chipFoldersColorInactive);
        } else if (item.id == ID_COLOR_TEXT_ACTIVE) {
            showColorPicker(AppearanceConfig.chipFoldersColorTextActive);
        } else if (item.id == ID_COLOR_TEXT_INACTIVE) {
            showColorPicker(AppearanceConfig.chipFoldersColorTextInactive);
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
        builder.setSingleChoiceItems(items, AppearanceConfig.chipFoldersStyle.Int(), (dialog, which) -> {
            AppearanceConfig.chipFoldersStyle.setConfigInt(which);
            ChipFoldersController.getInstance().updateSettings();
            if (foldersPreviewCell != null) {
                ChipFoldersController.getInstance().refreshPreview(foldersPreviewCell);
            }
            dialog.dismiss();
            listView.adapter.update(true);
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
        builder.setSingleChoiceItems(items, AppearanceConfig.chipFoldersShape.Int(), (dialog, which) -> {
            AppearanceConfig.chipFoldersShape.setConfigInt(which);
            ChipFoldersController.getInstance().updateSettings();
            if (foldersPreviewCell != null) {
                ChipFoldersController.getInstance().refreshPreview(foldersPreviewCell);
            }
            dialog.dismiss();
            listView.adapter.update(true);
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
        builder.setSingleChoiceItems(items, AppearanceConfig.chipFoldersAnim.Int(), (dialog, which) -> {
            AppearanceConfig.chipFoldersAnim.setConfigInt(which);
            ChipFoldersController.getInstance().updateSettings();
            if (foldersPreviewCell != null) {
                ChipFoldersController.getInstance().refreshPreview(foldersPreviewCell);
            }
            dialog.dismiss();
            listView.adapter.update(true);
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
        builder.setSingleChoiceItems(items, AppearanceConfig.chipFoldersSize.Int(), (dialog, which) -> {
            AppearanceConfig.chipFoldersSize.setConfigInt(which);
            ChipFoldersController.getInstance().updateSettings();
            if (foldersPreviewCell != null) {
                ChipFoldersController.getInstance().refreshPreview(foldersPreviewCell);
            }
            dialog.dismiss();
            listView.adapter.update(true);
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
        builder.setSingleChoiceItems(items, AppearanceConfig.chipFoldersSpacing.Int(), (dialog, which) -> {
            AppearanceConfig.chipFoldersSpacing.setConfigInt(which);
            ChipFoldersController.getInstance().updateSettings();
            if (foldersPreviewCell != null) {
                ChipFoldersController.getInstance().refreshPreview(foldersPreviewCell);
            }
            dialog.dismiss();
            listView.adapter.update(true);
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
            listView.adapter.update(true);
        });
        sheet.show();
    }
}
