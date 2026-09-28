package org.telegram.messenger;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import xyz.nextalone.nagram.NaConfig;

/**
 * ExteraMS: Скрытие и чтение эмодзи-статуса через аватарку пользователя (стеганография).
 *
 * Код статуса (64-битный document_id) кодируется в сетку 4x4 блоков в левом нижнем углу
 * аватарки (за пределами круглой маски аватара, поэтому обычным пользователям не виден).
 * Если при чтении цвета/контрольная сумма не совпадают, статус автоматически снимается.
 */
public final class AvatarStatusHelper {

    private static final byte MAGIC_1 = (byte) 0xEA;
    private static final byte MAGIC_2 = (byte) 0x5D;

    private static final Map<Long, Long> userStatusMap = new ConcurrentHashMap<>();
    private static final Set<Long> checkedUsers = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public static Long getDocumentId(long userId) {
        if (userId == 0) return null;
        Long cached = userStatusMap.get(userId);
        if (cached != null) {
            return cached != 0L ? cached : null;
        }
        if (!checkedUsers.contains(userId)) {
            checkedUsers.add(userId);
            Utilities.globalQueue.postRunnable(() -> checkCachedAvatar(userId));
        }
        return null;
    }

    public static void setExtractedStatus(long userId, Long documentId) {
        if (userId == 0) return;
        if (documentId != null && documentId != 0L) {
            Long prev = userStatusMap.put(userId, documentId);
            if (prev == null || !prev.equals(documentId)) {
                AndroidUtilities.runOnUIThread(() -> NotificationCenter.getInstance(UserConfig.selectedAccount).postNotificationName(
                        NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_EMOJI_STATUS));
            }
        } else {
            Long prev = userStatusMap.remove(userId);
            if (prev != null) {
                AndroidUtilities.runOnUIThread(() -> NotificationCenter.getInstance(UserConfig.selectedAccount).postNotificationName(
                        NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_EMOJI_STATUS));
            }
        }
    }

    public static void checkUserAvatar(TLRPC.User user, Bitmap bitmap) {
        if (user == null || bitmap == null) return;
        Long docId = extractStatus(bitmap);
        setExtractedStatus(user.id, docId);
    }

    public static void checkChatAvatar(TLRPC.Chat chat, Bitmap bitmap) {
        if (chat == null || bitmap == null) return;
        Long docId = extractStatus(bitmap);
        setExtractedStatus(-chat.id, docId);
    }

    private static void checkCachedAvatar(long userId) {
        try {
            int currentAccount = UserConfig.selectedAccount;
            TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(userId);
            if (user == null || user.photo == null) return;
            File f = FileLoader.getInstance(currentAccount).getPathToAttach(user.photo.photo_small, true);
            if (f == null || !f.exists()) {
                f = ImageReceiver.getAvatarLocalFile(currentAccount, user);
            }
            if (f == null || !f.exists()) {
                f = FileLoader.getInstance(currentAccount).getPathToAttach(user.photo.photo_big, true);
            }
            if (f != null && f.exists()) {
                Bitmap bmp = BitmapFactory.decodeFile(f.getAbsolutePath());
                if (bmp != null) {
                    Long docId = extractStatus(bmp);
                    bmp.recycle();
                    setExtractedStatus(userId, docId);
                }
            }
        } catch (Throwable t) {
            FileLog.e(t);
        }
    }

    /**
     * Встраивает document_id в нижний левый угол Bitmap (сетка 4x4, 13% ширины и высоты).
     */
    public static Bitmap embedStatus(Bitmap bitmap, long documentId) {
        if (bitmap == null || documentId == 0L) return bitmap;
        Bitmap target = bitmap.isMutable() ? bitmap : bitmap.copy(Bitmap.Config.ARGB_8888, true);
        int w = target.getWidth();
        int h = target.getHeight();
        if (w < 20 || h < 20) return target;

        byte[] payload = new byte[11];
        payload[0] = MAGIC_1;
        payload[1] = MAGIC_2;
        int sum = 0;
        for (int i = 0; i < 8; i++) {
            byte b = (byte) ((documentId >>> ((7 - i) * 8)) & 0xFF);
            payload[2 + i] = b;
            sum += (b & 0xFF);
        }
        payload[10] = (byte) (sum & 0xFF);

        int[] symbols = new int[48];
        for (int i = 0; i < 11; i++) {
            int b = payload[i] & 0xFF;
            symbols[i * 4] = (b >>> 6) & 0x03;
            symbols[i * 4 + 1] = (b >>> 4) & 0x03;
            symbols[i * 4 + 2] = (b >>> 2) & 0x03;
            symbols[i * 4 + 3] = b & 0x03;
        }

        int gridW = Math.max(8, (int) (w * 0.13f));
        int gridH = Math.max(8, (int) (h * 0.13f));
        float cellW = gridW / 4.0f;
        float cellH = gridH / 4.0f;

        Canvas canvas = new Canvas(target);
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);

