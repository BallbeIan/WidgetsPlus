package app.widgetsplus.weather

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import app.widgetsplus.R
import app.widgetsplus.core.ModuleWidgetReceiver
import app.widgetsplus.core.widget.EmptyMessage
import app.widgetsplus.core.widget.RefreshAction
import app.widgetsplus.core.widget.WidgetHeader
import app.widgetsplus.core.widget.WidgetSurface
import app.widgetsplus.core.widget.openAppIntent
import app.widgetsplus.core.widget.refreshStatus

/**
 * Weather for one place. Small: current temperature and conditions. Wider and taller sizes add the next hours,
 * then the coming days.
 */
class WeatherWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = WeatherRepository.get(context)
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        provideContent {
            val s by repo.state.collectAsState()
            GlanceTheme { Content(s, repo.placeForWidget(appWidgetId, s)) }
        }
    }

    @Composable
    private fun Content(s: WeatherState, place: Place?) {
        val context = LocalContext.current
        val size = LocalSize.current
        // Drop hours and days that have passed, in case the forecast is old (for example after time offline).
        val forecast = place?.let { s.forecasts[it.id] }?.withoutPast()
        val openApp = actionStartActivity(openAppIntent(context, WeatherModule.id))
        val compact = size.height < 110.dp

        WidgetSurface(GlanceModifier.padding(horizontal = 14.dp, vertical = 10.dp), onClick = openApp) {
            if (place == null || forecast == null) {
                if (!compact && place != null) {
                    WidgetHeader(place.name, refreshStatus(context, s.lastRefresh, s.lastRefreshFailed, s.refreshing), null, actionRunCallback<RefreshAction>(), s.refreshing)
                }
                EmptyMessage(context.getString(if (place == null) R.string.weather_widget_empty else R.string.weather_widget_no_data))
                return@WidgetSurface
            }
            if (compact) {
                Compact(place, forecast, s.unit)
                return@WidgetSurface
            }
            WidgetHeader(
                title = place.name,
                status = refreshStatus(context, s.lastRefresh, s.lastRefreshFailed, s.refreshing),
                onTitleClick = null,
                refresh = actionRunCallback<RefreshAction>(),
                refreshing = s.refreshing,
            )
            Current(forecast, s.unit, narrow = size.width < 160.dp, showFeelsLike = size.width >= 220.dp)

            if (size.height >= 180.dp && forecast.hours.size > 1) {
                Spacer(GlanceModifier.height(10.dp))
                val count = ((size.width.value - 28) / 52).toInt().coerceIn(3, 8)
                Hours(forecast, s.unit, count)
            }
            if (size.height >= 290.dp && forecast.days.isNotEmpty()) {
                Spacer(GlanceModifier.height(8.dp))
                val count = ((size.height.value - 250) / 30).toInt().coerceIn(1, forecast.days.size)
                forecast.days.take(count).forEachIndexed { i, day -> DayRow(day, i == 0, forecast.timeZone, s.unit) }
            }
        }
    }

    /** One line: icon, temperature, place and conditions. For the smallest sizes. */
    @Composable
    private fun Compact(place: Place, f: Forecast, unit: TemperatureUnit) {
        val context = LocalContext.current
        val size = LocalSize.current
        Row(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            if (size.width >= 130.dp) {
                Image(ImageProvider(WeatherFormat.icon(f.current.code, f.current.isDay)), contentDescription = null, modifier = GlanceModifier.size(36.dp))
                Spacer(GlanceModifier.width(8.dp))
            }
            Text(
                WeatherFormat.temp(f.current.temperature, unit),
                maxLines = 1,
                style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 26.sp, fontWeight = FontWeight.Bold),
            )
            Spacer(GlanceModifier.width(10.dp))
            if (size.width >= 170.dp) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(place.name, maxLines = 1, style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Medium))
                    Text(
                        context.getString(WeatherFormat.condition(f.current.code)),
                        maxLines = 1,
                        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
                    )
                }
            }
        }
    }

    @Composable
    private fun Current(f: Forecast, unit: TemperatureUnit, narrow: Boolean, showFeelsLike: Boolean) {
        val context = LocalContext.current
        val today = f.days.firstOrNull()
        Row(modifier = GlanceModifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(
                ImageProvider(WeatherFormat.icon(f.current.code, f.current.isDay)),
                contentDescription = null,
                modifier = GlanceModifier.size(if (narrow) 32.dp else 44.dp),
            )
            Spacer(GlanceModifier.width(if (narrow) 4.dp else 8.dp))
            Text(
                WeatherFormat.temp(f.current.temperature, unit),
                maxLines = 1,
                style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = if (narrow) 26.sp else 34.sp, fontWeight = FontWeight.Bold),
            )
            Spacer(GlanceModifier.width(if (narrow) 6.dp else 12.dp))
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    context.getString(WeatherFormat.condition(f.current.code)),
                    maxLines = 1,
                    style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Medium),
                )
                if (today != null) {
                    Text(
                        context.getString(R.string.weather_high_low, WeatherFormat.temp(today.max, unit), WeatherFormat.temp(today.min, unit)),
                        maxLines = 1,
                        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
                    )
                }
                if (showFeelsLike) {
                    Text(
                        context.getString(R.string.weather_feels_like, WeatherFormat.temp(f.current.feelsLike, unit)),
                        maxLines = 1,
                        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
                    )
                }
            }
        }
    }

    @Composable
    private fun Hours(f: Forecast, unit: TemperatureUnit, count: Int) {
        val context = LocalContext.current
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            f.hours.take(count).forEachIndexed { i, hour ->
                Column(modifier = GlanceModifier.defaultWeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (i == 0) context.getString(R.string.weather_now) else WeatherFormat.hour(context, hour.time, f.timeZone),
                        maxLines = 1,
                        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp, textAlign = TextAlign.Center),
                    )
                    Image(
                        ImageProvider(WeatherFormat.icon(hour.code, hour.isDay)),
                        contentDescription = null,
                        modifier = GlanceModifier.size(24.dp).padding(vertical = 2.dp),
                    )
                    Text(
                        WeatherFormat.temp(hour.temperature, unit),
                        maxLines = 1,
                        style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center),
                    )
                }
            }
        }
    }

    @Composable
    private fun DayRow(day: Forecast.Day, isToday: Boolean, zone: String, unit: TemperatureUnit) {
        val context = LocalContext.current
        Row(modifier = GlanceModifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (isToday) context.getString(R.string.weather_today) else WeatherFormat.weekday(day.time, zone),
                maxLines = 1,
                style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 13.sp),
                modifier = GlanceModifier.width(56.dp),
            )
            Image(ImageProvider(WeatherFormat.icon(day.code)), contentDescription = null, modifier = GlanceModifier.size(22.dp))
            Spacer(GlanceModifier.defaultWeight())
            Text(
                WeatherFormat.temp(day.min, unit),
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp, textAlign = TextAlign.End),
                modifier = GlanceModifier.width(40.dp),
            )
            Text(
                WeatherFormat.temp(day.max, unit),
                style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.End),
                modifier = GlanceModifier.width(40.dp),
            )
        }
    }
}

class WeatherWidgetReceiver : ModuleWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WeatherWidget()
}
