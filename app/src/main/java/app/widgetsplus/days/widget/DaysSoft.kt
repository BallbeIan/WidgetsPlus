package app.widgetsplus.days.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.Action
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.width
import app.widgetsplus.R
import app.widgetsplus.core.widget.soft.LocalSoft
import app.widgetsplus.core.widget.soft.SoftProgress
import app.widgetsplus.core.widget.soft.SoftSurface
import app.widgetsplus.core.widget.soft.SoftText
import app.widgetsplus.core.widget.soft.SoftWeight
import app.widgetsplus.core.widget.soft.softLineHeight
import app.widgetsplus.core.widget.soft.softTextWidth
import app.widgetsplus.days.DayEvent
import app.widgetsplus.days.DaysFormat
import app.widgetsplus.days.Period
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlin.math.abs
import kotlin.math.min

/*
 * The Soft design for the Days widgets: a raised panel with Nunito text; progress bars sit in sunken tracks.
 * Everything is laid out in dp from the inner size SoftSurface passes in.
 */

/** Countdown: a big number (teal while the event is ahead or today, ink once it has passed), label, name and date. */
@Composable
internal fun SoftCountdown(event: DayEvent?, today: LocalDate, onClick: Action) {
    val context = LocalContext.current
    SoftSurface(onClick = onClick, padH = 12f, padV = 10f) { inner ->
        val w = inner.width.value
        val h = inner.height.value
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (event == null) {
                SoftLines(context.getString(R.string.days_widget_empty), 13f, SoftWeight.BOLD, LocalSoft.current.secondary, w, h)
            } else {
                CountdownBody(context, event, today, w, h)
            }
        }
    }
}

@Composable
private fun CountdownBody(context: Context, event: DayEvent, today: LocalDate, w: Float, h: Float) {
    val p = LocalSoft.current
    val days = event.daysUntil(today)
    val isToday = days == 0L
    val big = if (isToday) context.getString(R.string.days_today) else DaysFormat.number(abs(days))
    val label = when {
        isToday -> null
        days > 0 -> context.resources.getQuantityString(R.plurals.days_to_go_label, abs(days).toInt())
        else -> context.resources.getQuantityString(R.plurals.days_ago_label, abs(days).toInt())
    }

    // Which lines fit next to a number of at least MIN_BIG, then the largest number that fits the rest.
    val lineFactor = softLineHeight(context, 100f) / 100f
    val minBig = 22f * lineFactor
    var left = h
    val labelH = softLineHeight(context, 12f)
    val nameH = softLineHeight(context, 14f) + 2f
    val dateH = softLineHeight(context, 11.5f)
    val showLabel = label != null && left - labelH >= minBig
    if (showLabel) left -= labelH
    val showName = left - nameH >= minBig
    if (showName) left -= nameH
    val showDate = showName && left - dateH >= minBig
    if (showDate) left -= dateH

    val byHeight = left / lineFactor
    val byWidth = w / (softTextWidth(context, big, 100f) / 100f).coerceAtLeast(0.01f)
    val bigSize = min(byHeight, byWidth).coerceIn(12f, 110f)

    SoftText(big, bigSize, SoftWeight.EXTRABOLD, if (days >= 0) p.up else p.ink, maxWidth = w, letterSpacing = -0.3f)
    if (showLabel && label != null) SoftText(label, 12f, SoftWeight.BOLD, p.secondary, maxWidth = w)
    if (showName) {
        Spacer(GlanceModifier.height(2.dp))
        SoftText(event.name, 14f, SoftWeight.EXTRABOLD, p.ink, maxWidth = w)
    }
    if (showDate) SoftText(DaysFormat.date(event.target(today)), 11.5f, SoftWeight.BOLD, p.secondary, maxWidth = w)
}

/** Progress: a label and percentage over a bar in a sunken track, for as many periods as fit, year first. */
@Composable
internal fun SoftProgressContent(onClick: Action) {
    val context = LocalContext.current
    val now = ZonedDateTime.now()
    SoftSurface(onClick = onClick, padH = 14f, padV = 10f) { inner ->
        val w = inner.width.value
        val h = inner.height.value
        val labelH = softLineHeight(context, 12f)
        val barH = if (h >= 60) 10f else 8f
        val rowH = labelH + 3f + barH
        val gap = 6f
        val rows = ((h + gap) / (rowH + gap)).toInt().coerceIn(1, Period.entries.size)
        val p = LocalSoft.current

        Spacer(GlanceModifier.defaultWeight())
        Period.entries.take(rows).forEachIndexed { i, period ->
            if (i > 0) Spacer(GlanceModifier.defaultWeight())
            val progress = period.progress(now)
            val pct = Period.percentText(progress)
            val pctW = softTextWidth(context, pct, 12f)
            Row(GlanceModifier.width(w.dp), verticalAlignment = Alignment.CenterVertically) {
                SoftText(context.getString(period.label), 12f, SoftWeight.EXTRABOLD, p.ink, maxWidth = (w - pctW - 6f).coerceAtLeast(10f))
                Spacer(GlanceModifier.defaultWeight())
                SoftText(pct, 12f, SoftWeight.BOLD, p.secondary)
            }
            Spacer(GlanceModifier.height(3.dp))
            SoftProgress(w, barH, progress, p.up)
        }
        Spacer(GlanceModifier.defaultWeight())
    }
}

/** Text wrapped over as many lines as fit [maxHeight], centred; the last line is shortened with an ellipsis. */
@Composable
private fun SoftLines(text: String, size: Float, weight: SoftWeight, color: Int, maxWidth: Float, maxHeight: Float) {
    val context = LocalContext.current
    val maxLines = (maxHeight / softLineHeight(context, size)).toInt().coerceAtLeast(1)
    for (line in wrap(context, text, size, weight, maxWidth, maxLines)) {
        SoftText(line, size, weight, color, maxWidth = maxWidth)
    }
}

private fun wrap(context: Context, text: String, size: Float, weight: SoftWeight, maxWidth: Float, maxLines: Int): List<String> {
    val lines = mutableListOf<String>()
    var current = ""
    val words = text.split(' ')
    for ((i, word) in words.withIndex()) {
        val candidate = if (current.isEmpty()) word else "$current $word"
        if (current.isEmpty() || softTextWidth(context, candidate, size, weight) <= maxWidth) {
            current = candidate
        } else {
            if (lines.size == maxLines - 1) {
                // Last line: the rest goes on it and SoftText cuts it short.
                lines += words.drop(i).joinToString(" ", prefix = "$current ")
                return lines
            }
            lines += current
            current = word
        }
    }
    if (current.isNotEmpty()) lines += current
    return lines
}
