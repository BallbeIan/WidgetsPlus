package app.widgetsplus.days.widget

import app.widgetsplus.core.widget.StyledWidget
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
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import app.widgetsplus.core.ModuleWidgetReceiver
import app.widgetsplus.core.widget.WidgetSurface
import app.widgetsplus.core.widget.openAppIntent
import app.widgetsplus.days.DaysModule
import app.widgetsplus.days.DaysRepository
import app.widgetsplus.days.Period
import java.time.ZonedDateTime

/** How far through this year, month, week and today we are, as bars. Shows as many rows as fit, year first. */
class ProgressWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = DaysRepository.get(context)
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        provideContent {
            // Collected only so the widget recomputes when the module redraws its widgets.
            val s by repo.state.collectAsState()
            StyledWidget(appWidgetId) { Content(s.version) }
        }
    }

    @Composable
    private fun Content(@Suppress("UNUSED_PARAMETER") version: Int) {
        val context = LocalContext.current
        val h = LocalSize.current.height.value
        val now = ZonedDateTime.now()
        // Each row is about 26dp (label and bar) plus a gap; the widget has 20dp of padding.
        val rows = when {
            h >= 136 -> 4
            h >= 104 -> 3
            h >= 72 -> 2
            else -> 1
        }
        val periods = Period.entries.take(rows)

        WidgetSurface(
            GlanceModifier.padding(horizontal = 14.dp, vertical = 10.dp),
            onClick = actionStartActivity(openAppIntent(context, DaysModule.id)),
        ) {
            Spacer(GlanceModifier.defaultWeight())
            periods.forEachIndexed { i, period ->
                if (i > 0) Spacer(GlanceModifier.defaultWeight())
                ProgressRow(context.getString(period.label), period.progress(now))
            }
            Spacer(GlanceModifier.defaultWeight())
        }
    }

    @Composable
    private fun ProgressRow(label: String, progress: Float) {
        Column(GlanceModifier.fillMaxWidth()) {
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    label,
                    maxLines = 1,
                    style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Medium),
                    modifier = GlanceModifier.defaultWeight(),
                )
                Text(
                    Period.percentText(progress),
                    maxLines = 1,
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp, textAlign = TextAlign.End),
                )
            }
            Spacer(GlanceModifier.height(3.dp))
            LinearProgressIndicator(
                progress = progress,
                modifier = GlanceModifier.fillMaxWidth().height(6.dp).cornerRadius(3.dp),
                color = GlanceTheme.colors.primary,
                backgroundColor = GlanceTheme.colors.secondaryContainer,
            )
        }
    }
}

class ProgressWidgetReceiver : ModuleWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ProgressWidget()
}
