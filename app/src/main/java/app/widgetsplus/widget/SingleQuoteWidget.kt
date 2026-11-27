package app.widgetsplus.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import app.widgetsplus.R
import app.widgetsplus.data.Format
import app.widgetsplus.data.Repository
import app.widgetsplus.data.Snapshot
import app.widgetsplus.data.WatchItem

/** One symbol: price, change and today's chart. The symbol is picked when the widget is added. */
class SingleQuoteWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = Repository.get(context)
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        provideContent {
            val s by repo.state.collectAsState()
            GlanceTheme { Content(s, repo.itemForWidget(appWidgetId, s)) }
        }
    }

    @Composable
    private fun Content(s: Snapshot, item: WatchItem?) {
        val context = LocalContext.current
        val size = LocalSize.current
        val quote = item?.let { s.quotes[it.key] }
        val tap = if (item != null) actionStartActivity(openPageIntent(item)) else actionStartActivity(openAppIntent(context))

        Column(
            modifier = GlanceModifier.fillMaxSize()
                .appWidgetBackground()
                .cornerRadius(20.dp)
                .background(GlanceTheme.colors.widgetBackground)
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .clickable(tap),
        ) {
            if (item == null) {
                Text(
                    context.getString(R.string.widget_empty),
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp),
                )
                return@Column
            }
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.label,
                    maxLines = 1,
                    style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold),
                    modifier = GlanceModifier.defaultWeight(),
                )
                if (quote != null) {
                    Text(
                        Format.percent(quote.changePercent),
                        maxLines = 1,
                        style = TextStyle(color = changeColor(quote.change), fontSize = 13.sp, fontWeight = FontWeight.Medium),
                    )
                }
            }
            if (size.height >= 100.dp) {
                Text(
                    item.name,
                    maxLines = 1,
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
                )
            }
            Text(
                quote?.let { Format.price(it.price, it.currency) } ?: "—",
                maxLines = 1,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = if (size.width >= 150.dp) 24.sp else 20.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )

            val showFooter = size.height >= 170.dp
            val chartHeight = size.height.value - 24 - 20 - 32 - (if (size.height >= 100.dp) 16 else 0) - (if (showFooter) 24 else 0)
            val chart = if (quote != null && chartHeight >= 28) {
                sparkline(context, quote.chart, quote.reference, size.width.value - 28, chartHeight - 8)
            } else null
            if (chart != null) {
                Spacer(GlanceModifier.height(8.dp))
                Image(
                    provider = ImageProvider(chart),
                    contentDescription = null,
                    modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                )
            } else {
                Spacer(GlanceModifier.defaultWeight())
            }

            if (showFooter) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth().padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        updatedText(context, s),
                        maxLines = 1,
                        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp),
                        modifier = GlanceModifier.defaultWeight(),
                    )
                    RefreshButton(s.refreshing)
                }
            }
        }
    }
}

@Composable
fun RefreshButton(refreshing: Boolean) {
    Box(
        modifier = GlanceModifier.size(28.dp).cornerRadius(14.dp).clickable(actionRunCallback<RefreshAction>()),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            provider = ImageProvider(if (refreshing) R.drawable.ic_refreshing else R.drawable.ic_refresh),
            contentDescription = LocalContext.current.getString(R.string.refresh),
            colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurfaceVariant),
            modifier = GlanceModifier.size(18.dp),
        )
    }
}

fun updatedText(context: Context, s: Snapshot): String = when {
    s.refreshing -> context.getString(R.string.updating)
    s.lastRefresh == 0L -> context.getString(R.string.not_updated_yet)
    s.lastRefreshFailed -> context.getString(R.string.offline_since, Format.time(context, s.lastRefresh))
    else -> Format.time(context, s.lastRefresh)
}

class SingleQuoteWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SingleQuoteWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        RefreshWorker.schedule(context)
        RefreshWorker.refreshSoon(context)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        val repo = Repository.get(context)
        appWidgetIds.forEach { repo.forgetWidget(it) }
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        RefreshWorker.cancelIfNoWidgets(context)
    }
}
