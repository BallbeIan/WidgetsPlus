package app.widgetsplus.weather

import android.content.Context
import android.text.format.DateFormat
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import app.widgetsplus.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

/** Weather codes (WMO, as used by Open-Meteo), temperatures and times, formatted for display. */
object WeatherFormat {

    @DrawableRes
    fun icon(code: Int, isDay: Boolean = true): Int = when (code) {
        0, 1 -> if (isDay) R.drawable.ic_wx_sun else R.drawable.ic_wx_moon
        2 -> if (isDay) R.drawable.ic_wx_partly_day else R.drawable.ic_wx_partly_night
        3 -> R.drawable.ic_wx_cloud
        45, 48 -> R.drawable.ic_wx_fog
        51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> R.drawable.ic_wx_rain
        71, 73, 75, 77, 85, 86 -> R.drawable.ic_wx_snow
        95, 96, 99 -> R.drawable.ic_wx_thunder
        else -> R.drawable.ic_wx_cloud
    }

    @StringRes
    fun condition(code: Int): Int = when (code) {
        0 -> R.string.wx_clear
        1 -> R.string.wx_mainly_clear
        2 -> R.string.wx_partly_cloudy
        3 -> R.string.wx_overcast
        45, 48 -> R.string.wx_fog
        51, 53, 55 -> R.string.wx_drizzle
        56, 57, 66, 67 -> R.string.wx_freezing_rain
        61, 63 -> R.string.wx_rain
        65 -> R.string.wx_heavy_rain
        80, 81, 82 -> R.string.wx_showers
        71, 73, 77 -> R.string.wx_snow
        75 -> R.string.wx_heavy_snow
        85, 86 -> R.string.wx_snow_showers
        95 -> R.string.wx_thunderstorm
        96, 99 -> R.string.wx_thunderstorm_hail
        else -> R.string.wx_unknown
    }

    /** "12°" in the chosen unit. Stored values are °C. */
    fun temp(celsius: Double, unit: TemperatureUnit): String {
        val v = if (unit == TemperatureUnit.FAHRENHEIT) celsius * 9 / 5 + 32 else celsius
        // Avoid "-0°".
        val rounded = v.roundToInt().let { if (it == 0) 0 else it }
        return "$rounded°"
    }

    /** "14:00" or "2 PM" in the place's own time, following the phone's 12/24-hour setting. */
    fun hour(context: Context, epochSeconds: Long, zone: String): String {
        val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h a"
        return DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
            .format(Instant.ofEpochSecond(epochSeconds).atZone(zoneOf(zone)))
    }

    /** "Mon" for a day's midnight in the place's time zone. */
    fun weekday(epochSeconds: Long, zone: String): String =
        Instant.ofEpochSecond(epochSeconds).atZone(zoneOf(zone)).dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())

    private fun zoneOf(id: String): ZoneId = runCatching { ZoneId.of(id) }.getOrDefault(ZoneId.systemDefault())
}
