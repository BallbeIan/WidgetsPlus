package app.widgetsplus.weather

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.widgetsplus.R
import app.widgetsplus.core.ui.Hue
import app.widgetsplus.core.WidgetKind
import app.widgetsplus.core.WidgetModule
import app.widgetsplus.weather.ui.WeatherConfigureActivity
import app.widgetsplus.weather.ui.WeatherScreen

/** Current weather and forecasts from Open-Meteo, for places the user adds by searching. */
object WeatherModule : WidgetModule {
    override val id = "weather"
    override val title = R.string.weather_title
    override val description = R.string.weather_description
    override val icon = R.drawable.ic_module_weather
    override val hue = Hue.BLUE

    val weatherWidget = WidgetKind(WeatherWidgetReceiver::class.java, R.string.widget_weather_name, R.string.widget_weather_description, R.drawable.ic_module_weather, WeatherConfigureActivity::class.java)
    override val widgets = listOf(weatherWidget)

    @Composable
    override fun summary(): String {
        val s by WeatherRepository.get(LocalContext.current).state.collectAsStateWithLifecycle()
        return pluralStringResource(R.plurals.weather_places, s.places.size, s.places.size)
    }

    @Composable
    override fun Screen() = WeatherScreen()

    override suspend fun refresh(context: Context) = WeatherRepository.get(context).refresh()

    override suspend fun updateWidgets(context: Context) = WeatherWidget().updateAll(context)

    override fun describeWidget(context: Context, appWidgetId: Int, kind: WidgetKind): String =
        WeatherRepository.get(context).placeForWidget(appWidgetId)?.name ?: context.getString(R.string.nothing_chosen)

    override fun forgetWidget(context: Context, appWidgetId: Int) = WeatherRepository.get(context).forgetWidget(appWidgetId)
}
