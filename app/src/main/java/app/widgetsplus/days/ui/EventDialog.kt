package app.widgetsplus.days.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import app.widgetsplus.R
import app.widgetsplus.days.DaysFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Asks for a new event's name, date and whether it repeats every year. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDialog(onDismiss: () -> Unit, onConfirm: (name: String, date: LocalDate, repeatsYearly: Boolean) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    // Kept as epoch days so it survives rotation.
    var epochDay by rememberSaveable { mutableStateOf(LocalDate.now().toEpochDay()) }
    var repeats by rememberSaveable { mutableStateOf(false) }
    var picking by rememberSaveable { mutableStateOf(false) }
    val date = LocalDate.ofEpochDay(epochDay)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.days_add_event)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.name)) },
                    placeholder = { Text(stringResource(R.string.days_name_hint)) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedButton(onClick = { picking = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(DaysFormat.date(date))
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.days_repeats_yearly),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(checked = repeats, onCheckedChange = { repeats = it })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name.trim(), date, repeats) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.add))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )

    if (picking) {
        // The date picker works in UTC milliseconds at midnight.
        val state = rememberDatePickerState(initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { epochDay = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay() }
                    picking = false
                }) { Text(stringResource(android.R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { picking = false }) { Text(stringResource(R.string.cancel)) } },
        ) {
            DatePicker(state = state)
        }
    }
}
