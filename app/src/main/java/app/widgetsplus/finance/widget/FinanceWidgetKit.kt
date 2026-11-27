package app.widgetsplus.finance.widget

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.runtime.Composable
import androidx.glance.color.ColorProvider
import androidx.glance.unit.ColorProvider as GlanceColor
import app.widgetsplus.core.widget.LocalWidgetStyle
import app.widgetsplus.core.widget.backgroundIsDark
import app.widgetsplus.core.widget.Charts
import app.widgetsplus.finance.FinanceFormat
import app.widgetsplus.finance.Quote
import app.widgetsplus.finance.WatchItem

// Price up and down colours, slightly brighter on dark backgrounds. The app screens use the same values.
val UpDay = Color(0xFF1B873F)
val UpNight = Color(0xFF4CD07D)
val DownDay = Color(0xFFC62828)
val DownNight = Color(0xFFFF6B6B)

private val UpColor = ColorProvider(day = UpDay, night = UpNight)
private val DownColor = ColorProvider(day = DownDay, night = DownNight)

/** Green or red; on a fixed light or dark background the version that reads on it, else the phone's day/night. */
@Composable
fun changeColor(change: Double): GlanceColor {
    val up = change >= 0
    return when (backgroundIsDark(LocalWidgetStyle.current)) {
        true -> androidx.glance.unit.ColorProvider(if (up) UpNight else DownNight)
        false -> androidx.glance.unit.ColorProvider(if (up) UpDay else DownDay)
        null -> if (up) UpColor else DownColor
    }
}

/** Today's (stocks) or the last 24 hours' (crypto) price chart, green or red, with the reference price dashed. */
fun priceChart(context: Context, quote: Quote, widthDp: Float, heightDp: Float, darkBackground: Boolean? = null): Bitmap? {
    val night = darkBackground
        ?: (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES)
    val up = (quote.chart.lastOrNull() ?: quote.price) >= quote.reference
    val color = (if (up) if (night) UpNight else UpDay else if (night) DownNight else DownDay).toArgb()
    return Charts.line(context, quote.chart, color, widthDp, heightDp, baseline = quote.reference)
}

/** Opens the symbol in the Yahoo Finance or Coinbase app if installed, otherwise in the browser. */
fun openPageIntent(item: WatchItem): Intent =
    Intent(Intent.ACTION_VIEW, FinanceFormat.pageFor(item)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
