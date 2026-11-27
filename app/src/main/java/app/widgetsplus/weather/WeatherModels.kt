package app.widgetsplus.weather

import org.json.JSONArray
import org.json.JSONObject

/** A place the user added by searching. No location permission is used. */
data class Place(
    val id: String,
    val name: String,
    /** "North Holland, The Netherlands": shown under the name to tell same-named places apart. */
    val region: String,
    val latitude: Double,
    val longitude: Double,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id).put("name", name).put("region", region)
        .put("latitude", latitude).put("longitude", longitude)

    companion object {
        fun fromJson(o: JSONObject) = Place(
            o.getString("id"), o.getString("name"), o.optString("region"),
            o.getDouble("latitude"), o.getDouble("longitude"),
        )
    }
}

/** Temperatures are always stored in °C and converted for display. */
data class Forecast(
    val fetchedAt: Long,
    /** The place's time zone, e.g. "Europe/Amsterdam", for hours and weekdays. */
    val timeZone: String,
    val current: Current,
    /** The next 24 hours, starting with the current hour. */
    val hours: List<Hour>,
    /** Today and the following days. */
    val days: List<Day>,
) {
    data class Current(val temperature: Double, val feelsLike: Double, val code: Int, val isDay: Boolean)

    /** This forecast without the hours and days that have already passed. */
    fun withoutPast(nowSeconds: Long = System.currentTimeMillis() / 1000): Forecast = copy(
        hours = hours.filter { it.time > nowSeconds - 3600 },
        days = days.filter { it.time + 24 * 3600 > nowSeconds },
    )

    data class Hour(val time: Long, val temperature: Double, val code: Int, val isDay: Boolean)
    data class Day(val time: Long, val code: Int, val max: Double, val min: Double)

    fun toJson(): JSONObject = JSONObject()
        .put("fetchedAt", fetchedAt)
        .put("timeZone", timeZone)
        .put("current", JSONObject().put("t", current.temperature).put("f", current.feelsLike).put("c", current.code).put("d", current.isDay))
        .put("hours", JSONArray(hours.map { JSONObject().put("time", it.time).put("t", it.temperature).put("c", it.code).put("d", it.isDay) }))
        .put("days", JSONArray(days.map { JSONObject().put("time", it.time).put("c", it.code).put("max", it.max).put("min", it.min) }))

    companion object {
        fun fromJson(o: JSONObject): Forecast {
            val c = o.getJSONObject("current")
            val h = o.getJSONArray("hours")
            val d = o.getJSONArray("days")
            return Forecast(
                fetchedAt = o.getLong("fetchedAt"),
                timeZone = o.getString("timeZone"),
                current = Current(c.getDouble("t"), c.getDouble("f"), c.getInt("c"), c.getBoolean("d")),
                hours = List(h.length()) { h.getJSONObject(it).let { x -> Hour(x.getLong("time"), x.getDouble("t"), x.getInt("c"), x.getBoolean("d")) } },
                days = List(d.length()) { d.getJSONObject(it).let { x -> Day(x.getLong("time"), x.getInt("c"), x.getDouble("max"), x.getDouble("min")) } },
            )
        }
    }
}

enum class TemperatureUnit { CELSIUS, FAHRENHEIT }

data class WeatherState(
    val places: List<Place>,
    val forecasts: Map<String, Forecast>,
    val unit: TemperatureUnit,
    val lastRefresh: Long,
    val lastRefreshFailed: Boolean,
    val refreshing: Boolean,
    /** Goes up when a widget's chosen place changes, so running widgets redraw. */
    val widgetSettingsVersion: Int = 0,
)
