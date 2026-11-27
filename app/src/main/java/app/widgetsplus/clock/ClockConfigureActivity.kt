package app.widgetsplus.clock

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import app.widgetsplus.R
import app.widgetsplus.core.ui.ChoiceCard
import app.widgetsplus.core.ui.ConfigureScaffold
import app.widgetsplus.core.ui.HelpText
import app.widgetsplus.core.ui.SectionTitle
import app.widgetsplus.core.ui.WidgetConfigureActivity
import java.time.ZoneId

/** Chooses the time zone (local or a city) and an optional label for a digital clock. */
class ClockConfigureActivity : WidgetConfigureActivity() {

    /** Time zones named after a city, sorted by city: "Europe/Paris", "America/Argentina/Buenos_Aires". */
    private val cityZones: List<ZoneId> by lazy {
        val regions = setOf("Africa", "America", "Antarctica", "Asia", "Atlantic", "Australia", "Europe", "Indian", "Pacific")
        ZoneId.getAvailableZoneIds()
            .filter { it.substringBefore('/') in regions && '/' in it }
            .map { ZoneId.of(it) }
            .sortedBy { ClockModule.cityName(it) }
    }

    @Composable
    override fun Content() {
        val current = remember { ClockModule.zoneFor(this, appWidgetId) }
        var label by remember { mutableStateOf(ClockModule.labelFor(this, appWidgetId) ?: "") }
        var query by remember { mutableStateOf("") }
        val matches = remember(query) {
            val q = query.trim()
            if (q.length < 2) emptyList()
            else cityZones.filter { ClockModule.cityName(it).contains(q, ignoreCase = true) || it.id.contains(q, ignoreCase = true) }.take(40)
        }

        fun choose(zone: ZoneId?) = finishWith(DigitalClockWidget()) { ClockModule.setClock(this, appWidgetId, zone, label) }

        ConfigureScaffold(stringResource(R.string.choose_clock)) {
            item {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.clock_label_field)) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                )
            }
            item { HelpText(stringResource(R.string.clock_label_help)) }
            item {
                ChoiceCard(
                    selected = current == null,
                    title = stringResource(R.string.local_time),
                    subtitle = stringResource(R.string.local_time_help),
                    onClick = { choose(null) },
                )
            }
            if (current != null) {
                item {
                    ChoiceCard(
                        selected = true,
                        title = ClockModule.cityName(current),
                        subtitle = "${ClockModule.zoneLongName(current)} · ${ClockModule.offsetText(current)}",
                        onClick = { choose(current) },
                    )
                }
            }
            item { SectionTitle(stringResource(R.string.other_time_zone)) }
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.search_city)) },
                    placeholder = { Text(stringResource(R.string.search_city_hint)) },
                )
            }
            if (query.trim().length >= 2 && matches.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.no_time_zone_found),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(4.dp),
                    )
                }
            }
            items(matches, key = { it.id }) { zone ->
                ChoiceCard(
                    selected = false,
                    title = ClockModule.cityName(zone),
                    subtitle = "${zone.id.substringBefore('/')} · ${ClockModule.zoneLongName(zone)} · ${ClockModule.offsetText(zone)}",
                    onClick = { choose(zone) },
                )
            }
        }
    }
}
