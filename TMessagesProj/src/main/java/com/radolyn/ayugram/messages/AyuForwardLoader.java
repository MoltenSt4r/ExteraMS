package com.radolyn.ayugram.messages;

import android.text.TextUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.BulletinFactory;

import java.util.ArrayList;
import java.util.HashMap;

import tw.nekomimi.nekogram.helpers.MessageHelper;

public final class AyuForwardLoader implements NotificationCenter.NotificationCenterDelegate {

    private final BaseFragment fragment;
    private final int account;
    private final Runnable onReady;
    private final HashMap<String, MessageObject> pending = new HashMap<>();
    private final HashMap<String, Float> progress = new HashMap<>();
    private final int total;
    private AlertDialog dialog;
    private boolean finished;

    private AyuForwardLoader(BaseFragment fragment, ArrayList<MessageObject> messages, Runnable onReady) {
        this.fragment = fragment;
        this.account = fragment.getCurrentAccount();
        this.onReady = onReady;
        for (MessageObject message : messages) {
            final String name = message.getFileName();
            if (!TextUtils.isEmpty(name)) {
                pending.put(name, message);
            }
        }
        this.total = pending.size();
    }

    public static boolean needsFile(MessageObject messageObject) {
        return messageObject != null && messageObject.messageOwner != null && !MessageHelper.isWebPageMessage(messageObject) && !messageObject.isSticker() && !messageObject.isAnimatedSticker() && !messageObject.isAnimatedEmoji()
                && (messageObject.isPhoto() || messageObject.isVideo() || messageObject.isRoundVideo() || messageObject.getDocument() != null);
    }

    public static ArrayList<MessageObject> getMissingMedia(int account, ArrayList<MessageObject> messages) {
        final ArrayList<MessageObject> missing = new ArrayList<>();
        if (messages == null) {
            return missing;
        }
        for (MessageObject message : messages) {
            if (needsFile(message) && TextUtils.isEmpty(MessageHelper.getPathToMessage(message, account))) {
                missing.add(message);
            }
        }
        return missing;
    }

    public static void load(BaseFragment fragment, ArrayList<MessageObject> missing, Runnable onReady) {
        final AyuForwardLoader loader = new AyuForwardLoader(fragment, missing, onReady);
        if (loader.total == 0) {
            onReady.run();
            return;
        }
        loader.start();
    }

    private void start() {
        final NotificationCenter center = NotificationCenter.getInstance(account);
        center.addObserver(this, NotificationCenter.fileLoaded);
        center.addObserver(this, NotificationCenter.fileLoadFailed);
        center.addObserver(this, NotificationCenter.fileLoadProgressChanged);
        if (fragment.getParentActivity() != null) {
            dialog = new AlertDialog(fragment.getParentActivity(), AlertDialog.ALERT_TYPE_LOADING, fragment.getResourceProvider());
            dialog.setTitle(LocaleController.getString(R.string.OEAyuForwardLoadingMedia));
            dialog.setMessage(LocaleController.formatString(R.string.OEAyuForwardLoadingCount, 0, total));
            dialog.setNegativeButton(LocaleController.getString(R.string.Cancel), (d, which) -> cancel());
            dialog.show();
        }
        for (MessageObject message : new ArrayList<>(pending.values())) {
            requestLoad(message);
        }
        checkExisting();
    }

    private void requestLoad(MessageObject message) {
        final FileLoader loader = FileLoader.getInstance(account);
        final TLRPC.Document document = message.getDocument();
        if (document != null) {
            loader.loadFile(document, message, FileLoader.PRIORITY_HIGH, 0);
            return;
        }
        final TLRPC.PhotoSize size = FileLoader.getClosestPhotoSizeWithSize(message.photoThumbs, AndroidUtilities.getPhotoSize());
        if (size != null) {
            loader.loadFile(ImageLocation.getForObject(size, message.photoThumbsObject), message, null, FileLoader.PRIORITY_HIGH, 0);
        }
    }

    private void checkExisting() {
        for (MessageObject message : new ArrayList<>(pending.values())) {
            if (!TextUtils.isEmpty(MessageHelper.getPathToMessage(message, account))) {
                pending.remove(message.getFileName());
            }
        }
        update();
    }

    private void update() {
        if (finished) {
            return;
        }
        if (pending.isEmpty()) {
            finish();
            onReady.run();
            return;
        }
        final int done = total - pending.size();
        float sum = done;
        for (String name : pending.keySet()) {
            final Float value = progress.get(name);
            if (value != null) {
                sum += value;
            }
        }
        if (dialog != null) {
            dialog.setMessage(LocaleController.formatString(R.string.OEAyuForwardLoadingCount, done, total));
            dialog.setProgress((int) (sum * 100 / total));
        }
    }

    private void cancel() {
        if (finished) {
            return;
        }
        final FileLoader loader = FileLoader.getInstance(account);
        for (MessageObject message : pending.values()) {
            final TLRPC.Document document = message.getDocument();
            if (document != null) {
                loader.cancelLoadFile(document);
            } else {
                final TLRPC.PhotoSize size = FileLoader.getClosestPhotoSizeWithSize(message.photoThumbs, AndroidUtilities.getPhotoSize());
                if (size != null) {
                    loader.cancelLoadFile(size);
                }
            }
        }
        finish();
    }

    private void finish() {
        finished = true;
        final NotificationCenter center = NotificationCenter.getInstance(account);
        center.removeObserver(this, NotificationCenter.fileLoaded);
        center.removeObserver(this, NotificationCenter.fileLoadFailed);
        center.removeObserver(this, NotificationCenter.fileLoadProgressChanged);
        if (dialog != null) {
            try {
                dialog.dismiss();
            } catch (Exception ignored) {
            }
            dialog = null;
        }
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (finished || args.length == 0 || !(args[0] instanceof String)) {
            return;
        }
        final String name = (String) args[0];
        if (!pending.containsKey(name)) {
            return;
        }
        if (id == NotificationCenter.fileLoaded) {
            pending.remove(name);
            progress.remove(name);
            update();
        } else if (id == NotificationCenter.fileLoadFailed) {
            finish();
            BulletinFactory.of(fragment).createErrorBulletin(LocaleController.getString(R.string.OEAyuForwardLoadFailed)).show();
        } else if (id == NotificationCenter.fileLoadProgressChanged && args.length >= 3 && args[1] instanceof Number && args[2] instanceof Number) {
            final long loaded = ((Number) args[1]).longValue();
            final long totalSize = ((Number) args[2]).longValue();
            if (totalSize > 0) {
                progress.put(name, Math.min(1f, loaded / (float) totalSize));
                update();
            }
        }
    }
}
