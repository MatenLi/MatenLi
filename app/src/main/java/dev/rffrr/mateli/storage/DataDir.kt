package dev.rffrr.mateli.storage

import android.content.Context
import android.os.Environment
import java.io.File

/**
 * Where MatenLi keeps its data. With all-files access the data lives in [FOLDER], which survives
 * uninstalling and updating. Without it, the data stays in the app's private storage.
 */
object DataDir {
    const val FOLDER = "/storage/emulated/0/MatenLi"

    private val FILES = listOf(
        "profiles.json",
        "selected_profile.txt",
        "route_rules.json",
        "subscriptions.json",
        "settings.json"
    )

    fun hasAllFilesAccess(): Boolean = Environment.isExternalStorageManager()

    fun dir(context: Context): File {
        if (!hasAllFilesAccess()) return context.filesDir
        return File(FOLDER).apply { mkdirs() }
    }

    fun file(context: Context, name: String): File = File(dir(context), name)

    /** Copies data files that exist in app storage but not yet in the folder. Safe to call often. */
    fun migrate(context: Context) {
        if (!hasAllFilesAccess()) return
        val target = dir(context)
        FILES.forEach { name ->
            val from = File(context.filesDir, name)
            val to = File(target, name)
            if (from.exists() && !to.exists()) {
                runCatching { from.copyTo(to) }
            }
        }
    }
}
