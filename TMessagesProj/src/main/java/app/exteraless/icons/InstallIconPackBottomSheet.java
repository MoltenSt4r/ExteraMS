package app.exteraless.icons;

import static org.telegram.messenger.LocaleController.getString;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Установка иконпака, присланного файлом: что внутри — видно до установки.
 *
 * Перенос {@code icons/ui/components/InstallIconPackBottomSheet}. Раньше на
 * месте этого листа стоял диалог с одной строкой «имя, N иконок» — по нему
 * нельзя понять, что за пак ставишь, а иконки меняют весь интерфейс.
 *
 * Превью читаются прямо из архива, ничего не распаковывая на диск: пак может
 * оказаться и не паком вовсе, а до нажатия «Установить» он должен оставаться
 * просто файлом.
 */
public class InstallIconPackBottomSheet extends BottomSheet {

    /** Сколько иконок показываем: две строки по пять — достаточно, чтобы узнать набор. */
    private static final int PREVIEW_COUNT = 10;
    private static final int PREVIEW_PER_ROW = 5;

    private final BaseFragment fragment;
    private final File archive;
    private final IconPack pack;
    private final LinearLayout previewsContainer;

    public InstallIconPackBottomSheet(BaseFragment fragment, File archive, IconPack pack) {
        super(fragment.getParentActivity(), false);
        this.fragment = fragment;
        this.archive = archive;
        this.pack = pack;

        final Context context = fragment.getParentActivity();
        setApplyTopPadding(false);
        setApplyBottomPadding(false);

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);

        TextView title = new TextView(context);
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        title.setTypeface(AndroidUtilities.bold());
        title.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        title.setGravity(Gravity.CENTER);
        title.setText(pack.getName());
        root.addView(title, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT,
                22, 18, 22, 0));

        TextView subtitle = new TextView(context);
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        subtitle.setTextColor(Theme.getColor(Theme.key_dialogTextGray2));
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setText(buildSubtitle());
        root.addView(subtitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT,
                22, 4, 22, 0));

        previewsContainer = new LinearLayout(context);
        previewsContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(previewsContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT,
                LayoutHelper.WRAP_CONTENT, 0, 14, 0, 6));

        TextCell install = new TextCell(context);
        install.setTextAndIcon(getString(R.string.IconPackInstall), R.drawable.msg_add, false);
        install.setOnClickListener(v -> install());
        root.addView(install, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 50));

        setCustomView(root);
        loadPreviews();
    }

    private CharSequence buildSubtitle() {
        List<String> parts = new ArrayList<>();
        if (!TextUtils.isEmpty(pack.getAuthor())) {
            parts.add(pack.getAuthor());
        }
        if (!TextUtils.isEmpty(pack.getVersion())) {
            parts.add(pack.getVersion());
        }
        parts.add(LocaleController.formatString(R.string.IconPackIconsCount, pack.getIconCount()));
        return TextUtils.join(" • ", parts);
    }

    // ---------- превью ----------

    private void loadPreviews() {
        Utilities.globalQueue.postRunnable(() -> {
            final List<Bitmap> previews = readPreviews();
            AndroidUtilities.runOnUIThread(() -> showPreviews(previews));
        });
    }

    /**
     * Первые несколько картинок пака прямо из zip.
     *
     * Имена берём из метаданных, а не перебором записей: в архиве лежит ещё и
     * pack.json, а порядок записей ничем не гарантирован.
     */
    private List<Bitmap> readPreviews() {
        List<Bitmap> result = new ArrayList<>();
        if (archive == null || !archive.exists()) {
            return result;
        }
        try (ZipFile zip = new ZipFile(archive)) {
            for (Map.Entry<String, String> entry : pack.getIcons().entrySet()) {
                if (result.size() >= PREVIEW_COUNT) {
                    break;
                }
                String path = entry.getValue();
                if (TextUtils.isEmpty(path)) {
                    continue;
                }
                ZipEntry zipEntry = findEntry(zip, path);
                if (zipEntry == null || zipEntry.isDirectory()) {
                    continue;
                }
                try (InputStream in = zip.getInputStream(zipEntry)) {
                    Bitmap bitmap;
                    if (IconPackManager.isSvg(path)) {
                        bitmap = IconPackManager.renderSvg(in, AndroidUtilities.dp(40), AndroidUtilities.dp(40));
                    } else {
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        // Иконки в паках бывают крупные; для превью 48dp хватает.
                        options.inSampleSize = 2;
                        bitmap = BitmapFactory.decodeStream(in, null, options);
                    }
                    if (bitmap != null) {
                        result.add(bitmap);
                    }
                } catch (Exception ignored) {
                    // Битая картинка не повод не показать остальные.
                }
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return result;
    }

    /**
     * Запись архива по пути из метаданных.
     *
     * Пути в metadata.json относительны корня пака, а в архиве этот корень
     * может быть завёрнут в папку — тогда прямого совпадения имени нет, и
     * приходится искать по хвосту.
     */
    private static ZipEntry findEntry(ZipFile zip, String path) {
        ZipEntry direct = zip.getEntry(path);
        if (direct != null) {
            return direct;
        }
        String suffix = "/" + path;
        java.util.Enumeration<? extends ZipEntry> entries = zip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (entry.getName().endsWith(suffix)) {
                return entry;
            }
        }
        return null;
    }

    private void showPreviews(List<Bitmap> previews) {
        if (previewsContainer == null || previews.isEmpty()) {
            return;
        }
        final Context context = previewsContainer.getContext();
        LinearLayout row = null;
        for (int i = 0; i < previews.size(); i++) {
            if (i % PREVIEW_PER_ROW == 0) {
                row = new LinearLayout(context);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER);
                previewsContainer.addView(row, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT,
                        LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8));
            }
            ImageView image = new ImageView(context);
            image.setScaleType(ImageView.ScaleType.FIT_CENTER);
            image.setImageBitmap(previews.get(i));
            row.addView(image, LayoutHelper.createLinear(40, 40, 6, 0, 6, 0));
        }
    }

    // ---------- установка ----------

    private void install() {
        dismiss();
        IconPackManager.getInstance().installPack(archive, error -> {
            if (fragment == null || fragment.getParentActivity() == null) {
                return;
            }
            if (error != null) {
                BulletinFactory.of(fragment).createErrorBulletin(error.getLocalizedMessage()).show();
                return;
            }
            BulletinFactory.of(fragment)
                    .createSimpleBulletin(R.raw.done, getString(R.string.IconPackInstalledToast))
                    .show();
        });
    }

    /** Кнопка «Установить» в листе; отдельный метод, чтобы вызов был читаем в трассе. */
    public static void show(BaseFragment fragment, File archive, IconPack pack) {
        if (fragment == null || fragment.getParentActivity() == null || pack == null) {
            return;
        }
        new InstallIconPackBottomSheet(fragment, archive, pack).show();
    }
}
