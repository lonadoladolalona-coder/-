package org.anmoljeevan.office.core.admin

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import org.anmoljeevan.office.core.Text
import org.anmoljeevan.office.core.arr
import org.anmoljeevan.office.core.str
import java.time.Instant

/** One row of the "Registrations" sheet. */
data class Registration(
    val id: String,
    val timestamp: String,
    val source: String,
    val name: String,
    val phone: String,
    val email: String,
    val details: String,
    val status: String,
    /** Name of the past event it was moved to; empty while it is still in the main list. */
    val archive: String,
    val archivedAt: String,
) {
    val isActive: Boolean get() = archive.isEmpty()
    val statusOrPending: String get() = if (status in Statuses.ALL) status else Statuses.PENDING
    val instant: Instant? get() = Text.parseInstant(timestamp)
}

object Statuses {
    const val PENDING = "Pending"
    const val CONTACTED = "Contacted"
    const val CONFIRMED = "Confirmed"
    val ALL = listOf(PENDING, CONTACTED, CONFIRMED)
}

object Sources {
    const val WOMENS = "Women's Meet"
    const val EVENT = "Event Booking"
    const val INVITE = "Speaking Invitation"
    val ALL = listOf(WOMENS, EVENT, INVITE)
}

data class RegistrationList(
    val rows: List<Registration>,
    /** False while the Google script is older than the Archive column (no "Move to history"). */
    val archiveSupported: Boolean,
)

fun parseRegistrations(obj: JsonObject): RegistrationList {
    val data = obj.arr("data") ?: JsonArray(emptyList())
    val objects = data.mapNotNull { it as? JsonObject }
    val rows = objects.map { o ->
        Registration(
            id = o["ID"].str(),
            timestamp = o["Timestamp"].str(),
            source = o["Source"].str(),
            name = o["Name"].str(),
            phone = o["Phone"].str(),
            email = o["Email"].str(),
            details = o["Details"].str(),
            status = o["Status"].str(),
            archive = o["Archive"].str(),
            archivedAt = o["ArchivedAt"].str(),
        )
    }
    return RegistrationList(rows, archiveSupported = objects.isEmpty() || objects.first().containsKey("Archive"))
}

object Phones {
    fun digits(phone: String): String = phone.filter { it in '0'..'9' }

    /** Last 10 digits, so "+91 98xxx" and "98xxx" match; "" when there are too few digits. */
    fun key(phone: String): String {
        val d = digits(phone)
        return if (d.length >= 8) d.takeLast(10) else ""
    }

    /** wa.me link for a phone (digits as typed, like the web page), or null without digits. */
    fun whatsAppUrl(phone: String, message: String? = null): String? {
        val d = digits(phone)
        if (d.isEmpty()) return null
        val base = "https://wa.me/$d"
        return if (message == null) base else base + "?text=" + java.net.URLEncoder.encode(message, "UTF-8").replace("+", "%20")
    }
}

enum class RegFilter(val label: String, val source: String?) {
    ALL("All", null),
    WOMENS("Women's Meet", Sources.WOMENS),
    EVENT("Event Booking", Sources.EVENT),
    INVITE("Speaking Invitation", Sources.INVITE),
    DUPLICATES("Duplicates", null),
}

enum class SortField { TIMESTAMP, NAME, PHONE, EMAIL, STATUS, SOURCE }

data class SortOption(val field: SortField, val ascending: Boolean, val label: String)

val SORT_OPTIONS = listOf(
    SortOption(SortField.TIMESTAMP, false, "Newest first"),
    SortOption(SortField.TIMESTAMP, true, "Oldest first"),
    SortOption(SortField.NAME, true, "Name (A–Z)"),
    SortOption(SortField.NAME, false, "Name (Z–A)"),
    SortOption(SortField.STATUS, true, "Status (A–Z)"),
    SortOption(SortField.SOURCE, true, "Source (A–Z)"),
)

data class RegStats(
    val total: Int,
    val bySource: Map<String, Int>,
    val pending: Int,
    val contacted: Int,
    val confirmed: Int,
    val duplicates: Int,
    val inHistory: Int,
)

/**
 * The admin page's rules for the registrations list. Duplicate detection is display-only: it
 * never removes or merges anything, it just flags rows that share a phone number.
 */
object RegistrationRules {

    /** How many people still in the main list share each phone key. */
    fun duplicateCounts(all: List<Registration>): Map<String, Int> {
        val counts = HashMap<String, Int>()
        all.forEach { r ->
            if (!r.isActive) return@forEach // people already in History aren't duplicates of today's list
            val k = Phones.key(r.phone)
            if (k.isNotEmpty()) counts[k] = (counts[k] ?: 0) + 1
        }
        return counts
    }

