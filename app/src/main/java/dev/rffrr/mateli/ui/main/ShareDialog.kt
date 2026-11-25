package dev.rffrr.mateli.ui.main

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import dev.rffrr.mateli.R
import dev.rffrr.mateli.profiles.Profile
import dev.rffrr.mateli.profiles.ShareLinkWriter

/**
 * Shares one profile: as a QR code, to the clipboard, or as a full sing-box config to the clipboard
 * or a file. Links use the Standard format. The SagerNet format is not ready yet.
 */
@Composable
fun ShareDialog(
    profile: Profile,
    configJson: String,
    onSaveFile: () -> Unit,
    onMessage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val standard = remember(profile) { ShareLinkWriter.standard(profile) }
    var qrText by remember { mutableStateOf<String?>(null) }
    val copied = stringResource(R.string.share_copied)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.share_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (standard == null) {
                    Text(
                        text = stringResource(R.string.share_no_link),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                ShareGroup(stringResource(R.string.share_qr)) {
                    ShareOption(stringResource(R.string.share_standard), enabled = standard != null) {
                        qrText = standard
                    }
                    ShareOption(stringResource(R.string.share_sagernet), enabled = false) {}
                }
                ShareGroup(stringResource(R.string.share_clipboard)) {
                    ShareOption(stringResource(R.string.share_standard), enabled = standard != null) {
                        copyToClipboard(context, standard.orEmpty())
                        onMessage(copied)
                    }
                    ShareOption(stringResource(R.string.share_sagernet), enabled = false) {}
                }
                ShareGroup(stringResource(R.string.share_json)) {
                    ShareOption(stringResource(R.string.share_clip), enabled = true) {
                        copyToClipboard(context, configJson)
                        onMessage(copied)
                    }
                    ShareOption(stringResource(R.string.share_file), enabled = true) {
                        onSaveFile()
                    }
                }
                Text(
                    text = stringResource(R.string.share_sagernet_soon),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_ok)) }
        }
    )

    qrText?.let { text ->
        QrDialog(text = text, onDismiss = { qrText = null })
    }
}

@Composable
private fun ShareGroup(title: String, options: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options()
        }
    }
}

@Composable
private fun ShareOption(label: String, enabled: Boolean, onClick: () -> Unit) {
    TextButton(onClick = onClick, enabled = enabled) {
        Text(label)
    }
}

@Composable
private fun QrDialog(text: String, onDismiss: () -> Unit) {
    val bitmap = remember(text) {
        runCatching { BarcodeEncoder().encodeBitmap(text, BarcodeFormat.QR_CODE, 720, 720) }.getOrNull()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.share_qr)) },
        text = {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = stringResource(R.string.share_qr),
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(stringResource(R.string.share_no_link))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_ok)) }
        }
    )
}

private fun copyToClipboard(context: Context, text: String) {
    context.getSystemService(ClipboardManager::class.java)
        ?.setPrimaryClip(ClipData.newPlainText("MatenLi", text))
}
