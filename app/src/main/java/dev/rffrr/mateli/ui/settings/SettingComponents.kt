@file:OptIn(ExperimentalMaterial3Api::class)

package dev.rffrr.mateli.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.rffrr.mateli.R
import dev.rffrr.mateli.settings.Prefs

/** One choice in a menu setting. The value is what gets stored. */
data class Option(val value: String, val label: String, val swatch: Color? = null)

/** Section title followed by its rows. */
@Composable
fun SettingSection(titleRes: Int, content: @Composable () -> Unit) {
    Column {
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, top = 20.dp, end = 16.dp, bottom = 4.dp)
        )
        content()
    }
}

/** On/off setting. Tapping the row or the switch changes the value. */
@Composable
fun SwitchSetting(
    prefs: Prefs,
    key: String,
    default: Boolean,
    titleRes: Int,
    summaryRes: Int? = null
) {
    var checked by remember(key) { mutableStateOf(prefs.bool(key, default)) }
    val onChange: (Boolean) -> Unit = { value ->
        checked = value
        prefs.putBool(key, value)
    }
    SettingRow(
        title = stringResource(titleRes),
        summary = summaryRes?.let { stringResource(it) },
        onClick = { onChange(!checked) },
        trailing = { Switch(checked = checked, onCheckedChange = onChange) }
    )
}

/** Setting with a list of choices in a dialog. [onChanged] runs after a pick. */
@Composable
fun MenuSetting(
    prefs: Prefs,
    key: String,
    default: String,
    titleRes: Int,
    options: List<Option>,
    onChanged: (() -> Unit)? = null
) {
    var value by remember(key) { mutableStateOf(prefs.string(key, default)) }
    var showDialog by remember { mutableStateOf(false) }
    val title = stringResource(titleRes)
    val label = options.firstOrNull { it.value == value }?.label ?: value

    SettingRow(
        title = title,
        summary = label,
        onClick = { showDialog = true },
        swatch = options.firstOrNull { it.value == value }?.swatch
    )

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(title) },
            text = {
                Column {
                    options.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    value = option.value
                                    prefs.putString(key, option.value)
                                    showDialog = false
                                    onChanged?.invoke()
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = option.value == value, onClick = null)
                            Spacer(Modifier.width(8.dp))
                            option.swatch?.let { color ->
                                Swatch(color)
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(option.label)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

/** Free text setting in a dialog. [numeric] uses a number keyboard, [multiline] allows several lines. */
@Composable
fun TextSetting(
    prefs: Prefs,
    key: String,
    default: String,
    titleRes: Int,
    numeric: Boolean = false,
    multiline: Boolean = false,
    onChanged: () -> Unit = {}
) {
    var value by remember(key) { mutableStateOf(prefs.string(key, default)) }
    var showDialog by remember { mutableStateOf(false) }

    SettingRow(
        title = stringResource(titleRes),
        summary = value.ifBlank { null },
        onClick = { showDialog = true }
    )

    if (showDialog) {
        TextDialog(
            title = stringResource(titleRes),
            initial = value,
            numeric = numeric,
            multiline = multiline,
            onSave = { saved ->
                value = saved
                prefs.putString(key, saved)
                showDialog = false
                onChanged()
            },
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
private fun TextDialog(
    title: String,
    initial: String,
    numeric: Boolean,
    multiline: Boolean,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = !multiline,
                minLines = if (multiline) 6 else 1,
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text
                )
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(text) }) {
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

@Composable
private fun SettingRow(
    title: String,
    summary: String?,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
    swatch: Color? = null
) {
    val end: @Composable () -> Unit = {
        if (swatch != null) Swatch(swatch) else trailing?.invoke()
    }
    val endSlot = if (swatch == null && trailing == null) null else end
    if (summary == null) {
        ListItem(
            headlineContent = { Text(title) },
            modifier = Modifier.clickable(onClick = onClick),
            trailingContent = endSlot
        )
    } else {
        ListItem(
            headlineContent = { Text(title) },
            supportingContent = { Text(summary) },
            modifier = Modifier.clickable(onClick = onClick),
            trailingContent = endSlot
        )
    }
}

/** Round colour sample for colour options. */
@Composable
private fun Swatch(color: Color) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .background(color, CircleShape)
    )
}
