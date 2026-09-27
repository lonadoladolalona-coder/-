package org.anmoljeevan.office.core.admin

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.anmoljeevan.office.core.ScriptClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.net.URLDecoder

class RunBulkTest {
    private fun params(url: String): Map<String, String> = url.substringAfter('?').split('&').associate {
        val (k, v) = it.split('=', limit = 2)
        URLDecoder.decode(k, "UTF-8") to URLDecoder.decode(v, "UTF-8")
    }

    @Test
    fun sendsBatchesOf25() {
        val calls = mutableListOf<Map<String, String>>()
        val client = ScriptClient("https://x/exec", { url ->
            val p = params(url).also { calls += it }
            """{"ok":true,"updated":${p.getValue("ids").split(',').size},"at":"2026-09-27T00:00:00Z"}"""
        }, Dispatchers.Unconfined)
        val ids = (1..60).map { "id$it" }
        val out = runBlocking { AdminApi(client, "k").runBulk("bulkStatus", ids, listOf("status" to "Confirmed")) }
        assertEquals(ids, out.doneIds)
        assertNull(out.error)
        assertEquals(listOf(25, 25, 10), calls.map { it.getValue("ids").split(',').size })
        assertEquals("Confirmed", calls.first()["status"])
        assertEquals("k", calls.first()["key"])
    }

    @Test
    fun olderScriptFallsBackToOneRequestPerPerson() {
        val actions = mutableListOf<String>()
        val client = ScriptClient("https://x/exec", { url ->
            val p = params(url)
            actions += p.getValue("action")
            if (p["action"] == "bulkDelete") """{"ok":true,"data":[]}""" else """{"ok":true}"""
        }, Dispatchers.Unconfined)
        val out = runBlocking { AdminApi(client, "k").runBulk("bulkDelete", listOf("a", "b")) }
        assertEquals(listOf("a", "b"), out.doneIds)
        assertEquals(listOf("bulkDelete", "delete", "delete"), actions)
    }

    @Test
    fun archiveOnAnOlderScriptAsksForTheUpdate() {
        val client = ScriptClient("https://x/exec", { """{"ok":true,"data":[]}""" }, Dispatchers.Unconfined)
        val out = runBlocking { AdminApi(client, "k").runBulk("archive", listOf("a"), listOf("name" to "Retreat")) }
        assertEquals(BulkOutcome.NEEDS_UPDATE, out.error)
        assertEquals(emptyList<String>(), out.doneIds)
    }

    @Test
    fun stopsAtTheFirstError() {
        var n = 0
        val client = ScriptClient("https://x/exec", {
            n++
            if (n == 1) """{"ok":true,"updated":25,"at":""}""" else """{"error":"unauthorized"}"""
        }, Dispatchers.Unconfined)
        val out = runBlocking { AdminApi(client, "k").runBulk("unarchive", (1..30).map { "$it" }) }
        assertEquals(25, out.doneIds.size)
        assertEquals("session expired — log in again", out.error)
    }

    @Test
    fun meetingMarkSendsTheMessageOnlyWhenGiven() {
        val seen = mutableListOf<Map<String, String>>()
        val client = ScriptClient("https://x/exec", { url ->
            seen += params(url)
            """{"ok":true,"meeting":{"id":"m1","name":"Meet","recipients":[{"id":"1","name":"A","phone":"9","result":"sent","at":"t"}],"sent":1,"skipped":0}}"""
        }, Dispatchers.Unconfined)
        val api = AdminApi(client, "k")
        val m = runBlocking { api.meetingMark(MeetingMark("Meet", "sent", "1", "A", "9", message = "Hi {name}")) }
        runBlocking { api.meetingMark(MeetingMark("Meet", "skipped", "2", "B", "8")) }
        assertEquals("Hi {name}", seen[0]["message"])
        assertNull(seen[1]["message"])
        assertEquals(1, m.sent)
        assertEquals("sent", m.recipients.single().result)
    }
}
