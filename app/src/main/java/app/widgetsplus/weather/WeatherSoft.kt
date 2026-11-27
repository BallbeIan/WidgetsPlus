package app.widgetsplus.weather

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.unit.ColorProvider
import app.widgetsplus.R
import app.widgetsplus.core.widget.RefreshAction
import app.widgetsplus.core.widget.refreshStatus
import app.widgetsplus.core.widget.soft.LocalSoft
import app.widgetsplus.core.widget.soft.SoftIcon
import app.widgetsplus.core.widget.soft.SoftKey
import app.widgetsplus.core.widget.soft.SoftSurface
import app.widgetsplus.core.widget.soft.SoftText
import app.widgetsplus.core.widget.soft.SoftWeight
import app.widgetsplus.core.widget.soft.SoftWell
import app.widgetsplus.core.widget.soft.softLineHeight
import app.widgetsplus.core.widget.soft.softTextWidth

/*
 * The Soft design for the weather widget. Compact (under 110dp tall): one row. Otherwise a header and the current
 * weather, then, as height allows, the next hours in a sunken well and the coming days as raised keys.
 * All sizes are dp, worked out from the inner size SoftSurface passes in.
 */

private const val HEADER_TEXT = 14f
private const val LABEL = 11.5f
private const val DAY_KEY_H = 30f
private const val DAY_KEY_GAP = 6f

@Composable
internal fun SoftWeather(s: WeatherState, place: Place?, forecast: Forecast?, openApp: Action) {
    val context = LocalContext.current
    val compact = LocalSize.current.height < 110.dp
    SoftSurface(onClick = openApp, padV = if (compact) 8f else 12f) { inner ->
        val w = inner.width.value
        val h = inner.height.value
        val p = LocalSoft.current
        if (place == null || forecast == null) {
            var left = h
            if (!compact && place != null) {
                Header(place.name, s, w)
                left -= softLineHeight(context, HEADER_TEXT)
            }
            Spacer(GlanceModifier.defaultWeight())
            SoftLines(context.getString(if (place == null) R.string.weather_widget_empty else R.string.weather_widget_no_data), 13f, p.secondary, w, left)
            Spacer(GlanceModifier.defaultWeight())
            return@SoftSurface
        }
        if (compact) {
            Compact(place, forecast, s.unit, w, h)
            return@SoftSurface
        }

        Header(place.name, s, w)
        var used = softLineHeight(context, HEADER_TEXT) + 6f
        Spacer(GlanceModifier.height(6.dp))
        val current = currentLayout(context, forecast, s.unit, w)
        Current(forecast, current, w)
        used += current.height

        val wellH = softLineHeight(context, LABEL) + 2f + 24f + 2f + softLineHeight(context, 13f) + 16f
        if (h - used >= wellH + 10f && forecast.hours.size > 1) {
            Spacer(GlanceModifier.height(10.dp))
            Hours(forecast, s.unit, w, wellH)
            used += wellH + 10f
        }
        val dayCount = ((h - used - 10f + DAY_KEY_GAP) / (DAY_KEY_H + DAY_KEY_GAP)).toInt().coerceIn(0, forecast.days.size)
        if (dayCount > 0) {
            Spacer(GlanceModifier.height(10.dp))
            forecast.days.take(dayCount).forEachIndexed { i, day ->
                if (i > 0) Spacer(GlanceModifier.height(DAY_KEY_GAP.dp))
                DayKey(day, i == 0, forecast.timeZone, s.unit, w)
            }
        }
    }
}

/** Place name on the left; update time and the refresh button (one tap target) on the right. */
@Composable
private fun Header(title: String, s: WeatherState, w: Float) {
    val context = LocalContext.current
    val p = LocalSoft.current
    val status = refreshStatus(context, s.lastRefresh, s.lastRefreshFailed, s.refreshing)
    val statusW = softTextWidth(context, status, LABEL, SoftWeight.BOLD).coerceAtMost(w * 0.5f)
    Row(GlanceModifier.width(w.dp), verticalAlignment = Alignment.CenterVertically) {
        SoftText(title, HEADER_TEXT, SoftWeight.EXTRABOLD, p.ink, maxWidth = (w - statusW - 30f).coerceAtLeast(20f))
        Spacer(GlanceModifier.defaultWeight())
        Row(GlanceModifier.clickable(actionRunCallback<RefreshAction>()), verticalAlignment = Alignment.CenterVertically) {
            SoftText(status, LABEL, SoftWeight.BOLD, p.secondary, maxWidth = w * 0.5f)
            Spacer(GlanceModifier.width(4.dp))
            Image(
                ImageProvider(if (s.refreshing) R.drawable.ic_refreshing else R.drawable.ic_refresh),
                contentDescription = context.getString(R.string.refresh),
                colorFilter = ColorFilter.tint(ColorProvider(Color(p.secondary))),
                modifier = GlanceModifier.size(16.dp),
            )
        }
    }
}

