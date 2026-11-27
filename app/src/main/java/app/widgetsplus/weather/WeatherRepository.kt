package app.widgetsplus.weather

import android.content.Context
import android.content.SharedPreferences
import java.util.Locale
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.json.JSONArray
import org.json.JSONObject

/** Saved places, their last forecasts, the temperature unit and each widget's place, in the `weather` prefs file. */
class WeatherRepository private constructor(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("weather", Context.MODE_PRIVATE)
    private val refreshLock = Mutex()

    private val _state = MutableStateFlow(load())
    val state: StateFlow<WeatherState> = _state.asStateFlow()

    private fun load(): WeatherState {
        val places = prefs.getString(KEY_PLACES, null)?.let { json ->
            JSONArray(json).let { a -> List(a.length()) { Place.fromJson(a.getJSONObject(it)) } }
        } ?: emptyList()
        val forecasts = prefs.getString(KEY_FORECASTS, null)?.let { json ->
            val o = JSONObject(json)
            o.keys().asSequence().mapNotNull { k -> runCatching { k to Forecast.fromJson(o.getJSONObject(k)) }.getOrNull() }.toMap()
        } ?: emptyMap()
        val unit = prefs.getString(KEY_UNIT, null)?.let { runCatching { TemperatureUnit.valueOf(it) }.getOrNull() } ?: defaultUnit()
        return WeatherState(
            places = places,
            forecasts = forecasts,
            unit = unit,
            lastRefresh = prefs.getLong(KEY_LAST_REFRESH, 0),
            lastRefreshFailed = prefs.getBoolean(KEY_LAST_FAILED, false),
            refreshing = false,
        )
    }

    /** Fahrenheit in the countries that use it, Celsius everywhere else. */
    private fun defaultUnit() =
        if (Locale.getDefault().country in setOf("US", "LR", "MM", "BS", "BZ", "KY", "PW")) TemperatureUnit.FAHRENHEIT
        else TemperatureUnit.CELSIUS

    private fun save(s: WeatherState) {
        val ids = s.places.map { it.id }.toSet()
        prefs.edit()
            .putString(KEY_PLACES, JSONArray(s.places.map { it.toJson() }).toString())
            .putString(KEY_FORECASTS, JSONObject().apply { s.forecasts.filterKeys { it in ids }.forEach { (k, f) -> put(k, f.toJson()) } }.toString())
            .putString(KEY_UNIT, s.unit.name)
            .putLong(KEY_LAST_REFRESH, s.lastRefresh)
            .putBoolean(KEY_LAST_FAILED, s.lastRefreshFailed)
            .apply()
    }

    private suspend fun change(block: (WeatherState) -> WeatherState) {
        _state.update(block)
        save(_state.value)
        WeatherModule.updateWidgets(context)
    }

    /** Adds a place from [OpenMeteo.search] and, if [fetch], waits for its forecast. */
    suspend fun addPlace(place: Place, fetch: Boolean = true) {
        change { s -> if (s.places.any { it.id == place.id }) s else s.copy(places = s.places + place) }
        if (!fetch) return
        runCatching { OpenMeteo.forecast(place) }.getOrNull()?.let { f ->
            change { s -> s.copy(forecasts = s.forecasts + (place.id to f)) }
        }
    }

    suspend fun removePlace(id: String) = change { s -> s.copy(places = s.places.filterNot { it.id == id }, forecasts = s.forecasts - id) }

    suspend fun movePlace(id: String, by: Int) = change { s ->
        val from = s.places.indexOfFirst { it.id == id }
        val to = from + by
        if (from < 0 || to !in s.places.indices) s
        else s.copy(places = s.places.toMutableList().apply { add(to, removeAt(from)) })
    }

    suspend fun setUnit(unit: TemperatureUnit) = change { it.copy(unit = unit) }

    /** Fetches every place's forecast. A place that fails keeps its previous forecast. */
    suspend fun refresh(): Boolean {
        if (!refreshLock.tryLock()) return true
        try {
            _state.update { it.copy(refreshing = true) }
            WeatherModule.updateWidgets(context)
            val places = _state.value.places
            val limit = Semaphore(3)
            val fetched = coroutineScope {
                places.map { p -> async { p.id to limit.withPermit { runCatching { OpenMeteo.forecast(p) }.getOrNull() } } }.awaitAll()
            }.mapNotNull { (id, f) -> f?.let { id to it } }.toMap()
            val failed = places.isNotEmpty() && fetched.isEmpty()
            change { s ->
                s.copy(
                    forecasts = s.forecasts + fetched,
                    lastRefresh = if (failed) s.lastRefresh else System.currentTimeMillis(),
                    lastRefreshFailed = failed,
                    refreshing = false,
                )
            }
            return !failed
        } finally {
            if (_state.value.refreshing) _state.update { it.copy(refreshing = false) }
            refreshLock.unlock()
        }
    }

    /** The place a widget shows: the one chosen for it, else the first place. */
    fun placeForWidget(appWidgetId: Int, s: WeatherState = _state.value): Place? {
        val id = prefs.getString("$KEY_WIDGET$appWidgetId", null)
        return s.places.firstOrNull { it.id == id } ?: s.places.firstOrNull()
    }

    fun setPlaceForWidget(appWidgetId: Int, placeId: String) {
        prefs.edit().putString("$KEY_WIDGET$appWidgetId", placeId).apply()
        _state.update { it.copy(widgetSettingsVersion = it.widgetSettingsVersion + 1) }
    }

    fun forgetWidget(appWidgetId: Int) = prefs.edit().remove("$KEY_WIDGET$appWidgetId").apply()

    companion object {
        private const val KEY_PLACES = "places"
        private const val KEY_FORECASTS = "forecasts"
        private const val KEY_UNIT = "unit"
        private const val KEY_LAST_REFRESH = "lastRefresh"
        private const val KEY_LAST_FAILED = "lastRefreshFailed"
        private const val KEY_WIDGET = "widget."

        @Volatile private var instance: WeatherRepository? = null

        fun get(context: Context): WeatherRepository =
            instance ?: synchronized(this) {
                instance ?: WeatherRepository(context.applicationContext).also { instance = it }
            }
    }
}
