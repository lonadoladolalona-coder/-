package org.anmoljeevan.office.core.media

import org.anmoljeevan.office.core.Csv
import org.anmoljeevan.office.core.Text
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** One work type in "Stock by type". Left to upload = Opening + Made − Uploaded − Not needed. */
data class LedgerRow(
    val type: String,
    val made: Int,
    val uploaded: Int,
    val opening: Int,
    val notNeeded: Int,
    /** Everything that should end up uploaded: Opening + Made − Not needed (never below 0). */
    val pipeline: Int,
    val left: Int,
    /** More was uploaded / marked not needed than was recorded as made. */
    val over: Boolean,
    /** Pieces of this type in the content list that are still "Ready". */
    val ready: Int,
    val percent: Int,
) {
    /** How the content list compares with what is left: 0 = all listed, >0 = to add, <0 = extra. */
    val toList: Int get() = left - ready
}

data class Ledger(
    val rows: List<LedgerRow>,
    val made: Int,
    val uploaded: Int,
    val opening: Int,
    val notNeeded: Int,
    val left: Int,
    val pipeline: Int,
    val percent: Int,
    /** Left to upload but not in the content list yet. */
    val unlisted: Int,
    /** Nothing filled in anywhere yet. */
    val isEmpty: Boolean,
)

data class MonthRow(val month: String, val perType: List<Int>, val made: Int, val uploaded: Int) {
    val label: String get() = StockRules.monthLabel(month)
}

data class MonthTable(val types: List<String>, val rows: List<MonthRow>, val sums: List<Int>, val made: Int, val uploaded: Int)

/** media.html's Stock Summary maths. */
object StockRules {
    fun percent(part: Int, whole: Int): Int = if (whole <= 0) 0 else min(100, (part.toDouble() / whole * 100).roundToInt())

    fun typeList(charts: List<MediaChart>, stock: Map<String, StockAdjustment>, items: List<ContentItem>): List<String> {
        val extra = mutableListOf<String>()
        fun add(k: String) { if (k !in WorkTypes.CATEGORIES && k !in extra) extra += k }
        charts.forEach { c -> (c.data.keys + c.uploaded.keys).forEach(::add) }
        stock.keys.forEach(::add)
        items.forEach { add(it.type) }
        return WorkTypes.CATEGORIES + extra
    }

    /** The stock numbers always look at every month. */
    fun ledger(charts: List<MediaChart>, stock: Map<String, StockAdjustment>, items: List<ContentItem>, hasStock: Boolean): Ledger {
        val all = charts.filter { it.hasData }
        val hasAdjust = stock.values.any { it.opening != 0 || it.notNeeded != 0 }
        val isEmpty = all.isEmpty() && !hasAdjust && !(hasStock && items.isNotEmpty())
        val rows = typeList(all, stock, items).map { type ->
            val made = all.sumOf { Cells.total(it.data[type]) }
            val up = all.sumOf { Cells.total(it.uploaded[type]) }
            val opening = stock[type]?.opening ?: 0
            val notNeeded = stock[type]?.notNeeded ?: 0
            val raw = opening + made - up - notNeeded
            val pipeline = max(0, opening + made - notNeeded)
            val ready = items.count { it.type == type && it.status == ItemStatus.READY }
            LedgerRow(type, made, up, opening, notNeeded, pipeline, max(0, raw), raw < 0, ready, percent(up, pipeline))
        }
        val made = rows.sumOf { it.made }
        val up = rows.sumOf { it.uploaded }
        val opening = rows.sumOf { it.opening }
        val notNeeded = rows.sumOf { it.notNeeded }
        val pipeline = max(0, opening + made - notNeeded)
        return Ledger(
            rows = rows,
            made = made,
            uploaded = up,
            opening = opening,
            notNeeded = notNeeded,
            left = rows.sumOf { it.left },
            pipeline = pipeline,
            percent = percent(up, pipeline),
            unlisted = rows.sumOf { max(0, it.left - it.ready) },
            isEmpty = isEmpty,
        )
    }

