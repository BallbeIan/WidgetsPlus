package app.widgetsplus.core.widget.soft

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils
import app.widgetsplus.R
import app.widgetsplus.core.TextTone
import app.widgetsplus.core.WidgetBackgrounds
import app.widgetsplus.core.WidgetStyle
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.sqrt

/*
 * The Soft design (neumorphism): widgets look pressed out of a pale blue-grey material. Depth comes only from paired
 * light/dark shadows (light top-left, dark bottom-right): raised panels, chips and rows, sunken wells for charts and
 * lists, Nunito as the typeface, and colour only for direction (teal up, coral down).
 *
 * Glance can't draw shadows or use custom fonts, so these pieces are painted into bitmaps here and placed by the
 * Glance layer in SoftGlance.kt.
 */

/** Soft's colours, worked out from a widget's style (background, opacity, text colour). All ARGB ints. */
data class SoftPalette(
    val base: Int,
    val highlight: Int,
    val lowlight: Int,
    val outerShadow: Int,
    val ink: Int,
    val secondary: Int,
    val refLine: Int,
    val up: Int,
    val down: Int,
    /** 0..1, from the style's opacity; applied to surfaces and their shadows, never to text. */
    val alpha: Float,
    val dark: Boolean,
) {
    fun direction(up: Boolean) = if (up) this.up else down

    companion object {
        const val DEFAULT_BASE = 0xFFE3E8EF.toInt()

        fun of(style: WidgetStyle): SoftPalette {
            val base = WidgetBackgrounds.byKey(style.background)?.argb?.toInt() ?: DEFAULT_BASE
            val dark = ColorUtils.calculateLuminance(base) < 0.35
            val isDefault = base == DEFAULT_BASE
            val highlight = if (dark) blend(base, Color.WHITE, 0.10f) else Color.WHITE
            val lowlight = when {
                isDefault -> 0xFFB8C1CE.toInt()
                dark -> blend(base, Color.BLACK, 0.45f)
                else -> blend(base, Color.BLACK, 0.18f)
            }
            val outer = when {
                isDefault -> Color.argb(115, 60, 72, 92)
                dark -> Color.argb(150, 0, 0, 0)
                else -> ColorUtils.setAlphaComponent(blend(base, Color.BLACK, 0.6f), 115)
            }
            val lightText = when (style.text) {
                TextTone.LIGHT -> true
                TextTone.DARK -> false
                TextTone.AUTO -> dark
            }
            return SoftPalette(
                base = base,
                highlight = highlight,
                lowlight = lowlight,
                outerShadow = outer,
                ink = if (lightText) 0xFFE9EDF3.toInt() else 0xFF3B4656.toInt(),
                secondary = if (lightText) 0xFF9AA4B2.toInt() else 0xFF8592A3.toInt(),
                refLine = if (dark) 0xFF5C6573.toInt() else 0xFFAAB4C2.toInt(),
                up = 0xFF2FB59A.toInt(),
                down = 0xFFF2716B.toInt(),
                alpha = style.opacity / 100f,
                dark = dark,
            )
        }

        private fun blend(a: Int, b: Int, t: Float) = ColorUtils.blendARGB(a, b, t)
    }
}

/** Font weights from the spec. */
enum class SoftWeight(val wght: Int) { SEMIBOLD(600), BOLD(700), EXTRABOLD(800) }

object SoftPaint {

    @Volatile private var nunito: Typeface? = null

    private fun font(context: Context): Typeface =
        nunito ?: synchronized(this) {
            nunito ?: (runCatching { ResourcesCompat.getFont(context, R.font.nunito) }.getOrNull() ?: Typeface.DEFAULT).also { nunito = it }
        }

    /** Pixels per dp for text: sharp, but capped so many small text images stay cheap. */
    fun textScale(context: Context) = context.resources.displayMetrics.density.coerceIn(1f, 3f)

    /** Pixels per dp for surfaces: shadows are soft anyway, so a lower resolution keeps big bitmaps small. */
    fun surfaceScale(context: Context, widthDp: Float, heightDp: Float): Float {
        val s = context.resources.displayMetrics.density.coerceIn(1f, 2f)
        // Keep each surface bitmap under about 700k pixels (2.8 MB).
        val maxScale = sqrt(700_000f / (widthDp * heightDp).coerceAtLeast(1f))
        return min(s, maxScale)
    }

