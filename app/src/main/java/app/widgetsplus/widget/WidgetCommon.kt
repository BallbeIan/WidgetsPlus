package app.widgetsplus.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll
import androidx.glance.color.ColorProvider
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.widgetsplus.data.Repository
import app.widgetsplus.data.WatchItem
import app.widgetsplus.data.Format
import app.widgetsplus.ui.MainActivity
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

// Price up and down colours, slightly brighter on dark backgrounds.
private val UpDay = Color(0xFF1B873F)
private val UpNight = Color(0xFF4CD07D)
private val DownDay = Color(0xFFC62828)
private val DownNight = Color(0xFFFF6B6B)

val UpColor = ColorProvider(day = UpDay, night = UpNight)
val DownColor = ColorProvider(day = DownDay, night = DownNight)

fun changeColor(change: Double) = if (change >= 0) UpColor else DownColor

private fun isNight(context: Context) =
    context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

/** Redraws every placed widget from the current data. */
suspend fun updateAllWidgets(context: Context) {
    SingleQuoteWidget().updateAll(context)
    WatchlistWidget().updateAll(context)
}

fun hasAnyWidgets(context: Context): Boolean {
    val manager = AppWidgetManager.getInstance(context)
    return listOf(SingleQuoteWidgetReceiver::class.java, WatchlistWidgetReceiver::class.java)
        .any { manager.getAppWidgetIds(ComponentName(context, it)).isNotEmpty() }
}

fun openAppIntent(context: Context): Intent =
    Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

fun openPageIntent(item: WatchItem): Intent =
    Intent(Intent.ACTION_VIEW, Format.pageFor(item)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

/** The refresh button on the widgets. */
class RefreshAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        Repository.get(context).refresh()
    }
}

/**
 * Draws a small line chart. The line is green or red depending on whether the last point is above [reference],
 * which is drawn as a dashed line when it falls inside the chart's range.
 */
fun sparkline(context: Context, points: List<Double>, reference: Double, widthDp: Float, heightDp: Float): Bitmap? {
    if (points.size < 2 || widthDp < 8 || heightDp < 8) return null
    val density = context.resources.displayMetrics.density
    // Widgets have a total bitmap memory limit, so keep charts at a modest resolution.
    val scale = density.coerceAtMost(2.5f)
    val w = (widthDp * scale).roundToInt().coerceAtMost(900)
    val h = (heightDp * scale).roundToInt().coerceAtMost(500)
    val night = isNight(context)
    val up = points.last() >= reference
    val color = (if (up) if (night) UpNight else UpDay else if (night) DownNight else DownDay).toArgb()

    val min = points.min()
    val max = points.max()
    val span = (max - min).takeIf { it > 0 } ?: 1.0
    val stroke = 1.6f * scale
    val pad = stroke
    fun x(i: Int) = pad + (w - 2 * pad) * i / (points.size - 1)
    fun y(v: Double) = (pad + (h - 2 * pad) * (1 - (v - min) / span)).toFloat()

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    if (reference in min..max) {
        val dash = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            alpha = 90
            strokeWidth = 1f * scale
            style = Paint.Style.STROKE
            pathEffect = DashPathEffect(floatArrayOf(3 * scale, 3 * scale), 0f)
        }
        canvas.drawLine(0f, y(reference), w.toFloat(), y(reference), dash)
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

/** Background refresh while at least one widget is on the home screen. */
class RefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        if (!hasAnyWidgets(applicationContext)) return Result.success()
        Repository.get(applicationContext).refresh()
        return Result.success()
    }

    companion object {
        private const val PERIODIC = "refresh-periodic"
        private const val NOW = "refresh-now"
        private val online = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        /** Every 30 minutes. Android may delay it when the phone is idle or in battery saver. */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<RefreshWorker>(30, TimeUnit.MINUTES).setConstraints(online).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        fun refreshSoon(context: Context) {
            val request = OneTimeWorkRequestBuilder<RefreshWorker>().setConstraints(online).build()
            WorkManager.getInstance(context).enqueueUniqueWork(NOW, ExistingWorkPolicy.KEEP, request)
        }

        fun cancelIfNoWidgets(context: Context) {
            if (!hasAnyWidgets(context)) WorkManager.getInstance(context).cancelUniqueWork(PERIODIC)
        }
    }
}