/** One row: icon, temperature, then place and conditions if there is room. */
@Composable
private fun Compact(place: Place, f: Forecast, unit: TemperatureUnit, w: Float, h: Float) {
    val context = LocalContext.current
    val p = LocalSoft.current
    val temp = WeatherFormat.temp(f.current.temperature, unit)
    val iconSize = minOf(36f, h)
    val showIcon = w >= 90f
    val tempSize = minOf(28f, h / 1.4f, w / 2.6f).coerceAtLeast(12f)
    val tempW = softTextWidth(context, temp, tempSize)
    val rest = w - (if (showIcon) iconSize + 8f else 0f) - tempW - 10f
    Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        if (showIcon) {
            SoftIcon(WeatherFormat.icon(f.current.code, f.current.isDay), iconSize)
            Spacer(GlanceModifier.width(8.dp))
        }
        SoftText(temp, tempSize, SoftWeight.EXTRABOLD, p.ink, maxWidth = w, letterSpacing = -0.3f)
        if (rest >= 48f) {
            Spacer(GlanceModifier.width(10.dp))
            Column {
                SoftText(place.name, 13f, SoftWeight.EXTRABOLD, p.ink, maxWidth = rest)
                if (h >= softLineHeight(context, 13f) + softLineHeight(context, LABEL)) {
                    SoftText(context.getString(WeatherFormat.condition(f.current.code)), LABEL, SoftWeight.BOLD, p.secondary, maxWidth = rest)
                }
            }
        }
    }
}

/** Sizes and lines of the current-weather row, worked out before drawing so the caller knows its height. */
private class CurrentLayout(val iconSize: Float, val gap1: Float, val gap2: Float, val temp: String, val tempSize: Float, val rest: Float, val lines: List<String>, val height: Float)

private fun currentLayout(context: Context, f: Forecast, unit: TemperatureUnit, w: Float): CurrentLayout {
    val today = f.days.firstOrNull()
    val narrow = w < 120f
    val iconSize = if (narrow) 32f else 44f
    val gap1 = if (narrow) 4f else 8f
    val gap2 = if (narrow) 6f else 12f
    val temp = WeatherFormat.temp(f.current.temperature, unit)
    val tempSize = if (narrow) 28f else 36f
    val rest = w - iconSize - gap1 - softTextWidth(context, temp, tempSize) - gap2
    val lines = buildList {
        add(context.getString(WeatherFormat.condition(f.current.code)))
        if (today != null) add(context.getString(R.string.weather_high_low, WeatherFormat.temp(today.max, unit), WeatherFormat.temp(today.min, unit)))
        if (w >= 175f) add(context.getString(R.string.weather_feels_like, WeatherFormat.temp(f.current.feelsLike, unit)))
    }
    val column = softLineHeight(context, 13f) + (lines.size - 1) * softLineHeight(context, LABEL)
    val height = maxOf(iconSize, softLineHeight(context, tempSize), if (rest >= 36f) column else 0f)
    return CurrentLayout(iconSize, gap1, gap2, temp, tempSize, rest, lines, height)
}

/** Icon, big temperature, then conditions, high/low and feels-like. */
@Composable
private fun Current(f: Forecast, l: CurrentLayout, w: Float) {
    val p = LocalSoft.current
    Row(GlanceModifier.width(w.dp), verticalAlignment = Alignment.CenterVertically) {
        SoftIcon(WeatherFormat.icon(f.current.code, f.current.isDay), l.iconSize)
        Spacer(GlanceModifier.width(l.gap1.dp))
        SoftText(l.temp, l.tempSize, SoftWeight.EXTRABOLD, p.ink, letterSpacing = -0.3f)
        if (l.rest >= 36f) {
            Spacer(GlanceModifier.width(l.gap2.dp))
            Column {
                l.lines.forEachIndexed { i, line ->
                    if (i == 0) SoftText(line, 13f, SoftWeight.EXTRABOLD, p.ink, maxWidth = l.rest)
                    else SoftText(line, LABEL, SoftWeight.BOLD, p.secondary, maxWidth = l.rest)
                }
            }
        }
    }
}

