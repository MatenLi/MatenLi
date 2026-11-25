package dev.rffrr.mateli.ui.backup

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import dev.rffrr.mateli.R
import dev.rffrr.mateli.backup.BackupManager
import dev.rffrr.mateli.ui.settings.SettingSection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun versionName(context: Context): String =
    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"

private fun showToast(context: Context, text: String) {
    Toast.makeText(context, text, Toast.LENGTH_LONG).show()
}

/** Create and load a backup, shown at the end of Settings. */
@Composable
fun BackupSection() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingRestore by remember { mutableStateOf<Uri?>(null) }

    val createLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        val out = context.contentResolver.openOutputStream(uri)
                            ?: error("the file could not be opened")
                        out.use { BackupManager.export(context, it, versionName(context)) }
                    }
                }
                val text = result.fold(
                    { count -> context.getString(R.string.backup_created, count) },
                    { error -> context.getString(R.string.backup_failed, error.message ?: "unknown error") }
                )
                showToast(context, text)
            }
        }
    }

    val openLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        pendingRestore = uri
    }

    SettingSection(R.string.backup_section) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.backup_create)) },
            supportingContent = { Text(stringResource(R.string.backup_create_summary)) },
            modifier = Modifier.clickable { createLauncher.launch("matenli-backup.zip") }
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.backup_load)) },
            supportingContent = { Text(stringResource(R.string.backup_load_summary)) },
            modifier = Modifier.clickable { openLauncher.launch(arrayOf("*/*")) }
        )
    }

    val chosen = pendingRestore
    if (chosen != null) {
        AlertDialog(
            onDismissRequest = { pendingRestore = null },
            title = { Text(stringResource(R.string.backup_confirm_title)) },
            text = { Text(stringResource(R.string.backup_confirm_text)) },
            confirmButton = {
                TextButton(onClick = {
                    pendingRestore = null
                    scope.launch {
                        val result = withContext(Dispatchers.IO) {
                            runCatching {
                                val input = context.contentResolver.openInputStream(chosen)
                                    ?: error("the file could not be opened")
                                input.use { BackupManager.restore(context, it) }
                            }
                        }
                        val text = result.fold(
                            { report ->
                                val names = report.restored.joinToString(", ")
                                if (report.skipped.isEmpty()) {
                                    context.getString(R.string.backup_restored, names)
                                } else {
                                    context.getString(R.string.backup_restored_partial, names, report.skipped.joinToString(", "))
                                }
                            },
                            { error -> context.getString(R.string.backup_invalid, error.message ?: "unknown error") }
                        )
                        showToast(context, text)
                    }
                }) { Text(stringResource(R.string.backup_confirm_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestore = null }) { Text(stringResource(R.string.backup_cancel)) }
            }
        )
    }
}
