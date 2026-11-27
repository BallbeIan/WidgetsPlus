package app.widgetsplus.clock

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import app.widgetsplus.R
import app.widgetsplus.core.ui.CheckRow
import app.widgetsplus.core.ui.ConfigureScaffold
import app.widgetsplus.core.ui.FieldRow
import app.widgetsplus.core.ui.Footnote
import app.widgetsplus.core.ui.Grouped
import app.widgetsplus.core.ui.Hairline
import app.widgetsplus.core.ui.ListRow
import app.widgetsplus.core.ui.SectionHeader
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
            item { SectionHeader(stringResource(R.string.clock_label_title)) }
            item {
                Grouped {
                    FieldRow(label, { label = it }, stringResource(R.string.clock_label_field), capitalization = KeyboardCapitalization.Words)
                }
            }
            item { Footnote(stringResource(R.string.clock_label_help)) }

            item { SectionHeader(stringResource(R.string.clock_time_zone_title)) }
            item {
                Grouped {
                    CheckRow(
                        title = stringResource(R.string.local_time),
                        selected = current == null,
                        subtitle = stringResource(R.string.local_time_help),
                        onClick = { choose(null) },
                    )
                    if (current != null) {
                        Hairline()
                        CheckRow(
                            title = ClockModule.cityName(current),
                            selected = true,
                            subtitle = "${ClockModule.zoneLongName(current)} · ${ClockModule.offsetText(current)}",
                            onClick = { choose(current) },
                        )
                    }
                }
            }

            item { SectionHeader(stringResource(R.string.other_time_zone)) }
            item {
                Grouped {
                    FieldRow(query, { query = it }, stringResource(R.string.search_city_hint), capitalization = KeyboardCapitalization.Words)
                    matches.forEach { zone ->
                        Hairline()
                        ListRow(
                            title = ClockModule.cityName(zone),
                            subtitle = "${zone.id.substringBefore('/')} · ${ClockModule.zoneLongName(zone)} · ${ClockModule.offsetText(zone)}",
                            onClick = { choose(zone) },
                        )
                    }
                }
            }
            if (query.trim().length >= 2 && matches.isEmpty()) {
                item { Footnote(stringResource(R.string.no_time_zone_found)) }
            }
        }
    }
}
