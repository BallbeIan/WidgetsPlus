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
import androidx.glance.appwidget.action.actionStartActivity
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
import androidx.glance.text.TextStyle
import app.widgetsplus.R
import app.widgetsplus.core.Design
import app.widgetsplus.core.ModuleWidgetReceiver
import app.widgetsplus.core.widget.EmptyMessage
import app.widgetsplus.core.widget.LocalWidgetStyle
import app.widgetsplus.core.widget.StyledWidget
import app.widgetsplus.core.widget.WidgetSurface
import app.widgetsplus.core.widget.backgroundIsDark
import app.widgetsplus.core.widget.soft.LocalSoft
import app.widgetsplus.core.widget.soft.SOFT_PAD_H
import app.widgetsplus.core.widget.soft.SOFT_PAD_V
import app.widgetsplus.core.widget.soft.SoftChart
import app.widgetsplus.core.widget.soft.SoftChip
import app.widgetsplus.core.widget.soft.SoftSurface
import app.widgetsplus.core.widget.soft.SoftText
import app.widgetsplus.core.widget.soft.SoftWeight
import app.widgetsplus.core.widget.soft.softLineHeight
import app.widgetsplus.core.widget.soft.softTextWidth
import app.widgetsplus.finance.FinanceFormat
import app.widgetsplus.finance.FinanceRepository
import app.widgetsplus.finance.FinanceState
import app.widgetsplus.finance.WatchItem

/**
 * One stock or coin with a big chart: name, change, price, chart, and high, low and volume. The symbol is chosen
 * like the single price widget's (the same per-widget setting; widget ids are unique across widget types).
 */
class ChartWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = FinanceRepository.get(context)
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        provideContent {
            val s by repo.state.collectAsState()
            StyledWidget(appWidgetId) {
                val item = repo.itemForWidget(appWidgetId, s)
                if (LocalWidgetStyle.current.design == Design.SOFT) SoftContent(s, item, appWidgetId) else ClassicContent(s, item, appWidgetId)
            }
        }
    }

    /**
     * Soft: name and "BTC-USD, 24h" with the % chip on the right; price with the change after it; the chart well
     * takes the rest; footer High / Low / Vol. When short, the footer goes first, then the "BTC-USD, 24h" line;
     * the chart is kept at 24dp or more. Below 210dp tall the price is 24dp instead of 30dp.
     */
    @Composable
    private fun SoftContent(s: FinanceState, item: WatchItem?, appWidgetId: Int) {
        val context = LocalContext.current
        val size = LocalSize.current
        val quote = item?.let { s.quotes[it.key] }
        val tap = actionStartActivity(if (item != null) openPageIntent(item) else configureIntent(context, appWidgetId))
        val padV = if (size.height < 170.dp) 10f else SOFT_PAD_V
        val padH = if (size.width < 200.dp) 12f else SOFT_PAD_H

        SoftSurface(onClick = tap, padH = padH, padV = padV) { inner ->
            val w = inner.width.value
            val h = inner.height.value
            if (item == null) {
                SoftMessage(context, context.getString(R.string.finance_tap_to_choose), w, h)
                return@SoftSurface
            }
            val p = LocalSoft.current
            val up = (quote?.change ?: 0.0) >= 0
            val color = p.direction(up)
            val full = size.height >= 210.dp

            val pct = quote?.let { FinanceFormat.percent(it.changePercent) }
            val chipW = pct?.let { softChipWidth(context, it) + 6f } ?: 0f
            val title = item.name.ifBlank { item.label }
            val priceText = quote?.let { FinanceFormat.price(it.price, it.currency) } ?: "—"
            val changeText = quote?.let { FinanceFormat.signed(it.change) }
            val priceSize = softFitSize(context, priceText, if (full) 30f else 24f, 14f, w)
            val changeW = changeText?.let { softTextWidth(context, it, CHANGE_SIZE) + 8f } ?: 0f
            val showChange = changeText != null && softTextWidth(context, priceText, priceSize) + changeW <= w
            val stats = quote?.let { statParts(context, item, it) }.orEmpty()

            val titleH = softLineHeight(context, SOFT_TICKER)
            val subH = softLineHeight(context, SOFT_LABEL)
            val chipH = if (pct != null) softChipHeight(context) else 0f
            val priceH = softLineHeight(context, priceSize) + 2f
            val footerH = subH + 8f
            val chartMin = 24f + 8f
            val hasChart = quote != null && quote.chart.size >= 2

            var showSub = true
            var used = maxOf(titleH + subH, chipH) + priceH
            if (used + (if (hasChart) chartMin else 0f) > h) {
                showSub = false
                used = maxOf(titleH, chipH) + priceH
            }
            val showChart = hasChart && used + chartMin <= h
            if (showChart) used += chartMin
            val showFooter = stats.isNotEmpty() && used + footerH <= h
            if (showFooter) used += footerH
            val chartH = 24f + (h - used - 3f).coerceAtLeast(0f)

            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(GlanceModifier.defaultWeight()) {
                    SoftText(title, SOFT_TICKER, maxWidth = (w - chipW).coerceAtLeast(24f))
                    if (showSub) SoftText(periodLabel(context, item), SOFT_LABEL, SoftWeight.BOLD, p.secondary, maxWidth = (w - chipW).coerceAtLeast(24f))
                }
                if (pct != null) SoftChip(pct, color)
            }
            Spacer(GlanceModifier.height(2.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                SoftText(priceText, priceSize, letterSpacing = -0.3f, maxWidth = w)
                if (showChange && changeText != null) {
                    Spacer(GlanceModifier.width(8.dp))
                    // Lifted so both texts share a baseline.
                    Column(GlanceModifier.padding(bottom = (softDescent(context, priceSize) - softDescent(context, CHANGE_SIZE)).coerceAtLeast(0f).dp)) {
                        SoftText(changeText, CHANGE_SIZE, color = color)
                    }
                }
            }
            if (showChart && quote != null) {
                Spacer(GlanceModifier.height(8.dp))
                SoftChart(w, chartH, quote.chart, quote.reference, up)
            } else {
                Spacer(GlanceModifier.defaultWeight())
            }
            if (showFooter) {
                Spacer(GlanceModifier.height(8.dp))
                Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    val each = (w - 12f * (stats.size - 1)) / stats.size
                    stats.forEachIndexed { i, part ->
                        if (i > 0) Spacer(GlanceModifier.defaultWeight())
                        SoftText(part, SOFT_LABEL, SoftWeight.BOLD, p.secondary, maxWidth = each)
                    }
                }
            }
        }
    }

    /** Classic: the single price widget's look with a bigger chart and the high, low and volume below it. */
    @Composable
    private fun ClassicContent(s: FinanceState, item: WatchItem?, appWidgetId: Int) {
        val context = LocalContext.current
        val size = LocalSize.current
        val quote = item?.let { s.quotes[it.key] }
        val tap = actionStartActivity(if (item != null) openPageIntent(item) else configureIntent(context, appWidgetId))

        WidgetSurface(GlanceModifier.padding(horizontal = 14.dp, vertical = 12.dp), onClick = tap) {
            if (item == null) {
                EmptyMessage(context.getString(R.string.finance_tap_to_choose))
                return@WidgetSurface
            }
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.name.ifBlank { item.label },
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
            val showSub = size.height >= 130.dp
            if (showSub) {
                Text(periodLabel(context, item), maxLines = 1, style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp))
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    quote?.let { FinanceFormat.price(it.price, it.currency) } ?: "—",
                    maxLines = 1,
                    style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 24.sp, fontWeight = FontWeight.Bold),
                )
                if (quote != null) {
                    Text(
                        FinanceFormat.signed(quote.change),
                        maxLines = 1,
                        style = TextStyle(color = changeColor(quote.change), fontSize = 13.sp, fontWeight = FontWeight.Medium),
                        modifier = GlanceModifier.padding(start = 8.dp, bottom = 3.dp),
                    )
                }
            }
            val stats = quote?.let { statParts(context, item, it) }.orEmpty()
            val showFooter = stats.isNotEmpty() && size.height >= 150.dp
            val chartHeight = size.height.value - 24 - 20 - 32 - (if (showSub) 16 else 0) - (if (showFooter) 20 else 0)
            val chart = if (quote != null && chartHeight >= 28) {
                priceChart(context, quote, size.width.value - 28, chartHeight - 8, backgroundIsDark(LocalWidgetStyle.current))
            } else null
            if (chart != null) {
                Spacer(GlanceModifier.height(8.dp))
                Image(ImageProvider(chart), contentDescription = null, modifier = GlanceModifier.fillMaxWidth().defaultWeight())
            } else {
                Spacer(GlanceModifier.defaultWeight())
            }
            if (showFooter) {
                Row(GlanceModifier.fillMaxWidth().padding(top = 4.dp)) {
                    stats.forEachIndexed { i, part ->
                        if (i > 0) Spacer(GlanceModifier.defaultWeight())
                        Text(part, maxLines = 1, style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp))
                    }
                }
            }
        }
    }
}

private const val CHANGE_SIZE = 13f

class ChartWidgetReceiver : ModuleWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ChartWidget()
}
