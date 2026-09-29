package app.exteraless.plugins;

import android.content.Context;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Автоматическая конвертация .py скриптов в полноценные плагины ExteraMS.
 * Извлекает или генерирует метаданные (__id__, __name__, __author__ и т.д.)
 * и при необходимости оборачивает код в адаптер BasePlugin.
 */
public final class PluginAutoConverter {

    private static final Pattern ID_PATTERN = Pattern.compile("^__id__\\s*=\\s*['\"]([^'\"]+)['\"]", Pattern.MULTILINE);
    private static final Pattern NAME_PATTERN = Pattern.compile("^__name__\\s*=\\s*['\"]([^'\"]+)['\"]", Pattern.MULTILINE);
    private static final Pattern VERSION_PATTERN = Pattern.compile("^__version__\\s*=\\s*['\"]([^'\"]+)['\"]", Pattern.MULTILINE);
    private static final Pattern AUTHOR_PATTERN = Pattern.compile("^__author__\\s*=\\s*['\"]([^'\"]+)['\"]", Pattern.MULTILINE);
    private static final Pattern DESC_PATTERN = Pattern.compile("^__description__\\s*=\\s*['\"]([^'\"]+)['\"]", Pattern.MULTILINE);

    private static final Pattern COMMENT_ID = Pattern.compile("^#\\s*(?:id|plugin_id):\\s*([A-Za-z0-9_-]+)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private static final Pattern COMMENT_NAME = Pattern.compile("^#\\s*name:\\s*(.+)$", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private static final Pattern COMMENT_AUTHOR = Pattern.compile("^#\\s*author:\\s*(.+)$", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private static final Pattern COMMENT_DESC = Pattern.compile("^#\\s*description:\\s*(.+)$", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

    private PluginAutoConverter() {
    }

    public static File autoConvertIfNeeded(File source) {
        if (source == null || !source.exists() || !source.canRead()) {
            return source;
        }
        String fileName = source.getName();
        String lowerName = fileName.toLowerCase();
        if (!lowerName.endsWith(".py") && !lowerName.endsWith(".plugin")) {
            return source;
        }

        try {
            StringBuilder sb = new StringBuilder((int) Math.min(source.length(), 1024 * 1024));
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(source), StandardCharsets.UTF_8))) {
                char[] buf = new char[8192];
                int r;
                while ((r = reader.read(buf)) > 0) {
                    sb.append(buf, 0, r);
                }
            }
            String content = sb.toString();

            boolean hasId = ID_PATTERN.matcher(content).find();
            boolean hasName = NAME_PATTERN.matcher(content).find();
            boolean hasBasePlugin = content.contains("BasePlugin");

            if (hasId && hasName && hasBasePlugin) {
                return source;
            }

            String id = null;
            if (hasId) {
                Matcher m = ID_PATTERN.matcher(content);
                if (m.find()) {
                    id = m.group(1);
                }
            }
            if (id == null) {
                Matcher m = COMMENT_ID.matcher(content);
                if (m.find()) {
                    id = m.group(1).trim();
                }
            }
            if (id == null || id.isEmpty()) {
                String base = fileName;
                if (base.contains(".")) {
                    base = base.substring(0, base.lastIndexOf('.'));
                }
                base = base.replaceAll("[^A-Za-z0-9_-]", "_");
                if (base.isEmpty() || Character.isDigit(base.charAt(0)) || base.charAt(0) == '_') {
                    base = "p_" + base;
                }
                if (base.length() > 30) {
                    base = base.substring(0, 30);
                }
                id = base.toLowerCase();
            }

            String name = null;
            if (hasName) {
                Matcher m = NAME_PATTERN.matcher(content);
                if (m.find()) {
                    name = m.group(1);
                }
            }
            if (name == null) {
                Matcher m = COMMENT_NAME.matcher(content);
                if (m.find()) {
                    name = m.group(1).trim();
                }
            }
            if (name == null || name.isEmpty()) {
                name = id.replace('_', ' ');
                if (!name.isEmpty()) {
                    name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
                }
            }

            String version = "1.0.0";
            Matcher vm = VERSION_PATTERN.matcher(content);
            if (vm.find()) {
                version = vm.group(1);
            }

            String author = "ExteraMS";
            Matcher am = AUTHOR_PATTERN.matcher(content);
            if (am.find()) {
                author = am.group(1);
            } else {
                Matcher cam = COMMENT_AUTHOR.matcher(content);
                if (cam.find()) {
                    author = cam.group(1).trim();
                }
            }

            String desc = "Auto-converted from " + fileName;
            Matcher dm = DESC_PATTERN.matcher(content);
            if (dm.find()) {
                desc = dm.group(1);
            } else {
                Matcher cdm = COMMENT_DESC.matcher(content);
                if (cdm.find()) {
                    desc = cdm.group(1).trim();
                }
            }

            StringBuilder converted = new StringBuilder();
            converted.append("# -*- coding: utf-8 -*-\n");
            converted.append("# Auto-converted plugin by ExteraMS\n");
            if (!hasId) {
                converted.append("__id__ = \"").append(escape(id)).append("\"\n");
            }
            if (!hasName) {
                converted.append("__name__ = \"").append(escape(name)).append("\"\n");
            }
            if (!VERSION_PATTERN.matcher(content).find()) {
                converted.append("__version__ = \"").append(escape(version)).append("\"\n");
            }
            if (!AUTHOR_PATTERN.matcher(content).find()) {
                converted.append("__author__ = \"").append(escape(author)).append("\"\n");
            }
            if (!DESC_PATTERN.matcher(content).find()) {
                converted.append("__description__ = \"").append(escape(desc)).append("\"\n");
            }
            converted.append("\n");

            converted.append(content);

            if (!hasBasePlugin) {
                converted.append("\n\n# --- Auto-generated plugin wrapper by ExteraMS ---\n");
                converted.append("try:\n");
                converted.append("    from base_plugin import BasePlugin\n\n");
                converted.append("    class _AutoPlugin(BasePlugin):\n");
                converted.append("        pass\n");
                converted.append("except Exception:\n");
                converted.append("    pass\n");
            }

            Context ctx = ApplicationLoader.applicationContext;
            File cacheDir = ctx != null ? ctx.getCacheDir() : source.getParentFile();
            File convDir = new File(cacheDir, "converted_plugins");
            convDir.mkdirs();
            File dest = new File(convDir, id + ".py");
            try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(dest), StandardCharsets.UTF_8)) {
                writer.write(converted.toString());
            }
            return dest;
        } catch (Throwable t) {
            FileLog.e("PluginAutoConverter: failed to convert " + source, t);
            return source;
        }
    }

    private static String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
