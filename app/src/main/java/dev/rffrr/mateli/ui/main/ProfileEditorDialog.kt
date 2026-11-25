package dev.rffrr.mateli.ui.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.rffrr.mateli.R
import dev.rffrr.mateli.profiles.FieldKind
import dev.rffrr.mateli.profiles.EditField
import dev.rffrr.mateli.profiles.Profile
import dev.rffrr.mateli.profiles.applyEdits
import dev.rffrr.mateli.profiles.fieldsFor
import dev.rffrr.mateli.profiles.manualProtocols
import dev.rffrr.mateli.profiles.readField

/** Full-height editor for one profile. Save validates and returns the new profile. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditorDialog(profile: Profile, onDismiss: () -> Unit, onSave: (Profile) -> Unit) {
    val fields = remember(profile.id) { fieldsFor(profile.type) }
    val values = remember(profile.id) {
        mutableStateMapOf<String, String>().apply {
            fields.forEach { put(it.key, readField(profile, it)) }
        }
    }
    var name by remember(profile.id) { mutableStateOf(profile.name) }
    var error by remember { mutableStateOf<String?>(null) }
    var choosing by remember { mutableStateOf<EditField?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_cancel))
                    }
                    Text(
                        text = stringResource(R.string.profile_edit_title, profile.type.uppercase()),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f).padding(start = 8.dp)
                    )
                    IconButton(onClick = {
                        runCatching {
                            applyEdits(profile, name, values.toMap())
                        }.onSuccess { edited ->
                            onSave(edited)
                        }.onFailure { e ->
                            error = e.message ?: "invalid profile"
                        }
                    }) {
                        Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.action_save))
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.profile_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    var lastSection = ""
                    fields.forEach { field ->
                        if (field.section != lastSection) {
                            lastSection = field.section
                            HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                            Text(
                                text = field.section,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        FieldRow(
                            field = field,
                            value = values[field.key].orEmpty(),
                            onValue = { values[field.key] = it },
                            onChoose = { choosing = field }
                        )
                    }
                    error?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }

    choosing?.let { field ->
        ChoiceDialog(
            field = field,
            selected = values[field.key].orEmpty(),
            onPick = {
                values[field.key] = it
                choosing = null
            },
            onDismiss = { choosing = null }
        )
    }
}

@Composable
private fun FieldRow(field: EditField, value: String, onValue: (String) -> Unit, onChoose: () -> Unit) {
    when (field.kind) {
        FieldKind.Bool -> Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = field.label, style = MaterialTheme.typography.bodyLarge)
            Switch(checked = value == "true", onCheckedChange = { onValue(it.toString()) })
        }
        FieldKind.Choice -> OutlinedTextField(
            value = value.ifEmpty { "-" },
            onValueChange = {},
            readOnly = true,
            label = { Text(field.label) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().clickable(onClick = onChoose)
        )
        FieldKind.Secret -> OutlinedTextField(
            value = value,
            onValueChange = onValue,
            label = { Text(field.label) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        FieldKind.Number, FieldKind.Text -> OutlinedTextField(
            value = value,
            onValueChange = onValue,
            label = { Text(field.label) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ChoiceDialog(field: EditField, selected: String, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(field.label) },
        text = {
            Column {
                field.choices.forEach { choice ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onPick(choice) }.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = choice == selected, onClick = null)
                        Text(text = choice.ifEmpty { "-" }, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

/** Manual Settings: pick a protocol, then the editor opens with its defaults. */
@Composable
fun ManualProtocolDialog(onDismiss: () -> Unit, onPick: (String) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.manual_settings_title)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                manualProtocols.forEach { type ->
                    Text(
                        text = type.uppercase(),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.fillMaxWidth().clickable { onPick(type) }.padding(vertical = 12.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}
