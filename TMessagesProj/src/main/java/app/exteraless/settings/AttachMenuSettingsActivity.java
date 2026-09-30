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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import app.exteraless.appearance.AppearanceConfig;

public class AttachMenuSettingsActivity extends BaseFragment {

    private UniversalRecyclerView listView;

    private static final int ID_TOGGLE_BASE = 100;
    private static final int ID_ORDER_BASE = 200;

    private static final String[] ALL_KEYS = {"photo", "camera", "file", "gif", "poll", "location", "contact", "music"};

    private final List<String> currentOrder = new ArrayList<>();
    private final Set<String> hiddenItems = new HashSet<>();

    @Override
    public boolean onFragmentCreate() {
        initData();
        return super.onFragmentCreate();
    }

    private void initData() {
        currentOrder.clear();
        String savedOrder = AppearanceConfig.attachMenuItemsOrder.String();
        if (savedOrder != null && !savedOrder.isEmpty()) {
            currentOrder.addAll(Arrays.asList(savedOrder.split(",")));
        } else {
            currentOrder.addAll(Arrays.asList(ALL_KEYS));
        }

        hiddenItems.clear();
        String savedHidden = AppearanceConfig.attachMenuHideItems.String();
        if (savedHidden != null && !savedHidden.isEmpty()) {
            hiddenItems.addAll(Arrays.asList(savedHidden.split(",")));
        }
    }

    private int getAttachKeyIndex(String key) {
        for (int i = 0; i < ALL_KEYS.length; i++) {
            if (ALL_KEYS[i].equals(key)) return i;
        }
        return 0;
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle(LocaleController.getString(R.string.OEAppearanceAttachMenuCustomization));
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
        items.add(UItem.asHeader(LocaleController.getString(R.string.OEAppearanceAttachMenuCustomization)));
        adapter.whiteSectionStart();
        for (int i = 0; i < currentOrder.size(); i++) {
            String key = currentOrder.get(i);
            boolean visible = !hiddenItems.contains(key);
            items.add(UItem.asCheck(ID_TOGGLE_BASE + i, getItemTitle(key), getItemIcon(key))
                    .setChecked(visible));
        }
        adapter.whiteSectionEnd();
        items.add(UItem.asShadow(null));

        items.add(UItem.asHeader(LocaleController.getString(R.string.OEAppearanceChatHeaderOrder)));
        adapter.whiteSectionStart();
        adapter.reorderSectionStart();
        for (int i = 0; i < currentOrder.size(); i++) {
            String key = currentOrder.get(i);
            items.add(UItem.asButton(ID_ORDER_BASE + getAttachKeyIndex(key), getItemIcon(key), getItemTitle(key)));
        }
        adapter.reorderSectionEnd();
        adapter.whiteSectionEnd();
        items.add(UItem.asShadow(null));
    }

    private String getItemTitle(String key) {
        switch (key) {
            case "photo":
                return LocaleController.getString(R.string.OEAppearanceAttachItemPhoto);
            case "camera":
                return LocaleController.getString(R.string.OEAppearanceAttachItemCamera);
            case "gif":
                return LocaleController.getString(R.string.OEAppearanceAttachItemGif);
            case "file":
                return LocaleController.getString(R.string.OEAppearanceAttachItemFile);
            case "poll":
                return LocaleController.getString(R.string.OEAppearanceAttachItemPoll);
            case "location":
                return LocaleController.getString(R.string.OEAppearanceAttachItemLocation);
            case "contact":
                return LocaleController.getString(R.string.OEAppearanceAttachItemContact);
            case "music":
            default:
                return LocaleController.getString(R.string.OEAppearanceAttachItemMusic);
        }
    }

    private int getItemIcon(String key) {
        switch (key) {
            case "photo":
                return R.drawable.baseline_image_24;
            case "camera":
                return R.drawable.baseline_camera_alt_24;
            case "gif":
                return R.drawable.deproko_baseline_gif_24;
            case "file":
                return R.drawable.baseline_insert_drive_file_24;
            case "poll":
                return R.drawable.baseline_poll_24;
            case "location":
                return R.drawable.baseline_location_on_24;
            case "contact":
                return R.drawable.baseline_person_24;
            case "music":
            default:
                return R.drawable.baseline_music_note_24;
        }
    }

    private void onItemClick(UItem item, View view, int position, float x, float y) {
        if (item.id >= ID_TOGGLE_BASE && item.id < ID_TOGGLE_BASE + currentOrder.size()) {
            int index = item.id - ID_TOGGLE_BASE;
            String key = currentOrder.get(index);
            if (hiddenItems.contains(key)) {
                hiddenItems.remove(key);
            } else {
                hiddenItems.add(key);
            }
            saveHiddenItems();
            listView.adapter.update(true);
        }
    }

    private void saveHiddenItems() {
        StringBuilder sb = new StringBuilder();
        int idx = 0;
        for (String h : hiddenItems) {
            if (idx > 0) sb.append(",");
            sb.append(h);
            idx++;
        }
        AppearanceConfig.attachMenuHideItems.setConfigString(sb.toString());
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
        AppearanceConfig.attachMenuItemsOrder.setConfigString(sb.toString());
    }
}
