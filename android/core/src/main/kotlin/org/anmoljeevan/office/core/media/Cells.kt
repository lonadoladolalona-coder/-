package org.anmoljeevan.office.core.media

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.time.YearMonth

/** The work types on the chart, in order, with the colours the web page uses. */
object WorkTypes {
    val CATEGORIES = listOf("Reel", "Long Content", "Sunday Flyer", "Monday Flyer", "Video Promo", "T-Shirt Work", "Other Work")

    private val COLORS = mapOf(
        "Reel" to 0xFF7C5CFF,
        "Long Content" to 0xFF2F6DF6,
        "Sunday Flyer" to 0xFFE8A33D,
        "Monday Flyer" to 0xFFF0733C,
        "Video Promo" to 0xFFE0567A,
        "T-Shirt Work" to 0xFF1BA39C,
        "Other Work" to 0xFF69758C,
    )

    /** ARGB colour for a work type; grey for anything saved under a name that isn't in the list. */
    fun color(type: String): Long = COLORS[type] ?: 0xFF69758C

    /** Work types the same way the chart and exports list them: the fixed ones, then any extra ones found. */
    fun withExtras(extras: Iterable<String>): List<String> {
        val out = CATEGORIES.toMutableList()
        extras.forEach { if (it !in out) out += it }
        return out
    }
}

/**
 * One cell of the chart: "" empty, "7" a number, or a status letter —
 * c = completed, p = in progress, r = under review, o = off / holiday.
 * Total = the numbers plus 1 for every "c".
 */
object Cells {
    const val DAYS = 31
    val STATUS_LETTERS = listOf("c", "p", "r", "o")

    fun isNumber(v: String): Boolean = v.isNotEmpty() && v.all { it in '0'..'9' }

    fun value(v: String): Int = when {
        isNumber(v) -> v.toIntOrNull() ?: 0
        v == "c" -> 1
        else -> 0
    }

    fun display(v: String): String = when (v) {
        "c" -> "✓"
        "p", "r", "o" -> v.uppercase()
        else -> v
    }

    fun label(v: String): String = when (v) {
        "c" -> "One finished piece"
        "p" -> "In progress"
        "r" -> "In review"
        "o" -> "Nothing that day"
        "" -> "Empty"
        else -> if (v == "1") "1 piece" else "$v pieces"
    }

    /** Cleans what someone typed into a cell — media.html's liveClean. */
    fun clean(raw: String): String {
        val s = raw.lowercase().trim()
        if (s.isEmpty()) return ""
        if (s.all { it in '0'..'9' }) return s.take(3).trimLeadingZeros()
        val last = s.last()
        if (last in "cpro✓✔") return if (last == '✓' || last == '✔') "c" else last.toString()
        return s.filter { it in '0'..'9' }.take(3).trimLeadingZeros()
    }

    /** What the script accepts for a cell: a whole number up to 3 digits, c / p / r / o, or empty. */
    fun token(t: String): String {
        val s = t.lowercase()
        if (s.length in 1..3 && s.all { it in '0'..'9' }) return s.toInt().toString()
        if (s in STATUS_LETTERS) return s
        return ""
    }

    /** +1 on a cell: a number goes up, a ✓ (which counts as one) becomes 2, anything else becomes 1. */
    fun increment(v: String): String = when {
        isNumber(v) -> minOf(999, value(v) + 1).toString()
        v == "c" -> "2"
        else -> "1"
    }

    /** −1 on a cell: numbers go down and 1 becomes empty; a ✓ becomes empty; letters clear. */
    fun decrement(v: String): String = when {
        isNumber(v) && value(v) > 1 -> (value(v) - 1).toString()
        else -> ""
    }

    fun split(s: String?): List<String> {
        val parts = (s ?: "").split('.').take(DAYS).toMutableList()
        while (parts.size < DAYS) parts += ""
        return parts
    }

    fun join(cells: List<String>): String = cells.joinToString(".").trimEnd('.')

    fun total(s: String?): Int = (s ?: "").split('.').sumOf { value(it) }

    fun monthKey(ym: YearMonth): String = "%04d-%02d".format(ym.year, ym.monthValue)

    fun parseMonthKey(key: String): YearMonth? = runCatching { YearMonth.parse(key) }.getOrNull()

    private fun String.trimLeadingZeros(): String = if (isEmpty()) this else trimStart('0').ifEmpty { "0" }
}

/**
 * One chart ("made" or "uploaded") as this screen sees it. [pending] holds the cells changed
 * here but not saved yet, e.g. {"Reel": {3: "2"}}. Only those are sent on save, and the
 * script merges them into the shared chart — so several people can fill it in at once.
 */
data class CellGrid(
    val cells: Map<String, List<String>> = emptyMap(),
    val pending: Map<String, Map<Int, String>> = emptyMap(),
) {
    fun get(type: String, day: Int): String = cells[type]?.getOrNull(day - 1).orEmpty()

    fun row(type: String): List<String> = cells[type] ?: List(Cells.DAYS) { "" }

    /** This screen changed a cell: it shows at once and is sent with the next save. */
    fun edit(type: String, day: Int, v: String): CellGrid {
        require(day in 1..Cells.DAYS)
        val row = row(type).toMutableList().also { it[day - 1] = v }
        val p = (pending[type] ?: emptyMap()) + (day to v)
        return CellGrid(cells + (type to row), pending + (type to p))
    }

    val hasPending: Boolean get() = pending.isNotEmpty()

    fun rowTotal(type: String, days: Int): Int = (1..days).sumOf { Cells.value(get(type, it)) }

    fun grandTotal(types: List<String>, days: Int): Int = types.sumOf { rowTotal(it, days) }

    /** True when any type has something filled in on this day. */
    fun hasEntry(day: Int): Boolean = cells.values.any { it.getOrNull(day - 1).orEmpty().isNotEmpty() }

    /** The pending cells as the script expects them: {"Reel": {"3": "2"}}. */
    fun pendingJson(): String = JsonObject(
        pending.mapValues { (_, days) -> JsonObject(days.entries.associate { (d, v) -> d.toString() to JsonPrimitive(v) }) },
    ).toString()

    /** Forget whatever was sent and hasn't been changed again since. */
    fun pruned(sent: Map<String, Map<Int, String>>): CellGrid {
        val p = pending.mapValues { (type, days) ->
            days.filter { (d, v) -> sent[type]?.get(d) != v }
        }.filterValues { it.isNotEmpty() }
        return copy(pending = p)
    }

    /** Bring in what the rest of the team saved, without touching cells changed here but not saved yet. */
    fun mergedWith(remote: Map<String, String>): CellGrid {
        val remoteCells = remote.mapValues { Cells.split(it.value) }
        val types = remoteCells.keys + cells.keys
        val merged = types.associateWith { type ->
            val mine = pending[type].orEmpty()
            List(Cells.DAYS) { i ->
                val d = i + 1
                if (d in mine) get(type, d) else remoteCells[type]?.get(i).orEmpty()
            }
        }
        return copy(cells = merged)
    }

    companion object {
        fun fromRemote(data: Map<String, String>): CellGrid = CellGrid(cells = data.mapValues { Cells.split(it.value) })
    }
}
