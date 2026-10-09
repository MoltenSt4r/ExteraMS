package app.exteraless.plugins.ui;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.LocaleController.getString;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.browser.Browser;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_chatlists;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import app.exteraless.plugins.Plugin;
import app.exteraless.plugins.PluginsController;

/**
 * Built-in Plugin Store for exteraGram / exteraless.
 * Fetches verified open-source community plugins from the community channels folder (https://t.me/addlist/MsGEKKJgoGlkOGM0).
 */
public class PluginStoreActivity extends BaseFragment {

    private static final String CATALOG_URL = "https://raw.githubusercontent.com/shareui/packit/main/configs/plugins.json";
    private static final String CACHE_FILE_NAME = "packit_plugins_cache.json";
    private static final String CHANNELS_FOLDER_SLUG = "MsGEKKJgoGlkOGM0";

    private static final int MENU_SEARCH = 0;
    private static final int MENU_REFRESH = 1;

    private static final int FILTER_ALL = 0;
    private static final int FILTER_OPEN_SOURCE = 1;
    private static final int FILTER_UI = 2;
    private static final int FILTER_TOOLS = 3;
    private static final int FILTER_TWEAKS = 4;

    public static class StorePlugin {
        public String id;
        public String name;
        public String author;
        public String sourceChannel;
        public String channelPost;
        public String version;
        public String icon;
        public String link;
        public String size;
        public String description;
        public String updateDate;
        public String format = ".plugin";
        public boolean isOpenSource = true;
        public List<String> tags = new ArrayList<>();
        public boolean downloading = false;
        public TLRPC.Document document;
        public TLRPC.Message message;

        public boolean isInstalled() {
            return PluginsController.getInstance().getPlugin(id) != null;
        }

        public boolean hasUpdate() {
            Plugin installed = PluginsController.getInstance().getPlugin(id);
            if (installed == null) return false;
            if (installed.version == null || version == null) return false;
            return !installed.version.equals(version);
        }
    }

    private UniversalRecyclerView listView;
    private LinearLayout chipsLayout;
    private final List<StorePlugin> allPlugins = new ArrayList<>();
    private String searchQuery;
    private int currentFilter = FILTER_ALL;
    private boolean loading = false;

