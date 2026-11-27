package app.widgetsplus.core.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import kotlin.math.roundToInt

/**
 * Glance widgets can't draw, so charts are drawn into a bitmap and shown as an image.
 * Widgets have a total bitmap memory limit, so charts are kept at a modest resolution.
 */
object Charts {

    /**
     * A small line chart with a light fill under the line. If [baseline] is given and falls inside the range,
     * it is drawn as a dashed line (for example yesterday's close).
     */
    fun line(
        context: Context,
        points: List<Double>,
        color: Int,
        widthDp: Float,
        heightDp: Float,
        baseline: Double? = null,
    ): Bitmap? {
        if (points.size < 2 || widthDp < 8 || heightDp < 8) return null
        val scale = context.resources.displayMetrics.density.coerceAtMost(2.5f)
        val w = (widthDp * scale).roundToInt().coerceAtMost(900)
        val h = (heightDp * scale).roundToInt().coerceAtMost(500)

        val min = points.min()
        val max = points.max()
        val span = (max - min).takeIf { it > 0 } ?: 1.0
        val stroke = 1.6f * scale
        val pad = stroke
        fun x(i: Int) = pad + (w - 2 * pad) * i / (points.size - 1)
        fun y(v: Double) = (pad + (h - 2 * pad) * (1 - (v - min) / span)).toFloat()

        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        if (baseline != null && baseline in min..max) {
            val dash = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                alpha = 90
                strokeWidth = 1f * scale
                style = Paint.Style.STROKE
                pathEffect = DashPathEffect(floatArrayOf(3 * scale, 3 * scale), 0f)
            }
            canvas.drawLine(0f, y(baseline), w.toFloat(), y(baseline), dash)
        }
        val line = Path().apply {
            moveTo(x(0), y(points[0]))
            for (i in 1 until points.size) lineTo(x(i), y(points[i]))
        }
        val fill = Path(line).apply {
            lineTo(x(points.size - 1), h.toFloat())
            lineTo(x(0), h.toFloat())
            close()
        }
        canvas.drawPath(fill, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; alpha = 40 })
        canvas.drawPath(line, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            strokeWidth = stroke
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
        })
        return bitmap
    }
}
