package app.exteraless.components;

import android.app.Activity;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Size;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import app.exteraless.chats.UserLookup;
import app.exteraless.utils.MediaUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.browser.Browser;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBarMenuSubItem;
import org.telegram.ui.ActionBar.ActionBarPopupWindow;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AnimatedFloat;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.PopupSwipeBackLayout;
import org.telegram.ui.ProfileActivity;

import java.io.File;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import tw.nekomimi.nekogram.DatacenterActivity;
import tw.nekomimi.nekogram.helpers.MessageHelper;

/**
 * Панель «детали сообщения» для выпадающего меню.
 *
 * Живёт в swipe back слое: сначала строит строки, которые известны сразу,
 * а разрешение, битрейт, координаты и платформу съёмки дочитывает из файла
 * в фоне и дописывает в готовые пункты.
 */
public abstract class MessageDetailsPopupWrapper {

    private static final int ITEM_STICKER_OWNER = 0;
    private static final int ITEM_FILE_PATH = 1;
    private static final int ITEM_LOCATION = 2;
    private static final int ITEM_BITRATE = 3;
    private static final int ITEM_RESOLUTION = 4;
    private static final int ITEM_PLATFORM = 5;

    private static final int MAX_HEIGHT_DP = 380;
    private static final int MIN_TAIL_DP = 112;

    private static final int GAP_HEIGHT_DP = 8;
    private static final int ITEM_HEIGHT_DP = 48;
    private static final int ITEM_WITH_SUBTITLE_HEIGHT_DP = 56;

    public LinearLayout swipeBack;

    private final BaseFragment fragment;
    private final Theme.ResourcesProvider resourcesProvider;

    private String filePath;
    private String[] geo;
    private long ownerId;

    private static class Item {

        final int id;
        final int icon;
        final CharSequence title;
        String subtitle;

        Item(int id, int icon, CharSequence title, String subtitle) {
            this.id = id;
            this.icon = icon;
            this.title = title;
            this.subtitle = subtitle;
        }

        Item(int icon, CharSequence title, String subtitle) {
            this(-1, icon, title, subtitle);
        }

        Item(int icon, CharSequence title, int subtitle) {
            this(-1, icon, title, String.valueOf(subtitle));
        }
    }

