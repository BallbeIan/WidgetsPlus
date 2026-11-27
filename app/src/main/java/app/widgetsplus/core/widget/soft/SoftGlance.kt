package app.widgetsplus.core.widget.soft

import android.graphics.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.RowScope
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import app.widgetsplus.core.widget.LocalWidgetStyle

/*
 * Glance building blocks for the Soft design. Every Soft widget is built from these, so they all share one look:
 * SoftSurface (the raised panel), SoftText (Nunito text), SoftChip (raised % chip), SoftKey (raised row),
 * SoftWell (sunken box) and SoftChart (a chart in a well).
 *
 * Sizes are explicit dp values: Soft widgets work out their layout from LocalSize (use SizeMode.Exact), because the
 * painted surfaces must be drawn at the size they are shown.
 */

/** The palette of the Soft widget being drawn. Provided by [SoftSurface]. */
val LocalSoft = staticCompositionLocalOf { SoftPalette.of(app.widgetsplus.core.WidgetStyle()) }

/** Room around the panel for its shadow, so the shadow isn't cut off at the widget's edge. */
const val SOFT_MARGIN = 7f
const val SOFT_PAD_H = 16f
const val SOFT_PAD_V = 14f

/**
 * The raised panel filling the widget. [content] gets the inner size (after shadow room and padding) to lay out in.
 */
@Composable
fun SoftSurface(
    onClick: Action? = null,
    padH: Float = SOFT_PAD_H,
    padV: Float = SOFT_PAD_V,
    content: @Composable ColumnScope.(inner: DpSize) -> Unit,
) {
    val context = LocalContext.current
    val style = LocalWidgetStyle.current
    val size = LocalSize.current
    val palette = remember(style) { SoftPalette.of(style) }
    val w = size.width.value
    val h = size.height.value
    val panel = remember(style, w, h) {
        SoftPaint.raised(context, w, h, style.corners.toFloat(), palette, SOFT_MARGIN, small = false)
    }
    val inner = DpSize((w - 2 * (SOFT_MARGIN + padH)).coerceAtLeast(0f).dp, (h - 2 * (SOFT_MARGIN + padV)).coerceAtLeast(0f).dp)
    var box = GlanceModifier.fillMaxSize()
    if (onClick != null) box = box.clickable(onClick)
    CompositionLocalProvider(LocalSoft provides palette) {
        Box(box) {
            Image(ImageProvider(panel), contentDescription = null, contentScale = ContentScale.FillBounds, modifier = GlanceModifier.fillMaxSize())
            Column(GlanceModifier.fillMaxSize().padding(horizontal = (SOFT_MARGIN + padH).dp, vertical = (SOFT_MARGIN + padV).dp)) {
                content(inner)
            }
        }
    }
}

/** One line of Nunito text. [maxWidth] (dp) shortens it with an ellipsis. Returns nothing; sized to the text. */
@Composable
fun SoftText(
    text: String,
    size: Float,
    weight: SoftWeight = SoftWeight.EXTRABOLD,
    color: Int = LocalSoft.current.ink,
    maxWidth: Float? = null,
    letterSpacing: Float = 0f,
    modifier: GlanceModifier = GlanceModifier,
) {
    val context = LocalContext.current
    val bmp = remember(text, size, weight, color, maxWidth) { SoftPaint.text(context, text, size, weight, color, maxWidth, letterSpacing) }
    val scale = SoftPaint.textScale(context)
    Image(
        ImageProvider(bmp),
        contentDescription = text,
        contentScale = ContentScale.Fit,
        modifier = modifier.size((bmp.width / scale).dp, (bmp.height / scale).dp),
    )
}

/** Width in dp that [SoftText] would take, for layouts that need to measure first. */
fun softTextWidth(context: android.content.Context, text: String, size: Float, weight: SoftWeight = SoftWeight.EXTRABOLD): Float {
    val scale = SoftPaint.textScale(context)
    return SoftPaint.textPaint(context, size, weight, 0, scale).measureText(text) / scale
}

/** Height in dp of one line of Soft text at [size]. */
fun softLineHeight(context: android.content.Context, size: Float): Float {
    val scale = SoftPaint.textScale(context)
    val fm = SoftPaint.textPaint(context, size, SoftWeight.EXTRABOLD, 0, scale).fontMetrics
    return (fm.descent - fm.ascent) / scale
}

