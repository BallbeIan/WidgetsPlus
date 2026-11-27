package app.widgetsplus.weather.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RemoveCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.widgetsplus.core.ui.MoveButtons
import app.widgetsplus.core.ui.RemoveButton
import app.widgetsplus.core.ui.EditToggle
import app.widgetsplus.R
import app.widgetsplus.core.ui.Accent
import app.widgetsplus.core.ui.FieldRow
import app.widgetsplus.core.ui.Footnote
import app.widgetsplus.core.ui.Grouped
import app.widgetsplus.core.ui.Hairline
import app.widgetsplus.core.ui.ListRow
import app.widgetsplus.core.ui.ModuleWidgetsCard
import app.widgetsplus.core.ui.Num
import app.widgetsplus.core.ui.Page
import app.widgetsplus.core.ui.PickerRow
import app.widgetsplus.core.ui.SectionHeader
import app.widgetsplus.core.ui.clickableNoRipple
import app.widgetsplus.core.widget.formatTime
import app.widgetsplus.weather.Forecast
import app.widgetsplus.weather.OpenMeteo
import app.widgetsplus.weather.Place
import app.widgetsplus.weather.TemperatureUnit
import app.widgetsplus.weather.WeatherFormat
import app.widgetsplus.weather.WeatherModule
import app.widgetsplus.weather.WeatherRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The Weather tab: places, adding a place, the temperature unit and the weather widget.
 * "Edit" next to Places shows remove and reorder buttons on each place.
 */
@Composable
fun WeatherScreen() {
    val context = LocalContext.current
    val repo = remember { WeatherRepository.get(context) }
    val s by repo.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var editing by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (System.currentTimeMillis() - repo.state.value.lastRefresh > 10 * 60_000) repo.refresh()
    }

    Page(
        title = stringResource(R.string.weather_title),
        actions = {
            if (s.refreshing) {
                Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(22.dp), color = Accent, strokeWidth = 2.dp)
                }
            } else {
                IconButton(onClick = { scope.launch { repo.refresh() } }) {
                    Icon(Icons.Rounded.Refresh, contentDescription = stringResource(R.string.refresh), tint = Accent)
                }
            }
        },
    ) {
        item(key = "places-header") {
            SectionHeader(stringResource(R.string.weather_places)) {
                if (s.places.isNotEmpty() || editing) {
                    EditToggle(editing) { editing = !editing }
                }
            }
        }
        item(key = "places") {
            Grouped {
                if (s.places.isEmpty()) {
                    Text(
                        stringResource(R.string.weather_no_places),
                        Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                s.places.forEachIndexed { i, place ->
                    if (i > 0) Hairline(if (editing) 60.dp else 16.dp)
                    PlaceRow(
                        place = place,
                        forecast = s.forecasts[place.id],
                        unit = s.unit,
                        showWeather = !editing,
                        leading = if (editing) ({
                            RemoveButton(stringResource(R.string.remove, place.name)) { scope.launch { repo.removePlace(place.id) } }
                        }) else null,
                        trailing = if (editing) ({
                            MoveButtons(
                                canUp = i > 0,
                                canDown = i < s.places.lastIndex,
                                onUp = { scope.launch { repo.movePlace(place.id, -1) } },
                                onDown = { scope.launch { repo.movePlace(place.id, 1) } },
                            )
                        }) else null,
                    )
                }
            }
        }
        if (s.lastRefresh != 0L) {
            item(key = "status") {
                Footnote(
                    if (s.lastRefreshFailed) stringResource(R.string.offline_since, formatTime(context, s.lastRefresh))
                    else stringResource(R.string.updated_at, formatTime(context, s.lastRefresh)),
                    error = s.lastRefreshFailed,
                )
            }
        }

        item(key = "search") { PlaceSearchCard(onPick = { place -> scope.launch { repo.addPlace(place) } }) }

        item { SectionHeader(stringResource(R.string.weather_units)) }
        item {
            Grouped {
                PickerRow(
                    title = stringResource(R.string.weather_unit),
                    options = TemperatureUnit.entries,
                    selected = s.unit,
                    label = { context.getString(if (it == TemperatureUnit.CELSIUS) R.string.weather_celsius else R.string.weather_fahrenheit) },
                    onPick = { unit -> scope.launch { repo.setUnit(unit) } },
                )
            }
        }

        item { SectionHeader(stringResource(R.string.widgets_title)) }
        item { ModuleWidgetsCard(WeatherModule) }
        item { Footnote(stringResource(R.string.weather_credit)) }
    }
}

/**
 * One place as a row of a grouped list: [leading], name and region, the current weather icon and temperature
 * (when [showWeather]), then [trailing].
 */
@Composable
fun PlaceRow(
    place: Place,
    forecast: Forecast?,
    unit: TemperatureUnit,
    modifier: Modifier = Modifier,
    showWeather: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        if (leading != null) { leading(); Spacer(Modifier.width(12.dp)) }
        Column(Modifier.weight(1f)) {
            Text(place.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (place.region.isNotBlank()) {
                Text(place.region, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (showWeather && forecast != null) {
            Icon(
                painterResource(WeatherFormat.icon(forecast.current.code, forecast.current.isDay)),
                contentDescription = stringResource(WeatherFormat.condition(forecast.current.code)),
                tint = Color.Unspecified,
                modifier = Modifier.padding(start = 8.dp).size(26.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(WeatherFormat.temp(forecast.current.temperature, unit), style = MaterialTheme.typography.bodyLarge.merge(Num))
        }
        if (trailing != null) { Spacer(Modifier.width(8.dp)); trailing() }
    }
}

/**
 * "Add a place": a header, a grouped card with the search field and the results as you type, and any error
 * underneath. Tapping a result calls [onPick].
 */
@Composable
fun PlaceSearchCard(modifier: Modifier = Modifier, onPick: (Place) -> Unit) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Place>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }

    LaunchedEffect(query) {
        error = false
        if (query.trim().length < 2) {
            results = emptyList()
            return@LaunchedEffect
        }
        delay(400) // Wait for typing to pause before searching.
        searching = true
        results = try {
            OpenMeteo.search(query).also { error = false }
        } catch (e: CancellationException) {
            throw e // The user typed more; a newer search takes over.
        } catch (e: Exception) {
            error = true
            emptyList()
        }
        searching = false
    }

    Column(modifier.fillMaxWidth()) {
        SectionHeader(stringResource(R.string.weather_add_place))
        Grouped {
            FieldRow(
                value = query,
                onChange = { query = it },
                placeholder = stringResource(R.string.weather_search_hint),
                capitalization = KeyboardCapitalization.Words,
                trailing = {
                    if (searching) CircularProgressIndicator(Modifier.padding(start = 8.dp).size(18.dp), color = Accent, strokeWidth = 2.dp)
                },
            )
            results.forEach { place ->
                Hairline()
                ListRow(
                    title = place.name,
                    subtitle = place.region.ifBlank { null },
                    onClick = {
                        onPick(place)
                        query = ""
                    },
                    trailing = { Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.add), tint = Accent) },
                )
            }
        }
        when {
            error -> Footnote(stringResource(R.string.no_connection), error = true)
            query.trim().length >= 2 && !searching && results.isEmpty() -> Footnote(stringResource(R.string.weather_no_results))
        }
    }
}