    private final StorePluginDelegate pluginDelegate = new StorePluginDelegate() {
        @Override
        public void onInstallClick(StorePlugin plugin) {
            downloadAndInstall(plugin);
        }

        @Override
        public void onInstalledClick(StorePlugin plugin) {
            showInstalledOptions(plugin);
        }

        @Override
        public void onAuthorClick(StorePlugin plugin) {
            String target = !TextUtils.isEmpty(plugin.channelPost) ? plugin.channelPost :
                    (!TextUtils.isEmpty(plugin.sourceChannel) ? plugin.sourceChannel : plugin.author);
            if (!TextUtils.isEmpty(target)) {
                String clean = target.trim();
                if (clean.startsWith("http://") || clean.startsWith("https://")) {
                    Browser.openUrl(getParentActivity(), clean);
                } else if (clean.startsWith("@")) {
                    Browser.openUrl(getParentActivity(), "https://t.me/" + clean.substring(1));
                } else {
                    Browser.openUrl(getParentActivity(), "https://t.me/" + clean);
                }
            }
        }
    };

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(getString(R.string.PluginsStoreTitle));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == MENU_REFRESH) {
                    fetchCatalogOnline(true);
                }
            }
        });

        ActionBarMenuItem search = actionBar.createMenu().addItem(MENU_SEARCH, R.drawable.ic_ab_search_solar)
                .setIsSearchField(true)
                .setActionBarMenuItemSearchListener(new ActionBarMenuItem.ActionBarMenuItemSearchListener() {
                    @Override
                    public void onSearchCollapse() {
                        searchQuery = null;
                        updateRows();
                    }

                    @Override
                    public void onTextChanged(android.widget.EditText editText) {
                        searchQuery = editText.getText().toString();
                        updateRows();
                    }
                });
        search.setSearchFieldHint(getString(R.string.PluginsStoreSearch));
        actionBar.createMenu().addItem(MENU_REFRESH, R.drawable.msg_retry);

        LinearLayout contentView = new LinearLayout(context);
        contentView.setOrientation(LinearLayout.VERTICAL);
        contentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        // Chips bar
        HorizontalScrollView chipsScroll = new HorizontalScrollView(context);
        chipsScroll.setHorizontalScrollBarEnabled(false);
        chipsScroll.setClipToPadding(false);
        chipsScroll.setPadding(dp(12), dp(8), dp(12), dp(8));
        chipsScroll.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        chipsLayout = new LinearLayout(context);
        chipsLayout.setOrientation(LinearLayout.HORIZONTAL);
        chipsScroll.addView(chipsLayout, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT));
        contentView.addView(chipsScroll, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        buildChips(context);

        // RecyclerView
        listView = new UniversalRecyclerView(this, this::fillItems, this::onItemClick, null);
        listView.setSections();
        listView.adapter.setApplyBackground(false);
        contentView.addView(listView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        actionBar.setAdaptiveBackground(listView);

        loadInitialCatalog();

        fragmentView = contentView;
        return fragmentView;
    }

    private void buildChips(Context context) {
        chipsLayout.removeAllViews();
        int[] filterIds = {FILTER_ALL, FILTER_OPEN_SOURCE, FILTER_UI, FILTER_TOOLS, FILTER_TWEAKS};
        int[] titleResIds = {
                R.string.PluginsStoreFilterAll,
                R.string.PluginsStoreFilterOpenSource,
                R.string.PluginsStoreFilterUi,
                R.string.PluginsStoreFilterTools,
                R.string.PluginsStoreFilterTweaks
        };

        for (int i = 0; i < filterIds.length; i++) {
            final int fId = filterIds[i];
            TextView chip = new TextView(context);
            chip.setText(getString(titleResIds[i]));
            chip.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            chip.setTypeface(AndroidUtilities.bold());
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(dp(14), dp(6), dp(14), dp(6));

            boolean active = (currentFilter == fId);
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dp(16));
            if (active) {
                int accent = Theme.getColor(Theme.key_featuredStickers_addButton);
                if (accent == 0) accent = 0xff00d2b4;
                bg.setColor(accent);
                chip.setTextColor(0xffffffff);
            } else {
                bg.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                chip.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            }
            chip.setBackground(bg);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.rightMargin = dp(8);
            chip.setOnClickListener(v -> {
                if (currentFilter != fId) {
                    currentFilter = fId;
                    buildChips(context);
                    updateRows();
                }
            });
            chipsLayout.addView(chip, lp);
        }
    }

    private void loadInitialCatalog() {
        Activity act = getParentActivity();
        if (act == null) return;

        // 1. Try disk cache
        File cache = new File(act.getCacheDir(), CACHE_FILE_NAME);
        if (cache.exists() && cache.length() > 0) {
            try (FileInputStream in = new FileInputStream(cache)) {
                String json = readStream(in);
                parseCatalogJson(json);
            } catch (Exception e) {
                FileLog.e(e);
            }
        }

        // 2. If still empty, load bundled community plugins from assets
        if (allPlugins.isEmpty()) {
            try (InputStream in = act.getAssets().open("packit_plugins.json")) {
                String json = readStream(in);
                parseCatalogJson(json);
            } catch (Exception e) {
                FileLog.e(e);
            }
        }

        updateRows();

        // 3. Scan folder channels (https://t.me/addlist/MsGEKKJgoGlkOGM0) for latest community plugins
        checkFolderChannelsOnline();

        // 4. In background, fetch fresh copy from catalog
        fetchCatalogOnline(false);
    }

    private void checkFolderChannelsOnline() {
        TL_chatlists.TL_chatlists_checkChatlistInvite req = new TL_chatlists.TL_chatlists_checkChatlistInvite();
        req.slug = CHANNELS_FOLDER_SLUG;
        ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> {
            if (response instanceof TL_chatlists.chatlist_ChatlistInvite) {
                TL_chatlists.chatlist_ChatlistInvite inv = (TL_chatlists.chatlist_ChatlistInvite) response;
                ArrayList<TLRPC.Chat> chats = null;
                if (inv instanceof TL_chatlists.TL_chatlists_chatlistInvite) {
                    chats = ((TL_chatlists.TL_chatlists_chatlistInvite) inv).chats;
                } else if (inv instanceof TL_chatlists.TL_chatlists_chatlistInviteAlready) {
                    chats = ((TL_chatlists.TL_chatlists_chatlistInviteAlready) inv).chats;
                }
                if (chats != null) {
                    searchPluginsInChats(chats);
                }
            }
        });
    }

    private void searchPluginsInChats(ArrayList<TLRPC.Chat> chats) {
        for (TLRPC.Chat chat : chats) {
            if (chat == null || TextUtils.isEmpty(chat.username)) continue;
            TLRPC.TL_messages_search sReq = new TLRPC.TL_messages_search();
            sReq.peer = MessagesController.getInstance(currentAccount).getInputPeer(chat);
            sReq.filter = new TLRPC.TL_inputMessagesFilterDocument();
            sReq.q = "";
            sReq.limit = 25;
            ConnectionsManager.getInstance(currentAccount).sendRequest(sReq, (res, err) -> {
                if (res instanceof TLRPC.messages_Messages) {
                    TLRPC.messages_Messages mRes = (TLRPC.messages_Messages) res;
                    AndroidUtilities.runOnUIThread(() -> {
                        boolean added = false;
                        for (TLRPC.Message msg : mRes.messages) {
                            if (msg != null && msg.media instanceof TLRPC.TL_messageMediaDocument) {
                                TLRPC.Document doc = msg.media.document;
                                String fName = FileLoader.getDocumentFileName(doc);
                                if (fName != null && (fName.endsWith(".plugin") || fName.endsWith(".py") || fName.endsWith(".eaf"))) {
                                    String id = fName.replaceAll("\\.[^.]+$", "").toLowerCase(Locale.ROOT).replace(" ", "_");
                                    boolean exists = false;
                                    for (StorePlugin sp : allPlugins) {
                                        if (TextUtils.equals(sp.id, id)) {
                                            sp.document = doc;
                                            sp.message = msg;
                                            exists = true;
                                            break;
                                        }
                                    }
                                    if (!exists) {
                                        StorePlugin sp = new StorePlugin();
                                        sp.id = id;
                                        sp.name = fName.replaceAll("\\.[^.]+$", "");
                                        sp.author = "@" + chat.username;
                                        sp.sourceChannel = "@" + chat.username;
                                        sp.channelPost = "https://t.me/" + chat.username + "/" + msg.id;
                                        sp.format = fName.endsWith(".py") ? ".py" : (fName.endsWith(".eaf") ? ".eaf" : ".plugin");
                                        sp.isOpenSource = !sp.format.endsWith(".eaf");
                                        sp.description = msg.message;
                                        sp.size = AndroidUtilities.formatFileSize(doc.size);
                                        sp.document = doc;
                                        sp.message = msg;
                                        sp.tags.add(sp.isOpenSource ? "OpenSource" : "Binary");
                                        allPlugins.add(0, sp);
                                        added = true;
                                    }
                                }
                            }
                        }
                        if (added) {
                            updateRows();
                        }
                    });
                }
            });
        }
    }

    private void fetchCatalogOnline(boolean showFeedback) {
        if (loading) return;
        loading = true;
        if (showFeedback && fragmentView != null) {
            BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_retry, getString(R.string.PluginsStoreLoading)).show();
        }

        Utilities.globalQueue.postRunnable(() -> {
            String result = null;
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(CATALOG_URL).openConnection();
                conn.setConnectTimeout(12000);
                conn.setReadTimeout(15000);
                conn.setRequestProperty("User-Agent", "exteraless/" + BuildVars.BUILD_VERSION_STRING);
                conn.setRequestProperty("Accept", "application/json");
                int code = conn.getResponseCode();
                if (code >= 200 && code < 300) {
                    try (InputStream in = conn.getInputStream()) {
                        result = readStream(in);
                    }
                }
                conn.disconnect();
            } catch (Exception e) {
                FileLog.e(e);
            }

            final String jsonResult = result;
            AndroidUtilities.runOnUIThread(() -> {
                loading = false;
                if (!TextUtils.isEmpty(jsonResult)) {
                    Activity act = getParentActivity();
                    if (act != null) {
                        try (FileOutputStream out = new FileOutputStream(new File(act.getCacheDir(), CACHE_FILE_NAME))) {
                            out.write(jsonResult.getBytes("UTF-8"));
                        } catch (Exception ignored) {}
                    }
                    parseCatalogJson(jsonResult);
                    updateRows();
                    if (showFeedback && fragmentView != null) {
                        BulletinFactory.of(PluginStoreActivity.this)
                                .createSimpleBulletin(R.drawable.msg_check, LocaleController.formatString(R.string.OEPlayerSourceSynced, "Plugins"))
                                .show();
                    }
                } else if (showFeedback && fragmentView != null) {
                    BulletinFactory.of(PluginStoreActivity.this)
                            .createSimpleBulletin(R.drawable.msg_delete, getString(R.string.PluginsStoreError))
                            .show();
                }
            });
        });
    }

    private void parseCatalogJson(String jsonStr) {
        if (TextUtils.isEmpty(jsonStr)) return;
        try {
            JSONObject root = new JSONObject(jsonStr);
            JSONArray arr = root.optJSONArray("plugins");
            if (arr == null) return;

            List<StorePlugin> list = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.optJSONObject(i);
                if (obj == null) continue;

                StorePlugin p = new StorePlugin();
                p.id = obj.optString("id");
                p.name = obj.optString("name");
                p.author = obj.optString("author");
                p.sourceChannel = obj.optString("source_channel", p.author);
                p.channelPost = obj.optString("channel_post");
                p.format = obj.optString("format", ".plugin");
                p.isOpenSource = obj.optBoolean("is_open_source", !p.format.endsWith(".eaf") && !p.format.endsWith(".elyx"));
                p.version = obj.optString("version");
                p.icon = obj.optString("icon");
                p.link = obj.optString("link");
                p.size = obj.optString("size");
                p.description = obj.optString("description");
                p.updateDate = obj.optString("update_date");

                JSONArray tagsArr = obj.optJSONArray("tags");
                if (tagsArr != null) {
                    for (int j = 0; j < tagsArr.length(); j++) {
                        Object tagObj = tagsArr.opt(j);
                        if (tagObj instanceof JSONArray) {
                            JSONArray sub = (JSONArray) tagObj;
                            if (sub.length() > 0) p.tags.add(sub.optString(0));
                        } else if (tagObj instanceof String) {
                            p.tags.add((String) tagObj);
                        }
                    }
                }
                list.add(p);
            }

            allPlugins.clear();
            allPlugins.addAll(list);
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    private List<StorePlugin> getFilteredPlugins() {
        List<StorePlugin> result = new ArrayList<>();
        String q = (searchQuery != null) ? searchQuery.toLowerCase(Locale.ROOT).trim() : null;

        for (StorePlugin p : allPlugins) {
            // Check filter
            if (currentFilter == FILTER_OPEN_SOURCE) {
                if (!p.isOpenSource) continue;
            } else if (currentFilter == FILTER_UI) {
                if (!p.tags.contains("UI") && !p.tags.contains("Player") && !p.tags.contains("Customization")) continue;
            } else if (currentFilter == FILTER_TOOLS) {
                if (!p.tags.contains("Tools") && !p.tags.contains("Utility") && !p.tags.contains("Library") && !p.tags.contains("Dev")) continue;
            } else if (currentFilter == FILTER_TWEAKS) {
                if (!p.tags.contains("Tweaks") && !p.tags.contains("Customization")) continue;
            }

            // Check search query
            if (!TextUtils.isEmpty(q)) {
                boolean match = (p.name != null && p.name.toLowerCase(Locale.ROOT).contains(q))
                        || (p.id != null && p.id.toLowerCase(Locale.ROOT).contains(q))
                        || (p.author != null && p.author.toLowerCase(Locale.ROOT).contains(q))
                        || (p.description != null && p.description.toLowerCase(Locale.ROOT).contains(q));
                if (!match) {
                    for (String t : p.tags) {
                        if (t.toLowerCase(Locale.ROOT).contains(q)) {
                            match = true;
                            break;
                        }
                    }
                }
                if (!match) continue;
            }

            result.add(p);
        }
        return result;
    }

    private void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        List<StorePlugin> list = getFilteredPlugins();
        if (list.isEmpty()) {
            if (!TextUtils.isEmpty(searchQuery)) {
                items.add(UItem.asShadow(getString(R.string.PluginsNotFound)));
            } else if (loading) {
                items.add(UItem.asShadow(getString(R.string.PluginsStoreLoading)));
            } else {
                items.add(UItem.asShadow(getString(R.string.PluginsNotFound)));
            }
            return;
        }

        for (StorePlugin p : list) {
            StorePluginCell cell = new StorePluginCell(getContext());
            cell.bind(p, pluginDelegate);
            items.add(UItem.asCustom(cell));
        }
    }

    private void onItemClick(UItem item, View view, int position, float x, float y) {
    }

    private void downloadAndInstall(StorePlugin plugin) {
        Activity activity = getParentActivity();
        if (activity == null) return;

        // 1. If plugin was directly fetched from a telegram channel
        if (plugin.document != null) {
            File localFile = FileLoader.getInstance(currentAccount).getPathToAttach(plugin.document, true);
            if (localFile != null && localFile.exists() && localFile.length() > 0) {
                PluginsController.getInstance().showInstallDialog(this, localFile.getAbsolutePath(), false);
                return;
            }
            plugin.downloading = true;
            updateRows();
            FileLoader.getInstance(currentAccount).loadFile(plugin.document, plugin.message, FileLoader.PRIORITY_HIGH, 0);
            NotificationCenter.getInstance(currentAccount).addObserver(new NotificationCenter.NotificationCenterDelegate() {
                @Override
                public void didReceivedNotification(int id, int account, Object... args) {
                    if (id == NotificationCenter.fileLoaded) {
                        String name = (String) args[0];
                        File f = (File) args[1];
                        if (f != null && TextUtils.equals(name, FileLoader.getDocumentFileName(plugin.document))) {
                            NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.fileLoaded);
                            plugin.downloading = false;
                            updateRows();
                            PluginsController.getInstance().showInstallDialog(PluginStoreActivity.this, f.getAbsolutePath(), false);
                        }
                    }
                }
            }, NotificationCenter.fileLoaded);
            return;
        }

        // 2. If it's a telegram post link (e.g. https://t.me/PESSDES_Plugins/91)
        if (!TextUtils.isEmpty(plugin.link) && plugin.link.startsWith("https://t.me/")) {
            Browser.openUrl(activity, plugin.link);
            return;
        }

        // 3. Otherwise download via HTTP
        if (TextUtils.isEmpty(plugin.link)) {
            BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_info, "Download link not found").show();
            return;
        }

        plugin.downloading = true;
        updateRows();

        Utilities.globalQueue.postRunnable(() -> {
            try {
                URL url = new URL(plugin.link);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(30000);
                conn.setRequestProperty("User-Agent", "exteraless/" + BuildVars.BUILD_VERSION_STRING);
                conn.connect();

                int code = conn.getResponseCode();
                if (code < 200 || code >= 300) {
                    throw new Exception("HTTP " + code);
                }

                String ext = plugin.format != null ? plugin.format : ".plugin";
                String linkLower = plugin.link.toLowerCase(Locale.ROOT);
                if (linkLower.endsWith(".eaf")) ext = ".eaf";
                else if (linkLower.endsWith(".py")) ext = ".py";
                else if (linkLower.endsWith(".plugin")) ext = ".plugin";

                File file = new File(activity.getCacheDir(), plugin.id + "_" + plugin.version + ext);
                try (InputStream in = conn.getInputStream();
                     FileOutputStream out = new FileOutputStream(file)) {
                    byte[] buf = new byte[8192];
                    int read;
                    while ((read = in.read(buf)) != -1) {
                        out.write(buf, 0, read);
                    }
                } finally {
                    conn.disconnect();
                }

                AndroidUtilities.runOnUIThread(() -> {
                    plugin.downloading = false;
                    updateRows();
                    PluginsController.getInstance().showInstallDialog(PluginStoreActivity.this, file.getAbsolutePath(), false);
                });
            } catch (Exception e) {
                FileLog.e(e);
                AndroidUtilities.runOnUIThread(() -> {
                    plugin.downloading = false;
                    updateRows();
                    BulletinFactory.of(PluginStoreActivity.this)
                            .createSimpleBulletin(R.drawable.msg_delete, getString(R.string.PluginsInstallError) + ": " + e.getMessage())
                            .show();
                });
            }
        });
    }

    private void showInstalledOptions(StorePlugin plugin) {
        Activity act = getParentActivity();
        if (act == null) return;

        CharSequence[] items = new CharSequence[]{
                getString(R.string.PluginSettingsTitle),
                getString(R.string.PluginPermissions),
                getString(R.string.PluginsInstallAction)
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(act);
        builder.setTitle(plugin.name != null ? plugin.name : plugin.id);
        builder.setItems(items, (dialog, which) -> {
            if (which == 0) {
                Plugin p = PluginsController.getInstance().getPlugin(plugin.id);
                if (p != null) {
                    app.exteraless.plugins.PythonPluginsEngine.getInstance().openPluginSettings(p, this);
                }
            } else if (which == 1) {
                presentFragment(new PluginPermissionsActivity(plugin.id));
            } else if (which == 2) {
                downloadAndInstall(plugin);
            }
        });
        builder.setNegativeButton(getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void updateRows() {
        if (listView != null && listView.adapter != null) {
            listView.adapter.update(true);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateRows();
    }

    private static String readStream(InputStream in) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int read;
        while ((read = in.read(buf)) > 0) {
            out.write(buf, 0, read);
        }
        return out.toString("UTF-8");
    }

    public interface StorePluginDelegate {
        void onInstallClick(StorePlugin plugin);
        void onInstalledClick(StorePlugin plugin);
        void onAuthorClick(StorePlugin plugin);
    }

    public static class StorePluginCell extends FrameLayout {
        private final LinearLayout card;
        private final BackupImageView iconView;
        private final ImageView defaultIconView;
        private final TextView nameView;
        private final TextView typeBadge;
        private final TextView subView;
        private final TextView descView;
        private final TextView tagsView;
        private final TextView actionButton;
        private final TextView openButton;
        private StorePlugin plugin;
        private StorePluginDelegate delegate;
        private boolean expanded = false;

        public StorePluginCell(Context context) {
            super(context);
            setPadding(dp(12), dp(4), dp(12), dp(6));

            card = new LinearLayout(context);
            card.setOrientation(LinearLayout.VERTICAL);
            int radius = app.exteraless.appearance.AppearanceConfig.sectionRadius();
            card.setBackground(Theme.createRoundRectDrawable(dp(radius), Theme.getColor(Theme.key_windowBackgroundWhite)));
            card.setPadding(dp(14), dp(14), dp(14), dp(14));
            addView(card, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            // Header row
            LinearLayout headerRow = new LinearLayout(context);
            headerRow.setOrientation(LinearLayout.HORIZONTAL);
            headerRow.setGravity(Gravity.CENTER_VERTICAL);
            card.addView(headerRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            FrameLayout iconContainer = new FrameLayout(context);
            GradientDrawable iconBg = new GradientDrawable();
            iconBg.setCornerRadius(dp(14));
            iconBg.setColor(Theme.getColor(Theme.key_windowBackgroundGray));
            iconContainer.setBackground(iconBg);

            defaultIconView = new ImageView(context);
            defaultIconView.setImageResource(R.drawable.baseline_extension_24);
            defaultIconView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            defaultIconView.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_featuredStickers_addButton), PorterDuff.Mode.SRC_IN));
            iconContainer.addView(defaultIconView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

            iconView = new BackupImageView(context);
            iconView.setRoundRadius(dp(14));
            iconView.setVisibility(GONE);
            iconContainer.addView(iconView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
            headerRow.addView(iconContainer, LayoutHelper.createLinear(50, 50));

            LinearLayout titleCol = new LinearLayout(context);
            titleCol.setOrientation(LinearLayout.VERTICAL);
            titleCol.setGravity(Gravity.CENTER_VERTICAL);
            headerRow.addView(titleCol, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, 12, 0, 8, 0));

            nameView = new TextView(context);
            nameView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            nameView.setTypeface(AndroidUtilities.bold());
            nameView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            nameView.setSingleLine(true);
            nameView.setEllipsize(TextUtils.TruncateAt.END);
            titleCol.addView(nameView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            typeBadge = new TextView(context);
            typeBadge.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
            typeBadge.setTypeface(AndroidUtilities.bold());
            typeBadge.setPadding(dp(7), dp(2), dp(7), dp(2));
            titleCol.addView(typeBadge, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 3, 0, 0));

            subView = new TextView(context);
            subView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            subView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
            subView.setSingleLine(true);
            subView.setEllipsize(TextUtils.TruncateAt.END);
            titleCol.addView(subView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));

            // Description
            descView = new TextView(context);
            descView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
            descView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            descView.setLineSpacing(0, 1.15f);
            descView.setMaxLines(3);
            descView.setEllipsize(TextUtils.TruncateAt.END);
            descView.setOnClickListener(v -> {
                expanded = !expanded;
                descView.setMaxLines(expanded ? Integer.MAX_VALUE : 3);
            });
            card.addView(descView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 10, 0, 0));

            // Tags
            tagsView = new TextView(context);
            tagsView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            tagsView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText4));
            card.addView(tagsView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 6, 0, 0));

            // Actions row
            LinearLayout actionsRow = new LinearLayout(context);
            actionsRow.setOrientation(LinearLayout.HORIZONTAL);
            actionsRow.setGravity(Gravity.CENTER_VERTICAL);
            card.addView(actionsRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 10, 0, 0));

            openButton = new TextView(context);
            openButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            openButton.setTextColor(Theme.getColor(Theme.key_featuredStickers_addButton));
            openButton.setSingleLine(true);
            openButton.setEllipsize(TextUtils.TruncateAt.END);
            openButton.setPadding(0, dp(4), dp(8), dp(4));
            actionsRow.addView(openButton, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f));

            actionButton = new TextView(context);
            actionButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
            actionButton.setTypeface(AndroidUtilities.bold());
            actionButton.setGravity(Gravity.CENTER);
            actionButton.setPadding(dp(16), dp(7), dp(16), dp(7));
            actionsRow.addView(actionButton, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, 34));
        }

        public void bind(StorePlugin p, StorePluginDelegate d) {
            this.plugin = p;
            this.delegate = d;
            nameView.setText(p.name != null ? p.name : p.id);

            GradientDrawable tbBg = new GradientDrawable();
            tbBg.setCornerRadius(dp(8));
            if (p.isOpenSource) {
                tbBg.setColor(0x2200c853);
                typeBadge.setTextColor(0xff00c853);
                typeBadge.setText("✓ " + getString(R.string.PluginsStoreOpenSource) + " (" + (p.format != null ? p.format : ".plugin") + ")");
            } else {
                tbBg.setColor(0x22888888);
                typeBadge.setTextColor(0xff888888);
                typeBadge.setText(p.format != null ? p.format : ".eaf");
            }
            typeBadge.setBackground(tbBg);

            StringBuilder sub = new StringBuilder();
            if (!TextUtils.isEmpty(p.author)) sub.append(p.author);
            if (!TextUtils.isEmpty(p.version)) {
                if (sub.length() > 0) sub.append(" • ");
                sub.append("v").append(p.version);
            }
            if (!TextUtils.isEmpty(p.size)) {
                if (sub.length() > 0) sub.append(" • ");
                sub.append(p.size);
            }
            subView.setText(sub.toString());

            descView.setText(p.description != null ? p.description : "");
            descView.setVisibility(TextUtils.isEmpty(p.description) ? GONE : VISIBLE);

            if (p.tags != null && !p.tags.isEmpty()) {
                StringBuilder tb = new StringBuilder();
                for (String t : p.tags) {
                    if (tb.length() > 0) tb.append("  ");
                    tb.append("#").append(t);
                }
                tagsView.setText(tb.toString());
                tagsView.setVisibility(VISIBLE);
            } else {
                tagsView.setVisibility(GONE);
            }

            String source = !TextUtils.isEmpty(p.sourceChannel) ? p.sourceChannel : p.author;
            if (!TextUtils.isEmpty(source)) {
                openButton.setText(source + (!TextUtils.isEmpty(p.channelPost) ? " ↗" : ""));
                openButton.setVisibility(VISIBLE);
                openButton.setOnClickListener(v -> {
                    if (delegate != null) delegate.onAuthorClick(p);
                });
            } else {
                openButton.setVisibility(GONE);
            }

            GradientDrawable btnBg = new GradientDrawable();
            btnBg.setCornerRadius(dp(17));
            int accent = Theme.getColor(Theme.key_featuredStickers_addButton);
            if (accent == 0) accent = 0xff00d2b4;

            if (p.downloading) {
                btnBg.setColor(Theme.getColor(Theme.key_windowBackgroundGray));
                actionButton.setBackground(btnBg);
                actionButton.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
                actionButton.setText(getString(R.string.PluginsStoreDownloading));
                actionButton.setEnabled(false);
            } else if (p.isInstalled()) {
                if (p.hasUpdate()) {
                    btnBg.setColor(accent);
                    actionButton.setBackground(btnBg);
                    actionButton.setTextColor(0xffffffff);
                    actionButton.setText(getString(R.string.PluginsStoreUpdate));
                    actionButton.setEnabled(true);
                    actionButton.setOnClickListener(v -> {
                        if (delegate != null) delegate.onInstallClick(p);
                    });
                } else {
                    btnBg.setColor(Theme.getColor(Theme.key_windowBackgroundGray));
                    actionButton.setBackground(btnBg);
                    actionButton.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
                    actionButton.setText("✓ " + getString(R.string.PluginsStoreInstalled));
                    actionButton.setEnabled(true);
                    actionButton.setOnClickListener(v -> {
                        if (delegate != null) delegate.onInstalledClick(p);
                    });
                }
            } else {
                btnBg.setColor(accent);
                actionButton.setBackground(btnBg);
                actionButton.setTextColor(0xffffffff);
                actionButton.setText(getString(R.string.PluginsStoreInstall));
                actionButton.setEnabled(true);
                actionButton.setOnClickListener(v -> {
                    if (delegate != null) delegate.onInstallClick(p);
                });
            }

            if (!TextUtils.isEmpty(p.icon)) {
                iconView.setTag(null);
                iconView.setImageDrawable(null);
                iconView.setVisibility(GONE);
                defaultIconView.setVisibility(VISIBLE);
                boolean applied = PluginIcons.apply(iconView, p.icon, () -> {
                    iconView.setVisibility(VISIBLE);
                    defaultIconView.setVisibility(GONE);
                });
                if (!applied) {
                    iconView.setVisibility(GONE);
                    defaultIconView.setVisibility(VISIBLE);
                }
            } else {
                iconView.setVisibility(GONE);
                defaultIconView.setVisibility(VISIBLE);
            }
        }
    }
}