    fun dupCount(r: Registration, counts: Map<String, Int>): Int {
        if (!r.isActive) return 1
        val k = Phones.key(r.phone)
        return if (k.isEmpty()) 1 else counts[k] ?: 1
    }

    /** One row per phone number — the newest registration wins. Rows without a phone all stay. */
    fun dedupeByPhone(rows: List<Registration>): List<Registration> {
        val map = LinkedHashMap<String, Registration>()
        var blank = 0
        rows.forEach { r ->
            val key = Phones.key(r.phone).ifEmpty { "__blank" + (blank++) }
            val existing = map[key]
            if (existing == null || isNewer(r, existing)) map[key] = r
        }
        return map.values.toList()
    }

    private fun isNewer(a: Registration, b: Registration): Boolean {
        val ta = a.instant ?: return false
        val tb = b.instant ?: return false
        return ta.isAfter(tb)
    }

    fun filter(all: List<Registration>, filter: RegFilter, query: String): List<Registration> {
        val counts = duplicateCounts(all)
        val active = all.filter { it.isActive }
        val base = when (filter) {
            RegFilter.ALL -> active
            RegFilter.DUPLICATES -> active.filter { dupCount(it, counts) > 1 }
            else -> active.filter { it.source == filter.source }
        }
        val q = query.trim().lowercase()
        if (q.isEmpty()) return base
        return base.filter { r -> listOf(r.name, r.phone, r.email, r.details).joinToString(" ").lowercase().contains(q) }
    }

    fun sort(rows: List<Registration>, option: SortOption): List<Registration> {
        val cmp = Comparator<Registration> { a, b -> Text.naturalCompare(a.field(option.field), b.field(option.field)) }
        return rows.sortedWith(if (option.ascending) cmp else cmp.reversed())
    }

    private fun Registration.field(f: SortField): String = when (f) {
        SortField.TIMESTAMP -> timestamp
        SortField.NAME -> name
        SortField.PHONE -> phone
        SortField.EMAIL -> email
        SortField.STATUS -> status
        SortField.SOURCE -> source
    }

    fun stats(all: List<Registration>): RegStats {
        val counts = duplicateCounts(all)
        val active = all.filter { it.isActive }
        val bySource = Sources.ALL.associateWith { s -> active.count { it.source == s } }
        return RegStats(
            total = active.size,
            bySource = bySource,
            pending = active.count { it.statusOrPending == Statuses.PENDING },
            contacted = active.count { it.statusOrPending == Statuses.CONTACTED },
            confirmed = active.count { it.statusOrPending == Statuses.CONFIRMED },
            duplicates = active.count { dupCount(it, counts) > 1 },
            inHistory = all.size - active.size,
        )
    }

    fun countFor(all: List<Registration>, filter: RegFilter): Int = filter(all, filter, "").size

    /** The Confirmed people a Zoom-link send goes to: one per phone number. */
    fun confirmedPool(rows: List<Registration>): List<Registration> =
        dedupeByPhone(rows.filter { it.status == Statuses.CONFIRMED })

    /** "Women's Meet — September 2026", or "Past registrations — …" for a mix of sources. */
    fun defaultArchiveName(rows: List<Registration>, today: java.time.LocalDate): String {
        val sources = rows.map { it.source }.filter { it.isNotEmpty() }.distinct()
        return (if (sources.size == 1) sources.first() else "Past registrations") + " — " + Text.monthYear(today)
    }

    /** Registrations moved to History, grouped by event name, newest move first. */
    fun pastGroups(all: List<Registration>, query: String): List<PastGroup> {
        val q = query.trim().lowercase()
        val groups = LinkedHashMap<String, MutableList<Registration>>()
        all.filter { !it.isActive }.forEach { groups.getOrPut(it.archive) { mutableListOf() }.add(it) }
        return groups.map { (name, rows) ->
            val at = rows.maxOfOrNull { it.archivedAt }.orEmpty()
            val shown = when {
                q.isEmpty() || name.lowercase().contains(q) -> rows
                else -> rows.filter { r -> listOf(r.name, r.phone, r.email, r.details).joinToString(" ").lowercase().contains(q) }
            }
            PastGroup(name, shown, rows, at)
        }.filter { it.shown.isNotEmpty() }.sortedByDescending { it.at }
    }

    fun pastEventCount(all: List<Registration>): Int = all.filter { !it.isActive }.map { it.archive }.distinct().size
}

data class PastGroup(
    val name: String,
    /** The people that match the search (all of them without a search). */
    val shown: List<Registration>,
    val all: List<Registration>,
    val at: String,
) {
    val confirmed: Int get() = all.count { it.status == Statuses.CONFIRMED }
}
