@file:OptIn(ExperimentalMaterial3Api::class)

package dev.rffrr.mateli.ui.groups

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.rffrr.mateli.R
import dev.rffrr.mateli.profiles.ProfileStore
import dev.rffrr.mateli.profiles.RefreshResult
import dev.rffrr.mateli.profiles.Subscription
import dev.rffrr.mateli.profiles.blankSubscription
import dev.rffrr.mateli.profiles.SubscriptionStore
import dev.rffrr.mateli.profiles.SubscriptionUpdater
import dev.rffrr.mateli.settings.Prefs
import dev.rffrr.mateli.ui.common.EmptyState
import dev.rffrr.mateli.ui.theme.MatenLiShapes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Subscriptions. Each one downloads a list of servers and replaces its own servers on refresh.
 * [onChanged] tells the shell the profile list changed. [onRefreshed] receives the update diff.
 */
@Composable
fun GroupsScreen(
    onChanged: () -> Unit,
    onRefreshed: (RefreshResult) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val profileStore = remember { ProfileStore(context) }
    val subStore = remember { SubscriptionStore(context) }
    var subs by remember { mutableStateOf(subStore.list()) }
    var adding by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf(blankSubscription()) }
    var editing by remember { mutableStateOf<Subscription?>(null) }
    var busy by remember { mutableStateOf<String?>(null) }
    var errors by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    fun refresh(sub: Subscription) {
        busy = sub.id
        errors = errors - sub.id
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { SubscriptionUpdater.refresh(profileStore, subStore, sub, Prefs.get(context)) }
            }
            busy = null
            result.onSuccess {
                subs = subStore.list()
                onChanged()
                onRefreshed(it)
            }.onFailure {
                errors = errors + (sub.id to (it.message ?: "unknown error"))
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (subs.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.Folder,
                title = stringResource(R.string.sub_empty_title),
                hint = stringResource(R.string.sub_empty_hint)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(subs, key = { it.id }) { sub ->
                    SubscriptionCard(
                        sub = sub,
                        busy = busy == sub.id,
                        error = errors[sub.id],
                        onEdit = { editing = sub },
                        onRefresh = { refresh(sub) },
                        onRemove = {
                            subStore.remove(sub.id, profileStore)
                            subs = subStore.list()
                            onChanged()
                        }
                    )
                }
            }
        }
        FloatingActionButton(
            onClick = { draft = blankSubscription(); adding = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.sub_add))
        }
    }

    if (adding) {
        GroupSettingsDialog(
            sub = draft,
            titleRes = R.string.sub_add_title,
            onDismiss = { adding = false },
            onSave = { created ->
                subStore.create(created)
                subs = subStore.list()
                onChanged()
                adding = false
            }
        )
    }

    editing?.let { target ->
        GroupSettingsDialog(
            sub = target,
            titleRes = R.string.group_settings_title,
            onDismiss = { editing = null },
            onSave = { updated ->
                subStore.update(updated)
                subs = subStore.list()
                onChanged()
                editing = null
            }
        )
    }
}

@Composable
private fun SubscriptionCard(
    sub: Subscription,
    busy: Boolean,
    error: String?,
    onEdit: () -> Unit,
    onRefresh: () -> Unit,
    onRemove: () -> Unit
) {
    val timeText = if (sub.updatedAt == 0L) {
        stringResource(R.string.sub_never)
    } else {
        stringResource(R.string.sub_updated, formatTime(sub.updatedAt))
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MatenLiShapes.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(start = 20.dp, end = 8.dp, top = 16.dp, bottom = 8.dp)) {
            Text(
                text = sub.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = sub.url,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${stringResource(R.string.sub_servers, sub.profileIds.size)}  |  $timeText",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
            if (error != null) {
                Text(
                    text = stringResource(R.string.sub_failed, error),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (busy) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text(
                        text = stringResource(R.string.sub_refreshing),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                } else {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.action_refresh))
                    }
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.action_edit))
                }
                IconButton(onClick = onRemove) {
                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.sub_remove))
                }
            }
        }
    }
}

@Composable
private fun SubscriptionDialog(onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    val validUrl = url.trim().startsWith("https://") || url.trim().startsWith("http://")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.sub_add_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.sub_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text(stringResource(R.string.sub_url)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = validUrl,
                onClick = {
                    val cleanUrl = url.trim()
                    val fallbackName = runCatching { URL(cleanUrl).host }.getOrNull() ?: cleanUrl
                    onSave(name.trim().ifBlank { fallbackName }, cleanUrl)
                }
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

/** Group settings: name, URL, auto update, filter, deduplication and the user agent sent on refresh. */
@Composable
private fun GroupSettingsDialog(
    sub: Subscription,
    titleRes: Int,
    onDismiss: () -> Unit,
    onSave: (Subscription) -> Unit
) {
    var name by remember(sub.id) { mutableStateOf(sub.name) }
    var url by remember(sub.id) { mutableStateOf(sub.url) }
    var autoUpdate by remember(sub.id) { mutableStateOf(sub.autoUpdate) }
    var minutes by remember(sub.id) { mutableStateOf(sub.updateMinutes.toString()) }
    var filter by remember(sub.id) { mutableStateOf(sub.filter) }
    var dedupe by remember(sub.id) { mutableStateOf(sub.dedupe) }
    var agent by remember(sub.id) { mutableStateOf(sub.userAgent) }
    var error by remember(sub.id) { mutableStateOf<String?>(null) }
    // Resolve strings in composable scope; stringResource cannot be called from the onClick lambda.
    val urlInvalid = stringResource(R.string.sub_url_invalid)
    val intervalInvalid = stringResource(R.string.group_interval_invalid)
    val filterInvalid = stringResource(R.string.sub_filter_invalid)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(titleRes)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.sub_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text(stringResource(R.string.sub_url)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.group_auto_update))
                    Switch(checked = autoUpdate, onCheckedChange = { autoUpdate = it })
                }
                OutlinedTextField(
                    value = minutes,
                    onValueChange = { minutes = it },
                    label = { Text(stringResource(R.string.group_auto_update_minutes)) },
                    singleLine = true,
                    enabled = autoUpdate,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = filter,
                    onValueChange = { filter = it },
                    label = { Text(stringResource(R.string.group_filter)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.group_dedupe))
                    Switch(checked = dedupe, onCheckedChange = { dedupe = it })
                }
                OutlinedTextField(
                    value = agent,
                    onValueChange = { agent = it },
                    label = { Text(stringResource(R.string.group_user_agent)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let {
                    Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val interval = minutes.trim().toIntOrNull()?.takeIf { it >= 15 }
                val cleanUrl = url.trim()
                when {
                    !(cleanUrl.startsWith("https://") || cleanUrl.startsWith("http://")) ->
                        error = urlInvalid
                    interval == null ->
                        error = intervalInvalid
                    filter.isNotBlank() && runCatching { Regex(filter) }.isFailure ->
                        error = filterInvalid
                    else -> onSave(
                        sub.copy(
                            name = name.trim().ifBlank { sub.name }.ifBlank {
                                runCatching { URL(cleanUrl).host }.getOrNull() ?: cleanUrl
                            },
                            url = cleanUrl,
                            autoUpdate = autoUpdate,
                            updateMinutes = interval,
                            filter = filter.trim(),
                            dedupe = dedupe,
                            userAgent = agent.trim().ifBlank { "MatenLi/0.1" }
                        )
                    )
                }
            }) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

private fun formatTime(millis: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(millis))
