package dev.rffrr.mateli.ui.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import dev.rffrr.mateli.R
import dev.rffrr.mateli.storage.DataDir

/** Shows the data folder, and opens the all-files access page while access is not granted. */
@Composable
fun StorageSection() {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(DataDir.hasAllFilesAccess()) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        granted = DataDir.hasAllFilesAccess()
        if (granted) DataDir.migrate(context)
    }

    SettingSection(R.string.storage_section) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.storage_path)) },
            supportingContent = {
                Text(if (granted) DataDir.FOLDER else stringResource(R.string.storage_missing))
            },
            modifier = Modifier.clickable {
                if (!granted) {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                    launcher.launch(intent)
                }
            }
        )
    }
}
