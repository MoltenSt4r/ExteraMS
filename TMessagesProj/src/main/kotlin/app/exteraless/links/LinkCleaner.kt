package app.exteraless.links

import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.SuggestionSpan
import android.text.style.URLSpan
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputConnectionWrapper
import android.widget.EditText
import app.exteraless.chats.ChatsConfig
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.FileLog
import org.telegram.messenger.Utilities
import org.telegram.ui.Components.URLSpanReplacement
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

object LinkCleaner {

    private const val FILE = "adguard_url_tracking.txt"
    private const val FILTER_URL = "https://filters.adtidy.org/extension/ublock/filters/17.txt"
    private val TIME_UPDATED = Regex("""^!\s*TimeUpdated:\s*(\S+)""", RegexOption.MULTILINE)

    @Volatile
    private var cleaner: UrlCleaner? = null

    @Volatile
    private var loaded = false

    @Volatile
    private var updated: String? = null

    @Volatile
    private var updatedRead = false

    @JvmStatic
    fun lastUpdated(): String? {
        if (!updatedRead) {
            updated = try {
                openSource().bufferedReader().use {
                    val buffer = CharArray(4096)
                    val read = it.read(buffer)
                    if (read > 0) parseTimeUpdated(String(buffer, 0, read)) else null
                }
            } catch (e: Throwable) {
                FileLog.e(e)
                null
            }
            updatedRead = true
        }
        return updated
    }

    @JvmStatic
    fun isUsingOverride(): Boolean = overrideFile().exists()

    private fun overrideFile() = File(ApplicationLoader.applicationContext.filesDir, FILE)

    private fun openSource(): InputStream =
        overrideFile().takeIf { it.exists() }?.inputStream()
            ?: ApplicationLoader.applicationContext.assets.open(FILE)

    private fun readSource(): String = openSource().bufferedReader().use { it.readText() }

    private fun parseTimeUpdated(text: String): String? =
        TIME_UPDATED.find(text)?.groupValues?.get(1)?.substringBefore('T')

    @Synchronized
    private fun getCleaner(): UrlCleaner? {
        if (loaded) return cleaner
        cleaner = try {
            val text = readSource()
            updated = parseTimeUpdated(text)
            updatedRead = true
            UrlCleaner.fromAdGuardFilter(text)
        } catch (e: Throwable) {
            FileLog.e(e)
            null
        }
        loaded = true
        return cleaner
    }

    @Synchronized
    private fun invalidate() {
        cleaner = null
        loaded = false
        updated = null
        updatedRead = false
    }

    @JvmStatic
    fun preloadIfEnabled() {
        if (ChatsConfig.stripTrackingOnOpen() || ChatsConfig.stripTrackingOnPaste()) {
            Utilities.globalQueue.postRunnable { getCleaner() }
        }
    }

    @JvmStatic
    @Throws(IOException::class)
    fun fetchLatest(): Boolean {
        val current = lastUpdated()
        val connection = (URL(FILTER_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
        }
        val downloaded = try {
            if (connection.responseCode !in 200..299) throw IOException("HTTP ${connection.responseCode}")
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
        val downloadedTime = parseTimeUpdated(downloaded) ?: throw IOException("not a filter list")
        if (downloadedTime == current) return false
        val target = overrideFile()
        val tmp = File(target.parentFile, "$FILE.tmp")
        tmp.writeText(downloaded)
        if (!tmp.renameTo(target)) {
            tmp.delete()
            throw IOException("rename failed")
        }
        invalidate()
        getCleaner()
        return true
    }

    @JvmStatic
    fun resetToBundled() {
        overrideFile().delete()
        invalidate()
        getCleaner()
    }

    @JvmStatic
    fun clean(uri: Uri?): Uri? {
        if (uri == null || !ChatsConfig.stripTrackingOnOpen()) return uri
        val original = uri.toString()
        val cleaned = cleanUrl(original)
        return if (cleaned === original) uri else Uri.parse(cleaned)
    }

    private fun cleanUrl(url: String): String {
        val scheme = url.substringBefore(':', "").lowercase()
        if (scheme != "http" && scheme != "https") return url
        return getCleaner()?.clean(url) ?: url
    }

    @JvmStatic
    fun cleanUrlString(url: CharSequence): CharSequence {
        if (!ChatsConfig.stripTrackingOnPaste()) return url
        val s = url.toString()
        val cleaned = cleanUrl(s)
        return if (cleaned === s) url else cleaned
    }

    @JvmStatic
    fun cleanString(text: String): String = cleanText(text)?.toString() ?: text

    @JvmStatic
    fun cleanText(text: CharSequence?): CharSequence? {
        if (text.isNullOrEmpty() || !ChatsConfig.stripTrackingOnPaste()) return text
        val matcher = AndroidUtilities.WEB_URL?.matcher(text) ?: return text
        var out: SpannableStringBuilder? = null
        var shift = 0
        while (matcher.find()) {
            val start = matcher.start()
            val end = matcher.end()
            val original = text.subSequence(start, end).toString()
            val cleaned = cleanUrl(original)
            if (cleaned === original) continue
            if (out == null) out = SpannableStringBuilder(text)
            out.replace(start + shift, end + shift, cleaned)
            shift += cleaned.length - (end - start)
        }
        if (out != null && out.length != text.length) {
            BaseInputConnection.removeComposingSpans(out)
            for (span in out.getSpans(0, out.length, SuggestionSpan::class.java)) {
                out.removeSpan(span)
            }
        }
        return out ?: text
    }

    @JvmStatic
    fun handleContextMenuPaste(editText: EditText): Boolean {
        if (!ChatsConfig.stripTrackingOnPaste()) return false
        val context = editText.context ?: return false
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return false
        val clip = clipboard.primaryClip?.takeIf { it.itemCount >= 1 } ?: return false
        return try {
            val raw = clip.getItemAt(0).coerceToText(context) ?: return false
            val cleaned = cleanText(raw) ?: return false
            if (cleaned === raw) return false
            val text = editText.text ?: return false
            val start = maxOf(0, editText.selectionStart)
            val end = minOf(text.length, editText.selectionEnd)
            text.replace(start, end, cleaned)
            editText.setSelection(start + cleaned.length)
            true
        } catch (e: Throwable) {
            FileLog.e(e)
            false
        }
    }

    @JvmStatic
    fun wrapInputConnection(ic: InputConnection?): InputConnection? {
        if (ic == null) return null
        return object : InputConnectionWrapper(ic, true) {
            override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
                val cleaned = if (text != null && ChatsConfig.stripTrackingOnPaste()
                    && text.toString().contains("https://")
                ) cleanText(text) else text
                return super.commitText(cleaned, newCursorPosition)
            }
        }
    }

    @JvmStatic
    fun cleanSpannedUrls(text: Spannable) {
        if (!ChatsConfig.stripTrackingOnPaste()) return
        for (span in text.getSpans(0, text.length, URLSpan::class.java)) {
            val url = span.url ?: continue
            val cleaned = cleanUrl(url)
            if (cleaned === url) continue
            val start = text.getSpanStart(span)
            val end = text.getSpanEnd(span)
            val flags = text.getSpanFlags(span)
            text.removeSpan(span)
            val replacement = if (span is URLSpanReplacement) URLSpanReplacement(cleaned, span.textStyleRun)
            else URLSpan(cleaned)
            text.setSpan(replacement, start, end, flags)
        }
    }
}