        for (int k = 0; k < 16; k++) {
            int r = symbols[k * 3] * 64 + 32;
            int g = symbols[k * 3 + 1] * 64 + 32;
            int b = symbols[k * 3 + 2] * 64 + 32;
            paint.setColor(Color.rgb(r, g, b));

            int row = k / 4;
            int col = k % 4;
            float x0 = col * cellW;
            float x1 = (col + 1) * cellW;
            float y0 = h - gridH + row * cellH;
            float y1 = h - gridH + (row + 1) * cellH;

            canvas.drawRect(x0, y0, x1, y1, paint);
        }
        return target;
    }

    /**
     * Очищает метку в нижнем левом углу, закрашивая её фоновым цветом соседних пикселей.
     */
    public static Bitmap clearStatus(Bitmap bitmap) {
        if (bitmap == null) return null;
        Bitmap target = bitmap.isMutable() ? bitmap : bitmap.copy(Bitmap.Config.ARGB_8888, true);
        int w = target.getWidth();
        int h = target.getHeight();
        if (w < 20 || h < 20) return target;

        int gridW = Math.max(8, (int) (w * 0.13f));
        int gridH = Math.max(8, (int) (h * 0.13f));

        int sampleX = Math.min(w - 1, gridW + 4);
        int sampleY = Math.max(0, h - 4);
        int sampleColor = target.getPixel(sampleX, sampleY);

        Canvas canvas = new Canvas(target);
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(sampleColor);
        canvas.drawRect(0, h - gridH, gridW, h, paint);
        return target;
    }

    /**
     * Извлекает document_id из нижнего левого угла Bitmap.
     * Если цвета не совпадают с кодом или контрольная сумма неверна, возвращает null.
     */
    public static Long extractStatus(Bitmap bitmap) {
        if (bitmap == null) return null;
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        if (w < 20 || h < 20) return null;

        int gridW = Math.max(8, (int) (w * 0.13f));
        int gridH = Math.max(8, (int) (h * 0.13f));
        float cellW = gridW / 4.0f;
        float cellH = gridH / 4.0f;

        int[] symbols = new int[48];
        for (int k = 0; k < 16; k++) {
            int row = k / 4;
            int col = k % 4;
            int cx = Math.round((col + 0.5f) * cellW);
            int cy = Math.round(h - gridH + (row + 0.5f) * cellH);
            cx = Math.max(0, Math.min(w - 1, cx));
            cy = Math.max(0, Math.min(h - 1, cy));

            int pixel = bitmap.getPixel(cx, cy);
            int r = Color.red(pixel);
            int g = Color.green(pixel);
            int b = Color.blue(pixel);

            symbols[k * 3] = Math.max(0, Math.min(3, Math.round((r - 32) / 64.0f)));
            symbols[k * 3 + 1] = Math.max(0, Math.min(3, Math.round((g - 32) / 64.0f)));
            symbols[k * 3 + 2] = Math.max(0, Math.min(3, Math.round((b - 32) / 64.0f)));
        }

        byte[] payload = new byte[11];
        for (int i = 0; i < 11; i++) {
            int b = (symbols[i * 4] << 6) | (symbols[i * 4 + 1] << 4) | (symbols[i * 4 + 2] << 2) | symbols[i * 4 + 3];
            payload[i] = (byte) (b & 0xFF);
        }

        if (payload[0] != MAGIC_1 || payload[1] != MAGIC_2) {
            return null;
        }

        long docId = 0L;
        int sum = 0;
        for (int i = 0; i < 8; i++) {
            int b = payload[2 + i] & 0xFF;
            docId = (docId << 8) | b;
            sum += b;
        }

        int checksum = payload[10] & 0xFF;
        if ((sum & 0xFF) != checksum) {
            return null;
        }
        if (docId == 0L) {
            return null;
        }
        return docId;
    }

    /**
     * Синхронизирует выбранный эмодзи-статус с аватаркой текущего пользователя (встраивает код в фото).
     */
    public static void syncEmojiStatusWithAvatar(int currentAccount, TLRPC.EmojiStatus status) {
        if (!NaConfig.INSTANCE.getCustomEmojiStatusThroughAvatar().Bool()) {
            return;
        }
        long docId = 0L;
        if (status instanceof TLRPC.TL_emojiStatus) {
            docId = ((TLRPC.TL_emojiStatus) status).document_id;
        } else if (status instanceof TLRPC.TL_emojiStatusCollectible) {
            docId = ((TLRPC.TL_emojiStatusCollectible) status).document_id;
        }
        final long documentId = docId;

        Utilities.globalQueue.postRunnable(() -> {
            try {
                TLRPC.User user = UserConfig.getInstance(currentAccount).getCurrentUser();
                if (user == null || user.photo == null) {
                    return;
                }
                File avatarFile = FileLoader.getInstance(currentAccount).getPathToAttach(user.photo.photo_big, true);
                if (avatarFile == null || !avatarFile.exists()) {
                    avatarFile = FileLoader.getInstance(currentAccount).getPathToAttach(user.photo.photo_small, true);
                }
                if (avatarFile == null || !avatarFile.exists()) {
                    avatarFile = ImageReceiver.getAvatarLocalFile(currentAccount, user);
                }
                if (avatarFile == null || !avatarFile.exists()) {
                    return;
                }

                Bitmap original = BitmapFactory.decodeFile(avatarFile.getAbsolutePath());
                if (original == null) {
                    return;
                }
                Bitmap mutable = original.copy(Bitmap.Config.ARGB_8888, true);
                original.recycle();

                if (documentId != 0L) {
                    mutable = embedStatus(mutable, documentId);
                } else {
                    mutable = clearStatus(mutable);
                }

                File cacheDir = FileLoader.getDirectory(FileLoader.MEDIA_DIR_CACHE);
                File uploadFile = new File(cacheDir, "avatar_status_" + System.currentTimeMillis() + ".jpg");
                try (FileOutputStream fos = new FileOutputStream(uploadFile)) {
                    mutable.compress(Bitmap.CompressFormat.JPEG, 87, fos);
                }
                mutable.recycle();

                AndroidUtilities.runOnUIThread(() -> uploadNewAvatar(currentAccount, uploadFile));
            } catch (Throwable t) {
                FileLog.e(t);
            }
        });
    }

    private static void uploadNewAvatar(int currentAccount, File uploadFile) {
        final String path = uploadFile.getAbsolutePath();
        NotificationCenter.NotificationCenterDelegate delegate = new NotificationCenter.NotificationCenterDelegate() {
            @Override
            public void didReceivedNotification(int id, int account, Object... args) {
                if (account != currentAccount) return;
                if (id == NotificationCenter.fileUploaded) {
                    String location = (String) args[0];
                    if (path.equals(location)) {
                        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.fileUploaded);
                        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.fileUploadFailed);
                        TLRPC.InputFile file = (TLRPC.InputFile) args[1];
                        TLRPC.TL_photos_uploadProfilePhoto req = new TLRPC.TL_photos_uploadProfilePhoto();
                        req.file = file;
                        req.flags |= 1;
                        ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> {
                            if (response instanceof TLRPC.TL_photos_photo) {
                                TLRPC.TL_photos_photo photo = (TLRPC.TL_photos_photo) response;
                                MessagesController.getInstance(currentAccount).putUsers(photo.users, false);
                                AndroidUtilities.runOnUIThread(() -> {
                                    NotificationCenter.getInstance(currentAccount).postNotificationName(
                                            NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
                                    NotificationCenter.getInstance(currentAccount).postNotificationName(
                                            NotificationCenter.mainUserInfoChanged);
                                });
                            }
                            uploadFile.delete();
                        });
                    }
                } else if (id == NotificationCenter.fileUploadFailed) {
                    String location = (String) args[0];
                    if (path.equals(location)) {
                        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.fileUploaded);
                        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.fileUploadFailed);
                        uploadFile.delete();
                    }
                }
            }
        };
        NotificationCenter.getInstance(currentAccount).addObserver(delegate, NotificationCenter.fileUploaded);
        NotificationCenter.getInstance(currentAccount).addObserver(delegate, NotificationCenter.fileUploadFailed);
        FileLoader.getInstance(currentAccount).uploadFile(path, false, true, ConnectionsManager.FileTypePhoto);
    }
}
