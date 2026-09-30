package app.exteraless.settings;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import app.exteraless.appearance.AppearanceConfig;

public class ChatHeaderSettingsActivity extends BaseFragment {

    private UniversalRecyclerView listView;

    private static final int ID_HIDE_AVATAR = 1;
    private static final int ID_HIDE_CALL = 2;
    private static final int ID_HIDE_VIDEO_CALL = 3;
    private static final int ID_HIDE_SEARCH = 4;
    private static final int ID_HIDE_MUTE = 5;

    private static final int ID_ORDER_BASE = 100;

    private static final String[] ALL_KEYS = {"call", "video_call", "search", "mute", "other"};

    private final List<String> currentOrder = new ArrayList<>();

    @Override
    public boolean onFragmentCreate() {
        initOrder();
        return super.onFragmentCreate();
    }

    private void initOrder() {
        currentOrder.clear();
        String saved = AppearanceConfig.chatHeaderItemsOrder.String();
        if (saved != null && !saved.isEmpty()) {
            currentOrder.addAll(Arrays.asList(saved.split(",")));
        } else {
            currentOrder.addAll(Arrays.asList(ALL_KEYS));
        }
    }

    private int getHeaderKeyIndex(String key) {
        for (int i = 0; i < ALL_KEYS.length; i++) {
            if (ALL_KEYS[i].equals(key)) return i;
        }
        return 0;
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle(LocaleController.getString(R.string.OEAppearanceChatHeaderCustomization));
        actionBar.setAllowOverlayTitle(true);
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
        listView.allowReorder(true);
        listView.listenReorder(this::onReordered);
        listView.setReorderClampToSection(true);
        contentView.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        actionBar.setAdaptiveBackground(listView);

        fragmentView = contentView;
        return fragmentView;
    }

    private void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asHeader(LocaleController.getString(R.string.OEAppearanceChatHeaderCustomization)));
        items.add(UItem.asCheck(ID_HIDE_AVATAR, LocaleController.getString(R.string.OEAppearanceChatHeaderHideAvatar))
                .setChecked(AppearanceConfig.chatHeaderHideAvatar.Bool()));
        items.add(UItem.asCheck(ID_HIDE_CALL, LocaleController.getString(R.string.OEAppearanceChatHeaderHideCall))
                .setChecked(AppearanceConfig.chatHeaderHideCall.Bool()));
        items.add(UItem.asCheck(ID_HIDE_VIDEO_CALL, LocaleController.getString(R.string.OEAppearanceChatHeaderHideVideoCall))
                .setChecked(AppearanceConfig.chatHeaderHideVideoCall.Bool()));
        items.add(UItem.asCheck(ID_HIDE_SEARCH, LocaleController.getString(R.string.OEAppearanceChatHeaderHideSearch))
                .setChecked(AppearanceConfig.chatHeaderHideSearch.Bool()));
        items.add(UItem.asCheck(ID_HIDE_MUTE, LocaleController.getString(R.string.OEAppearanceChatHeaderHideMute))
                .setChecked(AppearanceConfig.chatHeaderHideMute.Bool()));
        items.add(UItem.asShadow(null));

        items.add(UItem.asHeader(LocaleController.getString(R.string.OEAppearanceChatHeaderOrder)));
        adapter.whiteSectionStart();
        adapter.reorderSectionStart();
        for (int i = 0; i < currentOrder.size(); i++) {
            String key = currentOrder.get(i);
            items.add(UItem.asButton(ID_ORDER_BASE + getHeaderKeyIndex(key), getItemIcon(key), getItemTitle(key)));
        }
        adapter.reorderSectionEnd();
        adapter.whiteSectionEnd();
        items.add(UItem.asShadow(null));
    }

    private String getItemTitle(String key) {
        switch (key) {
            case "call":
                return LocaleController.getString(R.string.Call);
            case "video_call":
                return LocaleController.getString(R.string.VideoCall);
            case "search":
                return LocaleController.getString(R.string.Search);
            case "mute":
                return LocaleController.getString(R.string.Notifications);
            case "other":
            default:
                return LocaleController.getString(R.string.AccDescrMoreOptions);
        }
    }

    private int getItemIcon(String key) {
        switch (key) {
            case "call":
                return R.drawable.calls_menu_phone;
            case "video_call":
                return R.drawable.calls_video;
            case "search":
                return R.drawable.outline_header_search;
            case "mute":
                return R.drawable.msg_mute;
            case "other":
            default:
                return R.drawable.ic_ab_other;
        }
    }

    private void onItemClick(UItem item, View view, int position, float x, float y) {
        if (item.id == ID_HIDE_AVATAR) {
            AppearanceConfig.chatHeaderHideAvatar.setConfigBool(!AppearanceConfig.chatHeaderHideAvatar.Bool());
            listView.adapter.update(true);
        } else if (item.id == ID_HIDE_CALL) {
            AppearanceConfig.chatHeaderHideCall.setConfigBool(!AppearanceConfig.chatHeaderHideCall.Bool());
            listView.adapter.update(true);
        } else if (item.id == ID_HIDE_VIDEO_CALL) {
            AppearanceConfig.chatHeaderHideVideoCall.setConfigBool(!AppearanceConfig.chatHeaderHideVideoCall.Bool());
            listView.adapter.update(true);
        } else if (item.id == ID_HIDE_SEARCH) {
            AppearanceConfig.chatHeaderHideSearch.setConfigBool(!AppearanceConfig.chatHeaderHideSearch.Bool());
            listView.adapter.update(true);
        } else if (item.id == ID_HIDE_MUTE) {
            AppearanceConfig.chatHeaderHideMute.setConfigBool(!AppearanceConfig.chatHeaderHideMute.Bool());
            listView.adapter.update(true);
        }
    }

    private void onReordered(int section, ArrayList<UItem> reordered) {
        currentOrder.clear();
        for (UItem item : reordered) {
            int idx = item.id - ID_ORDER_BASE;
            if (idx >= 0 && idx < ALL_KEYS.length) {
                currentOrder.add(ALL_KEYS[idx]);
            }
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < currentOrder.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(currentOrder.get(i));
        }
        AppearanceConfig.chatHeaderItemsOrder.setConfigString(sb.toString());
    }
}
