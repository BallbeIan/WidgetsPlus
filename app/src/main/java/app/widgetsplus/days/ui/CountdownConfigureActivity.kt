package app.widgetsplus.days.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.widgetsplus.R
import app.widgetsplus.core.ui.ActionRow
import app.widgetsplus.core.ui.CheckRow
import app.widgetsplus.core.ui.ConfigureScaffold
import app.widgetsplus.core.ui.Footnote
import app.widgetsplus.core.ui.Grouped
import app.widgetsplus.core.ui.Hairline
import app.widgetsplus.core.ui.SectionHeader
import app.widgetsplus.core.ui.WidgetConfigureActivity
import app.widgetsplus.days.DaysFormat
import app.widgetsplus.days.DaysRepository
import app.widgetsplus.days.widget.CountdownWidget
import java.time.LocalDate
import kotlinx.coroutines.launch

/** Chooses the event a countdown widget shows, or creates a new one and chooses it. */
class CountdownConfigureActivity : WidgetConfigureActivity() {

    private val repo by lazy { DaysRepository.get(this) }

    @Composable
    override fun Content() {
        val s by repo.state.collectAsStateWithLifecycle()
        val today = LocalDate.now()
        val current = remember { repo.eventForWidget(appWidgetId)?.id }
        var adding by rememberSaveable { mutableStateOf(false) }
        val scope = rememberCoroutineScope()

        ConfigureScaffold(stringResource(R.string.days_choose_event)) {
            item { SectionHeader(stringResource(R.string.days_events_title)) }
            item {
                Grouped {
                    s.sorted(today).forEach { event ->
                        val count = DaysFormat.countText(this@CountdownConfigureActivity, event.daysUntil(today))
                        CheckRow(
                            title = event.name,
                            selected = event.id == current,
                            subtitle = "$count · " + DaysFormat.dateLine(this@CountdownConfigureActivity, event, today),
                            onClick = { choose(event.id) },
                        )
                        Hairline()
                    }
                    ActionRow(stringResource(R.string.days_add_event), onClick = { adding = true })
                }
            }
            if (s.events.isEmpty()) item { Footnote(stringResource(R.string.days_no_events)) }
            item { Footnote(stringResource(R.string.days_choose_event_help)) }
        }

        if (adding) {
            EventDialog(onDismiss = { adding = false }) { name, date, repeats ->
                adding = false
                scope.launch { choose(repo.addEvent(name, date, repeats)) }
            }
        }
    }

    private fun choose(eventId: String) = finishWith(CountdownWidget()) { repo.setEventForWidget(appWidgetId, eventId) }
}
