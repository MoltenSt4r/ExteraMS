package app.exteraless.plugins.ui;

import static org.telegram.messenger.LocaleController.getString;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.FrameLayout;

import org.telegram.messenger.FileLog;
import org.telegram.messenger.R;
import org.telegram.messenger.browser.Browser;
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

import app.exteraless.plugins.PluginsBackupHelper;
import app.exteraless.plugins.PluginsConstants;
import app.exteraless.plugins.PluginsController;

/**
 * Экран «Plugins Engine» — то, что у exteraGram открывается кнопкой (i) в шапке
 * списка плагинов. Порт
 * {@code com/exteragram/messenger/plugins/ui/PluginsInfoActivity.java}.
 *
 * Порядок секций и строк повторяет exteraGram: Settings (Developer Mode, Compact
 * View, Compatibility Mode, Safe Mode) → пояснение про safe mode → Python SDK
 * → Links → «Powered by Chaquopy».
 *
 * Расхождения, осознанные:
 *  - у exteraGram SDK докачивается с сервера (авто-обновление, бета-канал,
 *    «проверить обновления»); у нас SDK собран внутрь APK, поэтому вместо трёх
 *    неработающих переключателей — строка с его версией;
 *  - «Установить из файла» перенесено сюда с главного экрана: у exteraGram его нет
 *    вовсе (там ставят только тапом по .plugin), но убирать единственный
 *    надёжный способ установки было бы регрессом.
 */
public class PluginsInfoActivity extends BaseFragment {

    private static final int ID_DEVELOPER_MODE = 1;
    private static final int ID_COMPACT_VIEW = 2;
    private static final int ID_COMPATIBILITY = 3;
    private static final int ID_SAFE_MODE = 4;
    private static final int ID_SDK_VERSION = 5;
    private static final int ID_INSTALL_FROM_FILE = 6;
    private static final int ID_DOCUMENTATION = 7;
    private static final int ID_TRUSTED = 8;
    private static final int ID_UNSAFE_MODE = 9;
    private static final int ID_BETA_IMPORT_PY = 10;
    private static final int ID_EXPORT_FORMAT = 11;
    private static final int ID_BACKUP_CREATE = 12;
    private static final int ID_BACKUP_RESTORE = 13;

    private static final int REQUEST_CODE_PICK_BACKUP = 9782;

    private static final String DOCS_URL = "https://plugins.exteragram.app";
    private static final String TRUSTED_URL = "https://t.me/addlist/pPhOtEq00KhjYTc6";

    private UniversalRecyclerView listView;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(getString(R.string.PluginsEngineTitle));
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
        listView.setSections();
        listView.adapter.setApplyBackground(false);
        contentView.addView(listView,
                LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        actionBar.setAdaptiveBackground(listView);

        fragmentView = contentView;
        return fragmentView;
    }

    private void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        PluginsController controller = PluginsController.getInstance();
        boolean engineOn = controller.isEngineEnabled();
        boolean safeMode = controller.isSafeMode();

        items.add(UItem.asHeader(getString(R.string.Settings)));
        items.add(PluginUiItem.check(ID_DEVELOPER_MODE,
                        getString(R.string.PluginsDeveloperMode),
                        R.drawable.msg_settings)
                .setChecked(controller.isDeveloperMode())
                .setEnabled(engineOn && !safeMode));
        items.add(PluginUiItem.check(ID_COMPACT_VIEW,
                        getString(R.string.PluginsCompactView),
                        R.drawable.msg_topics)
                .setChecked(controller.isCompactView())
                .setEnabled(engineOn));
        items.add(PluginUiItem.check(ID_COMPATIBILITY,
                        getString(R.string.PluginsCompatibilityMode),
                        R.drawable.msg_link2)
                .setChecked(controller.isCompatibilityMode())
                .setValue(getString(R.string.PluginsCompatibilityModeInfo))
                .setMultiline(true)
                .setEnabled(engineOn));
        items.add(PluginUiItem.check(ID_SAFE_MODE, getString(R.string.PluginsSafeMode),
                        R.drawable.msg_secret)
                .setChecked(safeMode));
        items.add(UItem.asShadow(getString(R.string.PluginsSafeModeSummary)));

