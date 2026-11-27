package app.widgetsplus.days.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.widgetsplus.R
import app.widgetsplus.core.ui.ConfirmDialog
import app.widgetsplus.core.ui.HelpText
import app.widgetsplus.core.ui.ModuleScaffold
import app.widgetsplus.core.ui.ModuleWidgetsCard
import app.widgetsplus.core.ui.SectionTitle
import app.widgetsplus.days.DayEvent
import app.widgetsplus.days.DaysFormat
import app.widgetsplus.days.DaysModule
import app.widgetsplus.days.DaysRepository
import java.time.LocalDate
import kotlinx.coroutines.launch

/** Days settings: the events, an Add button, and the Days widgets. */
@Composable
fun DaysScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { DaysRepository.get(context) }
    val s by repo.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val today = LocalDate.now()
    val events = s.sorted(today)

    var adding by rememberSaveable { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<DayEvent?>(null) }

    ModuleScaffold(
        title = stringResource(R.string.days_title),
        onBack = onBack,
        actions = {
            IconButton(onClick = { adding = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.days_add_event))
            }
        },
    ) {
        item { SectionTitle(stringResource(R.string.days_events_title)) }
        item {
            Card(Modifier.fillMaxWidth()) {
                if (events.isEmpty()) {
                    Text(
                        stringResource(R.string.days_no_events),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp),
                    )
                } else {
                    Column(Modifier.padding(vertical = 4.dp)) {
                        events.forEachIndexed { i, event ->
                            if (i > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                            EventRow(event, today, onDelete = { deleting = event })
                        }
                    }
                }
            }
        }
        item {
            FilledTonalButton(onClick = { adding = true }) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text(stringResource(R.string.days_add_event))
            }
        }
        item { SectionTitle(stringResource(R.string.widgets_title)) }
        item { ModuleWidgetsCard(DaysModule, Modifier.fillMaxWidth()) }
        item { HelpText(stringResource(R.string.days_widgets_help)) }
    }

    if (adding) {
        EventDialog(onDismiss = { adding = false }) { name, date, repeats ->
            adding = false
            scope.launch { repo.addEvent(name, date, repeats) }
        }
    }
    deleting?.let { event ->
        ConfirmDialog(
            title = stringResource(R.string.days_delete_title, event.name),
            text = stringResource(R.string.days_delete_text),
            confirmLabel = stringResource(R.string.delete),
            onDismiss = { deleting = null },
        ) {
            deleting = null
            scope.launch { repo.deleteEvent(event.id) }
        }
    }
}

@Composable
private fun EventRow(event: DayEvent, today: LocalDate, onDelete: () -> Unit) {
    val context = LocalContext.current
    val days = event.daysUntil(today)
    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(event.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                DaysFormat.dateLine(context, event, today),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            DaysFormat.countText(context, days),
            style = MaterialTheme.typography.labelLarge,
            color = if (days >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp),
        )
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.days_delete_event, event.name))
        }
    }
}
