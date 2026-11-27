package app.widgetsplus.weather.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.widgetsplus.R
import app.widgetsplus.core.ui.HelpText
import app.widgetsplus.core.ui.ModuleScaffold
import app.widgetsplus.core.ui.ModuleWidgetsCard
import app.widgetsplus.core.ui.SectionTitle
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

/** Weather settings: places, temperature unit and the weather widget. */
@Composable
fun WeatherScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { WeatherRepository.get(context) }
    val s by repo.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        if (System.currentTimeMillis() - repo.state.value.lastRefresh > 10 * 60_000) repo.refresh()
    }

    ModuleScaffold(
        title = stringResource(R.string.weather_title),
        onBack = onBack,
        actions = {
            if (s.refreshing) {
                Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                }
            } else {
                IconButton(onClick = { scope.launch { repo.refresh() } }) {
                    Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.refresh))
                }
            }
        },
    ) {
        if (s.lastRefresh != 0L) {
            item {
                HelpText(
                    if (s.lastRefreshFailed) stringResource(R.string.offline_since, formatTime(context, s.lastRefresh))
                    else stringResource(R.string.updated_at, formatTime(context, s.lastRefresh)),
                )
            }
        }
        item { SectionTitle(stringResource(R.string.weather_places)) }
        item {
            Card(Modifier.fillMaxWidth()) {
                if (s.places.isEmpty()) {
                    Text(stringResource(R.string.weather_no_places), Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
                }
                s.places.forEachIndexed { i, place ->
                    if (i > 0) HorizontalDivider()
                    PlaceRow(place, s.forecasts[place.id], s.unit, Modifier.padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp)) {
                        Column {
                            IconButton(onClick = { scope.launch { repo.movePlace(place.id, -1) } }, enabled = i > 0, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = stringResource(R.string.move_up))
                            }
                            IconButton(onClick = { scope.launch { repo.movePlace(place.id, 1) } }, enabled = i < s.places.lastIndex, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = stringResource(R.string.move_down))
                            }
                        }
                        IconButton(onClick = { scope.launch { repo.removePlace(place.id) } }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.remove, place.name))
                        }
                    }
                }
            }
        }
        item { PlaceSearchCard(onPick = { place -> scope.launch { repo.addPlace(place) } }) }
        item { SectionTitle(stringResource(R.string.weather_unit)) }
        item { UnitPicker(s.unit) { unit -> scope.launch { repo.setUnit(unit) } } }
        item { SectionTitle(stringResource(R.string.widgets_title)) }
        item { ModuleWidgetsCard(WeatherModule) }
        item { HelpText(stringResource(R.string.weather_credit)) }
    }
}

/** Place name and region on the left; current icon and temperature on the right; then [trailing] buttons. */
@Composable
fun PlaceRow(place: Place, forecast: Forecast?, unit: TemperatureUnit, modifier: Modifier = Modifier, trailing: @Composable () -> Unit = {}) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(place.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
            if (place.region.isNotBlank()) {
                Text(place.region, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
        if (forecast != null) {
            Icon(
                painterResource(WeatherFormat.icon(forecast.current.code, forecast.current.isDay)),
                contentDescription = stringResource(WeatherFormat.condition(forecast.current.code)),
                tint = androidx.compose.ui.graphics.Color.Unspecified,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(WeatherFormat.temp(forecast.current.temperature, unit), style = MaterialTheme.typography.titleMedium)
        }
        trailing()
    }
}

/** Search field with results as you type. Tapping a result calls [onPick]. */
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

    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.weather_add_place), style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(R.string.search_city)) },
                placeholder = { Text(stringResource(R.string.weather_search_hint)) },
                trailingIcon = { if (searching) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) },
            )
            when {
                error -> Text(stringResource(R.string.no_connection), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                query.trim().length >= 2 && !searching && results.isEmpty() ->
                    Text(stringResource(R.string.weather_no_results), style = MaterialTheme.typography.bodySmall)
            }
            results.forEach { place ->
                Row(
                    Modifier.fillMaxWidth().clickable {
                        onPick(place)
                        query = ""
                    }.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(place.name, style = MaterialTheme.typography.bodyLarge)
                        Text(place.region, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnitPicker(unit: TemperatureUnit, onChange: (TemperatureUnit) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        TemperatureUnit.entries.forEachIndexed { i, u ->
            SegmentedButton(
                selected = unit == u,
                onClick = { onChange(u) },
                shape = SegmentedButtonDefaults.itemShape(i, TemperatureUnit.entries.size),
            ) { Text(stringResource(if (u == TemperatureUnit.CELSIUS) R.string.weather_celsius else R.string.weather_fahrenheit)) }
        }
    }
}
