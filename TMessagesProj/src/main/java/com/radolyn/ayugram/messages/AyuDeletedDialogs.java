package com.radolyn.ayugram.messages;

import com.radolyn.ayugram.database.AyuData;
import com.radolyn.ayugram.database.dao.DeletedDialogDao;
import com.radolyn.ayugram.database.entities.DeletedDialog;
import com.radolyn.ayugram.database.entities.DeletedMessageFull;
import com.radolyn.ayugram.proprietary.AyuHistoryHook;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.List;

import xyz.nextalone.nagram.NaConfig;

public final class AyuDeletedDialogs {

    private static final boolean[] restored = new boolean[UserConfig.MAX_ACCOUNT_COUNT];

    private AyuDeletedDialogs() {
    }

    private static boolean isEnabledFor(int account, long dialogId) {
        if (!NaConfig.INSTANCE.getEnableSaveDeletedMessages().Bool() || !NaConfig.INSTANCE.getSaveDeletedInPrivateChats().Bool()) {
            return false;
        }
        if (!DialogObject.isUserDialog(dialogId) || dialogId == UserConfig.getInstance(account).getClientUserId()) {
            return false;
        }
        if (AyuSavePreferences.getSaveDeletedExclusion(dialogId) || !AyuSavePreferences.saveInDialogFolder(account, dialogId)) {
            return false;
        }
        final TLRPC.User user = MessagesController.getInstance(account).getUser(dialogId);
        return user == null || !user.bot || NaConfig.INSTANCE.getSaveDeletedMessageForBotUser().Bool();
    }

    public static void onDialogEmptied(int account, long dialogId) {
        if (!isEnabledFor(account, dialogId)) {
            return;
        }
        final TLRPC.Dialog dialog = MessagesController.getInstance(account).dialogs_dict.get(dialogId);
        final int folderId = dialog != null ? dialog.folder_id : 0;
        final long userId = UserConfig.getInstance(account).getClientUserId();
        Utilities.globalQueue.postRunnable(() -> {
            final TLRPC.Message message = loadLastMessage(account, userId, dialogId);
            final DeletedDialogDao dao = AyuData.getDeletedDialogDao();
            if (message == null || dao == null) {
                return;
            }
            final DeletedDialog deletedDialog = new DeletedDialog();
            deletedDialog.userId = userId;
            deletedDialog.dialogId = dialogId;
            deletedDialog.folderId = folderId;
            deletedDialog.topMessage = message.id;
            deletedDialog.lastMessageDate = message.date;
            deletedDialog.entityCreateDate = (int) (System.currentTimeMillis() / 1000);
            try {
                dao.insert(deletedDialog);
            } catch (Exception e) {
                FileLog.e(e);
                return;
            }
            AndroidUtilities.runOnUIThread(() -> {
                if (apply(account, deletedDialog, message)) {
                    MessagesController.getInstance(account).sortDialogs(null);
                    NotificationCenter.getInstance(account).postNotificationName(NotificationCenter.dialogsNeedReload);
                }
            });
        });
    }

    public static void forget(int account, long dialogId) {
        if (!DialogObject.isUserDialog(dialogId)) {
            return;
        }
        final long userId = UserConfig.getInstance(account).getClientUserId();
        Utilities.globalQueue.postRunnable(() -> {
            final DeletedDialogDao dao = AyuData.getDeletedDialogDao();
            if (dao == null) {
                return;
            }
            try {
                dao.delete(userId, dialogId);
            } catch (Exception e) {
                FileLog.e(e);
            }
        });
    }

