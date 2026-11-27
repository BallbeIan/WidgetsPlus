package app.widgetsplus.core

import android.content.Context
import androidx.annotation.StringRes
import app.widgetsplus.R
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject

/**
 * How a widget looks, the same options for every widget: background colour and opacity, corner roundness and
 * text colour. Stored per widget; widgets that were never customised use their kind's default.
 */
data class WidgetStyle(
    /** The overall look. Soft is the default; Classic is the plain look from before 1.0. */
    val design: Design = Design.SOFT,
    /** A key from [WidgetBackgrounds]. [WidgetBackgrounds.SYSTEM] follows the phone's colours and dark mode. */
    val background: String = WidgetBackgrounds.SYSTEM,
    /** 0 (fully transparent) to 100. */
    val opacity: Int = 100,
    /** Corner radius in dp. Android 12 and newer only; older versions always draw square corners. */
    val corners: Int = DEFAULT_CORNERS,
    val text: TextTone = TextTone.AUTO,
) {
    fun toJson(): String = JSONObject()
        .put("design", design.name).put("bg", background).put("opacity", opacity).put("corners", corners).put("text", text.name)
        .toString()

    companion object {
        const val DEFAULT_CORNERS = 28
        const val MAX_CORNERS = 40

        fun fromJson(json: String): WidgetStyle? = runCatching {
            val o = JSONObject(json)
            WidgetStyle(
                design = runCatching { Design.valueOf(o.optString("design")) }.getOrDefault(Design.SOFT),
                background = o.optString("bg", WidgetBackgrounds.SYSTEM).takeIf { WidgetBackgrounds.byKey(it) != null } ?: WidgetBackgrounds.SYSTEM,
                opacity = o.optInt("opacity", 100).coerceIn(0, 100),
                corners = o.optInt("corners", DEFAULT_CORNERS).coerceIn(0, MAX_CORNERS),
                text = runCatching { TextTone.valueOf(o.optString("text")) }.getOrDefault(TextTone.AUTO),
            )
        }.getOrNull()
    }
}

/** The widget designs. Each one draws every widget type in its own way; the other style options apply to all. */
enum class Design(@StringRes val label: Int, @StringRes val description: Int) {
    /** Soft, tactile panels pressed out of a pale material: raised panels, sunken wells, a rounded typeface. */
    SOFT(R.string.design_soft, R.string.design_soft_description),
    /** Flat cards in the phone's own colours. */
    CLASSIC(R.string.design_classic, R.string.design_classic_description),
}

enum class TextTone(@StringRes val label: Int) {
    /** Readable on the chosen background (the phone's text colours for [WidgetBackgrounds.SYSTEM]). */
    AUTO(R.string.style_text_auto),
    LIGHT(R.string.style_text_light),
    DARK(R.string.style_text_dark),
}

/** The background colours a widget can have. */
object WidgetBackgrounds {
    const val SYSTEM = "system"

    data class Background(val key: String, @StringRes val label: Int, val argb: Long?)

    val all = listOf(
        Background(SYSTEM, R.string.style_bg_system, null),
        Background("white", R.string.style_bg_white, 0xFFFFFFFF),
        Background("mist", R.string.style_bg_mist, 0xFFE8E8EE),
        Background("sand", R.string.style_bg_sand, 0xFFF2E8D8),
        Background("graphite", R.string.style_bg_graphite, 0xFF1C1C1E),
        Background("black", R.string.style_bg_black, 0xFF000000),
        Background("navy", R.string.style_bg_navy, 0xFF13233F),
        Background("indigo", R.string.style_bg_indigo, 0xFF2B2A6B),
        Background("forest", R.string.style_bg_forest, 0xFF123526),
        Background("wine", R.string.style_bg_wine, 0xFF4A1C26),
        Background("plum", R.string.style_bg_plum, 0xFF3A2150),
    )

    fun byKey(key: String) = all.firstOrNull { it.key == key }
}

/** Reads and writes each widget's style. */
object WidgetStyles {
    private const val KEY = "style"
    @Volatile private var settings: WidgetSettings? = null

    private fun settings(context: Context) = settings ?: synchronized(this) {
        settings ?: WidgetSettings(context, "widget_style").also { settings = it }
    }

    /** Goes up on every change; widgets collect it so they redraw straight away. */
    fun version(context: Context): StateFlow<Int> = settings(context).version

    /** The widget's own style, or null if it was never customised. */
    fun own(context: Context, appWidgetId: Int): WidgetStyle? =
        settings(context).get(appWidgetId, KEY)?.let { WidgetStyle.fromJson(it) }

    /** What the widget looks like: its own style, else its kind's default. */
    fun of(context: Context, appWidgetId: Int): WidgetStyle =
        own(context, appWidgetId) ?: defaultFor(context, appWidgetId)

    fun defaultFor(context: Context, appWidgetId: Int): WidgetStyle =
        Modules.kindOf(context, appWidgetId)?.defaultStyle ?: WidgetStyle()

    fun set(context: Context, appWidgetId: Int, style: WidgetStyle) = settings(context).set(appWidgetId, KEY to style.toJson())

    /** Back to the kind's default look. */
    fun reset(context: Context, appWidgetId: Int) = settings(context).set(appWidgetId, KEY to null)

    fun forget(context: Context, appWidgetId: Int) = settings(context).forget(appWidgetId)
}
