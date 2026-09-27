package org.anmoljeevan.office.core.admin

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.anmoljeevan.office.core.ApiException
import org.anmoljeevan.office.core.ScriptClient
import org.anmoljeevan.office.core.obj
import org.anmoljeevan.office.core.requireOk
import org.anmoljeevan.office.core.str

/** The admin side of the Apps Script (everything admin.html calls), for one admin key. */
class AdminApi(private val client: ScriptClient, private val key: String) {

    private suspend fun call(vararg params: Pair<String, String>): JsonObject =
        client.call(params.toList() + ("key" to key))

    suspend fun list(): RegistrationList = parseRegistrations(call())

    suspend fun setStatus(id: String, status: String) {
        call("action" to "setStatus", "id" to id, "status" to status).requireOk()
    }

    suspend fun delete(id: String) {
        call("action" to "delete", "id" to id).requireOk()
    }

    /** null [BulkReply.updated] = the script is older than bulk actions (it answered with the plain list). */
    suspend fun bulk(action: String, ids: List<String>, extra: List<Pair<String, String>>): BulkReply {
        val o = client.call(listOf("action" to action, "ids" to ids.joinToString(",")) + extra + ("key" to key))
        val updated = (o["updated"] as? JsonPrimitive)?.content?.toDoubleOrNull()?.toInt()
        return BulkReply(updated, o["at"].str())
    }

    /** null = the script is older than meeting history. */
    suspend fun meetings(): List<Meeting>? {
        val o = call("action" to "meetings")
        val list = o["meetings"] as? JsonArray ?: return null
        return list.mapNotNull { it as? JsonObject }.map(::parseMeeting).sortedByDescending { it.updatedAt }
    }

    suspend fun meetingMark(mark: MeetingMark): Meeting {
        val params = mutableListOf(
            "action" to "meetingMark",
            "meeting" to mark.meeting,
            "result" to mark.result,
            "id" to mark.id,
            "name" to mark.name,
            "phone" to mark.phone,
        )
        mark.message?.let { params += "message" to it }
        val o = client.call(params + ("key" to key)).requireOk()
        return parseMeeting(o.obj("meeting") ?: throw ApiException.Server("not saved"))
    }

    /**
     * Runs a bulk action in batches of 25 and reports which IDs were done — admin.html's runBulk.
     * If the Google script is older (it answers with the plain list), status changes and deletes
     * fall back to one request per person; moving to History has no fallback and says so.
     */
    suspend fun runBulk(
        action: String,
        ids: List<String>,
        extra: List<Pair<String, String>> = emptyList(),
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> },
    ): BulkOutcome {
        val single = mapOf("bulkStatus" to "setStatus", "bulkDelete" to "delete")[action]
        val done = mutableListOf<String>()
        var at = ""
        var error: String? = null
        var useBulk = true
        var i = 0
        while (i < ids.size) {
            val chunk = if (useBulk) ids.subList(i, minOf(i + BATCH, ids.size)) else listOf(ids[i])
            try {
                if (useBulk) {
                    val reply = bulk(action, chunk, extra)
                    if (reply.updated == null) {
                        if (single == null) {
                            error = BulkOutcome.NEEDS_UPDATE
                            break
                        }
                        useBulk = false
                        continue
                    }
                    if (reply.at.isNotEmpty()) at = reply.at
                } else if (single == "setStatus") {
                    setStatus(chunk[0], extra.first { it.first == "status" }.second)
                } else {
                    delete(chunk[0])
                }
                done += chunk
                i += chunk.size
                onProgress(done.size, ids.size)
            } catch (e: ApiException) {
                error = if (e is ApiException.Unauthorized) "session expired — log in again" else e.message ?: "failed"
                break
            }
        }
        return BulkOutcome(done, error, at)
    }

    companion object {
        const val BATCH = 25
    }
}

data class BulkReply(val updated: Int?, val at: String)

data class BulkOutcome(val doneIds: List<String>, val error: String?, val at: String) {
    companion object {
        const val NEEDS_UPDATE = "needs-update"
    }
}

/** One person's result in a Zoom-link send, saved to the meeting's history as it happens. */
data class MeetingMark(
    val meeting: String,
    val result: String,
    val id: String,
    val name: String,
    val phone: String,
    /** Sent with the first result of a run only, so the message is saved with the meeting. */
    val message: String? = null,
)