/** A raised chip (radius 14dp) with coloured text, e.g. the % change. Includes 4dp of shadow room on each side. */
@Composable
fun SoftChip(text: String, color: Int, size: Float = 12.5f) {
    val context = LocalContext.current
    val p = LocalSoft.current
    val bmp = remember(text, color, size, p) {
        val scale = SoftPaint.textScale(context)
        val paint = SoftPaint.textPaint(context, size, SoftWeight.EXTRABOLD, color, scale)
        val fm = paint.fontMetrics
        val textW = paint.measureText(text) / scale
        val textH = (fm.descent - fm.ascent) / scale
        val margin = 4f
        val w = textW + 20f + 2 * margin
        val h = textH + 6f + 2 * margin
        val chip = SoftPaint.raised(context, w, h, 14f, p, margin, small = true, scale = scale)
        Canvas(chip).drawText(text, (margin + 10f) * scale, (margin + 3f) * scale - fm.ascent, paint)
        chip
    }
    val scale = SoftPaint.textScale(context)
    Image(ImageProvider(bmp), contentDescription = text, modifier = GlanceModifier.size((bmp.width / scale).dp, (bmp.height / scale).dp))
}

/** A raised row (radius 14dp) of [width] × [height] dp; [content] is laid out inside it, centred vertically. */
@Composable
fun SoftKey(width: Float, height: Float, onClick: Action? = null, padH: Float = 14f, content: @Composable RowScope.() -> Unit) {
    val context = LocalContext.current
    val p = LocalSoft.current
    val bmp = remember(width, height, p) { SoftPaint.raised(context, width, height, 14f, p, 3f, small = true) }
    var m = GlanceModifier.size(width.dp, height.dp)
    if (onClick != null) m = m.clickable(onClick)
    Box(m) {
        Image(ImageProvider(bmp), contentDescription = null, contentScale = ContentScale.FillBounds, modifier = GlanceModifier.fillMaxSize())
        Row(GlanceModifier.fillMaxSize().padding(horizontal = (3f + padH).dp), verticalAlignment = Alignment.CenterVertically, content = content)
    }
}

/** A sunken box (radius 18dp) of [width] × [height] dp with [content] inside it. */
@Composable
fun SoftWell(width: Float, height: Float, padH: Float = 14f, padV: Float = 8f, content: @Composable ColumnScope.() -> Unit) {
    val context = LocalContext.current
    val p = LocalSoft.current
    val bmp = remember(width, height, p) { SoftPaint.well(context, width, height, 18f, p) }
    Box(GlanceModifier.size(width.dp, height.dp)) {
        Image(ImageProvider(bmp), contentDescription = null, contentScale = ContentScale.FillBounds, modifier = GlanceModifier.fillMaxSize())
        Column(GlanceModifier.fillMaxSize().padding(horizontal = padH.dp, vertical = padV.dp), content = content)
    }
}

/** A chart drawn in a well, [width] × [height] dp. */
@Composable
fun SoftChart(width: Float, height: Float, points: List<Double>, reference: Double?, up: Boolean, radius: Float = 18f) {
    val context = LocalContext.current
    val p = LocalSoft.current
    val bmp = remember(width, height, points, reference, up, p) { SoftPaint.chartWell(context, width, height, radius, p, points, reference, up) }
    Image(ImageProvider(bmp), contentDescription = null, contentScale = ContentScale.FillBounds, modifier = GlanceModifier.size(width.dp, height.dp))
}

/** A progress bar in a sunken track. */
@Composable
fun SoftProgress(width: Float, height: Float, progress: Float, color: Int) {
    val context = LocalContext.current
    val p = LocalSoft.current
    val bmp = remember(width, height, progress, color, p) { SoftPaint.progressWell(context, width, height, p, progress, color) }
    Image(ImageProvider(bmp), contentDescription = null, contentScale = ContentScale.FillBounds, modifier = GlanceModifier.size(width.dp, height.dp))
}

/** Any drawable (e.g. a weather icon) as a Soft-sized image. */
@Composable
fun SoftIcon(resId: Int, size: Float) {
    Image(ImageProvider(resId), contentDescription = null, modifier = GlanceModifier.size(size.dp))
}
