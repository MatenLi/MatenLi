package dev.rffrr.mateli.ui.about

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import io.nekohasekai.libbox.Libbox
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.rffrr.mateli.R
import dev.rffrr.mateli.ui.theme.MatenLiShapes

private const val SOURCE_URL = "https://github.com/MatenLi/MatenLi"
private const val DESIGN_URL = "$SOURCE_URL/blob/main/docs/design.md"
private const val LICENSE_URL = "https://www.apache.org/licenses/LICENSE-2.0"
private const val TELEGRAM_URL = "https://t.me/r_FFR_r"

/** About page laid out like NekoBox: app card, version card, project links, licence. */
@Composable
fun AboutScreen(modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        InfoCard {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.about_app_description),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        InfoCard {
            InfoRow(
                icon = Icons.Filled.History,
                title = stringResource(R.string.about_version_title),
                value = stringResource(R.string.about_version_value)
            )
            InfoRow(
                icon = Icons.Filled.Layers,
                title = stringResource(R.string.about_engine_singbox),
                value = remember { singBoxVersion() }
            )
            InfoRow(
                icon = Icons.Filled.Layers,
                title = stringResource(R.string.about_modules_title),
                value = stringResource(R.string.about_modules_value)
            )
        }

        InfoCard {
            SectionTitle(stringResource(R.string.about_project_title))
            LinkRow(Icons.Filled.Code, stringResource(R.string.about_source_code)) {
                uriHandler.openUri(SOURCE_URL)
            }
            LinkRow(Icons.Filled.Description, stringResource(R.string.about_design_notes)) {
                uriHandler.openUri(DESIGN_URL)
            }
            LinkRow(Icons.Filled.Send, stringResource(R.string.about_telegram)) {
                uriHandler.openUri(TELEGRAM_URL)
            }
        }

        InfoCard {
            SectionTitle(stringResource(R.string.about_license_title))
            Text(
                text = stringResource(R.string.about_copyright),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = stringResource(R.string.about_license_text),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LinkRow(Icons.Filled.Security, stringResource(R.string.about_full_license)) {
                uriHandler.openUri(LICENSE_URL)
            }
        }
    }
}

/** Version reported by the bundled sing-box core. */
private fun singBoxVersion(): String = runCatching { Libbox.version() }.getOrDefault("unknown")

@Composable
private fun InfoCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MatenLiShapes.card,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun InfoRow(icon: ImageVector, title: String, value: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LinkRow(icon: ImageVector, title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Filled.OpenInNew,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}
