package app.widgetsplus.clock

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import app.widgetsplus.core.widget.soft.SoftPaint
import app.widgetsplus.core.widget.soft.SoftPalette
import kotlin.math.cos
import kotlin.math.sin

/** Room around the Soft dial for its shadows, in dp. */
internal const val SOFT_DIAL_MARGIN = 12f

/**
 * The Soft analog clock's face: a raised disc with paired shadows, a sunken inner face and 12 tick marks, as a
 * [sizeDp] square bitmap. The disc's diameter is [sizeDp] minus twice [SOFT_DIAL_MARGIN].
 */
internal fun paintSoftDial(context: Context, sizeDp: Float, p: SoftPalette): Bitmap {
    val scale = SoftPaint.surfaceScale(context, sizeDp, sizeDp)
    val bmp = SoftPaint.raised(context, sizeDp, sizeDp, sizeDp / 2, p, SOFT_DIAL_MARGIN, small = false, scale = scale)
    val c = Canvas(bmp)
    val cx = bmp.width / 2f
    val cy = bmp.height / 2f
    val discR = (sizeDp / 2 - SOFT_DIAL_MARGIN) * scale
    val faceR = discR * 0.86f
    SoftPaint.sunken(c, RectF(cx - faceR, cy - faceR, cx + faceR, cy + faceR), faceR, p, scale)

    // Ticks are drawn at full strength, like text, so the time stays readable at any opacity.
    val tick = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = p.secondary
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    for (i in 0 until 12) {
        val quarter = i % 3 == 0
        tick.strokeWidth = (if (quarter) 3f else 2f) * scale * (sizeDp / 200f).coerceIn(0.6f, 1.2f)
        val a = Math.toRadians(i * 30.0)
        val r1 = faceR * (if (quarter) 0.74f else 0.80f)
        val r2 = faceR * 0.88f
        c.drawLine(
            cx + (r1 * sin(a)).toFloat(), cy - (r1 * cos(a)).toFloat(),
            cx + (r2 * sin(a)).toFloat(), cy - (r2 * cos(a)).toFloat(),
            tick,
        )
    }
    return bmp
}
