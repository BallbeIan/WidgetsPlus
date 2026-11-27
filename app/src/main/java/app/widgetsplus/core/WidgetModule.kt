package app.widgetsplus.core

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import app.widgetsplus.finance.FinanceModule

/**
 * One category of widgets (finance, later weather, exercise, ...). A module owns its data, its settings screen,
 * its widgets and how they refresh. To add a category: implement this, add it to [Modules.all], and declare its
 * widget receivers and configure activity in the manifest.
 */
interface WidgetModule {
    /** Stable id, used for navigation and storage. Never change it once released. */
    val id: String
    @get:StringRes val title: Int
    @get:StringRes val description: Int
    @get:DrawableRes val icon: Int

    /** The widget types this module offers in the launcher's widget picker. */
    val widgets: List<WidgetKind>

    /** One line for the home screen, e.g. "2 lists, 6 symbols". */
    @Composable fun summary(): String

    /** The module's own settings screen inside the app. */
    @Composable fun Screen(onBack: () -> Unit)

    /** Fetches fresh data. Returns false if nothing could be fetched. */
    suspend fun refresh(context: Context): Boolean

    /** Redraws all of this module's widgets from stored data. */
    suspend fun updateWidgets(context: Context)

    /** What a placed widget shows, e.g. "Crypto" or "BTC". */
    fun describeWidget(context: Context, appWidgetId: Int, kind: WidgetKind): String

    /** Called when a widget is removed, to delete its settings. */
    fun forgetWidget(context: Context, appWidgetId: Int)

    /** Whether any of this module's widgets are on the home screen. */
    fun hasWidgets(context: Context): Boolean = widgets.any { it.placedIds(context).isNotEmpty() }
}

/** One widget type, as listed in the launcher's widget picker. */
data class WidgetKind(
    val receiver: Class<out GlanceAppWidgetReceiver>,
    @StringRes val name: Int,
    @StringRes val description: Int,
    @DrawableRes val icon: Int,
    /** Opened to choose what the widget shows. Null if the widget has nothing to choose. */
    val configure: Class<out Activity>?,
) {
    fun component(context: Context) = ComponentName(context, receiver)
    fun placedIds(context: Context): IntArray = AppWidgetManager.getInstance(context).getAppWidgetIds(component(context))
}

/** A widget currently on the home screen. */
data class PlacedWidget(val appWidgetId: Int, val module: WidgetModule, val kind: WidgetKind)

object Modules {
    val all: List<WidgetModule> = listOf(FinanceModule)

    fun byId(id: String): WidgetModule? = all.firstOrNull { it.id == id }

    fun placedWidgets(context: Context): List<PlacedWidget> =
        all.flatMap { module -> module.widgets.flatMap { kind -> kind.placedIds(context).map { PlacedWidget(it, module, kind) } } }

    fun moduleForReceiver(receiver: Class<*>): WidgetModule? = all.firstOrNull { m -> m.widgets.any { it.receiver == receiver } }
}
