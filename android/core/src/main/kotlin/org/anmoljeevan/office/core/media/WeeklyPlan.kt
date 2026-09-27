package org.anmoljeevan.office.core.media

import org.anmoljeevan.office.core.Text
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** A piece of the plan. [days]: 0 = Sunday … 6 = Saturday. [span]: made over a stretch of days. */
data class PlanPart(
    val what: String,
    val qty: String,
    val sub: String,
    val days: List<Int> = emptyList(),
    val span: IntRange? = null,
    val icon: String? = null,
) {
    val allDays: List<Int> get() = span?.toList() ?: days
}

data class PlanRow(val n: Int, val type: String, val color: Long, val icon: String, val parts: List<PlanPart>)

data class PlanEvent(val date: LocalDate, val name: String, val theme: String, val quote: String)

data class PlanChip(val color: Long, val icon: String, val title: String, val sub: String, val isEvent: Boolean = false)

data class PlanRibbon(val chip: PlanChip, val fromDay: Int, val toDay: Int)

data class WeekBar(val label: String, val isNow: Boolean, val isEvent: Boolean)

/**
 * The media team's weekly content plan — the same plan media.html shows. The same plan repeats
 * every week. To change it, edit [ROWS] (and [PEOPLE] / [EVENT]).
 */
object WeeklyPlan {
    val ROWS = listOf(
        PlanRow(1, "Reels", WorkTypes.color("Reel"), "reel", listOf(
            PlanPart("Reel from Sermon", "2 Reels from Sermon / Week", "Reels · 2 a week", days = listOf(2, 4)),
            PlanPart("Live Reel", "1 Live Reel / Week", "Reels · 1 a week", days = listOf(6), icon = "live"),
        )),
        PlanRow(2, "Long Content", WorkTypes.color("Long Content"), "long", listOf(
            PlanPart("Long Content", "3 Long Content / Week", "3 a week", days = listOf(1, 3, 5)),
        )),
        PlanRow(3, "Short Story Video", 0xFFF0733C, "story", listOf(
            PlanPart("Short Story Video", "1 Video / Week", "1 a week", days = listOf(4)),
        )),
        PlanRow(4, "Promo Creative", 0xFFE8A33D, "promo", listOf(
            PlanPart("Promo Creative", "1 Promo Creative", "Sunday → Friday", span = 0..5),
        )),
        PlanRow(5, "Promo with Pic & Video", 0xFFE0567A, "promovid", listOf(
            PlanPart("Promo with Picture & Video", "Promo with Picture & Video", "Promo", days = listOf(2, 3)),
        )),
        PlanRow(6, "Songs", 0xFF1BA39C, "song", listOf(
            PlanPart("Live Worship Video / Practice Time", "Live Worship Video / Practice Time", "Songs", days = listOf(2)),
        )),
    )

    /** Content selection */
    val PEOPLE = listOf("Lucky", "Babu")

    val EVENT = PlanEvent(LocalDate.of(2026, 10, 20), "One Day Youth Meet", "Zen Z / Gen Z", "Zen for Jesus")

    val FULL_DAYS = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
    val SHORT_DAYS = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    val DAY_LETTERS = listOf("S", "M", "T", "W", "T", "F", "S")

    /** 0 = Sunday … 6 = Saturday, like JavaScript's getDay(). */
    fun dow(date: LocalDate): Int = date.dayOfWeek.value % 7

    fun weekStart(date: LocalDate): LocalDate = date.minusDays(dow(date).toLong())

    /** The made-on-one-day pieces for a date (plus the event itself, when it falls on that date). */
    fun chips(date: LocalDate, event: PlanEvent = EVENT): List<PlanChip> {
        val d = dow(date)
        val out = mutableListOf<PlanChip>()
        ROWS.forEach { r -> r.parts.forEach { p -> if (d in p.days) out += PlanChip(r.color, p.icon ?: r.icon, p.what, p.sub) } }
        if (date == event.date) out.add(0, PlanChip(0xFFE8A33D, "star", event.name, "Youth Meet day", isEvent = true))
        return out
    }

    /** The pieces made over a stretch of days that includes this weekday. */
    fun spans(dow: Int): List<PlanChip> {
        val out = mutableListOf<PlanChip>()
        ROWS.forEach { r -> r.parts.forEach { p -> if (p.span != null && dow in p.span) out += PlanChip(r.color, p.icon ?: r.icon, p.what, p.sub) } }
        return out
    }

    /** Everything on a date: stretches first, then the one-day pieces. */
    fun dayItems(date: LocalDate): List<PlanChip> = spans(dow(date)) + chips(date)

    fun ribbons(): List<PlanRibbon> = ROWS.flatMap { r ->
        r.parts.mapNotNull { p -> p.span?.let { PlanRibbon(PlanChip(r.color, p.icon ?: r.icon, p.what, p.sub), it.first, it.last) } }
    }

    /** Days until the event (0 = today); negative once it has passed. */
    fun daysUntil(today: LocalDate, event: PlanEvent = EVENT): Long = ChronoUnit.DAYS.between(today, event.date)

    /** One bar per week, from this week to the week of the event (empty when it is too far off or past). */
    fun weeksToEvent(today: LocalDate, event: PlanEvent = EVENT): List<WeekBar> {
        if (daysUntil(today, event) < 0) return emptyList()
        val thisWeek = weekStart(today)
        val gap = (ChronoUnit.DAYS.between(thisWeek, weekStart(event.date)) / 7).toInt()
        if (gap > 11) return emptyList()
        return (0..gap).map { i ->
            val label = when (i) {
                gap -> "Meet week"
                0 -> "This week"
                else -> Text.dayMonth(thisWeek.plusWeeks(i.toLong()))
            }
            WeekBar(label, isNow = i == 0 && i != gap, isEvent = i == gap)
        }
    }

    /** How far ‹ › can move from this week. */
    val OFFSETS = -4..12
}
