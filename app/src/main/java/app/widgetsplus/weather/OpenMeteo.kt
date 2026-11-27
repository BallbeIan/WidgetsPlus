package app.widgetsplus.weather

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Open-Meteo (open-meteo.com): free weather and place search without an account or key, for non-commercial use.
 * Its data is CC BY 4.0, so the app credits it. Only the place name typed (search) or its coordinates (forecast) are sent.
 */
object OpenMeteo {

    suspend fun search(query: String): List<Place> {
        val q = query.trim()
        if (q.length < 2) return emptyList()
        val lang = Locale.getDefault().language
        val body = get("https://geocoding-api.open-meteo.com/v1/search?name=${enc(q)}&count=10&language=$lang&format=json")
        val results = JSONObject(body).optJSONArray("results") ?: return emptyList()
        return (0 until results.length()).map { results.getJSONObject(it) }.map { r ->
            Place(
                id = r.optLong("id").toString(),
                name = r.getString("name"),
                region = listOf(r.optString("admin1"), r.optString("country")).filter { it.isNotBlank() }.joinToString(", "),
                latitude = r.getDouble("latitude"),
                longitude = r.getDouble("longitude"),
            )
        }
    }

    suspend fun forecast(place: Place): Forecast {
        val url = "https://api.open-meteo.com/v1/forecast?latitude=${place.latitude}&longitude=${place.longitude}" +
            "&current=temperature_2m,apparent_temperature,weather_code,is_day" +
            "&hourly=temperature_2m,weather_code,is_day" +
            "&daily=weather_code,temperature_2m_max,temperature_2m_min" +
            "&timezone=auto&timeformat=unixtime&forecast_days=7"
        val o = JSONObject(get(url))
        val c = o.getJSONObject("current")
        val h = o.getJSONObject("hourly")
        val d = o.getJSONObject("daily")

        val hourTimes = h.getJSONArray("time")
        // The hour that contains "now". Hourly times are on the place's hour, which is not always a whole UTC hour.
        val now = c.getLong("time")
        val hours = (0 until hourTimes.length())
            .filter { hourTimes.getLong(it) > now - 3600 }
            .take(24)
            .map { i ->
                Forecast.Hour(
                    time = hourTimes.getLong(i),
                    temperature = h.getJSONArray("temperature_2m").getDouble(i),
                    code = h.getJSONArray("weather_code").getInt(i),
                    isDay = h.getJSONArray("is_day").getInt(i) == 1,
                )
            }
        val dayTimes = d.getJSONArray("time")
        val days = (0 until dayTimes.length()).map { i ->
            Forecast.Day(
                time = dayTimes.getLong(i),
                code = d.getJSONArray("weather_code").getInt(i),
                max = d.getJSONArray("temperature_2m_max").getDouble(i),
                min = d.getJSONArray("temperature_2m_min").getDouble(i),
            )
        }
        return Forecast(
            fetchedAt = System.currentTimeMillis(),
            timeZone = o.optString("timezone", "UTC"),
            current = Forecast.Current(
                temperature = c.getDouble("temperature_2m"),
                feelsLike = c.getDouble("apparent_temperature"),
                code = c.getInt("weather_code"),
                isDay = c.getInt("is_day") == 1,
            ),
            hours = hours,
            days = days,
        )
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    private suspend fun get(url: String): String = withContext(Dispatchers.IO) {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 10_000
            conn.readTimeout = 15_000
            conn.setRequestProperty("User-Agent", "WidgetsPlus (Android)")
            conn.setRequestProperty("Accept", "application/json")
            val status = conn.responseCode
            if (status !in 200..299) throw IOException("HTTP $status from ${URL(url).host}")
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }
}