    public MessageDetailsPopupWrapper(BaseFragment fragment, PopupSwipeBackLayout swipeBackLayout, MessageObject messageObject, Theme.ResourcesProvider resourcesProvider) {
        this.fragment = fragment;
        this.resourcesProvider = resourcesProvider;

        Activity activity = fragment.getParentActivity();

        swipeBack = new LinearLayout(activity);
        swipeBack.setOrientation(LinearLayout.VERTICAL);

        ScrollView scrollView = new ScrollView(activity) {

            private final AnimatedFloat shadowAlpha = new AnimatedFloat(this, 350, CubicBezierInterpolator.EASE_OUT_QUINT);
            private Drawable topShadowDrawable;
            private boolean wasCanScrollVertically;

            @Override
            public void onNestedScroll(View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed) {
                super.onNestedScroll(target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed);
                boolean canScroll = canScrollVertically(-1);
                if (wasCanScrollVertically != canScroll) {
                    invalidate();
                    wasCanScrollVertically = canScroll;
                }
            }

            @Override
            protected void dispatchDraw(Canvas canvas) {
                super.dispatchDraw(canvas);
                float alpha = shadowAlpha.set(canScrollVertically(-1) ? 1.0f : 0.0f) * 0.5f;
                if (alpha <= 0.0f) {
                    return;
                }
                if (topShadowDrawable == null) {
                    topShadowDrawable = ContextCompat.getDrawable(getContext(), R.drawable.header_shadow);
                }
                if (topShadowDrawable != null) {
                    topShadowDrawable.setBounds(0, getScrollY(), getWidth(), getScrollY() + topShadowDrawable.getIntrinsicHeight());
                    topShadowDrawable.setAlpha((int) (alpha * 255));
                    topShadowDrawable.draw(canvas);
                }
            }
        };

        LinearLayout itemsLayout = new LinearLayout(activity);
        itemsLayout.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(itemsLayout);

        ActionBarMenuSubItem backItem = new ActionBarMenuSubItem(activity, true, false, resourcesProvider);
        backItem.setItemHeight(44);
        backItem.setTextAndIcon(LocaleController.getString(R.string.Back), R.drawable.msg_arrow_back);
        backItem.getTextView().setPadding(LocaleController.isRTL ? 0 : AndroidUtilities.dp(40), 0, LocaleController.isRTL ? AndroidUtilities.dp(40) : 0, 0);
        backItem.setOnClickListener(view -> swipeBackLayout.closeForeground());
        swipeBack.addView(backItem, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        itemsLayout.addView(createGap(), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, GAP_HEIGHT_DP));

        List<Item> items = fillItems(messageObject);

        boolean isVideo = messageObject.isVideo() || messageObject.isRoundVideo() || messageObject.isVideoSticker() || messageObject.isGif();
        boolean isPhoto = isPhotoAsDocument(messageObject) || messageObject.isPhoto() || messageObject.isSticker();

        int height = 0;
        for (Item item : items) {
            if (item == null) {
                itemsLayout.addView(createGap(), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, GAP_HEIGHT_DP));
                height += GAP_HEIGHT_DP;
                continue;
            }
            ActionBarMenuSubItem cell = new ActionBarMenuSubItem(activity, false, false, resourcesProvider);
            cell.setTextAndIcon(item.title, item.icon);
            cell.setMinimumWidth(AndroidUtilities.dp(196));
            itemsLayout.addView(cell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, ITEM_HEIGHT_DP));
            if (item.subtitle != null) {
                cell.setSubtext(item.subtitle);
                cell.subtextView.setEllipsize(TextUtils.TruncateAt.MARQUEE);
                cell.subtextView.setMarqueeRepeatLimit(-1);
                cell.subtextView.setSelected(true);
                cell.setItemHeight(ITEM_WITH_SUBTITLE_HEIGHT_DP);
                height += ITEM_WITH_SUBTITLE_HEIGHT_DP;
            } else {
                height += ITEM_HEIGHT_DP;
            }

            if (item.id == ITEM_STICKER_OWNER && ownerId > 0) {
                resolveOwner(cell, item);
            } else if (item.id == ITEM_LOCATION) {
                Utilities.globalQueue.postRunnable(() -> {
                    geo = getLatLongFromPhoto(new File(filePath));
                    AndroidUtilities.runOnUIThread(() -> {
                        if (geo == null) {
                            cell.setVisibility(View.GONE);
                            return;
                        }
                        item.subtitle = geo[0] + ", " + geo[1];
                        cell.setSubtext(item.subtitle);
                    });
                });
            } else if (item.id == ITEM_BITRATE) {
                Utilities.globalQueue.postRunnable(() -> {
                    int bitrate = getBitrate(messageObject, filePath);
                    AndroidUtilities.runOnUIThread(() -> {
                        if (bitrate <= 0) {
                            cell.setVisibility(View.GONE);
                            return;
                        }
                        item.subtitle = LocaleController.formatString(R.string.OEDetailsBitrateValue, bitrate);
                        cell.setSubtext(item.subtitle);
                    });
                });
            } else if (item.id == ITEM_RESOLUTION) {
                Utilities.globalQueue.postRunnable(() -> {
                    Size resolution = isVideo ? getVideoResolution(messageObject, filePath) : getPhotoResolution(messageObject, filePath);
                    AndroidUtilities.runOnUIThread(() -> {
                        if (resolution == null) {
                            cell.setVisibility(View.GONE);
                            return;
                        }
                        item.subtitle = resolution.toString();
                        cell.setSubtext(item.subtitle);
                    });
                });
            } else if (item.id == ITEM_PLATFORM) {
                Utilities.globalQueue.postRunnable(() -> {
                    String platform = MediaUtils.getPhotoPlatform(filePath);
                    AndroidUtilities.runOnUIThread(() -> {
                        if (TextUtils.isEmpty(platform)) {
                            cell.setVisibility(View.GONE);
                            return;
                        }
                        item.subtitle = platform;
                        cell.setSubtext(platform);
                    });
                });
            }

