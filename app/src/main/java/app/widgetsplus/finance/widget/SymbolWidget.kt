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
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import app.widgetsplus.R
import app.widgetsplus.core.ModuleWidgetReceiver
import app.widgetsplus.core.widget.EmptyMessage
import app.widgetsplus.core.widget.RefreshAction
import app.widgetsplus.core.widget.RefreshButton
import app.widgetsplus.core.widget.WidgetSurface
import app.widgetsplus.core.widget.openAppIntent
import app.widgetsplus.core.widget.refreshStatus
import app.widgetsplus.finance.FinanceFormat
import app.widgetsplus.finance.FinanceModule
import app.widgetsplus.finance.FinanceRepository
import app.widgetsplus.finance.FinanceState
import app.widgetsplus.finance.WatchItem

/** One stock or coin: price, change and chart. The symbol is chosen when the widget is added. */
class SymbolWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = FinanceRepository.get(context)
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        provideContent {
            val s by repo.state.collectAsState()
            GlanceTheme { Content(s, repo.itemForWidget(appWidgetId, s)) }
        }
    }

    @Composable
    private fun Content(s: FinanceState, item: WatchItem?) {
        val context = LocalContext.current
        val size = LocalSize.current
        val quote = item?.let { s.quotes[it.key] }
        val tap = actionStartActivity(if (item != null) openPageIntent(item) else openAppIntent(context, FinanceModule.id))

        WidgetSurface(GlanceModifier.padding(horizontal = 14.dp, vertical = 12.dp), onClick = tap) {
            if (item == null) {
                EmptyMessage(context.getString(R.string.finance_widget_empty))
                return@WidgetSurface
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
                        FinanceFormat.percent(quote.changePercent),
                        maxLines = 1,
                        style = TextStyle(color = changeColor(quote.change), fontSize = 13.sp, fontWeight = FontWeight.Medium),
                    )
                }
            }
            val showName = size.height >= 100.dp
            if (showName) {
                Text(item.name, maxLines = 1, style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp))
            }
            Text(
                quote?.let { FinanceFormat.price(it.price, it.currency) } ?: "—",
                maxLines = 1,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = if (size.width >= 150.dp) 24.sp else 20.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )

            val showFooter = size.height >= 170.dp
            // Height left for the chart after padding, the rows above and the footer.
            val chartHeight = size.height.value - 24 - 20 - 32 - (if (showName) 16 else 0) - (if (showFooter) 32 else 0)
            val chart = if (quote != null && chartHeight >= 28) priceChart(context, quote, size.width.value - 28, chartHeight - 8) else null
            if (chart != null) {
                Spacer(GlanceModifier.height(8.dp))
                Image(ImageProvider(chart), contentDescription = null, modifier = GlanceModifier.fillMaxWidth().defaultWeight())
            } else {
                Spacer(GlanceModifier.defaultWeight())
            }

            if (showFooter) {
                Row(modifier = GlanceModifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        refreshStatus(context, s.lastRefresh, s.lastRefreshFailed, s.refreshing),
                        maxLines = 1,
                        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp),
                        modifier = GlanceModifier.defaultWeight(),
                    )
                    RefreshButton(actionRunCallback<RefreshAction>(), s.refreshing)
                }
            }
        }
    }
}

class SymbolWidgetReceiver : ModuleWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SymbolWidget()
}
