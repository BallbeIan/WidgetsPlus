package app.widgetsplus.weather.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.widgetsplus.R
import app.widgetsplus.core.RefreshWorker
import app.widgetsplus.core.ui.ChoiceCard
import app.widgetsplus.core.ui.ConfigureScaffold
import app.widgetsplus.core.ui.HelpText
import app.widgetsplus.core.ui.WidgetConfigureActivity
import app.widgetsplus.weather.WeatherFormat
import app.widgetsplus.weather.WeatherRepository
import app.widgetsplus.weather.WeatherWidget
import kotlinx.coroutines.launch

/** Chooses the place a weather widget shows, or adds a new one by searching. */
class WeatherConfigureActivity : WidgetConfigureActivity() {

    private val repo by lazy { WeatherRepository.get(this) }

    @Composable
    override fun Content() {
        val s by repo.state.collectAsStateWithLifecycle()
        val current = remember { repo.placeForWidget(appWidgetId)?.id }
        val scope = rememberCoroutineScope()

        fun choose(placeId: String) = finishWith(WeatherWidget()) { repo.setPlaceForWidget(appWidgetId, placeId) }

        ConfigureScaffold(stringResource(R.string.weather_choose_place)) {
            items(s.places, key = { it.id }) { place ->
                val f = s.forecasts[place.id]
                ChoiceCard(
                    selected = place.id == current,
                    title = place.name,
                    subtitle = listOfNotNull(
                        place.region.ifBlank { null },
                        f?.let { "${WeatherFormat.temp(it.current.temperature, s.unit)} ${getString(WeatherFormat.condition(it.current.code))}" },
                    ).joinToString(" · "),
                    onClick = { choose(place.id) },
                )
            }
            item {
                PlaceSearchCard(Modifier.padding(top = 8.dp)) { place ->
                    // Save the choice straight away; the forecast is fetched in the background.
                    scope.launch {
                        repo.addPlace(place, fetch = false)
                        choose(place.id)
                        RefreshWorker.refreshSoon(this@WeatherConfigureActivity)
                    }
                }
            }
            item { HelpText(stringResource(R.string.weather_credit)) }
        }
    }
}
