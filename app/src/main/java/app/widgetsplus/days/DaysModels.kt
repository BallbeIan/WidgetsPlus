package app.widgetsplus.days

import android.content.Context
import androidx.annotation.StringRes
import app.widgetsplus.R
import java.text.NumberFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale
import org.json.JSONObject

/** A date to count down to (or up from). Repeating events count down to their next yearly occurrence. */
data class DayEvent(
    val id: String,
    val name: String,
    val date: LocalDate,
    val repeatsYearly: Boolean,
) {
    /**
     * The date the countdown is about: the event's own date, or for a repeating event its next occurrence from
     * [today] on (today itself included). Feb 29 becomes Feb 28 in other years ([LocalDate.withYear] does that).
     */
    fun target(today: LocalDate): LocalDate {
        if (!repeatsYearly || !date.isBefore(today)) return date
        val thisYear = date.withYear(today.year)
        return if (thisYear.isBefore(today)) date.withYear(today.year + 1) else thisYear
    }

    /** Days from [today] to [target]: positive ahead, 0 today, negative for past one-off events. */
    fun daysUntil(today: LocalDate): Long = ChronoUnit.DAYS.between(today, target(today))

    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("date", date.toString())
        .put("yearly", repeatsYearly)

    companion object {
        fun fromJson(o: JSONObject) = DayEvent(
            id = o.getString("id"),
            name = o.getString("name"),
            date = LocalDate.parse(o.getString("date")),
            repeatsYearly = o.optBoolean("yearly", false),
        )
    }
}

data class DaysState(
    val events: List<DayEvent>,
    /** Goes up when a widget's choice changes or widgets should recompute "today", so running widgets redraw. */
    val version: Int = 0,
) {
    /** Upcoming events soonest first, then past events most recent first. */
    fun sorted(today: LocalDate): List<DayEvent> {
        val (upcoming, past) = events.partition { it.daysUntil(today) >= 0 }
        return upcoming.sortedBy { it.daysUntil(today) } + past.sortedByDescending { it.daysUntil(today) }
    }
}

object DaysFormat {
    fun date(date: LocalDate): String = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault()).format(date)

    fun number(n: Long): String = NumberFormat.getIntegerInstance().format(n)

    /** "Today", "12 days to go", "3 days ago". */
    fun countText(context: Context, days: Long): String {
        val n = kotlin.math.abs(days).toInt()
        return when {
            days == 0L -> context.getString(R.string.days_today)
            days > 0 -> context.resources.getQuantityString(R.plurals.days_count_to_go, n, number(days))
            else -> context.resources.getQuantityString(R.plurals.days_count_ago, n, number(-days))
        }
    }

    /** "12 Dec 2026", plus "every year" for repeating events. */
    fun dateLine(context: Context, event: DayEvent, today: LocalDate): String {
        val d = date(event.target(today))
        return if (event.repeatsYearly) context.getString(R.string.days_date_yearly, d) else d
    }
}

/** How far through today, this week, this month and this year we are. */
enum class Period(@StringRes val label: Int) {
    YEAR(R.string.days_this_year),
    MONTH(R.string.days_this_month),
    WEEK(R.string.days_this_week),
    DAY(R.string.days_today);

    /** 0..1. Uses real time between the period's start and end, so DST days are handled. */
    fun progress(now: ZonedDateTime = ZonedDateTime.now(), locale: Locale = Locale.getDefault()): Float {
        val zone: ZoneId = now.zone
        val today = now.toLocalDate()
        val start: LocalDate
        val end: LocalDate
        when (this) {
            DAY -> { start = today; end = today.plusDays(1) }
            WEEK -> {
                start = today.with(TemporalAdjusters.previousOrSame(WeekFields.of(locale).firstDayOfWeek))
                end = start.plusWeeks(1)
            }
            MONTH -> { start = today.withDayOfMonth(1); end = start.plusMonths(1) }
            YEAR -> { start = today.withDayOfYear(1); end = start.plusYears(1) }
        }
        val from = start.atStartOfDay(zone).toInstant().toEpochMilli()
        val to = end.atStartOfDay(zone).toInstant().toEpochMilli()
        val at = now.toInstant().toEpochMilli()
        return ((at - from).toFloat() / (to - from)).coerceIn(0f, 1f)
    }

    companion object {
        /** Whole percent, rounded down so 100% only shows at the very end. */
        fun percentText(progress: Float): String =
            NumberFormat.getPercentInstance().apply { maximumFractionDigits = 0 }.format(kotlin.math.floor(progress * 100) / 100)
    }
}
