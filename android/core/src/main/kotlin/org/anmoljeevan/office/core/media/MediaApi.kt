package org.anmoljeevan.office.core.media

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import org.anmoljeevan.office.core.ScriptClient
import org.anmoljeevan.office.core.Text
import org.anmoljeevan.office.core.int
import org.anmoljeevan.office.core.obj
import org.anmoljeevan.office.core.requireOk
import org.anmoljeevan.office.core.str
import java.time.Instant

/** One month's shared chart: what was made each day, and what was uploaded each day. */
data class MediaChart(
    val month: String,
    val total: Int,
    val submittedAt: String,
    val updatedAt: String,
    val note: String,
    val data: Map<String, String>,
    val uploaded: Map<String, String>,
    val uploadedTotal: Int,
) {
    val hasData: Boolean get() = data.isNotEmpty() || uploaded.isNotEmpty()

    /** Work types saved under a name that isn't one of the fixed ones (and that have something in them). */
    fun extraTypes(): List<String> {
        val out = mutableListOf<String>()
        listOf(data, uploaded).forEach { d ->
            d.forEach { (k, v) ->
                if (k !in WorkTypes.CATEGORIES && k !in out && v.replace(".", "").isNotEmpty()) out += k
            }
        }
        return out
    }
}

data class StockAdjustment(val opening: Int, val notNeeded: Int)

/** One piece of content in the Content Stock list. */
data class ContentItem(
    val id: String,
    val title: String,
    val type: String,
    val link: String,
    val status: String,
    val platform: String,
    val addedAt: String,
    val updatedAt: String,
)

object ItemStatus {
    const val READY = "Ready"
    const val UPLOADED = "Uploaded"
    const val NOT_NEEDED = "Not needed"
    val ALL = listOf(READY, UPLOADED, NOT_NEEDED)
}

val PLATFORMS = listOf("YouTube", "Instagram", "Facebook", "WhatsApp", "Other")

data class MediaLogin(val version: Int, val serverNow: Instant?) {
    /** The page needs at least version 2 of the Google script. */
    val supported: Boolean get() = version >= 2
    /** Version 3+: the "uploaded" chart. */
    val hasUploads: Boolean get() = version >= 3
    /** Version 4+: the content list and stock adjustments. */
    val hasStock: Boolean get() = version >= 4
}

data class MediaListing(val charts: List<MediaChart>, val stock: Map<String, StockAdjustment>)

/** The media-office side of the Apps Script (everything media.html calls), for one media key. */
class MediaApi(private val client: ScriptClient, private val key: String) {

    private suspend fun call(action: String, vararg params: Pair<String, String>): JsonObject =
        client.call(listOf("action" to action, "key" to key) + params.toList())

    suspend fun login(): MediaLogin {
        val o = call("mediaLogin")
        return MediaLogin(version = o["version"].int(0), serverNow = Text.parseInstant(o["now"].str()))
    }

    suspend fun chart(month: String): MediaChart? = call("mediaGet", "month" to month).obj("chart")?.let(::parseChart)

    suspend fun save(month: String, changes: String, upChanges: String?, note: String?): MediaChart {
        val params = mutableListOf("month" to month, "changes" to changes)
        if (upChanges != null) params += "upChanges" to upChanges
        if (note != null) params += "note" to note
        val o = call("mediaSave", *params.toTypedArray()).requireOk()
        return parseChart(o.obj("chart") ?: JsonObject(emptyMap()))
    }

    suspend fun list(): MediaListing {
        val o = call("mediaList")
        val charts = (o["charts"] as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }.map(::parseChart)
        return MediaListing(charts, parseStock(o.obj("stock")))
    }

    suspend fun setStock(type: String, field: String, value: Int): Map<String, StockAdjustment> =
        parseStock(call("mediaStockSet", "type" to type, "field" to field, "value" to value.toString()).obj("stock"))

    suspend fun items(): List<ContentItem> =
        (call("mediaItems")["items"] as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }.map(::parseItem)

    /** Creates (no [id]) or updates one piece of content. Only the fields that are passed change. */
    suspend fun saveItem(
        id: String? = null,
        title: String? = null,
        type: String? = null,
        link: String? = null,
        status: String? = null,
        platform: String? = null,
    ): ContentItem {
        val p = mutableListOf<Pair<String, String>>()
        id?.let { p += "id" to it }
        title?.let { p += "title" to it }
        type?.let { p += "type" to it }
        link?.let { p += "link" to it }
        status?.let { p += "status" to it }
        platform?.let { p += "platform" to it }
        val o = call("mediaItemSave", *p.toTypedArray()).requireOk()
        return parseItem(o.obj("item") ?: JsonObject(emptyMap()))
    }

    suspend fun deleteItem(id: String) {
        call("mediaItemDelete", "id" to id).requireOk()
    }
}

private fun stringMap(o: JsonObject?): Map<String, String> =
    o?.mapValues { it.value.str() }.orEmpty()

fun parseChart(o: JsonObject): MediaChart = MediaChart(
    month = o["month"].str(),
    total = o["total"].int(0),
    submittedAt = o["submittedAt"].str(),
    updatedAt = o["updatedAt"].str(),
    note = o["note"].str(),
    data = stringMap(o.obj("data")),
    uploaded = stringMap(o.obj("uploaded")),
    uploadedTotal = o["uploadedTotal"].int(0),
)

fun parseStock(o: JsonObject?): Map<String, StockAdjustment> =
    o?.mapNotNull { (k, v) ->
        (v as? JsonObject)?.let { k to StockAdjustment(it["opening"].int(0), it["notNeeded"].int(0)) }
    }?.toMap().orEmpty()

fun parseItem(o: JsonObject): ContentItem = ContentItem(
    id = o["id"].str(),
    title = o["title"].str(),
    type = o["type"].str(),
    link = o["link"].str(),
    status = o["status"].str(),
    platform = o["platform"].str(),
    addedAt = o["addedAt"].str(),
    updatedAt = o["updatedAt"].str(),
)
