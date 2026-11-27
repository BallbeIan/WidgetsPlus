package app.widgetsplus.finance.widget

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
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import app.widgetsplus.R
import app.widgetsplus.core.ModuleWidgetReceiver
import app.widgetsplus.core.widget.EmptyMessage
import app.widgetsplus.core.widget.RefreshAction
import app.widgetsplus.core.widget.WidgetHeader
import app.widgetsplus.core.widget.WidgetSurface
import app.widgetsplus.core.widget.openAppIntent
import app.widgetsplus.core.widget.refreshStatus
import app.widgetsplus.finance.FinanceFormat
import app.widgetsplus.finance.FinanceModule
import app.widgetsplus.finance.FinanceRepository
import app.widgetsplus.finance.FinanceState
import app.widgetsplus.finance.PriceList
import app.widgetsplus.finance.Quote
import app.widgetsplus.finance.WatchItem

/** One of the user's lists as a scrolling list. Small charts appear when the widget is wide enough. */
class PriceListWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = FinanceRepository.get(context)
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        provideContent {
            val s by repo.state.collectAsState()
            GlanceTheme { Content(s, repo.listForWidget(appWidgetId, s)) }
        }
    }

    @Composable
    private fun Content(s: FinanceState, list: PriceList?) {
        val context = LocalContext.current
        val showCharts = LocalSize.current.width >= 260.dp
        val openApp = actionStartActivity(openAppIntent(context, FinanceModule.id))

        WidgetSurface(GlanceModifier.padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 6.dp)) {
            Column(GlanceModifier.padding(end = 2.dp)) {
                WidgetHeader(
                    title = list?.name ?: context.getString(R.string.finance_title),
                    status = refreshStatus(context, s.lastRefresh, s.lastRefreshFailed, s.refreshing),
                    onTitleClick = openApp,
                    refresh = actionRunCallback<RefreshAction>(),
                    refreshing = s.refreshing,
                )
            }
            if (list == null || list.items.isEmpty()) {
                Column(GlanceModifier.clickable(openApp)) {
                    EmptyMessage(context.getString(if (list == null) R.string.finance_widget_empty else R.string.finance_list_empty))
                }
            } else {
                LazyColumn(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                    items(list.items, itemId = { it.key.hashCode().toLong() }) { item ->
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
                Text(item.name, maxLines = 1, style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp))
            }
            if (showChart && quote != null) {
                priceChart(context, quote, 64f, 26f)?.let {
                    Image(ImageProvider(it), contentDescription = null, modifier = GlanceModifier.width(64.dp).height(26.dp))
                    Spacer(GlanceModifier.width(10.dp))
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    quote?.let { FinanceFormat.price(it.price, it.currency) } ?: "—",
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
                        FinanceFormat.percent(quote.changePercent),
                        maxLines = 1,
                        style = TextStyle(color = changeColor(quote.change), fontSize = 12.sp, textAlign = TextAlign.End),
                    )
                }
            }
        }
    }
}

class PriceListWidgetReceiver : ModuleWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PriceListWidget()
}
