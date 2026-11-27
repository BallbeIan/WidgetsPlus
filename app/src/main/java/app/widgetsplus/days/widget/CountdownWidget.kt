package app.widgetsplus.days.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import app.widgetsplus.R
import app.widgetsplus.core.ModuleWidgetReceiver
import app.widgetsplus.core.widget.EmptyMessage
import app.widgetsplus.core.widget.WidgetSurface
import app.widgetsplus.core.widget.openAppIntent
import app.widgetsplus.days.DayEvent
import app.widgetsplus.days.DaysFormat
import app.widgetsplus.days.DaysModule
import app.widgetsplus.days.DaysRepository
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.min

/** One event: a big number of days, "days to go" or "days ago", the event's name and date. */
class CountdownWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = DaysRepository.get(context)
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        provideContent {
            // Collected so the widget redraws when events, its choice or the day change.
            val s by repo.state.collectAsState()
            val today = LocalDate.now()
            GlanceTheme { Content(repo.eventForWidget(appWidgetId, s, today), today) }
        }
    }

    @Composable
    private fun Content(event: DayEvent?, today: LocalDate) {
        val context = LocalContext.current
        val size = LocalSize.current
        val openApp = actionStartActivity(openAppIntent(context, DaysModule.id))

        WidgetSurface(GlanceModifier.padding(horizontal = 10.dp, vertical = 8.dp), onClick = openApp) {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (event == null) {
                    EmptyMessage(context.getString(R.string.days_widget_empty))
                } else {
                    Countdown(context, event, today, size.width.value, size.height.value)
                }
            }
        }
    }

    @Composable
    private fun Countdown(context: Context, event: DayEvent, today: LocalDate, w: Float, h: Float) {
        val days = event.daysUntil(today)
        val isToday = days == 0L
        val big = if (isToday) context.getString(R.string.days_today) else DaysFormat.number(abs(days))
        val label = when {
            isToday -> null
            days > 0 -> context.resources.getQuantityString(R.plurals.days_to_go_label, abs(days).toInt())
            else -> context.resources.getQuantityString(R.plurals.days_ago_label, abs(days).toInt())
        }

        // Which lines fit, then the largest number that fits the rest. Line heights are in dp, roughly 1.3 x text size.
        val showName = h >= 96
        val showDate = showName && h >= 128
        val reserved = 16f + (if (label != null) 16f else 0f) + (if (showName) 20f else 0f) + (if (showDate) 16f else 0f)
        val byHeight = (h - reserved) / 1.2f
        val byWidth = (w - 20f) / (0.6f * big.length.coerceAtLeast(1))
        // Glance text sizes are in sp; undo the phone's font scale, since the size is already fitted to the widget.
        val bigSize = min(byHeight, byWidth).coerceIn(16f, 96f) / context.resources.configuration.fontScale

        Text(
            big,
            maxLines = 1,
            style = TextStyle(
                color = GlanceTheme.colors.primary,
                fontSize = bigSize.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            ),
        )
        if (label != null) {
            Text(
                label,
                maxLines = 1,
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp, textAlign = TextAlign.Center),
            )
        }
        if (showName) {
            Text(
                event.name,
                maxLines = 1,
                style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                modifier = GlanceModifier.padding(top = 4.dp),
            )
        }
        if (showDate) {
            Text(
                DaysFormat.date(event.target(today)),
                maxLines = 1,
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp, textAlign = TextAlign.Center),
            )
        }
    }
}

class CountdownWidgetReceiver : ModuleWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CountdownWidget()
}
