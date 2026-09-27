package org.anmoljeevan.office.core

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Formatting and text helpers shared by the admin and media sides. */
object Text {
    private val spaces = Regex("\\s+")

    /** Collapses runs of whitespace and trims — the same clean-up the script applies to names. */
    fun clean(v: String): String = v.replace(spaces, " ").trim()

    /** For comparing names: clean, then lower-case. */
    fun norm(v: String): String = clean(v).lowercase()

    fun parseInstant(iso: String): Instant? =
        if (iso.isBlank()) null else runCatching { Instant.parse(iso.trim()) }.getOrNull()

    private val dayMonthYear = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
    private val dayMonth = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
    private val monthYear = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
    private val longDate = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH)
    private val dateTime = DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", Locale.ENGLISH)
    private val time = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)

    fun dayMonthYear(d: LocalDate): String = dayMonthYear.format(d)
    fun dayMonth(d: LocalDate): String = dayMonth.format(d)
    fun monthYear(d: LocalDate): String = monthYear.format(d)

    /** "27 Sep 2026, 4:32 PM", or "" when the text isn't a date. */
    fun dateTime(iso: String, zone: ZoneId = ZoneId.systemDefault()): String =
        parseInstant(iso)?.let { dateTime.format(it.atZone(zone)) }.orEmpty()

    /** "27 Sep", or "" when the text isn't a date. */
    fun shortDate(iso: String, zone: ZoneId = ZoneId.systemDefault()): String =
        parseInstant(iso)?.let { dayMonth.format(it.atZone(zone)) }.orEmpty()

    /** "September 27, 2026", or "—". */
    fun longDate(iso: String, zone: ZoneId = ZoneId.systemDefault()): String =
        parseInstant(iso)?.let { longDate.format(it.atZone(zone)) } ?: "—"

    /** "today 4:32 PM" / "21 Sep, 4:32 PM". */
    fun whenShort(iso: String, today: LocalDate, zone: ZoneId = ZoneId.systemDefault()): String {
        val at = parseInstant(iso)?.atZone(zone) ?: return ""
        val t = time.format(at)
        return if (at.toLocalDate() == today) "today $t" else dayMonth.format(at) + ", " + t
    }

    /** Indian digit grouping, like the web pages' toLocaleString('en-IN'): 1,23,456. */
    fun number(n: Int): String {
        if (n < 0) return "-" + number(-n)
        val s = n.toString()
        if (s.length <= 3) return s
        val last3 = s.takeLast(3)
        val rest = s.dropLast(3).reversed().chunked(2).joinToString(",").reversed()
        return "$rest,$last3"
    }

    /** "–" for zero, like the web tables. */
    fun numberOrDash(n: Int): String = if (n == 0) "–" else number(n)

    /** HTML-escapes text that came from a public form. */
    fun html(s: String): String = buildString(s.length) {
        for (c in s) when (c) {
            '&' -> append("&amp;")
            '<' -> append("&lt;")
            '>' -> append("&gt;")
            '"' -> append("&quot;")
            '\'' -> append("&#39;")
            else -> append(c)
        }
    }

    /** "AB" from "Anita Bose", "?" for no name. */
    fun initials(name: String): String {
        val parts = name.trim().split(spaces).filter { it.isNotEmpty() }
        if (parts.isEmpty()) return "?"
        val first = parts.first().first()
        val last = if (parts.size > 1) parts.last().first().toString() else ""
        return (first + last).uppercase()
    }

    /**
     * Compares like JavaScript's localeCompare(…, {numeric: true}): case-insensitive, and runs of
     * digits compare as numbers, so "Item 2" sorts before "Item 10".
     */
    val natural: Comparator<String> = Comparator { a, b -> naturalCompare(a, b) }

    fun naturalCompare(a: String, b: String): Int {
        var i = 0
        var j = 0
        while (i < a.length && j < b.length) {
            val ca = a[i]
            val cb = b[j]
            if (ca.isAsciiDigit() && cb.isAsciiDigit()) {
                val si = i
                val sj = j
                while (i < a.length && a[i].isAsciiDigit()) i++
                while (j < b.length && b[j].isAsciiDigit()) j++
                val na = a.substring(si, i).trimStart('0')
                val nb = b.substring(sj, j).trimStart('0')
                if (na.length != nb.length) return na.length.compareTo(nb.length)
                val c = na.compareTo(nb)
                if (c != 0) return c
            } else {
                val c = ca.lowercaseChar().compareTo(cb.lowercaseChar())
                if (c != 0) return c
                i++
                j++
            }
        }
        val rest = (a.length - i).compareTo(b.length - j)
        return if (rest != 0) rest else a.compareTo(b)
    }

    private fun Char.isAsciiDigit() = this in '0'..'9'
}

/** Google's clock, so a phone with the wrong date can't open the wrong month or day. */
class ServerClock(private val offsetMillis: Long = 0) {
    fun now(): Instant = Instant.ofEpochMilli(System.currentTimeMillis() + offsetMillis)
    fun today(zone: ZoneId = ZoneId.systemDefault()): LocalDate = now().atZone(zone).toLocalDate()

    companion object {
        fun fromServerTime(serverNow: Instant?): ServerClock =
            ServerClock(if (serverNow == null) 0 else serverNow.toEpochMilli() - System.currentTimeMillis())
    }
}
