package app.widgetsplus.days

import android.content.Context
import android.content.SharedPreferences
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray

/**
 * The Days module's events and per-widget choices, kept in their own SharedPreferences file "days".
 * Shared by the app screens, the widgets and the background refresh, which all run in the same process.
 */
class DaysRepository private constructor(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("days", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(DaysState(load()))
    val state: StateFlow<DaysState> = _state.asStateFlow()

    private fun load(): List<DayEvent> = prefs.getString(KEY_EVENTS, null)?.let { json ->
        runCatching { JSONArray(json).let { a -> List(a.length()) { DayEvent.fromJson(a.getJSONObject(it)) } } }.getOrNull()
    } ?: emptyList()

    private suspend fun change(block: (List<DayEvent>) -> List<DayEvent>) {
        _state.update { it.copy(events = block(it.events)) }
        prefs.edit().putString(KEY_EVENTS, JSONArray(_state.value.events.map { it.toJson() }).toString()).apply()
        DaysModule.updateWidgets(context)
    }

    /** Creates an event and returns its id. */
    suspend fun addEvent(name: String, date: LocalDate, repeatsYearly: Boolean): String {
        val id = UUID.randomUUID().toString().take(8)
        change { it + DayEvent(id, name.trim(), date, repeatsYearly) }
        return id
    }

    suspend fun deleteEvent(id: String) = change { events -> events.filterNot { it.id == id } }

    /** Makes running widgets recompute, for example after the date changed. */
    fun touch() = _state.update { it.copy(version = it.version + 1) }

    // Widget settings

    /** The event a countdown widget shows: the one picked for it, else the first (soonest) event. */
    fun eventForWidget(appWidgetId: Int, s: DaysState = _state.value, today: LocalDate = LocalDate.now()): DayEvent? {
        val id = prefs.getString("$KEY_COUNTDOWN_WIDGET$appWidgetId", null)
        return s.events.firstOrNull { it.id == id } ?: s.sorted(today).firstOrNull()
    }

    fun setEventForWidget(appWidgetId: Int, eventId: String) {
        prefs.edit().putString("$KEY_COUNTDOWN_WIDGET$appWidgetId", eventId).apply()
        touch()
    }

    fun forgetWidget(appWidgetId: Int) = prefs.edit().remove("$KEY_COUNTDOWN_WIDGET$appWidgetId").apply()

    companion object {
        private const val KEY_EVENTS = "events"
        private const val KEY_COUNTDOWN_WIDGET = "countdownWidget."

        @Volatile private var instance: DaysRepository? = null

        fun get(context: Context): DaysRepository =
            instance ?: synchronized(this) {
                instance ?: DaysRepository(context.applicationContext).also { instance = it }
            }
    }
}
