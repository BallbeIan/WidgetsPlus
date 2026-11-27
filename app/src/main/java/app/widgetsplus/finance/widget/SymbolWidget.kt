package app.widgetsplus.finance.widget

import app.widgetsplus.core.widget.backgroundIsDark
import app.widgetsplus.core.widget.LocalWidgetStyle
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
import app.widgetsplus.core.Design
import app.widgetsplus.core.widget.soft.LocalSoft
import app.widgetsplus.core.widget.soft.SOFT_PAD_H
import app.widgetsplus.core.widget.soft.SOFT_PAD_V
import app.widgetsplus.core.widget.soft.SoftChart
import app.widgetsplus.core.widget.soft.SoftChip
import app.widgetsplus.core.widget.soft.SoftSurface
import app.widgetsplus.core.widget.soft.SoftText
import app.widgetsplus.core.widget.soft.SoftWeight
import app.widgetsplus.core.widget.soft.softLineHeight

/** One stock or coin: price, change and chart. The symbol is chosen when the widget is added. */
class SymbolWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = FinanceRepository.get(context)
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        provideContent {
            val s by repo.state.collectAsState()
            StyledWidget(appWidgetId) {
                val item = repo.itemForWidget(appWidgetId, s)
                if (LocalWidgetStyle.current.design == Design.SOFT) SoftContent(s, item, appWidgetId) else ClassicContent(s, item)
            }
        }
    }

    /**
     * Soft: ticker, name, price, % chip, chart well and the day range, each shown only while it fits. Missing room
     * is taken from the bottom up: the day range goes first, then the name; the chip moves next to the ticker
     * when it can't have its own row; the chart is never shorter than 30dp.
     */
    @Composable
    private fun SoftContent(s: FinanceState, item: WatchItem?, appWidgetId: Int) {
        val context = LocalContext.current
        val size = LocalSize.current
        val quote = item?.let { s.quotes[it.key] }
        val tap = actionStartActivity(if (item != null) openPageIntent(item) else configureIntent(context, appWidgetId))
        val padH = if (size.width < 150.dp) 12f else SOFT_PAD_H
        val padV = if (size.height < 150.dp) 10f else SOFT_PAD_V

        SoftSurface(onClick = tap, padH = padH, padV = padV) { inner ->
            val w = inner.width.value
            val h = inner.height.value
            if (item == null) {
                SoftMessage(context, context.getString(R.string.finance_tap_to_choose), w, h)
                return@SoftSurface
            }
            val p = LocalSoft.current
            val full = size.width >= 200.dp && size.height >= 200.dp
            val up = (quote?.change ?: 0.0) >= 0
            val priceText = quote?.let { FinanceFormat.price(it.price, it.currency) } ?: "—"
            val priceSize = softFitSize(context, priceText, if (full) 30f else 24f, 14f, w)
            val pct = quote?.let { FinanceFormat.percent(it.changePercent) }
            val range = quote?.let { q -> if (q.low != null && q.high != null) FinanceFormat.range(q.low, q.high) else null }

            val tickerH = softLineHeight(context, SOFT_TICKER)
            val nameH = softLineHeight(context, SOFT_LABEL)
            val chipH = softChipHeight(context)
            val footerH = nameH + 6f
            val chartMin = 30f
            val chartGap = 8f
            val hasChart = quote != null && quote.chart.size >= 2

            var used = tickerH + softLineHeight(context, priceSize) + 2f
            val chipOwnRow = pct != null && used + chipH + (if (hasChart) chartGap + chartMin else 0f) <= h
            if (chipOwnRow) used += chipH
            val showChart = hasChart && used + chartGap + chartMin <= h
            if (showChart) used += chartGap + chartMin
            val showName = item.name.isNotBlank() && item.name != item.label && used + nameH <= h
            if (showName) used += nameH
            val showFooter = full && range != null && used + footerH <= h
            if (showFooter) used += footerH
            // Text images round up to whole pixels, so leave a little slack.
            val chartH = chartMin + (h - used - 3f).coerceAtLeast(0f)

            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                val chipW = if (pct != null && !chipOwnRow) softChipWidth(context, pct) + 4f else 0f
                SoftText(item.label, SOFT_TICKER, maxWidth = (w - chipW).coerceAtLeast(16f))
                if (pct != null && !chipOwnRow) {
                    Spacer(GlanceModifier.defaultWeight())
                    SoftChip(pct, p.direction(up))
                }
            }
            if (showName) SoftText(item.name, SOFT_LABEL, SoftWeight.BOLD, p.secondary, maxWidth = w)
            Spacer(GlanceModifier.height(2.dp))
            SoftText(priceText, priceSize, letterSpacing = -0.3f, maxWidth = w)
            if (pct != null && chipOwnRow) SoftChip(pct, p.direction(up))
            if (showChart && quote != null) {
                Spacer(GlanceModifier.height(chartGap.dp))
                SoftChart(w, chartH, quote.chart, quote.reference, up)
            } else {
                Spacer(GlanceModifier.defaultWeight())
            }
            if (showFooter && range != null) {
                Spacer(GlanceModifier.height(6.dp))
                SoftText(context.getString(R.string.finance_day_range, range), SOFT_LABEL, SoftWeight.BOLD, p.secondary, maxWidth = w)
            }
        }
    }

    @Composable
    private fun ClassicContent(s: FinanceState, item: WatchItem?) {
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
            val chart = if (quote != null && chartHeight >= 28) priceChart(context, quote, size.width.value - 28, chartHeight - 8, backgroundIsDark(LocalWidgetStyle.current)) else null
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
