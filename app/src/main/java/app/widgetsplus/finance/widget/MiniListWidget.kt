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
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import app.widgetsplus.R
import app.widgetsplus.core.Design
import app.widgetsplus.core.ModuleWidgetReceiver
import app.widgetsplus.core.widget.EmptyMessage
import app.widgetsplus.core.widget.LocalWidgetStyle
import app.widgetsplus.core.widget.RefreshAction
import app.widgetsplus.core.widget.StyledWidget
import app.widgetsplus.core.widget.WidgetSurface
import app.widgetsplus.core.widget.openAppIntent
import app.widgetsplus.core.widget.soft.LocalSoft
import app.widgetsplus.core.widget.soft.SoftSurface
import app.widgetsplus.core.widget.soft.SoftText
import app.widgetsplus.core.widget.soft.SoftWeight
import app.widgetsplus.core.widget.soft.SoftWell
import app.widgetsplus.core.widget.soft.softLineHeight
import app.widgetsplus.core.widget.soft.softTextWidth
import app.widgetsplus.finance.FinanceFormat
import app.widgetsplus.finance.FinanceModule
import app.widgetsplus.finance.FinanceRepository
import app.widgetsplus.finance.FinanceState
import app.widgetsplus.finance.PriceList

/**
 * A 2×2 watchlist: ticker and % change for the first few symbols of one list. The list is chosen like the price
 * list widget's (the same per-widget setting; widget ids are unique across widget types).
 */
class MiniListWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = FinanceRepository.get(context)
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        provideContent {
            val s by repo.state.collectAsState()
            StyledWidget(appWidgetId) {
                val list = repo.listForWidget(appWidgetId, s)
                if (LocalWidgetStyle.current.design == Design.SOFT) SoftContent(s, list, appWidgetId) else ClassicContent(s, list, appWidgetId)
            }
        }
    }

    private fun emptyText(context: Context, list: PriceList?) =
        context.getString(if (list == null) R.string.finance_tap_to_choose else R.string.finance_list_empty)

    private fun emptyAction(context: Context, list: PriceList?, appWidgetId: Int) =
        actionStartActivity(if (list == null) configureIntent(context, appWidgetId) else openAppIntent(context, FinanceModule.id))

    /**
     * Soft: the list name, then one well with ticker left and % right in evenly spread rows: 5 rows when the well
     * has room for 5 comfortable rows (about 2×2 on a big screen), else 4, else as many as fit. The status
     * ("Updating…", "Offline, 10:26") appears next to the name only when there is something to say; tapping the
     * name refreshes.
     */
    @Composable
    private fun SoftContent(s: FinanceState, list: PriceList?, appWidgetId: Int) {
        val context = LocalContext.current
        val size = LocalSize.current
        val padH = if (size.width < 170.dp) 12f else 16f
        val padV = if (size.height < 170.dp) 11f else 14f

        SoftSurface(onClick = actionStartActivity(openAppIntent(context, FinanceModule.id)), padH = padH, padV = padV) { inner ->
            val p = LocalSoft.current
            val w = inner.width.value
            val h = inner.height.value
            if (list == null || list.items.isEmpty()) {
                Box(GlanceModifier.clickable(emptyAction(context, list, appWidgetId))) {
                    SoftMessage(context, emptyText(context, list), w, h)
                }
                return@SoftSurface
            }

            val status = if (s.refreshing || s.lastRefreshFailed) softStatus(context, s) else null
            val statusW = status?.let { softTextWidth(context, it, SOFT_LABEL, SoftWeight.BOLD) + 8f } ?: 0f
            Row(GlanceModifier.fillMaxWidth().clickable(actionRunCallback<RefreshAction>()), verticalAlignment = Alignment.CenterVertically) {
                SoftText(list.name, SOFT_LABEL, SoftWeight.BOLD, p.secondary, maxWidth = (w - statusW).coerceAtLeast(24f))
                if (status != null) {
                    Spacer(GlanceModifier.defaultWeight())
                    SoftText(status, SOFT_LABEL, SoftWeight.BOLD, p.secondary, maxWidth = w / 2)
                }
            }
            Spacer(GlanceModifier.height(6.dp))

            val wellH = h - softLineHeight(context, SOFT_LABEL) - 6f - 2f
            val wellPadV = 8f
            val wellPadH = if (w < 130f) 12f else 14f
            val textSize = if (w >= 150f && wellH >= 150f) 15f else 14f
            val lineH = softLineHeight(context, textSize)
            val roomy = (wellH - 2 * wellPadV) / 5 >= lineH + 8f
            val slots = minOf(if (roomy) 5 else 4, ((wellH - 2 * wellPadV) / (lineH + 1f)).toInt())
            if (slots <= 0) return@SoftSurface
            val shown = list.items.take(slots)
            val innerW = w - 2 * wellPadH
            val pcts = shown.map { s.quotes[it.key] }

            SoftWell(w, wellH, padH = wellPadH, padV = wellPadV) {
                shown.forEachIndexed { i, item ->
                    val q = pcts[i]
                    val pct = q?.let { FinanceFormat.percent(it.changePercent) } ?: "—"
                    val pctW = softTextWidth(context, pct, textSize)
                    Row(
                        GlanceModifier.fillMaxWidth().defaultWeight().clickable(actionStartActivity(openPageIntent(item))),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SoftText(item.label, textSize, maxWidth = (innerW - pctW - 6f).coerceAtLeast(16f))
                        Spacer(GlanceModifier.defaultWeight())
                        SoftText(pct, textSize, color = if (q != null) p.direction(q.change >= 0) else p.secondary)
                    }
                }
                // Keep the spacing of a full well when the list is shorter.
                repeat(slots - shown.size) { Spacer(GlanceModifier.defaultWeight()) }
            }
        }
    }

    /** Classic: the list name, then ticker and % change rows spread over the height. */
    @Composable
    private fun ClassicContent(s: FinanceState, list: PriceList?, appWidgetId: Int) {
        val context = LocalContext.current
        val size = LocalSize.current
        WidgetSurface(GlanceModifier.padding(horizontal = 14.dp, vertical = 10.dp), onClick = actionStartActivity(openAppIntent(context, FinanceModule.id))) {
            if (list == null || list.items.isEmpty()) {
                Column(GlanceModifier.clickable(emptyAction(context, list, appWidgetId))) { EmptyMessage(emptyText(context, list)) }
                return@WidgetSurface
            }
            Text(
                list.name,
                maxLines = 1,
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Medium),
                modifier = GlanceModifier.fillMaxWidth().clickable(actionRunCallback<RefreshAction>()),
            )
            val slots = (((size.height.value - 20 - 18) / 22).toInt()).coerceIn(1, 5)
            val shown = list.items.take(slots)
            shown.forEach { item ->
                val q = s.quotes[item.key]
                Row(
                    GlanceModifier.fillMaxWidth().defaultWeight().clickable(actionStartActivity(openPageIntent(item))),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        item.label,
                        maxLines = 1,
                        style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold),
                        modifier = GlanceModifier.defaultWeight(),
                    )
                    Text(
                        q?.let { FinanceFormat.percent(it.changePercent) } ?: "—",
                        maxLines = 1,
                        style = TextStyle(
                            color = if (q != null) changeColor(q.change) else GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
            }
            repeat(slots - shown.size) { Spacer(GlanceModifier.defaultWeight()) }
        }
    }
}

class MiniListWidgetReceiver : ModuleWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MiniListWidget()
}
