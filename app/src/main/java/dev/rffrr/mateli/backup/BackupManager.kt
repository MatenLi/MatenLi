package dev.rffrr.mateli.backup

import android.content.Context
import dev.rffrr.mateli.settings.Prefs
import dev.rffrr.mateli.storage.DataDir
import org.json.JSONObject
import org.json.JSONTokener
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Copies all user data into one ZIP file, and back. The data lives in the data folder already,
 * so a backup is only needed to move to another device.
 *
 * Archive layout: manifest.json (format, app id, app version, time) plus the data files by name.
 * Only those names are read or written, so a crafted archive cannot touch other files.
 */
object BackupManager {
    const val FORMAT = 1
    const val APP_ID = "MatenLi"

    private const val MANIFEST = "manifest.json"
    private val DATA_FILES = listOf(
        "profiles.json",
        "selected_profile.txt",
        "route_rules.json",
        "subscriptions.json",
        "settings.json"
    )

    /** What a restore did. [restored] and [skipped] hold file names. */
    data class Report(val restored: List<String>, val skipped: List<String>)

    /** Writes a backup. Returns how many entries were written. */
    fun export(context: Context, out: OutputStream, appVersion: String): Int {
        var entries = 0
        ZipOutputStream(out).use { zip ->
            val manifest = JSONObject()
                .put("format", FORMAT)
                .put("app", APP_ID)
                .put("appVersion", appVersion)
                .put("createdAt", System.currentTimeMillis())
            zip.putBytes(MANIFEST, manifest.toString(2).toByteArray(Charsets.UTF_8))
            entries++
            for (name in DATA_FILES) {
                val file = DataDir.file(context, name)
                if (file.exists()) {
                    zip.putBytes(name, file.readBytes())
                    entries++
                }
            }
        }
        return entries
    }

    /**
     * Reads a backup and replaces the current data with it. Throws if the stream is not a MatenLi
     * backup or was written by a newer format. Files that are missing or not valid JSON are skipped
     * and reported; the rest is still restored.
     */
    fun restore(context: Context, input: InputStream): Report {
        val entries = HashMap<String, ByteArray>()
        ZipInputStream(input).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    entries[entry.name] = zip.readBytes()
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }

        val manifestBytes = entries[MANIFEST] ?: throw IllegalArgumentException("manifest.json is missing")
        val manifest = JSONObject(String(manifestBytes, Charsets.UTF_8))
        require(manifest.optString("app") == APP_ID) { "this is not a MatenLi backup" }
        val format = manifest.optInt("format", 0)
        require(format in 1..FORMAT) { "backup format $format is newer than this app supports" }

        val restored = mutableListOf<String>()
        val skipped = mutableListOf<String>()
        for (name in DATA_FILES) {
            val bytes = entries[name]
            if (bytes == null || !parsesAsJson(name, bytes)) {
                skipped += name
                continue
            }
            DataDir.file(context, name).writeBytes(bytes)
            restored += name
        }
        Prefs.get(context).reload()
        return Report(restored, skipped)
    }

    private fun parsesAsJson(name: String, bytes: ByteArray): Boolean {
        if (name == "selected_profile.txt") return true
        return runCatching { JSONTokener(String(bytes, Charsets.UTF_8)).nextValue() }.isSuccess
    }

    private fun ZipOutputStream.putBytes(name: String, bytes: ByteArray) {
        putNextEntry(ZipEntry(name))
        write(bytes)
        closeEntry()
    }
}
