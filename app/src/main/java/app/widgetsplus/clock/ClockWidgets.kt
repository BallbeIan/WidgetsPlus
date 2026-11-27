package app.widgetsplus.clock

import app.widgetsplus.core.widget.StyledWidget
import android.content.Context
import android.os.Build
import android.text.format.DateFormat
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.compose.runtime.remember
import androidx.glance.layout.Alignment
import androidx.glance.layout.ContentScale
import androidx.glance.layout.size
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import app.widgetsplus.R
import app.widgetsplus.core.Design
import app.widgetsplus.core.ModuleWidgetReceiver
import app.widgetsplus.core.widget.soft.LocalSoft
import app.widgetsplus.core.widget.soft.SoftPalette
import app.widgetsplus.core.widget.soft.SoftSurface
import app.widgetsplus.core.widget.soft.softLineHeight
import app.widgetsplus.core.widget.soft.softTextWidth
import app.widgetsplus.core.widget.LocalWidgetStyle
import app.widgetsplus.core.widget.WidgetSurface
import app.widgetsplus.core.widget.fixedTextColors
import java.time.ZoneId
import java.util.Locale
import kotlin.math.min

/**
 * Digital clock with the date, in the phone's time zone or a chosen one (with a label, for a world clock).
 * The clock itself is a TextClock inside Glance, because Glance has no ticking text of its own.
 */
class DigitalClockWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val settings = ClockModule.settings(context)
        provideContent {
            // Read so the widget redraws when its time zone or label is changed.
            val version by settings.version.collectAsState()
            StyledWidget(appWidgetId) { Content(appWidgetId, version) }
        }
    }

    @Composable
    private fun Content(appWidgetId: Int, @Suppress("UNUSED_PARAMETER") version: Int) {
        val context = LocalContext.current
        val size = LocalSize.current
        val zone = ClockModule.zoneFor(context, appWidgetId)
        val label = ClockModule.labelFor(context, appWidgetId) ?: zone?.let { ClockModule.cityName(it) }

        val open = actionStartActivity(ClockModule.clockAppIntent(context))

        if (LocalWidgetStyle.current.design == Design.SOFT) {
            // Less padding than other Soft panels, so the time can be as large as possible.
            SoftSurface(onClick = open, padH = 12f, padV = 6f) { inner ->
                val p = LocalSoft.current
                val views = RemoteViews(context.packageName, R.layout.clock_digital_soft)
                views.setTextColor(R.id.clock_time, p.ink)
                views.setTextColor(R.id.clock_date, p.secondary)
                views.setTextColor(R.id.clock_label, p.secondary)
                // Nunito ExtraBold is wider and taller than the Classic light font: measure it instead of guessing.
                val timeWidth = softTextWidth(context, "00:00", 100f) / 100f
                val lineHeight = softLineHeight(context, 100f) / 100f
                // The sizing below expects the widget's size with 8dp padding; the inner size plus that padding gives it.
                digitalSizing(views, zone, label, inner.width.value + 16, inner.height.value + 16, timeWidth, 1f / lineHeight)
                AndroidRemoteViews(remoteViews = views, modifier = GlanceModifier.fillMaxSize())
            }
            return
        }

        WidgetSurface(GlanceModifier.padding(8.dp), onClick = open) {
            AndroidRemoteViews(
                remoteViews = digitalViews(context, zone, label, size.width.value, size.height.value, fixedTextColors(LocalWidgetStyle.current)),
                modifier = GlanceModifier.fillMaxSize(),
            )
        }
    }

    private fun digitalViews(context: Context, zone: ZoneId?, label: String?, widthDp: Float, heightDp: Float, text: Pair<Int, Int>?): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.clock_digital)
        // Always set, also back to the defaults: the launcher may update the existing views in place, which would
        // otherwise keep a previously chosen colour.
        if (text != null) {
            views.setTextColor(R.id.clock_time, text.first)
            views.setTextColor(R.id.clock_date, text.first)
            views.setTextColor(R.id.clock_label, text.second)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Resource colours, so dark mode and wallpaper colours keep applying live.
            views.setColor(R.id.clock_time, "setTextColor", R.color.widget_on_surface)
            views.setColor(R.id.clock_date, "setTextColor", R.color.widget_accent)
            views.setColor(R.id.clock_label, "setTextColor", R.color.widget_on_surface_variant)
        } else {
            views.setTextColor(R.id.clock_time, context.getColor(R.color.widget_on_surface))
            views.setTextColor(R.id.clock_date, context.getColor(R.color.widget_accent))
            views.setTextColor(R.id.clock_label, context.getColor(R.color.widget_on_surface_variant))
        }
        digitalSizing(views, zone, label, widthDp, heightDp, 2.7f, 0.8f)
        return views
    }

    /**
     * Time size, date format, time zone and label. [timeWidth] is how many times the text size "12:59" is wide;
     * [heightFactor] the text size per dp of height.
     */
    private fun digitalSizing(views: RemoteViews, zone: ZoneId?, label: String?, widthDp: Float, heightDp: Float, timeWidth: Float, heightFactor: Float) {
        val showDate = heightDp >= 80
        val showLabel = label != null && heightDp >= 70
        // Largest time that fits: "12:59" is about 2.7 times as wide as the text size, and needs room for the other lines.
        val spare = (if (showDate) 24 else 0) + (if (showLabel) 20 else 0) + 16
        val timeSize = min((widthDp - 24) / timeWidth, (heightDp - spare) * heightFactor).coerceIn(20f, 110f)
        // dp, not sp: the size is already fitted to the widget, so the phone's font scale must not enlarge it again.
        views.setTextViewTextSize(R.id.clock_time, TypedValue.COMPLEX_UNIT_DIP, timeSize)

        val datePattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), if (widthDp >= 200) "EEEEMMMMd" else "EEEMMMd")
        views.setCharSequence(R.id.clock_date, "setFormat12Hour", datePattern)
        views.setCharSequence(R.id.clock_date, "setFormat24Hour", datePattern)
        views.setViewVisibility(R.id.clock_date, if (showDate) View.VISIBLE else View.GONE)

        // Always set, also to null (the phone's zone): on Android 12+ the launcher may update the existing
        // TextClock in place, which would otherwise keep a previously chosen city.
        views.setString(R.id.clock_time, "setTimeZone", zone?.id)
        views.setString(R.id.clock_date, "setTimeZone", zone?.id)
        views.setViewVisibility(R.id.clock_label, if (showLabel) View.VISIBLE else View.GONE)
        if (showLabel) views.setTextViewText(R.id.clock_label, label)
    }
}

