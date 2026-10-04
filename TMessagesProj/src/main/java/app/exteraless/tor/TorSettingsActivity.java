package app.exteraless.tor;

import android.app.Dialog;
import android.content.Context;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextDetailSettingsCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextRadioCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;

import tw.nekomimi.nekogram.settings.BaseNekoSettingsActivity;
import tw.nekomimi.nekogram.ui.cells.HeaderCell;

/**
 * Экран настроек встроенного Tor (Orbot) в ExteraMS.
 * Реализует выбор страны выходного узла, мостов (Snowflake, obfs4, direct, custom)
 * и отображение прогресса подключения.
 */
public class TorSettingsActivity extends BaseNekoSettingsActivity implements TorController.Listener {

    private int headerStatusRow;
    private int enableTorRow;
    private int statusRow;
    private int newIdentityRow;
    private int statusShadowRow;

    private int headerRoutingRow;
    private int exitCountryRow;
    private int bridgeTypeRow;
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

        headerStatusRow = addRow();
        enableTorRow = addRow();
        statusRow = addRow();
        newIdentityRow = addRow();
        statusShadowRow = addRow();

        headerRoutingRow = addRow();
        exitCountryRow = addRow();
        bridgeTypeRow = addRow();
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
                listAdapter.notifyItemChanged(enableTorRow);
                listAdapter.notifyItemChanged(statusRow);
                listAdapter.notifyItemChanged(newIdentityRow);
            }
        });
    }

    @Override
    public void onLog(String line) {
        // Логирование событий для возможной диагностики
    }

    @Override
    protected void onItemClick(View view, int position, float x, float y) {
        TorController controller = TorController.getInstance();

        if (position == enableTorRow) {
            boolean newState = !controller.isEnabled();
            controller.setEnabled(newState);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(newState);
            }
            if (listAdapter != null) {
                listAdapter.notifyItemChanged(statusRow);
                listAdapter.notifyItemChanged(newIdentityRow);
            }
        } else if (position == newIdentityRow) {
            if (controller.getStatus() == TorController.STATUS_CONNECTED) {
                controller.newIdentity();
                BulletinFactory.of(this).createSimpleBulletin(R.raw.done, LocaleController.getString(R.string.TorNewIdentitySuccess)).show();
            } else {
                BulletinFactory.of(this).createSimpleBulletin(R.raw.info, LocaleController.getString(R.string.TorNotConnected)).show();
            }
        } else if (position == exitCountryRow) {
            showCountryPickerDialog();
        } else if (position == bridgeTypeRow) {
            showBridgePickerDialog();
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

        final EditText editText = new EditText(context);
        editText.setTextSize(14);
        editText.setText(TorController.getInstance().getCustomBridges());
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        editText.setLines(4);
        editText.setMaxLines(8);
        editText.setTextColor(getThemedColor(Theme.key_dialogTextBlack));
        editText.setHintTextColor(getThemedColor(Theme.key_dialogTextGray));
        editText.setHint("obfs4 IP:PORT FINGERPRINT cert=... iat-mode=0");

        FrameLayout frameLayout = new FrameLayout(context);
        frameLayout.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(8), AndroidUtilities.dp(24), AndroidUtilities.dp(8));
        frameLayout.addView(editText, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        builder.setView(frameLayout);

        builder.setPositiveButton(LocaleController.getString(R.string.TorCustomBridgesSave), (dialog, which) -> {
            TorController.getInstance().setCustomBridges(editText.getText().toString());
            TorController.getInstance().setBridgeType(TorController.BRIDGE_CUSTOM);
            if (listAdapter != null) {
                listAdapter.notifyItemChanged(bridgeTypeRow);
            }
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
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

    private class ListAdapter extends BaseListAdapter {

        public ListAdapter(Context context) {
            super(context);
        }

        @Override
        public int getItemViewType(int position) {
            if (position == headerStatusRow || position == headerRoutingRow) {
                return TYPE_HEADER;
            } else if (position == enableTorRow) {
                return TYPE_CHECK;
            } else if (position == statusRow) {
                return TYPE_DETAIL_SETTINGS;
            } else if (position == newIdentityRow) {
                return TYPE_TEXT;
            } else if (position == exitCountryRow || position == bridgeTypeRow) {
                return TYPE_SETTINGS;
            } else if (position == statusShadowRow || position == routingShadowRow) {
                return TYPE_SHADOW;
            } else if (position == infoPrivacyRow) {
                return TYPE_INFO_PRIVACY;
            }
            return TYPE_SETTINGS;
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, boolean partial) {
            TorController controller = TorController.getInstance();

            if (position == headerStatusRow) {
                HeaderCell cell = (HeaderCell) holder.itemView;
                cell.setText(LocaleController.getString(R.string.TorStatusHeader));
            } else if (position == enableTorRow) {
                TextCheckCell cell = (TextCheckCell) holder.itemView;
                cell.setTextAndCheck(LocaleController.getString(R.string.TorEnableCheck), controller.isEnabled(), true);
            } else if (position == statusRow) {
                TextDetailSettingsCell cell = (TextDetailSettingsCell) holder.itemView;
                String statusText;
                String detailText;
                int status = controller.getStatus();
                if (status == TorController.STATUS_CONNECTED) {
                    statusText = LocaleController.getString(R.string.TorConnected);
                    detailText = LocaleController.getString(R.string.TorProtectedDetail);
                } else if (status == TorController.STATUS_CONNECTING) {
                    statusText = LocaleController.formatString(R.string.TorConnecting, controller.getProgress());
                    detailText = controller.getStatusMessage();
                } else if (status == TorController.STATUS_STARTING) {
                    statusText = LocaleController.getString(R.string.TorStarting);
                    detailText = controller.getStatusMessage();
                } else if (status == TorController.STATUS_STOPPING) {
                    statusText = LocaleController.getString(R.string.TorStopping);
                    detailText = controller.getStatusMessage();
                } else if (status == TorController.STATUS_ERROR) {
                    statusText = LocaleController.getString(R.string.TorError);
                    detailText = controller.getStatusMessage();
                } else {
                    statusText = LocaleController.getString(R.string.TorDisabled);
                    detailText = LocaleController.getString(R.string.TorDisabledDetail);
                }
                cell.setTextAndValue(statusText, detailText, true);
            } else if (position == newIdentityRow) {
                TextCell cell = (TextCell) holder.itemView;
                cell.setTextAndIcon(LocaleController.getString(R.string.TorNewIdentity), R.drawable.msg_retry_solar, false);
            } else if (position == headerRoutingRow) {
                HeaderCell cell = (HeaderCell) holder.itemView;
                cell.setText(LocaleController.getString(R.string.TorRoutingHeader));
            } else if (position == exitCountryRow) {
                TextSettingsCell cell = (TextSettingsCell) holder.itemView;
                cell.setTextAndValue(LocaleController.getString(R.string.TorExitCountry), getCountryNameByCode(controller.getExitCountry()), true);
            } else if (position == bridgeTypeRow) {
                TextSettingsCell cell = (TextSettingsCell) holder.itemView;
                cell.setTextAndValue(LocaleController.getString(R.string.TorBridgeType), getBridgeName(controller.getBridgeType()), false);
            } else if (position == infoPrivacyRow) {
                TextInfoPrivacyCell cell = (TextInfoPrivacyCell) holder.itemView;
                cell.setText(LocaleController.getString(R.string.TorInfoPrivacy));
            }
        }
    }
}
