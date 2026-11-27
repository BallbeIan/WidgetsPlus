package app.widgetsplus.clock

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.glance.appwidget.updateAll
import app.widgetsplus.R
import app.widgetsplus.core.WidgetKind
import app.widgetsplus.core.WidgetModule
import app.widgetsplus.core.WidgetSettings
import app.widgetsplus.core.ui.HelpText
import app.widgetsplus.core.ui.ModuleScaffold
import app.widgetsplus.core.ui.ModuleWidgetsCard
import app.widgetsplus.core.ui.SectionTitle
import app.widgetsplus.core.widget.openAppIntent
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.TextStyle
import java.util.Locale

/** Digital and analog clocks. They tick by themselves, so there is nothing to fetch or refresh. */
object ClockModule : WidgetModule {
    override val id = "clock"
    override val title = R.string.clock_title
    override val description = R.string.clock_description
    override val icon = R.drawable.ic_module_clock

    val digital = WidgetKind(DigitalClockWidgetReceiver::class.java, R.string.widget_digital_clock_name, R.string.widget_digital_clock_description, R.drawable.ic_module_clock, ClockConfigureActivity::class.java)
    val analog = WidgetKind(AnalogClockWidgetReceiver::class.java, R.string.widget_analog_clock_name, R.string.widget_analog_clock_description, R.drawable.ic_widget_analog, null)
    override val widgets = listOf(digital, analog)

    private const val KEY_ZONE = "zone"
    private const val KEY_LABEL = "label"
    @Volatile private var settings: WidgetSettings? = null
    fun settings(context: Context) = settings ?: synchronized(this) {
        settings ?: WidgetSettings(context, "clock").also { settings = it }
    }

    /** The time zone a digital clock shows, or null for the phone's own time. */
    fun zoneFor(context: Context, appWidgetId: Int): ZoneId? =
        settings(context).get(appWidgetId, KEY_ZONE)?.let { runCatching { ZoneId.of(it) }.getOrNull() }

    fun labelFor(context: Context, appWidgetId: Int): String? = settings(context).get(appWidgetId, KEY_LABEL)?.ifBlank { null }

    fun setClock(context: Context, appWidgetId: Int, zone: ZoneId?, label: String?) =
        settings(context).set(appWidgetId, KEY_ZONE to zone?.id, KEY_LABEL to label?.trim()?.ifEmpty { null })

    /** "Tokyo" for Asia/Tokyo, "New York" for America/New_York. */
    fun cityName(zone: ZoneId): String = zone.id.substringAfterLast('/').replace('_', ' ')

    /** "UTC+9", "UTC−3:30". */
    fun offsetText(zone: ZoneId): String {
        val seconds = ZonedDateTime.now(zone).offset.totalSeconds
        if (seconds == 0) return "UTC"
        val sign = if (seconds > 0) "+" else "−"
        val h = kotlin.math.abs(seconds) / 3600
        val m = kotlin.math.abs(seconds) % 3600 / 60
        return "UTC$sign$h" + if (m != 0) ":%02d".format(m) else ""
    }

    fun zoneLongName(zone: ZoneId): String = zone.getDisplayName(TextStyle.FULL, Locale.getDefault())

    /** Tapping a clock opens the phone's clock app, or WidgetsPlus if there is none. */
    fun clockAppIntent(context: Context): Intent {
        val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return if (intent.resolveActivity(context.packageManager) != null) intent else openAppIntent(context, id)
    }

    @Composable
    override fun summary(): String = stringResource(R.string.clock_summary)

    @Composable
    override fun Screen(onBack: () -> Unit) {
        val context = LocalContext.current
        ModuleScaffold(stringResource(R.string.clock_title), onBack) {
            item { HelpText(stringResource(R.string.clock_help)) }
            item { SectionTitle(stringResource(R.string.widgets_title)) }
            item { ModuleWidgetsCard(ClockModule) }
            item { HelpText(stringResource(R.string.clock_widgets_help, context.getString(R.string.on_home_screen))) }
        }
    }

    override suspend fun refresh(context: Context) = true

    override suspend fun updateWidgets(context: Context) {
        DigitalClockWidget().updateAll(context)
        AnalogClockWidget().updateAll(context)
    }

    override fun describeWidget(context: Context, appWidgetId: Int, kind: WidgetKind): String {
        if (kind == analog) return context.getString(R.string.local_time)
        val zone = zoneFor(context, appWidgetId)
        val name = labelFor(context, appWidgetId) ?: zone?.let { cityName(it) } ?: context.getString(R.string.local_time)
        return if (zone != null) "$name · ${offsetText(zone)}" else name
    }

    override fun forgetWidget(context: Context, appWidgetId: Int) = settings(context).forget(appWidgetId)
}
