package dev.rffrr.mateli.ui.main

import android.content.Context
import android.content.ClipboardManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.rffrr.mateli.R
import dev.rffrr.mateli.profiles.Profile
import dev.rffrr.mateli.profiles.ShareLinkParser

/**
 * Add profiles by pasting share links (one per line) or one sing-box outbound JSON.
 * All lines must parse. Otherwise nothing is saved and the bad lines are listed.
 */
@Composable
fun AddProfileDialog(onDismiss: () -> Unit, onSave: (List<Profile>) -> Unit) {
    val context = LocalContext.current
    var text by remember { mutableStateOf("") }
    var errors by remember { mutableStateOf<List<Pair<Int, String>>>(emptyList()) }
    var notice by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.profile_add_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.profile_add_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        errors = emptyList()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4
                )
                TextButton(onClick = {
                    val pasted = clipboardText(context)
                    if (pasted == null) {
                        notice = context.getString(R.string.profile_clipboard_empty)
                    } else {
                        text = pasted
                        errors = emptyList()
                        notice = null
                    }
                }) {
                    Text(stringResource(R.string.action_paste))
                }
                notice?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                errors.forEach { (line, message) ->
                    Text(
                        text = stringResource(R.string.profile_line_error, line, message),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val outcome = parseProfiles(text)
                when {
                    outcome.errors.isNotEmpty() -> errors = outcome.errors
                    outcome.profiles.isEmpty() -> notice = context.getString(R.string.profile_add_hint)
                    else -> onSave(outcome.profiles)
                }
            }) {
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

private class ParseOutcome(val profiles: List<Profile>, val errors: List<Pair<Int, String>>)

private fun parseProfiles(input: String): ParseOutcome {
    val trimmed = input.trim()
    if (trimmed.startsWith("{")) {
        return runCatching { ShareLinkParser.parse(trimmed) }.fold(
            onSuccess = { ParseOutcome(listOf(it), emptyList()) },
            onFailure = { ParseOutcome(emptyList(), listOf(1 to (it.message ?: "invalid JSON"))) }
        )
    }
    val profiles = mutableListOf<Profile>()
    val errors = mutableListOf<Pair<Int, String>>()
    trimmed.lines()
        .map { it.trim() }
        .withIndex()
        .filter { it.value.isNotEmpty() }
        .forEach { (index, line) ->
            runCatching { ShareLinkParser.parse(line) }
                .onSuccess { profiles.add(it) }
                .onFailure { errors.add((index + 1) to (it.message ?: "invalid link")) }
        }
    return ParseOutcome(profiles, errors)
}

private fun clipboardText(context: Context): String? {
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return null
    val clip = clipboard.primaryClip ?: return null
    if (clip.itemCount == 0) return null
    return clip.getItemAt(0).coerceToText(context)?.toString()?.takeIf { it.isNotBlank() }
}