    public static void restore(int account) {
        if (account < 0 || account >= restored.length || restored[account]) {
            return;
        }
        restored[account] = true;
        if (!NaConfig.INSTANCE.getEnableSaveDeletedMessages().Bool() || !NaConfig.INSTANCE.getSaveDeletedInPrivateChats().Bool()) {
            return;
        }
        final long userId = UserConfig.getInstance(account).getClientUserId();
        if (userId == 0) {
            return;
        }
        Utilities.globalQueue.postRunnable(() -> {
            final DeletedDialogDao dao = AyuData.getDeletedDialogDao();
            if (dao == null) {
                return;
            }
            final List<DeletedDialog> dialogs;
            try {
                dialogs = dao.getAll(userId);
            } catch (Exception e) {
                FileLog.e(e);
                return;
            }
            if (dialogs == null || dialogs.isEmpty()) {
                return;
            }
            final ArrayList<DeletedDialog> found = new ArrayList<>();
            final ArrayList<TLRPC.Message> messages = new ArrayList<>();
            final ArrayList<Long> userIds = new ArrayList<>();
            for (DeletedDialog deletedDialog : dialogs) {
                final TLRPC.Message message = loadLastMessage(account, userId, deletedDialog.dialogId);
                if (message == null) {
                    continue;
                }
                found.add(deletedDialog);
                messages.add(message);
                userIds.add(deletedDialog.dialogId);
            }
            if (found.isEmpty()) {
                return;
            }
            final MessagesStorage storage = MessagesStorage.getInstance(account);
            storage.getStorageQueue().postRunnable(() -> {
                final ArrayList<TLRPC.User> users = new ArrayList<>();
                try {
                    storage.getUsersInternal(userIds, users);
                } catch (Exception e) {
                    FileLog.e(e);
                }
                AndroidUtilities.runOnUIThread(() -> {
                    final MessagesController controller = MessagesController.getInstance(account);
                    controller.putUsers(users, true);
                    boolean changed = false;
                    for (int i = 0; i < found.size(); i++) {
                        if (controller.getUser(found.get(i).dialogId) != null && apply(account, found.get(i), messages.get(i))) {
                            changed = true;
                        }
                    }
                    if (changed) {
                        controller.sortDialogs(null);
                        NotificationCenter.getInstance(account).postNotificationName(NotificationCenter.dialogsNeedReload);
                    }
                });
            });
        });
    }

    private static TLRPC.Message loadLastMessage(int account, long userId, long dialogId) {
        try {
            final List<DeletedMessageFull> latest = AyuMessagesController.getInstance().getLatestMessages(userId, dialogId, 10);
            if (latest == null) {
                return null;
            }
            for (DeletedMessageFull full : latest) {
                if (AyuHistoryHook.hasContent(full)) {
                    final TLRPC.TL_message message = AyuHistoryHook.map(full, account);
                    message.unread = false;
                    message.media_unread = false;
                    return message;
                }
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return null;
    }

    private static boolean apply(int account, DeletedDialog deletedDialog, TLRPC.Message message) {
        final MessagesController controller = MessagesController.getInstance(account);
        final long dialogId = deletedDialog.dialogId;
        TLRPC.Dialog dialog = controller.dialogs_dict.get(dialogId);
        final ArrayList<MessageObject> current = controller.dialogMessage.get(dialogId);
        if (dialog != null && current != null && !current.isEmpty() && current.get(0) != null && !current.get(0).isAyuDeleted()) {
            return false;
        }
        if (dialog == null) {
            dialog = new TLRPC.TL_dialog();
            dialog.id = dialogId;
            dialog.peer = new TLRPC.TL_peerUser();
            dialog.peer.user_id = dialogId;
            dialog.folder_id = deletedDialog.folderId;
            dialog.notify_settings = new TLRPC.TL_peerNotifySettings();
            controller.dialogs_dict.put(dialogId, dialog);
            controller.getAllDialogs().add(dialog);
        }
        dialog.top_message = message.id;
        dialog.last_message_date = message.date;
        dialog.unread_count = 0;
        dialog.unread_mark = false;
        final MessageObject messageObject = new MessageObject(account, message, false, true);
        messageObject.setIsRead();
        messageObject.setContentIsRead();
        final ArrayList<MessageObject> messages = new ArrayList<>();
        messages.add(messageObject);
        controller.dialogMessage.put(dialogId, messages);
        return true;
    }
}
