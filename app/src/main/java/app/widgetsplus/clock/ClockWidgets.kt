package app.widgetsplus.clock

import android.content.Context
import android.text.format.DateFormat
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import app.widgetsplus.R
import app.widgetsplus.core.ModuleWidgetReceiver
import app.widgetsplus.core.widget.WidgetSurface
import java.time.ZoneId
import java.util.Locale
import kotlin.math.min

/**
 * Digital clock with the date, in the phone's time zone or a chosen one (with a label, for a world clock).
 * The clock itself is a TextClock inside Glance, because Glance has no ticking text of its own.
 */
class DigitalClockWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val settings = ClockModule.settings(context)
        provideContent {
            // Read so the widget redraws when its time zone or label is changed.
            val version by settings.version.collectAsState()
            GlanceTheme { Content(appWidgetId, version) }
        }
    }

    @Composable
    private fun Content(appWidgetId: Int, @Suppress("UNUSED_PARAMETER") version: Int) {
        val context = LocalContext.current
        val size = LocalSize.current
        val zone = ClockModule.zoneFor(context, appWidgetId)
        val label = ClockModule.labelFor(context, appWidgetId) ?: zone?.let { ClockModule.cityName(it) }

        WidgetSurface(GlanceModifier.padding(8.dp), onClick = actionStartActivity(ClockModule.clockAppIntent(context))) {
            AndroidRemoteViews(
                remoteViews = digitalViews(context, zone, label, size.width.value, size.height.value),
                modifier = GlanceModifier.fillMaxSize(),
            )
        }
    }

    private fun digitalViews(context: Context, zone: ZoneId?, label: String?, widthDp: Float, heightDp: Float): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.clock_digital)
        val showDate = heightDp >= 80
        val showLabel = label != null && heightDp >= 70
        // Largest time that fits: "12:59" is about 2.7 times as wide as the text size, and needs room for the other lines.
        val spare = (if (showDate) 24 else 0) + (if (showLabel) 20 else 0) + 16
        val timeSize = min((widthDp - 24) / 2.7f, (heightDp - spare) * 0.8f).coerceIn(20f, 110f)
        // dp, not sp: the size is already fitted to the widget, so the phone's font scale must not enlarge it again.
        views.setTextViewTextSize(R.id.clock_time, TypedValue.COMPLEX_UNIT_DIP, timeSize)

        val datePattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), if (widthDp >= 200) "EEEEMMMMd" else "EEEMMMd")
        views.setCharSequence(R.id.clock_date, "setFormat12Hour", datePattern)
        views.setCharSequence(R.id.clock_date, "setFormat24Hour", datePattern)
        views.setViewVisibility(R.id.clock_date, if (showDate) View.VISIBLE else View.GONE)

        // Always set, also to null (the phone's zone): on Android 12+ the launcher may update the existing
        // TextClock in place, which would otherwise keep a previously chosen city.
        views.setString(R.id.clock_time, "setTimeZone", zone?.id)
        views.setString(R.id.clock_date, "setTimeZone", zone?.id)
        views.setViewVisibility(R.id.clock_label, if (showLabel) View.VISIBLE else View.GONE)
        if (showLabel) views.setTextViewText(R.id.clock_label, label)
        return views
    }
}

/** An analog clock face in the phone's time zone, coloured to match the theme. */
class AnalogClockWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val context = LocalContext.current
            Box(
                modifier = GlanceModifier.fillMaxSize().padding(4.dp).clickable(actionStartActivity(ClockModule.clockAppIntent(context))),
            ) {
                AndroidRemoteViews(RemoteViews(context.packageName, R.layout.clock_analog), GlanceModifier.fillMaxSize())
            }
        }
    }
}

class DigitalClockWidgetReceiver : ModuleWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DigitalClockWidget()
}

class AnalogClockWidgetReceiver : ModuleWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AnalogClockWidget()
}