    fun years(charts: List<MediaChart>): List<String> = charts.map { it.month.take(4) }.distinct().sortedDescending()

    /** Charts with something in them, for one year ("all" = every month). */
    fun visible(charts: List<MediaChart>, year: String): List<MediaChart> =
        charts.filter { (year == "all" || it.month.startsWith("$year-")) && it.hasData }

    /** "By month": newest month first; respects the year filter. */
    fun monthTable(charts: List<MediaChart>, year: String, stock: Map<String, StockAdjustment>, items: List<ContentItem>): MonthTable {
        val all = charts.filter { it.hasData }
        val shown = visible(charts, year)
        val types = typeList(shown.ifEmpty { all }, stock, items)
        val rows = shown.sortedByDescending { it.month }.map { c ->
            val per = types.map { Cells.total(c.data[it]) }
            MonthRow(c.month, per, per.sum(), types.sumOf { Cells.total(c.uploaded[it]) })
        }
        val sums = types.indices.map { i -> rows.sumOf { it.perType[i] } }
        return MonthTable(types, rows, sums, sums.sum(), rows.sumOf { it.uploaded })
    }

    fun monthLabel(month: String): String {
        val ym = Cells.parseMonthKey(month) ?: return month
        return Text.monthYear(ym.atDay(1))
    }

    /** One row per month and work type — easy to sort or pivot in Google Sheets. */
    fun totalsCsv(charts: List<MediaChart>, year: String, hasUploads: Boolean, stock: Map<String, StockAdjustment>, items: List<ContentItem>): String? {
        val shown = visible(charts, year)
        if (shown.isEmpty()) return null
        val types = typeList(shown, stock, items)
        val rows = mutableListOf<List<String>>()
        shown.sortedByDescending { it.month }.forEach { c ->
            types.forEach { t ->
                val m = Cells.total(c.data[t])
                val u = Cells.total(c.uploaded[t])
                if (m == 0 && u == 0) return@forEach
                rows += listOf(monthLabel(c.month), t, m.toString()) + if (hasUploads) listOf(u.toString()) else emptyList()
            }
        }
        return Csv.table(listOf("Month", "Type", "Made") + if (hasUploads) listOf("Uploaded") else emptyList(), rows)
    }
}

data class ItemView(val counts: Map<String, Int>, val baseCount: Int, val shown: List<ContentItem>)

/** media.html's Content Stock list rules. */
object ItemRules {
    const val ALL = "all"

    fun types(items: List<ContentItem>, extraTypes: List<String>): List<String> {
        val extra = items.map { it.type }.filter { it !in WorkTypes.CATEGORIES }.distinct()
        return WorkTypes.CATEGORIES + extraTypes.filter { it !in extra && it !in WorkTypes.CATEGORIES } + extra
    }

    fun matches(item: ContentItem, query: String): Boolean =
        query.isBlank() || (item.title + " " + item.type).lowercase().contains(query.trim().lowercase())

    /** Counts respect the type filter and the search, so the numbers on the status tabs match the list. */
    fun view(items: List<ContentItem>, status: String, type: String, query: String): ItemView {
        val base = items.filter { (type == ALL || it.type == type) && matches(it, query) }
        val counts = ItemStatus.ALL.associateWith { s -> base.count { it.status == s } }
        val shown = base.filter { status == ALL || it.status == status }.let { list ->
            if (status == ItemStatus.READY || status == ALL) list.sortedByDescending { it.addedAt } else list.sortedByDescending { it.updatedAt }
        }
        return ItemView(counts, base.size, shown)
    }

    fun safeUrl(link: String): String? = link.trim().takeIf { it.startsWith("http://", true) || it.startsWith("https://", true) }

    fun csv(items: List<ContentItem>): String = Csv.table(
        listOf("Title", "Type", "Status", "Uploaded to", "Link", "Added", "Last changed"),
        items.sortedByDescending { it.addedAt }.map {
            listOf(it.title, it.type, it.status, it.platform, it.link, Text.longDate(it.addedAt), Text.longDate(it.updatedAt))
        },
    )
}