/** An analog clock face in the phone's time zone, coloured to match the theme. */
class AnalogClockWidget : GlanceAppWidget() {

    // Exact, so the Soft dial is painted at the size it is shown (Classic looks the same either way).
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        provideContent {
            StyledWidget(appWidgetId) {
                val context = LocalContext.current
                if (LocalWidgetStyle.current.design == Design.SOFT) {
                    SoftAnalog()
                    return@StyledWidget
                }
                // Transparent by default (see ClockModule.analog), so the face floats on the wallpaper.
                WidgetSurface(GlanceModifier.padding(4.dp), onClick = actionStartActivity(ClockModule.clockAppIntent(context))) {
                    AndroidRemoteViews(RemoteViews(context.packageName, R.layout.clock_analog), GlanceModifier.fillMaxSize())
                }
            }
        }
    }
}

/**
 * The Soft analog clock: the raised disc is the widget, so there is no panel behind it. Its opacity is the style's,
 * except that 0 (the analog clock's default, meant to float the Classic face on the wallpaper) shows the disc fully,
 * and other values are kept at least [MIN_SOFT_DIAL_OPACITY] so the disc never disappears.
 */
@Composable
private fun SoftAnalog() {
    val context = LocalContext.current
    val size = LocalSize.current
    val style = LocalWidgetStyle.current
    val opacity = if (style.opacity == 0) 100 else style.opacity.coerceAtLeast(MIN_SOFT_DIAL_OPACITY)
    val palette = remember(style) { SoftPalette.of(style).copy(alpha = opacity / 100f) }
    // The hands are drawn at most 200dp across (AnalogClock never scales its drawables up), so the disc is too.
    val disc = (min(size.width.value, size.height.value) - 2 * SOFT_DIAL_MARGIN).coerceIn(40f, 200f)
    val box = disc + 2 * SOFT_DIAL_MARGIN
    val bmp = remember(palette, box) { paintSoftDial(context, box, palette) }
    Box(
        GlanceModifier.fillMaxSize().clickable(actionStartActivity(ClockModule.clockAppIntent(context))),
        contentAlignment = Alignment.Center,
    ) {
        Box(GlanceModifier.size(box.dp), contentAlignment = Alignment.Center) {
            Image(ImageProvider(bmp), contentDescription = null, contentScale = ContentScale.FillBounds, modifier = GlanceModifier.size(box.dp))
            val views = RemoteViews(context.packageName, R.layout.clock_analog_soft)
            // On Android 12+ the hour hand takes the palette's text colour, so it stays visible on dark backgrounds.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                views.setColorStateList(R.id.clock_analog, "setHourHandTintList", android.content.res.ColorStateList.valueOf(palette.ink))
            }
            AndroidRemoteViews(views, GlanceModifier.size(disc.dp))
        }
    }
}

private const val MIN_SOFT_DIAL_OPACITY = 30

class DigitalClockWidgetReceiver : ModuleWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DigitalClockWidget()
}

class AnalogClockWidgetReceiver : ModuleWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AnalogClockWidget()
}
