package app.widgetsplus.finance.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.layout.Column
import androidx.glance.layout.padding
import app.widgetsplus.R
import app.widgetsplus.core.widget.formatTime
import app.widgetsplus.core.widget.refreshStatus
import app.widgetsplus.core.widget.soft.LocalSoft
import app.widgetsplus.core.widget.soft.SoftPaint
import app.widgetsplus.core.widget.soft.SoftText
import app.widgetsplus.core.widget.soft.SoftWeight
import app.widgetsplus.core.widget.soft.softLineHeight
import app.widgetsplus.core.widget.soft.softTextWidth
import app.widgetsplus.finance.AssetType
import app.widgetsplus.finance.FinanceFormat
import app.widgetsplus.finance.FinanceState
import app.widgetsplus.finance.Quote
import app.widgetsplus.finance.WatchItem
import app.widgetsplus.finance.ui.FinanceConfigureActivity

/*
 * Small helpers the Soft finance widgets share: status text, measuring, wrapped messages. Sizes are dp, as in the kit.
 */

/** Label sizes from the spec. */
internal const val SOFT_LABEL = 11.5f
internal const val SOFT_TICKER = 15f
internal const val SOFT_CHIP = 12.5f

/** "Updating…", "Offline, 10:26" (data still shown), or the time of the last update. */
internal fun softStatus(context: Context, s: FinanceState): String =
    if (s.lastRefreshFailed && !s.refreshing && s.lastRefresh != 0L) {
        context.getString(R.string.finance_offline_short, formatTime(context, s.lastRefresh))
    } else {
        refreshStatus(context, s.lastRefresh, s.lastRefreshFailed, s.refreshing)
    }

/** Height in dp below the baseline of Soft text at [size], to line up texts of different sizes on one baseline. */
internal fun softDescent(context: Context, size: Float): Float {
    val scale = SoftPaint.textScale(context)
    return SoftPaint.textPaint(context, size, SoftWeight.EXTRABOLD, 0, scale).fontMetrics.descent / scale
}

/** Size of a [app.widgetsplus.core.widget.soft.SoftChip], including its 4dp shadow room. */
internal fun softChipWidth(context: Context, text: String, size: Float = SOFT_CHIP) = softTextWidth(context, text, size) + 20f + 8f
internal fun softChipHeight(context: Context, size: Float = SOFT_CHIP) = softLineHeight(context, size) + 6f + 8f

/** The biggest size from [preferred] down to [min] at which [text] fits in [width]. */
internal fun softFitSize(context: Context, text: String, preferred: Float, min: Float, width: Float): Float {
    var size = preferred
    while (size > min && softTextWidth(context, text, size) > width) size -= 1f
    return size
}

/** "BTC-USD, 24h" or "AAPL, today". */
internal fun periodLabel(context: Context, item: WatchItem): String =
    context.getString(
        R.string.finance_symbol_period,
        item.code,
        context.getString(if (item.type == AssetType.CRYPTO) R.string.finance_period_24h else R.string.finance_period_today),
    )

/** The footer parts that are known: "High 58,984.37", "Low 54,443.52", "Vol 546.9 BTC". */
internal fun statParts(context: Context, item: WatchItem, q: Quote): List<String> = listOfNotNull(
    q.high?.let { context.getString(R.string.finance_high, FinanceFormat.plain(it)) },
    q.low?.let { context.getString(R.string.finance_low, FinanceFormat.plain(it)) },
    q.volume?.let { v ->
        val unit = if (item.type == AssetType.CRYPTO) " " + item.code.substringBefore('-') else ""
        context.getString(R.string.finance_volume, FinanceFormat.compact(v) + unit)
    },
)

/** Opens this widget's configure screen, to choose what it shows. */
internal fun configureIntent(context: Context, appWidgetId: Int): Intent =
    Intent(context, FinanceConfigureActivity::class.java)
        .setAction(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

/** Splits [text] into lines no wider than [maxWidth] at [size]. */
internal fun softWrap(context: Context, text: String, size: Float, maxWidth: Float, maxLines: Int): List<String> {
    val lines = mutableListOf<String>()
    var line = ""
    for (word in text.split(' ')) {
        val next = if (line.isEmpty()) word else "$line $word"
        if (line.isNotEmpty() && softTextWidth(context, next, size, SoftWeight.BOLD) > maxWidth) {
            lines += line
            line = word
        } else {
            line = next
        }
    }
    if (line.isNotEmpty()) lines += line
    if (lines.size <= maxLines) return lines
    // The last line that fits gets the rest, shortened with an ellipsis by SoftText.
    return lines.take(maxLines - 1) + lines.drop(maxLines - 1).joinToString(" ")
}

/** A short message (not set up, empty list) in the secondary colour, wrapped to [width] and [height]. */
@Composable
internal fun SoftMessage(context: Context, text: String, width: Float, height: Float, size: Float = 13f) {
    val lineH = softLineHeight(context, size)
    val maxLines = (height / lineH).toInt().coerceIn(1, 6)
    Column(GlanceModifier.padding(top = 2.dp)) {
        softWrap(context, text, size, width, maxLines).forEach {
            SoftText(it, size, SoftWeight.BOLD, LocalSoft.current.secondary, maxWidth = width)
        }
    }
}
