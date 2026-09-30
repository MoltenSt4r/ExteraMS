package app.exteraless.components;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.LayoutHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import app.exteraless.appearance.AppearanceConfig;

public class Md3AttachAlert extends BottomSheet {

    public interface AttachMenuCallback {
        void onItemSelected(String key);
    }

    private final ChatActivity chatActivity;
    private final AttachMenuCallback callback;

    public Md3AttachAlert(Context context, ChatActivity chatActivity, AttachMenuCallback callback) {
        super(context, true);
        this.chatActivity = chatActivity;
        this.callback = callback;
        setApplyTopPadding(false);
        setApplyBottomPadding(true);

        GradientDrawable backgroundDrawable = new GradientDrawable();
        int bgColor = Theme.getColor(Theme.key_dialogBackground);
        backgroundDrawable.setColor(bgColor);
        float r = AndroidUtilities.dp(28);
        backgroundDrawable.setCornerRadii(new float[]{r, r, r, r, 0, 0, 0, 0});
        setBackgroundColor(bgColor);

        LinearLayout contentView = new LinearLayout(context);
        contentView.setOrientation(LinearLayout.VERTICAL);
        contentView.setBackground(backgroundDrawable);

        View dragHandle = new View(context);
        GradientDrawable handleDrawable = new GradientDrawable();
        handleDrawable.setColor(Theme.getColor(Theme.key_sheet_scrollUp));
        handleDrawable.setCornerRadius(AndroidUtilities.dp(2));
        dragHandle.setBackground(handleDrawable);
        contentView.addView(dragHandle, LayoutHelper.createLinear(32, 4, Gravity.CENTER_HORIZONTAL, 0, 12, 0, 8));

        List<String> order = new ArrayList<>();
        String savedOrder = AppearanceConfig.attachMenuItemsOrder.String();
        if (savedOrder != null && !savedOrder.isEmpty()) {
            order.addAll(Arrays.asList(savedOrder.split(",")));
        } else {
            order.addAll(Arrays.asList("photo", "camera", "gif", "file", "poll", "location", "contact", "music"));
        }

        Set<String> hidden = new HashSet<>();
        String savedHidden = AppearanceConfig.attachMenuHideItems.String();
        if (savedHidden != null && !savedHidden.isEmpty()) {
            hidden.addAll(Arrays.asList(savedHidden.split(",")));
        }

        ScrollView scrollView = new ScrollView(context);
        LinearLayout itemsLayout = new LinearLayout(context);
        itemsLayout.setOrientation(LinearLayout.VERTICAL);
        itemsLayout.setPadding(0, 0, 0, AndroidUtilities.dp(16));

        for (String key : order) {
            String itemKey = key.trim();
            if (hidden.contains(itemKey)) {
                continue;
            }
            View itemView = createItemRow(context, itemKey);
            if (itemView != null) {
                itemsLayout.addView(itemView);
            }
        }

        scrollView.addView(itemsLayout);
        contentView.addView(scrollView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        setCustomView(contentView);
    }

    private View createItemRow(Context context, String key) {
        int iconRes;
        int titleRes;
        switch (key) {
            case "photo":
                iconRes = R.drawable.msg_photos;
                titleRes = R.string.OEAppearanceAttachItemPhoto;
                break;
            case "camera":
                iconRes = R.drawable.msg_camera;
                titleRes = R.string.OEAppearanceAttachItemCamera;
                break;
            case "gif":
                iconRes = R.drawable.msg_gif;
                titleRes = R.string.OEAppearanceAttachItemGif;
                break;
            case "file":
                iconRes = R.drawable.baseline_insert_drive_file_24;
                titleRes = R.string.OEAppearanceAttachItemFile;
                break;
            case "poll":
                iconRes = R.drawable.baseline_poll_24;
                titleRes = R.string.OEAppearanceAttachItemPoll;
                break;
            case "location":
                iconRes = R.drawable.msg_location;
                titleRes = R.string.OEAppearanceAttachItemLocation;
                break;
            case "contact":
                iconRes = R.drawable.msg_contact;
                titleRes = R.string.OEAppearanceAttachItemContact;
                break;
            case "music":
                iconRes = R.drawable.baseline_music_note_24;
                titleRes = R.string.OEAppearanceAttachItemMusic;
                break;
            default:
                return null;
        }

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(AndroidUtilities.dp(52));
        row.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector)));
        row.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(10), AndroidUtilities.dp(20), AndroidUtilities.dp(10));

        ImageView iconView = new ImageView(context);
        iconView.setImageResource(iconRes);
        iconView.setColorFilter(Theme.getColor(Theme.key_dialogTextBlack));
        row.addView(iconView, LayoutHelper.createLinear(24, 24, Gravity.CENTER_VERTICAL, 0, 0, 16, 0));

        TextView textView = new TextView(context);
        textView.setText(LocaleController.getString(titleRes));
        textView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        textView.setSingleLine(true);
        row.addView(textView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL));

        row.setOnClickListener(v -> {
            dismiss();
            if (callback != null) {
                callback.onItemSelected(key);
            }
        });

        return row;
    }
}
