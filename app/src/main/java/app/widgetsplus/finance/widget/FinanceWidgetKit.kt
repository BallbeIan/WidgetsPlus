package app.widgetsplus.finance.widget

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.color.ColorProvider
import app.widgetsplus.core.widget.Charts
import app.widgetsplus.finance.FinanceFormat
import app.widgetsplus.core.RefreshWorker
import app.widgetsplus.finance.Quote
import app.widgetsplus.finance.WatchItem

// Price up and down colours, slightly brighter on dark backgrounds. The app screens use the same values.
val UpDay = Color(0xFF1B873F)
val UpNight = Color(0xFF4CD07D)
val DownDay = Color(0xFFC62828)
val DownNight = Color(0xFFFF6B6B)

private val UpColor = ColorProvider(day = UpDay, night = UpNight)
private val DownColor = ColorProvider(day = DownDay, night = DownNight)

fun changeColor(change: Double) = if (change >= 0) UpColor else DownColor

/** Today's (stocks) or the last 24 hours' (crypto) price chart, green or red, with the reference price dashed. */
fun priceChart(context: Context, quote: Quote, widthDp: Float, heightDp: Float): Bitmap? {
    val night = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    val up = (quote.chart.lastOrNull() ?: quote.price) >= quote.reference
    val color = (if (up) if (night) UpNight else UpDay else if (night) DownNight else DownDay).toArgb()
    return Charts.line(context, quote.chart, color, widthDp, heightDp, baseline = quote.reference)
}

/** Opens the symbol in the Yahoo Finance or Coinbase app if installed, otherwise in the browser. */
fun openPageIntent(item: WatchItem): Intent =
    Intent(Intent.ACTION_VIEW, FinanceFormat.pageFor(item)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

/**
 * The refresh button on the finance widgets. Actions run in a short-lived broadcast, so the download itself is
 * handed to WorkManager, which also waits for a network connection.
 */
class FinanceRefreshAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        RefreshWorker.refreshSoon(context)
    }
}
