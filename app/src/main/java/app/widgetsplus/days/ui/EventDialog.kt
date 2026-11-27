package app.widgetsplus.days.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.widgetsplus.R
import app.widgetsplus.core.ui.Accent
import app.widgetsplus.core.ui.AppAlert
import app.widgetsplus.core.ui.AppSwitch
import app.widgetsplus.core.ui.DialogField
import app.widgetsplus.core.ui.Hairline
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

    AppAlert(
        title = stringResource(R.string.days_add_event),
        confirmLabel = stringResource(R.string.add),
        onConfirm = { onConfirm(name.trim(), date, repeats) },
        onDismiss = onDismiss,
        confirmEnabled = name.isNotBlank(),
        body = {
            DialogField(name, { name = it }, stringResource(R.string.days_name_hint))
            // Date and repeat, as one small grouped box like the name field.
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
                Row(
                    Modifier.fillMaxWidth().clickable { picking = true }.padding(horizontal = 16.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.days_date), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    Text(DaysFormat.date(date), style = MaterialTheme.typography.bodyLarge, color = Accent)
                }
                Hairline()
                Row(
                    Modifier.fillMaxWidth().clickable { repeats = !repeats }.padding(start = 16.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.days_repeats_yearly), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    AppSwitch(repeats, { repeats = it })
                }
            }
        },
    )

    if (picking) {
        // The date picker works in UTC milliseconds at midnight.
        val state = rememberDatePickerState(initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        val colors = DatePickerDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            selectedDayContainerColor = Accent, selectedDayContentColor = Color.White,
            todayContentColor = Accent, todayDateBorderColor = Accent,
            selectedYearContainerColor = Accent, selectedYearContentColor = Color.White,
            currentYearContentColor = Accent,
        )
        DatePickerDialog(
            onDismissRequest = { picking = false },
            colors = colors,
            shape = RoundedCornerShape(26.dp),
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { epochDay = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay() }
                    picking = false
                }) { Text(stringResource(android.R.string.ok), color = Accent, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = { TextButton(onClick = { picking = false }) { Text(stringResource(R.string.cancel), color = Accent) } },
        ) {
            DatePicker(state = state, colors = colors)
        }
    }
}
