package app.exteraless.plugins.ui.components;

import android.text.TextUtils;

import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_iv;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.BulletinFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import app.exteraless.utils.MarkdownUtils;

public final class PluginFileViewer {

    static final long MAX_SIZE = 512 * 1024;
    private static final int CHUNK_SIZE = 8192;

    private PluginFileViewer() {
    }

    public static boolean canOpen(File file) {
        if (file == null || !file.isFile() || file.length() > MAX_SIZE) {
            return false;
        }
        return readSource(file) != null;
    }

    public static boolean open(BaseFragment fragment, File file, String fileName) {
        if (fragment == null) {
            return false;
        }
        String source = file == null || file.length() > MAX_SIZE ? null : readSource(file);
        if (source == null) {
            BulletinFactory.of(fragment).createSimpleBulletin(R.raw.error,
                    org.telegram.messenger.LocaleController.getString(R.string.ErrorOccurred)).show();
            return false;
        }
        MessageObject message = createMessageObject(file, name(file, fileName), source);
        if (message == null) {
            return false;
        }
        fragment.createArticleViewer(false).open(message);
        return true;
    }

    private static String name(File file, String fileName) {
        String result = TextUtils.isEmpty(fileName) ? file.getName() : fileName;
        return result.indexOf('.') == -1 ? result + ".plugin" : result;
    }

    static String readSource(File file) {
        if (isZip(file)) {
            return readFromArchive(file);
        }
        try (FileInputStream stream = new FileInputStream(file)) {
            byte[] bytes = new byte[(int) file.length()];
            int read = stream.read(bytes);
            if (read <= 0) {
                return null;
            }
            for (int a = 0; a < read; a++) {
                if (bytes[a] == 0) {
                    return null;
                }
            }
            return new String(bytes, 0, read, StandardCharsets.UTF_8);
        } catch (Throwable t) {
            FileLog.e("PluginFileViewer: cannot read " + file, t);
            return null;
        }
    }

    private static String readFromArchive(File file) {
        try (ZipFile archive = new ZipFile(file)) {
            ZipEntry best = null;
            Enumeration<? extends ZipEntry> entries = archive.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory() || !entry.getName().endsWith(".py")
                        || entry.getSize() > MAX_SIZE) {
                    continue;
                }
                if (best == null || entry.getSize() > best.getSize()) {
                    best = entry;
                }
            }
            if (best == null) {
                return null;
            }
            try (InputStream stream = archive.getInputStream(best)) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int read;
                while ((read = stream.read(buffer)) > 0) {
                    out.write(buffer, 0, read);
                }
                return best.getName() + "\n\n"
                        + new String(out.toByteArray(), StandardCharsets.UTF_8);
            }
        } catch (Throwable t) {
            FileLog.e("PluginFileViewer: cannot read archive " + file, t);
            return null;
        }
    }

    private static boolean isZip(File file) {
        try (FileInputStream stream = new FileInputStream(file)) {
            byte[] head = new byte[4];
            if (stream.read(head) != 4) {
                return false;
            }
            return head[0] == 'P' && head[1] == 'K'
                    && (head[2] == 3 || head[2] == 5 || head[2] == 7);
        } catch (Throwable t) {
            return false;
        }
    }

    private static MessageObject createMessageObject(File file, String fileName, String source) {
        try {
            TL_iv.TL_page page = new TL_iv.TL_page();
            page.local = file;
            page.url = fileName;
            MarkdownUtils.appendPreformattedBlocks(page.blocks, source, "python", CHUNK_SIZE);

            TLRPC.TL_webPage webPage = new TLRPC.TL_webPage();
            webPage.id = file.getAbsolutePath().hashCode();
            webPage.url = fileName;
            webPage.display_url = fileName;
            webPage.title = fileName;
            webPage.flags |= 1028;
            webPage.cached_page = page;

            long selfId = UserConfig.getInstance(UserConfig.selectedAccount).clientUserId;
            TLRPC.TL_message message = new TLRPC.TL_message();
            message.id = 0;
            message.date = (int) (System.currentTimeMillis() / 1000);
            message.message = fileName;
            message.out = true;
            TLRPC.TL_peerUser peer = new TLRPC.TL_peerUser();
            peer.user_id = selfId;
            message.peer_id = peer;
            TLRPC.TL_peerUser from = new TLRPC.TL_peerUser();
            from.user_id = selfId;
            message.from_id = from;
            TLRPC.TL_messageMediaWebPage media = new TLRPC.TL_messageMediaWebPage();
            media.webpage = webPage;
            message.media = media;

            return new MessageObject(UserConfig.selectedAccount, message, false, true);
        } catch (Throwable t) {
            FileLog.e("PluginFileViewer: cannot build the page for " + fileName, t);
            return null;
        }
    }
}
