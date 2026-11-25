@file:OptIn(ExperimentalMaterial3Api::class)

package dev.rffrr.mateli.ui.routes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.rffrr.mateli.R
import dev.rffrr.mateli.config.RouteRule
import dev.rffrr.mateli.config.RouteStore
import dev.rffrr.mateli.profiles.ProfileStore
import dev.rffrr.mateli.ui.common.EmptyState
import dev.rffrr.mateli.ui.theme.MatenLiShapes

/** Routing rules: match domains, IP ranges or ports and send them to proxy, direct or block. */
@Composable
fun RoutesScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val store = remember { RouteStore(context) }
    val hasProfile = remember { ProfileStore(context).selectedId() != null }
    var rules by remember { mutableStateOf(store.list()) }
    var adding by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        if (rules.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.CallSplit,
                title = stringResource(R.string.route_empty_title),
                hint = stringResource(R.string.route_empty_hint)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Column(modifier = Modifier.padding(bottom = 4.dp)) {
                        Text(
                            text = stringResource(R.string.route_bypass_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!hasProfile) {
                            Text(
                                text = stringResource(R.string.route_no_proxy),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
                items(rules, key = { it.id }) { rule ->
                    RouteCard(
                        rule = rule,
                        onDelete = {
                            store.remove(rule.id)
                            rules = store.list()
                        }
                    )
                }
            }
        }
        FloatingActionButton(
            onClick = { adding = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.route_add))
        }
    }

    if (adding) {
        RouteDialog(
            onDismiss = { adding = false },
            onSave = { kind, value, outbound ->
                store.add(kind, value, outbound)
                rules = store.list()
                adding = false
            }
        )
    }
}

@Composable
private fun RouteCard(rule: RouteRule, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MatenLiShapes.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = rule.value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${stringResource(kindLabel(rule.kind))}  ->  ${stringResource(outboundLabel(rule.outbound))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete))
            }
        }
    }
}

@Composable
private fun RouteDialog(onDismiss: () -> Unit, onSave: (String, String, String) -> Unit) {
    var kind by remember { mutableStateOf("domain_suffix") }
    var outbound by remember { mutableStateOf("proxy") }
    var value by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.route_add_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.route_match), style = MaterialTheme.typography.titleSmall)
                listOf("domain_suffix", "domain_keyword", "ip_cidr", "port").forEach { option ->
                    ChoiceRow(
                        label = stringResource(kindLabel(option)),
                        selected = kind == option,
                        onClick = { kind = option }
                    )
                }
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text(stringResource(R.string.route_values)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(stringResource(R.string.route_action), style = MaterialTheme.typography.titleSmall)
                listOf("proxy", "direct", "block").forEach { option ->
                    ChoiceRow(
                        label = stringResource(outboundLabel(option)),
                        selected = outbound == option,
                        onClick = { outbound = option }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = value.isNotBlank(),
                onClick = { onSave(kind, value.trim(), outbound) }
            ) {
                Text(stringResource(R.string.action_add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
private fun ChoiceRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(text = label, modifier = Modifier.padding(start = 8.dp))
    }
}

private fun kindLabel(kind: String): Int = when (kind) {
    "domain_keyword" -> R.string.route_kind_keyword
    "ip_cidr" -> R.string.route_kind_ip
    "port" -> R.string.route_kind_port
    else -> R.string.route_kind_suffix
}

private fun outboundLabel(outbound: String): Int = when (outbound) {
    "direct" -> R.string.route_outbound_direct
    "block" -> R.string.route_outbound_block
    else -> R.string.route_outbound_proxy
}
