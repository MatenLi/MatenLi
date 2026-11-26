package dev.rffrr.mateli.ui.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.rffrr.mateli.R
import dev.rffrr.mateli.config.unsupportedReason
import dev.rffrr.mateli.core.UrlResult
import dev.rffrr.mateli.profiles.Profile
import dev.rffrr.mateli.ui.common.EmptyState
import dev.rffrr.mateli.ui.theme.MatenLiShapes

/**
 * Profile list for the selected group. Each card has its name, type and server, the last URL test
 * result as a badge, and the Edit, URL test, Share and Delete actions.
 * [urlPings] holds the last URL test per profile id: a time, or the reason it failed.
 * [query] filters by name or server.
 */
@Composable
fun ConfigurationScreen(
    profiles: List<Profile>,
    selectedId: String?,
    urlPings: Map<String, UrlResult>,
    urlTesting: Set<String>,
    query: String,
    onSelect: (String) -> Unit,
    onDelete: (String) -> Unit,
    onEdit: (Profile) -> Unit,
    onUrlTest: (Profile) -> Unit,
    onShare: (Profile) -> Unit,
    modifier: Modifier = Modifier
) {
    val visible = profiles.filter { p ->
        query.isBlank() ||
            p.name.contains(query, ignoreCase = true) ||
            p.outbound.optString("server").contains(query, ignoreCase = true)
    }
    if (visible.isEmpty()) {
        EmptyState(
            icon = Icons.Filled.Dns,
            title = stringResource(R.string.profile_empty_title),
            hint = stringResource(R.string.profile_empty_hint),
            modifier = modifier
        )
        return
    }

    var deleting by remember { mutableStateOf<Profile?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(visible, key = { it.id }) { profile ->
            ProfileCard(
                profile = profile,
                selected = profile.id == selectedId,
                urlResult = urlPings[profile.id],
                urlTesting = profile.id in urlTesting,
                onSelect = { onSelect(profile.id) },
                onEdit = { onEdit(profile) },
                onUrlTest = { onUrlTest(profile) },
                onShare = { onShare(profile) },
                onDelete = { deleting = profile }
            )
        }
    }

    deleting?.let { target ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.profile_delete_title)) },
            text = { Text(stringResource(R.string.profile_delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(target.id)
                    deleting = null
                }) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun ProfileCard(
    profile: Profile,
    selected: Boolean,
    urlResult: UrlResult?,
    urlTesting: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onUrlTest: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val server = profile.outbound.optString("server")
    val port = profile.outbound.optInt("server_port")
    val unsupported = unsupportedReason(profile) != null

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onSelect),
        shape = MatenLiShapes.card,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            }
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = selected, onClick = onSelect)
                Column(modifier = Modifier.weight(1f).padding(horizontal = 4.dp)) {
                    Text(
                        text = profile.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${profile.type.uppercase()} · $server:$port",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (unsupported) {
                        Text(
                            text = stringResource(R.string.profile_unsupported),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                ResultBadge(testing = urlTesting, result = urlResult)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.action_edit))
                }
                IconButton(onClick = onUrlTest) {
                    Icon(Icons.Filled.Timer, contentDescription = stringResource(R.string.action_url_test))
                }
                IconButton(onClick = onShare) {
                    Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.action_share))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete))
                }
            }
        }
    }
}

/** Spinner while testing, then a pill with the time in green or the reason in red. */
@Composable
private fun ResultBadge(testing: Boolean, result: UrlResult?) {
    if (testing) {
        CircularProgressIndicator(modifier = Modifier.size(24.dp))
        return
    }
    if (result == null) return
    val ms = result.ms
    val container = if (ms != null) {
        MaterialTheme.colorScheme.tertiaryContainer
    } else {
        MaterialTheme.colorScheme.errorContainer
    }
    val content = if (ms != null) {
        MaterialTheme.colorScheme.onTertiaryContainer
    } else {
        MaterialTheme.colorScheme.onErrorContainer
    }
    val label = if (ms != null) {
        stringResource(R.string.profile_url_ms, ms)
    } else {
        stringResource(R.string.profile_url_error, result.error.orEmpty())
    }
    Surface(shape = RoundedCornerShape(50), color = container, contentColor = content) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 160.dp).padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}
