package app.widgetsplus.days.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.RemoveCircle
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.widgetsplus.core.ui.RemoveButton
import app.widgetsplus.core.ui.EditToggle
import app.widgetsplus.R
import app.widgetsplus.core.ui.Accent
import app.widgetsplus.core.ui.ActionRow
import app.widgetsplus.core.ui.ConfirmDialog
import app.widgetsplus.core.ui.Footnote
import app.widgetsplus.core.ui.Grouped
import app.widgetsplus.core.ui.Hairline
import app.widgetsplus.core.ui.IconRowIndent
import app.widgetsplus.core.ui.ListRow
import app.widgetsplus.core.ui.ModuleWidgetsCard
import app.widgetsplus.core.ui.Num
import app.widgetsplus.core.ui.Page
import app.widgetsplus.core.ui.SectionHeader
import app.widgetsplus.core.ui.Squircle
import app.widgetsplus.core.ui.clickableNoRipple
import app.widgetsplus.days.DayEvent
import app.widgetsplus.days.DaysFormat
import app.widgetsplus.days.DaysModule
import app.widgetsplus.days.DaysRepository
import java.time.LocalDate
import kotlinx.coroutines.launch

/**
 * The Days tab: the events with "Add event" under them, and the Days widgets.
 * "Edit" next to Events shows a remove button on each event.
 */
@Composable
fun DaysScreen() {
    val context = LocalContext.current
    val repo = remember { DaysRepository.get(context) }
    val s by repo.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val today = LocalDate.now()
    val events = s.sorted(today)

    var adding by rememberSaveable { mutableStateOf(false) }
    var editing by rememberSaveable { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<DayEvent?>(null) }

    Page(stringResource(R.string.days_title)) {
        item(key = "events-header") {
            SectionHeader(stringResource(R.string.days_events_title)) {
                if (events.isNotEmpty() || editing) {
                    EditToggle(editing) { editing = !editing }
                }
            }
        }
        item(key = "events") {
            Grouped {
                events.forEach { event ->
                    EventRow(event, today, editing, onDelete = { deleting = event })
                    Hairline(IconRowIndent)
                }
                ActionRow(stringResource(R.string.days_add_event), onClick = { adding = true })
            }
        }
        if (events.isEmpty()) item { Footnote(stringResource(R.string.days_no_events)) }

        item { SectionHeader(stringResource(R.string.widgets_title)) }
        item { ModuleWidgetsCard(DaysModule) }
        item { Footnote(stringResource(R.string.days_widgets_help)) }
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

/** An event: countdown squircle (a red remove button in edit mode), name, date, and the days to go or since. */
@Composable
private fun EventRow(event: DayEvent, today: LocalDate, editing: Boolean, onDelete: () -> Unit) {
    val context = LocalContext.current
    val days = event.daysUntil(today)
    ListRow(
        title = event.name,
        subtitle = DaysFormat.dateLine(context, event, today),
        leading = {
            if (editing) {
                RemoveButton(stringResource(R.string.days_delete_event, event.name), onDelete)
            } else {
                Squircle(R.drawable.ic_widget_countdown, DaysModule.hue.color)
            }
        },
        trailing = {
            Text(
                DaysFormat.countText(context, days),
                style = MaterialTheme.typography.bodyMedium.merge(Num),
                color = if (days >= 0) Accent else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
    )
}
