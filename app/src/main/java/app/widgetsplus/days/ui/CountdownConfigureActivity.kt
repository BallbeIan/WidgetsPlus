package app.widgetsplus.days.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.widgetsplus.R
import app.widgetsplus.core.ui.ChoiceCard
import app.widgetsplus.core.ui.ConfigureScaffold
import app.widgetsplus.core.ui.HelpText
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
            if (s.events.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.days_no_events),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(4.dp),
                    )
                }
            }
            items(s.sorted(today), key = { it.id }) { event ->
                ChoiceCard(
                    selected = event.id == current,
                    title = event.name,
                    subtitle = DaysFormat.dateLine(this@CountdownConfigureActivity, event, today),
                    onClick = { choose(event.id) },
                ) {
                    Text(
                        DaysFormat.countText(this@CountdownConfigureActivity, event.daysUntil(today)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item {
                FilledTonalButton(onClick = { adding = true }, modifier = Modifier.padding(top = 8.dp)) {
                    Text(stringResource(R.string.days_add_event))
                }
            }
            item { HelpText(stringResource(R.string.days_choose_event_help)) }
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