        items.add(PluginUiItem.check(ID_UNSAFE_MODE, getString(R.string.PluginsUnsafeMode),
                        R.drawable.msg_warning)
                .setChecked(controller.isUnsafeMode())
                .setValue(getString(R.string.PluginsUnsafeModeInfo))
                .setMultiline(true)
                .setEnabled(engineOn && !safeMode));
        items.add(UItem.asShadow(getString(R.string.PluginsUnsafeModeSummary)));

        items.add(UItem.asHeader(getString(R.string.PluginsBetaTitle)));
        items.add(PluginUiItem.check(ID_BETA_IMPORT_PY,
                        getString(R.string.PluginsBetaImportPy),
                        R.drawable.msg_bot)
                .setChecked(controller.isBetaImportPyEnabled())
                .setValue(getString(R.string.PluginsBetaImportPyInfo))
                .setMultiline(true)
                .setEnabled(engineOn));
        int exportFormat = controller.getPluginExportFormat();
        String formatName = exportFormat == PluginsConstants.EXPORT_FORMAT_PY
                ? getString(R.string.PluginsExportFormatPy)
                : (exportFormat == PluginsConstants.EXPORT_FORMAT_ASK
                ? getString(R.string.PluginsExportFormatAsk)
                : getString(R.string.PluginsExportFormatPlugin));
        items.add(UItem.asButton(ID_EXPORT_FORMAT, getString(R.string.PluginsExportFormat))
                .setValue(formatName)
                .setIcon(R.drawable.msg_share));
        items.add(UItem.asShadow(null));

        items.add(UItem.asHeader(getString(R.string.PluginsBackupTitle)));
        items.add(UItem.asButton(ID_BACKUP_CREATE, getString(R.string.PluginsBackupCreate))
                .setIcon(R.drawable.msg_download));
        items.add(UItem.asButton(ID_BACKUP_RESTORE, getString(R.string.PluginsBackupRestore))
                .setIcon(R.drawable.msg_retry));
        items.add(UItem.asShadow(getString(R.string.PluginsBackupSummary)));

        items.add(UItem.asHeader("Python SDK"));
        items.add(UItem.asButton(ID_SDK_VERSION, getString(R.string.PluginsPythonSdk))
                .setValue("v" + PluginsConstants.SDK_VERSION));
        items.add(UItem.asButton(ID_INSTALL_FROM_FILE, getString(R.string.PluginsInstallFromFile))
                .accent()
                .setIcon(R.drawable.msg_add));
        items.add(UItem.asShadow(getString(R.string.PluginsPythonSdkInfo)));

