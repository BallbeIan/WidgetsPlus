package app.widgetsplus.core

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Per-widget choices for one module (which place, which time zone, ...), in their own SharedPreferences file.
 * Widgets read these while composing, so [version] goes up on every change: collect it in the widget so a
 * running widget redraws when its choice changes.
 */
class WidgetSettings(context: Context, fileName: String) {

    private val prefs = context.applicationContext.getSharedPreferences(fileName, Context.MODE_PRIVATE)
    private val _version = MutableStateFlow(0)
    val version: StateFlow<Int> = _version.asStateFlow()

    fun get(appWidgetId: Int, key: String): String? = prefs.getString("$appWidgetId.$key", null)

    fun set(appWidgetId: Int, vararg values: Pair<String, String?>) {
        val edit = prefs.edit()
        values.forEach { (key, value) ->
            if (value == null) edit.remove("$appWidgetId.$key") else edit.putString("$appWidgetId.$key", value)
        }
        edit.apply()
        _version.value++
    }

    fun forget(appWidgetId: Int) {
        val edit = prefs.edit()
        prefs.all.keys.filter { it.startsWith("$appWidgetId.") }.forEach { edit.remove(it) }
        edit.apply()
    }
}
