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
import androidx.glance.layout.Box
import app.widgetsplus.core.Design
import app.widgetsplus.core.widget.soft.LocalSoft
import app.widgetsplus.core.widget.soft.SOFT_PAD_V
import app.widgetsplus.core.widget.soft.SoftKey
import app.widgetsplus.core.widget.soft.SoftSurface
import app.widgetsplus.core.widget.soft.SoftText
import app.widgetsplus.core.widget.soft.SoftWeight
import app.widgetsplus.core.widget.soft.softLineHeight
import app.widgetsplus.core.widget.soft.softTextWidth

/** One of the user's lists as a scrolling list. Small charts appear when the widget is wide enough. */
class PriceListWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = FinanceRepository.get(context)
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        provideContent {
            val s by repo.state.collectAsState()
            StyledWidget(appWidgetId) {
                val list = repo.listForWidget(appWidgetId, s)
                if (LocalWidgetStyle.current.design == Design.SOFT) SoftContent(s, list, appWidgetId) else ClassicContent(s, list)
            }
        }
    }

    /**
     * Soft: header (list name, time), then one raised key per symbol, as many as fit (at least 40dp each, at most
     * 60dp). Each key shows ticker, price and % change; between them the day range when it fits whole (wide
     * widgets), otherwise the name, shortened to the room left.
     */
    @Composable
    private fun SoftContent(s: FinanceState, list: PriceList?, appWidgetId: Int) {
        val context = LocalContext.current
        val size = LocalSize.current
        val openApp = actionStartActivity(openAppIntent(context, FinanceModule.id))
        val padV = if (size.height < 150.dp) 10f else SOFT_PAD_V

        SoftSurface(onClick = openApp, padV = padV) { inner ->
            val p = LocalSoft.current
            val w = inner.width.value
            val h = inner.height.value
            val title = list?.name ?: context.getString(R.string.finance_title)
            val status = softStatus(context, s)
            val statusW = softTextWidth(context, status, SOFT_LABEL, SoftWeight.BOLD) + 12f
            val headerH = maxOf(softLineHeight(context, SOFT_TICKER), softLineHeight(context, SOFT_LABEL) + 4f)

            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                SoftText(title, SOFT_TICKER, maxWidth = (w - statusW).coerceAtLeast(24f))
                Spacer(GlanceModifier.defaultWeight())
                // Tapping the time refreshes.
                Box(GlanceModifier.clickable(actionRunCallback<RefreshAction>()).padding(start = 12.dp, top = 2.dp, bottom = 2.dp)) {
                    SoftText(status, SOFT_LABEL, SoftWeight.BOLD, p.secondary)
                }
            }
            Spacer(GlanceModifier.height(6.dp))
            val area = h - headerH - 6f
            if (list == null || list.items.isEmpty()) {
                val msg = if (list == null) R.string.finance_tap_to_choose else R.string.finance_list_empty
                Box(GlanceModifier.clickable(actionStartActivity(if (list == null) configureIntent(context, appWidgetId) else openAppIntent(context, FinanceModule.id)))) {
                    SoftMessage(context, context.getString(msg), w, area)
                }
                return@SoftSurface
            }

            val rows = minOf(list.items.size, (area / MIN_KEY).toInt(), MAX_ROWS)
            if (rows <= 0) return@SoftSurface
            val keyH = minOf((area - 1f) / rows, MAX_KEY)
            val shown = list.items.take(rows)
            val quotes = shown.map { s.quotes[it.key] }

            // Column widths, measured over the rows shown so prices and changes line up.
            val keyPad = if (w < 220f) 10f else 14f
            val cw = w - 2 * (3f + keyPad)
            val tickerW = shown.maxOf { softTextWidth(context, it.label, SOFT_TICKER) }
            val priceTexts = quotes.map { q -> q?.let { FinanceFormat.plain(it.price) } ?: "—" }
            val pctTexts = quotes.map { q -> q?.let { FinanceFormat.percent(it.changePercent) } ?: "" }
            val pctW = pctTexts.maxOf { softTextWidth(context, it, PCT_SIZE) } + 12f
            var priceW = priceTexts.maxOf { softTextWidth(context, it, SOFT_TICKER) } + 10f
            // Too narrow for prices: drop them before the change.
            if (tickerW.coerceAtMost(cw * 0.4f) + priceW + pctW > cw) priceW = 0f
            val middle = cw - tickerW - priceW - pctW - 8f
            val ranges = quotes.map { q -> q?.let { if (it.low != null && it.high != null) FinanceFormat.range(it.low, it.high) else null } }
            val rangeW = ranges.maxOf { r -> r?.let { softTextWidth(context, it, SOFT_LABEL, SoftWeight.SEMIBOLD) } ?: 0f }
            val wide = ranges.any { it != null } && rangeW + 4f <= middle

            Column(GlanceModifier.fillMaxWidth()) {
                shown.forEachIndexed { i, item ->
                    val q = quotes[i]
                    SoftKey(w, keyH, onClick = actionStartActivity(openPageIntent(item)), padH = keyPad) {
                        val tickerMax = if (priceW == 0f) cw - pctW else (cw - priceW - pctW).coerceAtLeast(24f)
                        SoftText(item.label, SOFT_TICKER, maxWidth = tickerMax)
                        if (!wide && middle >= 30f && item.name.isNotBlank() && item.name != item.label) {
                            val tw = softTextWidth(context, item.label, SOFT_TICKER).coerceAtMost(tickerMax)
                            Spacer(GlanceModifier.width(6.dp))
                            SoftText(item.name, SOFT_LABEL, SoftWeight.BOLD, p.secondary, maxWidth = (cw - tw - 6f - priceW - pctW).coerceAtLeast(1f))
                        }
                        Spacer(GlanceModifier.defaultWeight())
                        val range = ranges[i]
                        if (wide && range != null) {
                            SoftText(range, SOFT_LABEL, SoftWeight.SEMIBOLD, p.secondary)
                        }
                        if (priceW > 0f) {
                            Box(GlanceModifier.width(priceW.dp), contentAlignment = Alignment.CenterEnd) {
                                SoftText(priceTexts[i], SOFT_TICKER)
                            }
                        }
                        Box(GlanceModifier.width(pctW.dp), contentAlignment = Alignment.CenterEnd) {
                            if (q != null) SoftText(pctTexts[i], PCT_SIZE, color = p.direction(q.change >= 0))
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun ClassicContent(s: FinanceState, list: PriceList?) {
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
                priceChart(context, quote, 64f, 26f, backgroundIsDark(LocalWidgetStyle.current))?.let {
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

private const val MIN_KEY = 40f
private const val MAX_KEY = 60f
private const val PCT_SIZE = 14f
/** Glance allows 10 children per column. */
private const val MAX_ROWS = 8

class PriceListWidgetReceiver : ModuleWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PriceListWidget()
}