        items.add(UItem.asHeader(getString(R.string.PluginsLinks)));
        items.add(UItem.asButton(ID_DOCUMENTATION, getString(R.string.PluginsDocumentation))
                .setIcon(R.drawable.menu_intro));
        items.add(UItem.asButton(ID_TRUSTED, getString(R.string.PluginsTrusted))
                .accent()
                .setIcon(R.drawable.msg2_policy));
        items.add(UItem.asShadow(getString(R.string.PluginsPoweredBy)));
    }

    private void onItemClick(UItem item, View view, int position, float x, float y) {
        if (!item.enabled) {
            return;
        }
        PluginsController controller = PluginsController.getInstance();
        if (item.id == ID_DEVELOPER_MODE) {
            controller.setDeveloperMode(!controller.isDeveloperMode());
        } else if (item.id == ID_COMPACT_VIEW) {
            controller.setCompactView(!controller.isCompactView());
        } else if (item.id == ID_COMPATIBILITY) {
            boolean enabled = !controller.isCompatibilityMode();
            controller.setCompatibilityMode(enabled);
            BulletinFactory.of(this)
                    .createSimpleBulletin(R.raw.contact_check, getString(R.string.RestartRequired))
                    .show();
        } else if (item.id == ID_SAFE_MODE) {
            controller.setSafeMode(!controller.isSafeMode());
            BulletinFactory.of(this)
                    .createSimpleBulletin(R.raw.contact_check, getString(R.string.RestartRequired))
                    .show();
        } else if (item.id == ID_UNSAFE_MODE) {
            if (controller.isUnsafeMode()) {
                controller.setUnsafeMode(false);
            } else {
                confirmUnsafeMode();
                return;
            }
        } else if (item.id == ID_BETA_IMPORT_PY) {
            controller.setBetaImportPyEnabled(!controller.isBetaImportPyEnabled());
        } else if (item.id == ID_EXPORT_FORMAT) {
            showExportFormatDialog();
            return;
        } else if (item.id == ID_BACKUP_CREATE) {
            PluginsBackupHelper.createBackup(this);
            return;
        } else if (item.id == ID_BACKUP_RESTORE) {
            openBackupPicker();
            return;
        } else if (item.id == ID_INSTALL_FROM_FILE) {
            PluginsActivity.openPluginPicker(this);
            return;
        } else if (item.id == ID_DOCUMENTATION) {
            openUrl(DOCS_URL);
            return;
        } else if (item.id == ID_TRUSTED) {
            openUrl(TRUSTED_URL);
            return;
        } else {
            return;
        }
        if (listView != null) {
            listView.adapter.update(true);
        }
    }

    private void showExportFormatDialog() {
        if (getParentActivity() == null) {
            return;
        }
        PluginsController controller = PluginsController.getInstance();
        CharSequence[] options = new CharSequence[]{
                getString(R.string.PluginsExportFormatPlugin),
                getString(R.string.PluginsExportFormatPy),
                getString(R.string.PluginsExportFormatAsk)
        };
        new AlertDialog.Builder(getParentActivity())
                .setTitle(getString(R.string.PluginsExportFormat))
                .setItems(options, (dialog, which) -> {
                    int format = which == 1 ? PluginsConstants.EXPORT_FORMAT_PY
                            : (which == 2 ? PluginsConstants.EXPORT_FORMAT_ASK : PluginsConstants.EXPORT_FORMAT_PLUGIN);
                    controller.setPluginExportFormat(format);
                    if (listView != null) {
                        listView.adapter.update(true);
                    }
                })
                .show();
    }

    private void openBackupPicker() {
        Activity activity = getParentActivity();
        if (activity == null) {
            return;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            startActivityForResult(intent, REQUEST_CODE_PICK_BACKUP);
        } catch (Exception e) {
            FileLog.e("PluginsInfoActivity: pick backup failed", e);
        }
    }

    @Override
    public void onActivityResultFragment(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_PICK_BACKUP && resultCode == Activity.RESULT_OK
                && data != null && data.getData() != null) {
            PluginsBackupHelper.restoreBackup(this, data.getData(), () -> {
                if (listView != null) {
                    listView.adapter.update(true);
                }
            });
            return;
        }
        super.onActivityResultFragment(requestCode, resultCode, data);
    }

    private void confirmUnsafeMode() {
        if (getParentActivity() == null) {
            return;
        }
        new org.telegram.ui.ActionBar.AlertDialog.Builder(getParentActivity())
                .setTitle(getString(R.string.PluginsUnsafeMode))
                .setMessage(getString(R.string.PluginsUnsafeModeConfirm))
                .setPositiveButton(getString(R.string.PluginsUnsafeModeEnable), (dialog, which) -> {
                    PluginsController.getInstance().setUnsafeMode(true);
                    if (listView != null) {
                        listView.adapter.update(true);
                    }
                    BulletinFactory.of(this)
                            .createSimpleBulletin(R.raw.chats_infotip,
                                    getString(R.string.PluginsUnsafeModeOn))
                            .show();
                })
                .setNegativeButton(getString(R.string.Cancel), null)
                .makeRed(org.telegram.ui.ActionBar.AlertDialog.BUTTON_POSITIVE)
                .show();
    }

    private void openUrl(String url) {
        try {
            Browser.openUrl(getParentActivity(), url);
        } catch (Throwable t) {
            FileLog.e("PluginsInfoActivity: cannot open " + url, t);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (listView != null) {
            listView.adapter.update(false);
        }
    }

    @Override
    public boolean isSupportEdgeToEdge() {
        return true;
    }

    @Override
    public void onInsets(int left, int top, int right, int bottom) {
        if (listView != null) {
            listView.setPadding(0, 0, 0, bottom);
            listView.setClipToPadding(false);
        }
    }
}
