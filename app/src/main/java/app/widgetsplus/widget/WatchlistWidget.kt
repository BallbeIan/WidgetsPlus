package app.widgetsplus.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import app.widgetsplus.R
import app.widgetsplus.data.Format
import app.widgetsplus.data.Quote
import app.widgetsplus.data.Repository
import app.widgetsplus.data.Snapshot
import app.widgetsplus.data.WatchItem

/** The whole watchlist as a scrolling list. Small charts appear when the widget is wide enough. */
class WatchlistWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = Repository.get(context)
        provideContent {
            val s by repo.state.collectAsState()
            GlanceTheme { Content(s) }
        }
    }

    @Composable
    private fun Content(s: Snapshot) {
        val context = LocalContext.current
        val showCharts = LocalSize.current.width >= 260.dp

        Column(
            modifier = GlanceModifier.fillMaxSize()
                .appWidgetBackground()
                .cornerRadius(20.dp)
                .background(GlanceTheme.colors.widgetBackground)
                .padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 6.dp),
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth().padding(end = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    context.getString(R.string.watchlist),
                    maxLines = 1,
                    style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold),
                    modifier = GlanceModifier.clickable(actionStartActivity(openAppIntent(context))),
                )
                Spacer(GlanceModifier.defaultWeight())
                Text(
                    updatedText(context, s),
                    maxLines = 1,
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp),
                )
                RefreshButton(s.refreshing)
            }
            if (s.items.isEmpty()) {
                Text(
                    context.getString(R.string.widget_empty),
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp),
                    modifier = GlanceModifier.padding(top = 8.dp).clickable(actionStartActivity(openAppIntent(context))),
                )
            } else {
                LazyColumn(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                    items(s.items, itemId = { it.key.hashCode().toLong() }) { item ->
                        ItemRow(item, s.quotes[item.key], showCharts)
                    }
                }
            }
        }
    }

    @Composable
    private fun ItemRow(item: WatchItem, quote: Quote?, showChart: Boolean) {
        val context = LocalContext.current
        Row(
            modifier = GlanceModifier.fillMaxWidth()
                .padding(top = 5.dp, bottom = 5.dp, end = 6.dp)
                .clickable(actionStartActivity(openPageIntent(item))),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    item.label,
                    maxLines = 1,
                    style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold),
                )
                Text(
                    item.name,
                    maxLines = 1,
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp),
                )
            }
            if (showChart && quote != null) {
                sparkline(context, quote.chart, quote.reference, 64f, 26f)?.let {
                    Image(ImageProvider(it), contentDescription = null, modifier = GlanceModifier.width(64.dp).height(26.dp))
                    Spacer(GlanceModifier.width(10.dp))
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    quote?.let { Format.price(it.price, it.currency) } ?: "—",
                    maxLines = 1,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.End,
                    ),
                )
                if (quote != null) {
                    Text(
                        Format.percent(quote.changePercent),
                        maxLines = 1,
                        style = TextStyle(color = changeColor(quote.change), fontSize = 12.sp, textAlign = TextAlign.End),
                    )
                }
            }
        }
    }
}

class WatchlistWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WatchlistWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        RefreshWorker.schedule(context)
        RefreshWorker.refreshSoon(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        RefreshWorker.cancelIfNoWidgets(context)
    }
}