/** The next hours in a sunken well: time, icon, temperature. */
@Composable
private fun Hours(f: Forecast, unit: TemperatureUnit, w: Float, wellH: Float) {
    val context = LocalContext.current
    val p = LocalSoft.current
    val padH = 10f
    val count = ((w - 2 * padH) / 46f).toInt().coerceIn(3, 8).coerceAtMost(f.hours.size)
    val colW = (w - 2 * padH) / count
    SoftWell(w, wellH, padH = padH, padV = 8f) {
        Row(GlanceModifier.fillMaxSize()) {
            f.hours.take(count).forEachIndexed { i, hour ->
                Column(GlanceModifier.width(colW.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    SoftText(
                        if (i == 0) context.getString(R.string.weather_now) else WeatherFormat.hour(context, hour.time, f.timeZone),
                        LABEL, SoftWeight.BOLD, p.secondary, maxWidth = colW,
                    )
                    Spacer(GlanceModifier.height(2.dp))
                    SoftIcon(WeatherFormat.icon(hour.code, hour.isDay), 24f)
                    Spacer(GlanceModifier.height(2.dp))
                    SoftText(WeatherFormat.temp(hour.temperature, unit), 13f, SoftWeight.EXTRABOLD, p.ink, maxWidth = colW)
                }
            }
        }
    }
}

/** One day as a raised key: weekday, icon, low and high. */
@Composable
private fun DayKey(day: Forecast.Day, isToday: Boolean, zone: String, unit: TemperatureUnit, w: Float) {
    val context = LocalContext.current
    val p = LocalSoft.current
    val tempW = if (w >= 190f) 40f else 34f
    SoftKey(w, DAY_KEY_H, padH = 12f) {
        Box(GlanceModifier.width(52.dp)) {
            SoftText(
                if (isToday) context.getString(R.string.weather_today) else WeatherFormat.weekday(day.time, zone),
                13f, SoftWeight.EXTRABOLD, p.ink, maxWidth = 50f,
            )
        }
        SoftIcon(WeatherFormat.icon(day.code), 20f)
        Spacer(GlanceModifier.defaultWeight())
        Box(GlanceModifier.width(tempW.dp), contentAlignment = Alignment.CenterEnd) {
            SoftText(WeatherFormat.temp(day.min, unit), 13f, SoftWeight.BOLD, p.secondary)
        }
        Box(GlanceModifier.width(tempW.dp), contentAlignment = Alignment.CenterEnd) {
            SoftText(WeatherFormat.temp(day.max, unit), 13f, SoftWeight.EXTRABOLD, p.ink)
        }
    }
}

/** Text wrapped over as many lines as fit [maxHeight]; the last line is shortened with an ellipsis. */
@Composable
private fun SoftLines(text: String, size: Float, color: Int, maxWidth: Float, maxHeight: Float) {
    val context = LocalContext.current
    val maxLines = (maxHeight / softLineHeight(context, size)).toInt().coerceAtLeast(1)
    for (line in wrap(context, text, size, maxWidth, maxLines)) {
        SoftText(line, size, SoftWeight.BOLD, color, maxWidth = maxWidth)
    }
}

private fun wrap(context: Context, text: String, size: Float, maxWidth: Float, maxLines: Int): List<String> {
    val lines = mutableListOf<String>()
    var current = ""
    val words = text.split(' ')
    for ((i, word) in words.withIndex()) {
        val candidate = if (current.isEmpty()) word else "$current $word"
        if (current.isEmpty() || softTextWidth(context, candidate, size, SoftWeight.BOLD) <= maxWidth) {
            current = candidate
        } else {
            if (lines.size == maxLines - 1) {
                lines += words.drop(i).joinToString(" ", prefix = "$current ")
                return lines
            }
            lines += current
            current = word
        }
    }
    if (current.isNotEmpty()) lines += current
    return lines
}