    fun textPaint(context: Context, sizeDp: Float, weight: SoftWeight, color: Int, scale: Float, letterSpacingDp: Float = 0f) =
        Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            typeface = font(context)
            fontVariationSettings = "'wght' ${weight.wght}"
            fontFeatureSettings = "tnum"
            textSize = sizeDp * scale
            this.color = color
            if (letterSpacingDp != 0f) letterSpacing = letterSpacingDp / sizeDp
        }

    /** Shortens [text] with an ellipsis until it fits [maxWidthPx]. */
    fun fit(text: String, paint: Paint, maxWidthPx: Float?): String {
        if (maxWidthPx == null || paint.measureText(text) <= maxWidthPx) return text
        var end = text.length
        while (end > 1 && paint.measureText(text.substring(0, end) + "…") > maxWidthPx) end--
        return text.substring(0, end).trimEnd() + "…"
    }

    /** One line of text as a bitmap, sized to the text. Its size in dp is bitmap size / [scale]. */
    fun text(context: Context, text: String, sizeDp: Float, weight: SoftWeight, color: Int, maxWidthDp: Float? = null, letterSpacingDp: Float = 0f): Bitmap {
        val scale = textScale(context)
        val paint = textPaint(context, sizeDp, weight, color, scale, letterSpacingDp)
        val line = fit(text, paint, maxWidthDp?.times(scale))
        val fm = paint.fontMetrics
        val w = ceil(paint.measureText(line)).toInt().coerceAtLeast(1)
        val h = ceil(fm.descent - fm.ascent).toInt().coerceAtLeast(1)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        Canvas(bmp).drawText(line, 0f, -fm.ascent, paint)
        return bmp
    }

    // ------------------------------------------------------------------ surfaces

    /**
     * A raised rounded rectangle with its two shadows, inset by [marginDp] inside the bitmap so the shadows have room.
     * Used for panels (big shadows) and for keys, chips and rows ([small]).
     */
    fun raised(context: Context, widthDp: Float, heightDp: Float, radiusDp: Float, p: SoftPalette, marginDp: Float, small: Boolean, scale: Float = surfaceScale(context, widthDp, heightDp)): Bitmap {
        val bmp = Bitmap.createBitmap((widthDp * scale).toInt().coerceAtLeast(1), (heightDp * scale).toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val m = marginDp * scale
        val rect = RectF(m, m, bmp.width - m, bmp.height - m)
        val r = (radiusDp * scale).coerceAtMost(min(rect.width(), rect.height()) / 2)
        val off = (if (small) 3f else 6f) * scale
        val blur = (if (small) 7f else 15f) * scale
        val alpha = (p.alpha * 255).toInt()

        fun shadow(color: Int, dx: Float, dy: Float, strength: Float) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                this.alpha = (Color.alpha(color) * strength * p.alpha).toInt().coerceIn(0, 255)
                maskFilter = BlurMaskFilter(blur / 2, BlurMaskFilter.Blur.NORMAL)
            }
            c.drawRoundRect(RectF(rect).apply { offset(dx, dy) }, r, r, paint)
        }
        if (small) {
            shadow(p.lowlight, off, off, 1f)
            shadow(p.highlight, -off, -off, 1f)
        } else {
            shadow(p.highlight, -off, -off, 0.65f)
            shadow(p.outerShadow, off, off, 1f)
        }
        c.drawRoundRect(rect, r, r, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = p.base; this.alpha = alpha })
        return bmp
    }

    /** A sunken well: the base colour with inset shadows (dark top-left inside, light bottom-right inside). */
    fun sunken(c: Canvas, rect: RectF, radiusPx: Float, p: SoftPalette, scale: Float) {
        val r = radiusPx.coerceAtMost(min(rect.width(), rect.height()) / 2)
        val shape = Path().apply { addRoundRect(rect, r, r, Path.Direction.CW) }
        c.drawPath(shape, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = p.base; alpha = (p.alpha * 255).toInt() })
        val off = 4f * scale
        val blur = 8f * scale
        c.save()
        c.clipPath(shape)
        // A thick frame around the well; only its shadow falls inside the clip.
        fun inset(color: Int, dx: Float, dy: Float) {
            val frame = Path().apply {
                fillType = Path.FillType.EVEN_ODD
                addRect(RectF(rect).apply { inset(-blur * 3, -blur * 3) }, Path.Direction.CW)
                addRoundRect(RectF(rect).apply { offset(dx, dy) }, r, r, Path.Direction.CW)
            }
            c.drawPath(frame, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                alpha = (Color.alpha(color) * p.alpha).toInt()
                maskFilter = BlurMaskFilter(blur / 2, BlurMaskFilter.Blur.NORMAL)
            })
        }
        inset(p.lowlight, off, off)
        inset(p.highlight, -off, -off)
        c.restore()
    }

    fun well(context: Context, widthDp: Float, heightDp: Float, radiusDp: Float, p: SoftPalette): Bitmap {
        val scale = surfaceScale(context, widthDp, heightDp)
        val bmp = Bitmap.createBitmap((widthDp * scale).toInt().coerceAtLeast(1), (heightDp * scale).toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        sunken(Canvas(bmp), RectF(0f, 0f, bmp.width.toFloat(), bmp.height.toFloat()), radiusDp * scale, p, scale)
        return bmp
    }

    /**
     * A chart inside a well: smooth curve (quadratic midpoints) in the direction colour with a soft glow, a dotted
     * reference line, and an end dot ringed in the base colour. [reference] may be null (no line).
     */
    fun chartWell(context: Context, widthDp: Float, heightDp: Float, radiusDp: Float, p: SoftPalette, points: List<Double>, reference: Double?, up: Boolean): Bitmap {
        val scale = context.resources.displayMetrics.density.coerceIn(1f, 2.5f).let { s ->
            min(s, sqrt(600_000f / (widthDp * heightDp).coerceAtLeast(1f)))
        }
        val bmp = Bitmap.createBitmap((widthDp * scale).toInt().coerceAtLeast(1), (heightDp * scale).toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        sunken(c, RectF(0f, 0f, bmp.width.toFloat(), bmp.height.toFloat()), radiusDp * scale, p, scale)
        if (points.size < 2) return bmp

        val pad = 10f * scale
        val area = RectF(pad, pad, bmp.width - pad, bmp.height - pad)
        var min = points.min()
        var max = points.max()
        if (reference != null) { min = minOf(min, reference); max = maxOf(max, reference) }
        val span = (max - min).takeIf { it > 0 } ?: 1.0
        fun x(i: Int) = area.left + area.width() * i / (points.size - 1)
        fun y(v: Double) = (area.top + area.height() * (1 - (v - min) / span)).toFloat()
        val color = p.direction(up)

        if (reference != null) {
            val dots = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = p.refLine
                strokeWidth = 2f * scale
                strokeCap = Paint.Cap.ROUND
                style = Paint.Style.STROKE
                pathEffect = DashPathEffect(floatArrayOf(0.01f, 6f * scale), 0f)
            }
            c.drawLine(area.left, y(reference), area.right, y(reference), dots)
        }
        val path = Path().apply {
            moveTo(x(0), y(points[0]))
            for (i in 1 until points.size) {
                val mx = (x(i - 1) + x(i)) / 2
                val my = (y(points[i - 1]) + y(points[i])) / 2
                quadTo(x(i - 1), y(points[i - 1]), mx, my)
            }
            lineTo(x(points.lastIndex), y(points.last()))
        }
        // Soft under-glow, then the line.
        c.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = ColorUtils.setAlphaComponent(color, 46)
            strokeWidth = 5f * scale
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            maskFilter = BlurMaskFilter(2f * scale, BlurMaskFilter.Blur.NORMAL)
        })
        c.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            strokeWidth = 2.2f * scale
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
        })
        val ex = x(points.lastIndex)
        val ey = y(points.last())
        c.drawCircle(ex, ey, 5f * scale, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = p.base })
        c.drawCircle(ex, ey, 3.2f * scale, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color })
        return bmp
    }

    /** A horizontal bar in a sunken track, for progress (0..1). */
    fun progressWell(context: Context, widthDp: Float, heightDp: Float, p: SoftPalette, progress: Float, color: Int): Bitmap {
        val scale = context.resources.displayMetrics.density.coerceIn(1f, 2.5f)
        val bmp = Bitmap.createBitmap((widthDp * scale).toInt().coerceAtLeast(1), (heightDp * scale).toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val r = bmp.height / 2f
        sunken(c, RectF(0f, 0f, bmp.width.toFloat(), bmp.height.toFloat()), r, p, scale)
        val inset = 3f * scale
        val w = (bmp.width - 2 * inset) * progress.coerceIn(0f, 1f)
        if (w > 0) {
            val fr = RectF(inset, inset, inset + maxOf(w, bmp.height - 2 * inset), bmp.height - inset)
            c.drawRoundRect(fr, fr.height() / 2, fr.height() / 2, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color })
        }
        return bmp
    }
}
