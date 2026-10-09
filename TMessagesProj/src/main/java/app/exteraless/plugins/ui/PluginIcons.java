package app.exteraless.plugins.ui;

import android.graphics.drawable.Drawable;
import android.text.TextUtils;

import org.telegram.messenger.DocumentObject;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BackupImageView;

import app.exteraless.plugins.Plugin;

/**
 * Иконка плагина: стикер из набора, который плагин назвал сам.
 *
 * Плагины пишут в метаданных {@code __icon__ = "exteraPlugins/12"} — короткое
 * имя набора и номер стикера в нём. Своих картинок в файле плагина нет вовсе,
 * поэтому единственный способ показать иконку — сходить за набором.
 *
 * Загрузка асинхронная и может не приехать (набор удалён, сети нет): вызов
 * возвращает лишь «имя разобрано», а картинка появляется позже. Метка на вьюхе
 * защищает от переиспользования в списке — пока набор едет, ячейку могли
 * отдать другому плагину.
 */
public final class PluginIcons {

    private PluginIcons() {
    }

    /**
     * Ставит картинку, когда она приедет, и только тогда показывает вьюху.
     *
     * Показывать её сразу нельзя: набор может не приехать вовсе (сети нет,
     * набор удалён), и в карточке остаётся дыра 56dp над именем — ровно это и
     * было видно у AdBlock без сети.
     *
     * @param onLoaded выполнится на UI-потоке, когда картинка встала: вызывающему
     *                 обычно нужно пересчитать раскладку.
     * @return true, если у плагина вообще есть чем показаться (набор и номер
     *         разобраны); картинка приедет позже.
     */
    public static boolean apply(BackupImageView imageView, Plugin plugin, Runnable onLoaded) {
        return apply(imageView, plugin != null ? plugin.icon : null, onLoaded);
    }

    public static boolean apply(BackupImageView imageView, String icon, Runnable onLoaded) {
        if (imageView == null || TextUtils.isEmpty(icon)) {
            return false;
        }
        final int slash = icon.lastIndexOf('/');
        if (slash <= 0 || slash == icon.length() - 1) {
            return false;
        }
        final String pack = icon.substring(0, slash);
        final int index;
        try {
            index = Integer.parseInt(icon.substring(slash + 1).trim());
        } catch (NumberFormatException e) {
            return false;
        }
        if (index < 0) {
            return false;
        }

        final String tag = "plugin_icon_" + pack + "_" + index;
        imageView.setTag(tag);
        imageView.setImageDrawable(null);
        imageView.setVisibility(android.view.View.GONE);

        TLRPC.TL_inputStickerSetShortName input = new TLRPC.TL_inputStickerSetShortName();
        input.short_name = pack;
        MediaDataController.getInstance(UserConfig.selectedAccount)
                .getStickerSet(input, 0, false, set -> {
                    if (set == null || set.documents == null || index >= set.documents.size()) {
                        return;
                    }
                    if (!tag.equals(imageView.getTag())) {
                        return;  // ячейку успели отдать другому плагину
                    }
                    TLRPC.Document document = set.documents.get(index);
                    Drawable thumb = DocumentObject.getSvgThumb(document,
                            Theme.key_windowBackgroundWhiteGrayIcon, 0.2f);
                    imageView.setImage(ImageLocation.getForDocument(document), "100_100",
                            thumb, 0, document);
                    imageView.setVisibility(android.view.View.VISIBLE);
                    imageView.invalidate();
                    if (onLoaded != null) {
                        onLoaded.run();
                    }
                });
        return true;
    }
}