            cell.setTag(item);
            cell.setOnClickListener(view -> onItemClick(item, activity, messageObject, isPhoto, isVideo));
            cell.setOnLongClickListener(view -> {
                copy(textToCopy(item));
                return true;
            });
        }

        if (height > MAX_HEIGHT_DP && Math.abs(height - MAX_HEIGHT_DP) > MIN_TAIL_DP) {
            swipeBack.addView(scrollView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, MAX_HEIGHT_DP));
        } else {
            swipeBack.addView(scrollView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        }
    }

    public void closeMenu() {
    }

    public abstract void copy(String text);

    private List<Item> fillItems(MessageObject messageObject) {
        List<Item> items = new ArrayList<>();
        TLRPC.Message message = messageObject.messageOwner;

        if (message.views > 0) {
            items.add(new Item(R.drawable.msg_view_file, String.format(LocaleController.getPluralString("Views", message.views), AndroidUtilities.formatCount(message.views)), (String) null));
        }
        if (message.forwards > 0) {
            items.add(new Item(R.drawable.msg_forward, String.format(LocaleController.getPluralString("Shares", message.forwards), AndroidUtilities.formatCount(message.forwards)), (String) null));
        }
        if (!items.isEmpty()) {
            items.add(null);
        }

        items.add(new Item(R.drawable.msg_info, "ID", message.id));
        if (message.date > 0) {
            items.add(new Item(R.drawable.msg_calendar2, LocaleController.getString(R.string.OEDetailsDate), formatTime(message.date)));
        }
        if (message.fwd_from != null && message.fwd_from.date > 0 && message.fwd_from.date != message.date) {
            items.add(new Item(R.drawable.msg_recent, LocaleController.getString(R.string.OEDetailsForwardedDate), formatTime(message.fwd_from.date)));
        }
        if (message.edit_date > 0 && message.edit_date != message.date && !message.edit_hide) {
            items.add(new Item(R.drawable.msg_edit, LocaleController.getString(R.string.OEDetailsEditedDate), formatTime(message.edit_date)));
        }
        items.add(null);

        if (messageObject.getSize() > 0) {
            items.add(new Item(R.drawable.msg_sendfile, LocaleController.getString(R.string.OEDetailsFileSize), AndroidUtilities.formatFileSize(messageObject.getSize())));
        }
        if (!TextUtils.isEmpty(messageObject.getMimeType())) {
            items.add(new Item(R.drawable.msg_media, LocaleController.getString(R.string.OEDetailsMimeType), messageObject.getMimeType()));
        }
        if (MessageObject.getMedia(message) != null && MessageObject.getMedia(message).document != null) {
            for (TLRPC.DocumentAttribute attribute : MessageObject.getMedia(message).document.attributes) {
                if (attribute instanceof TLRPC.TL_documentAttributeFilename) {
                    items.add(new Item(R.drawable.msg_log, LocaleController.getString(R.string.OEDetailsFileName), attribute.file_name));
                }
                if (attribute instanceof TLRPC.TL_documentAttributeSticker && attribute.stickerset != null) {
                    ownerId = extractOwnerId(attribute.stickerset.id);
                    if (ownerId > 0) {
                        items.add(new Item(ITEM_STICKER_OWNER, R.drawable.msg_sticker, LocaleController.getString(R.string.ChannelCreator), String.valueOf(ownerId)));
                    }
                }
            }
        }

        filePath = MessageHelper.getPathToMessage(messageObject);
        if (!TextUtils.isEmpty(filePath)) {
            items.add(new Item(ITEM_FILE_PATH, R.drawable.msg_map, LocaleController.getString(R.string.OEDetailsFilePath), LocaleController.getString(R.string.Open)));
        }

        boolean isAudio = messageObject.isVoice() || messageObject.isMusic();
        boolean isVideo = messageObject.isVideo() || messageObject.isRoundVideo() || messageObject.isVideoSticker() || messageObject.isGif();
        boolean isPhotoAsDocument = isPhotoAsDocument(messageObject);
        boolean isPhoto = isPhotoAsDocument || messageObject.isPhoto() || messageObject.isSticker();

        if (isPhoto && !TextUtils.isEmpty(filePath)) {
            items.add(new Item(ITEM_PLATFORM, R.drawable.menu_devices, LocaleController.getString(R.string.OEDetailsPlatform), LocaleController.getString(R.string.NumberUnknown)));
        }
        if (isVideo || isPhoto) {
            items.add(new Item(ITEM_RESOLUTION, R.drawable.msg_photo_crop, LocaleController.getString(R.string.OEDetailsResolution), "0x0"));
        }
        if (isPhotoAsDocument && !TextUtils.isEmpty(filePath)) {
            items.add(new Item(ITEM_LOCATION, R.drawable.msg_location, LocaleController.getString(R.string.ShareLocation), "0.0, 0.0"));
        }
        if (isVideo || isAudio) {
            items.add(new Item(ITEM_BITRATE, R.drawable.msg_noise_on, LocaleController.getString(R.string.OEDetailsBitrate), LocaleController.formatString(R.string.OEDetailsBitrateValue, 0)));
            int duration = (int) messageObject.getDuration();
            if (duration > 0) {
                items.add(new Item(R.drawable.msg2_animations, LocaleController.getString(R.string.OEDetailsDuration), AndroidUtilities.formatShortDuration(duration)));
            }
        }

        int datacenter = getDatacenter(message);
        if (datacenter != 0) {
            items.add(new Item(R.drawable.msg_satellite, LocaleController.getString(R.string.OEDetailsDatacenter), String.format(Locale.ROOT, "DC%d, %s", datacenter, DatacenterActivity.getDCLocation(datacenter))));
        }
        if (!items.isEmpty() && items.get(items.size() - 1) == null) {
            items.remove(items.size() - 1);
        }
        return items;
    }

    private void onItemClick(Item item, Activity activity, MessageObject messageObject, boolean isPhoto, boolean isVideo) {
        closeMenu();
        if (item.id == ITEM_FILE_PATH && !TextUtils.isEmpty(filePath)) {
            openFile(activity, messageObject, isPhoto || isVideo);
            return;
        }
        if (item.id == ITEM_STICKER_OWNER) {
            long owner = ownerId;
            UserLookup.show(activity, fragment.getCurrentAccount(), fragment.getResourceProvider(), owner, user -> {
                if (user == null) {
                    copy(String.valueOf(owner));
                    return;
                }
                Bundle args = new Bundle();
                args.putLong("user_id", user.id);
                fragment.presentFragment(new ProfileActivity(args));
            });
            return;
        }
        if (item.id == ITEM_LOCATION && geo != null) {
            Browser.openUrl(fragment.getParentActivity(), String.format(Locale.ROOT, "https://maps.google.com/?q=%s,%s", geo[0], geo[1]));
            return;
        }
        copy(textToCopy(item));
    }

    private String textToCopy(Item item) {
        if (item.id == ITEM_FILE_PATH && !TextUtils.isEmpty(filePath)) {
            return filePath;
        }
        if (item.id == ITEM_STICKER_OWNER) {
            return String.valueOf(ownerId);
        }
        return item.subtitle != null ? item.subtitle : String.valueOf(item.title);
    }

    private void openFile(Activity activity, MessageObject messageObject, boolean viewable) {
        try {
            Uri uri = FileProvider.getUriForFile(activity, ApplicationLoader.getApplicationId() + ".provider", new File(filePath));
            if (viewable) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.setDataAndType(uri, messageObject.getMimeType());
                if (!activity.getPackageManager().queryIntentActivities(intent, 0).isEmpty()) {
                    activity.startActivity(intent);
                    return;
                }
            }
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.setDataAndType(uri, messageObject.getMimeType());
            activity.startActivity(Intent.createChooser(intent, LocaleController.getString(R.string.ShareFile)));
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    private void resolveOwner(ActionBarMenuSubItem cell, Item item) {
        TLRPC.User user = MessagesController.getInstance(fragment.getCurrentAccount()).getUser(ownerId);
        if (user == null) {
            return;
        }
        if (TextUtils.isEmpty(UserObject.getPublicUsername(user))) {
            item.subtitle = ContactsController.formatName(user);
        } else {
            item.subtitle = "@" + UserObject.getPublicUsername(user);
        }
        cell.setSubtext(item.subtitle);
    }

    private View createGap() {
        ActionBarPopupWindow.GapView gap = new ActionBarPopupWindow.GapView(fragment.getContext(), resourcesProvider);
        gap.setDividerVisible(false);
        return gap;
    }

    private String formatTime(int date) {
        if (date == Integer.MAX_VALUE - 1) {
            return LocaleController.getString(R.string.SendWhenOnline);
        }
        long time = date * 1000L;
        return LocaleController.formatString("formatDateAtTime", R.string.formatDateAtTime,
                LocaleController.getInstance().getFormatterYear().format(new Date(time)),
                LocaleController.getInstance().getFormatterDayWithSeconds().format(new Date(time)));
    }

    private static int getDatacenter(TLRPC.Message message) {
        TLRPC.MessageMedia media = MessageObject.getMedia(message);
        if (media == null) {
            return 0;
        }
        if (media.photo != null && media.photo.dc_id > 0) {
            return media.photo.dc_id;
        }
        if (media.document != null && media.document.dc_id > 0) {
            return media.document.dc_id;
        }
        if (media.webpage != null && media.webpage.photo != null && media.webpage.photo.dc_id > 0) {
            return media.webpage.photo.dc_id;
        }
        if (media.webpage != null && media.webpage.document != null && media.webpage.document.dc_id > 0) {
            return media.webpage.document.dc_id;
        }
        return 0;
    }

    private static boolean isPhotoAsDocument(MessageObject messageObject) {
        TLRPC.MessageMedia media = MessageObject.getMedia(messageObject.messageOwner);
        if (media == null || media.document == null) {
            return false;
        }
        for (TLRPC.DocumentAttribute attribute : media.document.attributes) {
            if (attribute instanceof TLRPC.TL_documentAttributeImageSize && attribute.w > 0 && attribute.h > 0) {
                return true;
            }
        }
        return false;
    }

    private static long extractOwnerId(long stickerSetId) {
        long ownerId = stickerSetId >> 32;
        if (((stickerSetId >> 16) & 0xff) == 0x3f) {
            ownerId |= 0x80000000L;
        }
        return ((stickerSetId >> 24) & 0xff) != 0 ? ownerId + 0x100000000L : ownerId;
    }

    public static int getBitrate(MessageObject messageObject, String path) {
        int bitrate = -1;
        if (!TextUtils.isEmpty(path)) {
            bitrate = getBitrateFromPath(path);
        }
        return bitrate != -1 ? bitrate : getBitrateFromAttributes(messageObject);
    }

    public static int getBitrateFromPath(String path) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        int bitrate = -1;
        try {
            retriever.setDataSource(path);
            String value = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);
            if (value != null) {
                bitrate = Integer.parseInt(value) / 1000;
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        try {
            retriever.release();
        } catch (Throwable t) {
            FileLog.e(t);
        }
        return bitrate;
    }

    public static int getBitrateFromAttributes(MessageObject messageObject) {
        long size = MessageObject.getMessageSize(messageObject.messageOwner);
        TLRPC.MessageMedia media = MessageObject.getMedia(messageObject.messageOwner);
        if (size <= 0 || media == null || media.document == null) {
            return -1;
        }
        for (TLRPC.DocumentAttribute attribute : media.document.attributes) {
            if ((attribute instanceof TLRPC.TL_documentAttributeAudio || attribute instanceof TLRPC.TL_documentAttributeVideo) && attribute.duration > 0) {
                return (int) ((size / attribute.duration) * 8 / 1000);
            }
        }
        return -1;
    }

    public static Size getVideoResolution(MessageObject messageObject, String path) {
        Size size = null;
        if (!TextUtils.isEmpty(path)) {
            size = getVideoResolutionFromPath(path);
        }
        return size != null ? size : getVideoResolutionFromAttributes(messageObject);
    }

    public static Size getVideoResolutionFromPath(String path) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        int width = 0;
        int height = 0;
        try {
            retriever.setDataSource(path);
            String widthValue = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
            String heightValue = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
            if (widthValue != null) {
                width = Integer.parseInt(widthValue);
            }
            if (heightValue != null) {
                height = Integer.parseInt(heightValue);
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        try {
            retriever.release();
        } catch (Throwable t) {
            FileLog.e(t);
        }
        return width > 0 && height > 0 ? new Size(width, height) : null;
    }

    public static Size getVideoResolutionFromAttributes(MessageObject messageObject) {
        TLRPC.MessageMedia media = MessageObject.getMedia(messageObject.messageOwner);
        if (media == null || media.document == null) {
            return null;
        }
        for (TLRPC.DocumentAttribute attribute : media.document.attributes) {
            if (attribute instanceof TLRPC.TL_documentAttributeVideo && attribute.w > 0 && attribute.h > 0) {
                return new Size(attribute.w, attribute.h);
            }
        }
        return null;
    }

    public static Size getPhotoResolution(MessageObject messageObject, String path) {
        Size size = null;
        if (!TextUtils.isEmpty(path)) {
            size = getPhotoResolutionFromPath(path);
        }
        return size != null ? size : getPhotoResolutionFromAttributes(messageObject);
    }

    public static Size getPhotoResolutionFromPath(String path) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        return options.outWidth > 0 && options.outHeight > 0 ? new Size(options.outWidth, options.outHeight) : null;
    }

    public static Size getPhotoResolutionFromAttributes(MessageObject messageObject) {
        TLRPC.MessageMedia media = MessageObject.getMedia(messageObject.messageOwner);
        if (media == null) {
            return null;
        }
        if (media.photo != null) {
            TLRPC.PhotoSize photoSize = FileLoader.getClosestPhotoSizeWithSize(media.photo.sizes, AndroidUtilities.getPhotoSize(), false, null, true);
            if (photoSize != null && photoSize.w > 0 && photoSize.h > 0) {
                return new Size(photoSize.w, photoSize.h);
            }
            TLRPC.VideoSize videoSize = FileLoader.getClosestVideoSizeWithSize(media.photo.video_sizes, AndroidUtilities.getPhotoSize(), false, true);
            if (videoSize != null && videoSize.w > 0 && videoSize.h > 0) {
                return new Size(videoSize.w, videoSize.h);
            }
            return null;
        }
        if (media.document != null) {
            for (TLRPC.DocumentAttribute attribute : media.document.attributes) {
                if (attribute instanceof TLRPC.TL_documentAttributeImageSize && attribute.w > 0 && attribute.h > 0) {
                    return new Size(attribute.w, attribute.h);
                }
            }
        }
        return null;
    }

    public static String[] getLatLongFromPhoto(File file) {
        try {
            ExifInterface exif = new ExifInterface(file.getAbsolutePath());
            String latitude = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE);
            String longitude = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE);
            String latitudeRef = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF);
            String longitudeRef = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF);
            if (latitude == null || longitude == null || latitudeRef == null || longitudeRef == null) {
                return null;
            }
            double lat = convertToDegrees(latitude);
            if ("S".equalsIgnoreCase(latitudeRef)) {
                lat = -lat;
            }
            double lon = convertToDegrees(longitude);
            if ("W".equalsIgnoreCase(longitudeRef)) {
                lon = -lon;
            }
            DecimalFormat format = new DecimalFormat("#.######");
            format.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.ENGLISH));
            return new String[]{format.format(lat), format.format(lon)};
        } catch (Exception e) {
            FileLog.e(e);
            return null;
        }
    }

    private static double convertToDegrees(String value) {
        String[] parts = value.split(",");
        if (parts.length < 3) {
            return 0;
        }
        return convertToDouble(parts[0]) + convertToDouble(parts[1]) / 60 + convertToDouble(parts[2]) / 3600;
    }

    private static double convertToDouble(String value) {
        String[] parts = value.split("/");
        if (parts.length == 1) {
            return Double.parseDouble(parts[0]);
        }
        if (parts.length != 2) {
            FileLog.e("Invalid rational number format: " + value);
            return 0;
        }
        double divider = Double.parseDouble(parts[1]);
        if (divider == 0) {
            FileLog.e("Division by zero in GPS data");
            return 0;
        }
        return Double.parseDouble(parts[0]) / divider;
    }
}
