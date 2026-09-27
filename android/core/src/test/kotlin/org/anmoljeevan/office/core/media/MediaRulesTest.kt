package org.anmoljeevan.office.core.media

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class CellsTest {
    @Test
    fun valuesAndTotals() {
        assertEquals(7, Cells.value("7"))
        assertEquals(1, Cells.value("c"))
        assertEquals(0, Cells.value("p"))
        assertEquals(0, Cells.value(""))
        assertEquals(4, Cells.total("2..1.c"))
        assertEquals(0, Cells.total(null))
        assertEquals("✓", Cells.display("c"))
        assertEquals("R", Cells.display("r"))
        assertEquals("12", Cells.display("12"))
    }

    @Test
    fun cleaningWhatWasTyped() {
        assertEquals("12", Cells.clean("12"))
        assertEquals("7", Cells.clean("007"))
        assertEquals("123", Cells.clean("12345"))
        assertEquals("c", Cells.clean("C"))
        assertEquals("c", Cells.clean("✓"))
        assertEquals("p", Cells.clean("2p"))
        assertEquals("3", Cells.clean("3x"))
        assertEquals("", Cells.clean("xyz"))
        assertEquals("0", Cells.clean("0"))
        assertEquals("5", Cells.token("005"))
        assertEquals("", Cells.token("1234"))
        assertEquals("o", Cells.token("O"))
    }

    @Test
    fun stepper() {
        assertEquals("1", Cells.increment(""))
        assertEquals("1", Cells.increment("p"))
        assertEquals("2", Cells.increment("c"))
        assertEquals("4", Cells.increment("3"))
        assertEquals("999", Cells.increment("999"))
        assertEquals("", Cells.decrement("1"))
        assertEquals("2", Cells.decrement("3"))
        assertEquals("", Cells.decrement("c"))
        assertEquals("", Cells.decrement(""))
    }

    @Test
    fun splitAndJoin() {
        val cells = Cells.split("2..1.c")
        assertEquals(31, cells.size)
        assertEquals(listOf("2", "", "1", "c"), cells.take(4))
        assertEquals("2..1.c", Cells.join(cells))
        assertEquals("2026-09", Cells.monthKey(YearMonth.of(2026, 9)))
    }
}

class CellGridTest {
    @Test
    fun editsArePendingUntilSaved() {
        val g = CellGrid.fromRemote(mapOf("Reel" to "1.2")).edit("Reel", 3, "c").edit("Long Content", 1, "4")
        assertEquals("c", g.get("Reel", 3))
        assertEquals("1", g.get("Reel", 1))
        assertTrue(g.hasPending)
        assertEquals(4, g.rowTotal("Reel", 30))
        assertEquals(8, g.grandTotal(listOf("Reel", "Long Content"), 30))
        val json = Json.parseToJsonElement(g.pendingJson()) as JsonObject
        assertEquals("""{"3":"c"}""", json["Reel"].toString())
        assertEquals("""{"1":"4"}""", json["Long Content"].toString())
    }

    @Test
    fun mergeKeepsCellsChangedHere() {
        val mine = CellGrid.fromRemote(mapOf("Reel" to "1")).edit("Reel", 2, "5")
        val merged = mine.mergedWith(mapOf("Reel" to "9.1.1", "Video Promo" to "..2"))
        assertEquals("9", merged.get("Reel", 1)) // a teammate's change comes in
        assertEquals("5", merged.get("Reel", 2)) // mine is kept until it is saved
        assertEquals("1", merged.get("Reel", 3))
        assertEquals("2", merged.get("Video Promo", 3))
        assertTrue(merged.hasPending)
    }

    @Test
    fun mergeClearsCellsATeammateCleared() {
        val g = CellGrid.fromRemote(mapOf("Reel" to "1.1")).mergedWith(mapOf("Reel" to ".1"))
        assertEquals("", g.get("Reel", 1))
        assertEquals("1", g.get("Reel", 2))
    }

    @Test
    fun pruneOnlyForgetsWhatWasSentUnchanged() {
        val g = CellGrid().edit("Reel", 1, "1").edit("Reel", 2, "2")
        val sent = g.pending
        val later = g.edit("Reel", 2, "3") // changed again while the save was on its way
        val pruned = later.pruned(sent)
        assertEquals(mapOf("Reel" to mapOf(2 to "3")), pruned.pending)
        assertFalse(g.pruned(sent).hasPending)
    }

    @Test
    fun parsesTheChartAndFindsExtraTypes() {
        val chart = parseChart(
            Json.parseToJsonElement(
                """{"month":"2026-09","total":"5","submittedAt":"","updatedAt":"2026-09-27T06:00:00.000Z","note":"hi","data":{"Reel":"2..1","Podcast":"1"},"uploaded":{"Reel":"1","Empty":"..."},"uploadedTotal":1}""",
            ) as JsonObject,
        )
        assertEquals(5, chart.total)
        assertEquals("2..1", chart.data["Reel"])
        assertEquals(listOf("Podcast"), chart.extraTypes())
    }
}

class StockRulesTest {
    private fun chart(month: String, data: Map<String, String>, uploaded: Map<String, String> = emptyMap()) =
        MediaChart(month, 0, "", "", "", data, uploaded, 0)

    private fun item(type: String, status: String, added: String = "2026-09-01") =
        ContentItem(added + type + status, "t", type, "", status, "", added, added)

    @Test
    fun ledgerFollowsLeftToUploadFormula() {
        val charts = listOf(
            chart("2026-08", mapOf("Reel" to "2.2"), mapOf("Reel" to "1")),
            chart("2026-09", mapOf("Reel" to "c.1", "Video Promo" to "1"), mapOf("Video Promo" to "3")),
            chart("2026-07", emptyMap()),
        )
        val stock = mapOf("Reel" to StockAdjustment(opening = 5, notNeeded = 2))
        val items = listOf(item("Reel", ItemStatus.READY), item("Reel", ItemStatus.UPLOADED))
        val l = StockRules.ledger(charts, stock, items, hasStock = true)
        val reel = l.rows.first { it.type == "Reel" }
        assertEquals(6, reel.made)
        assertEquals(1, reel.uploaded)
        assertEquals(9, reel.pipeline) // 5 + 6 − 2
        assertEquals(8, reel.left) // 5 + 6 − 1 − 2
        assertEquals(11, reel.percent) // 1 of 9
        assertEquals(1, reel.ready)
        assertEquals(7, reel.toList)
        val promo = l.rows.first { it.type == "Video Promo" }
        assertTrue(promo.over)
        assertEquals(0, promo.left)
        assertEquals(7, l.made)
        assertEquals(4, l.uploaded)
        assertEquals(10, l.pipeline) // 5 + 7 − 2
        assertEquals(40, l.percent)
        assertEquals(7, l.unlisted)
        assertFalse(l.isEmpty)
        assertTrue(StockRules.ledger(emptyList(), emptyMap(), emptyList(), true).isEmpty)
    }

    @Test
    fun monthTableAndCsv() {
        val charts = listOf(
            chart("2025-12", mapOf("Reel" to "1")),
            chart("2026-09", mapOf("Reel" to "2", "Podcast" to "1"), mapOf("Reel" to "1")),
        )
        val t = StockRules.monthTable(charts, "all", emptyMap(), emptyList())
        assertEquals(listOf("2026-09", "2025-12"), t.rows.map { it.month })
        assertEquals("Podcast", t.types.last())
        assertEquals(4, t.made)
        assertEquals(1, t.uploaded)
        assertEquals("September 2026", t.rows.first().label)
        assertEquals(listOf("2026", "2025"), StockRules.years(charts))
        assertEquals(listOf("2025-12"), StockRules.visible(charts, "2025").map { it.month })
        val csv = StockRules.totalsCsv(charts, "2026", true, emptyMap(), emptyList())!!
        assertEquals(
            listOf("﻿\"Month\",\"Type\",\"Made\",\"Uploaded\"", "\"September 2026\",\"Reel\",\"2\",\"1\"", "\"September 2026\",\"Podcast\",\"1\",\"0\""),
            csv.lines(),
        )
    }

    @Test
    fun itemView() {
        val items = listOf(
            item("Reel", ItemStatus.READY, "2026-09-02"),
            item("Reel", ItemStatus.READY, "2026-09-05"),
            item("Video Promo", ItemStatus.UPLOADED),
            item("Podcast", ItemStatus.NOT_NEEDED),
        )
        val v = ItemRules.view(items, ItemStatus.READY, ItemRules.ALL, "")
        assertEquals(listOf("2026-09-05", "2026-09-02"), v.shown.map { it.addedAt })
        assertEquals(mapOf(ItemStatus.READY to 2, ItemStatus.UPLOADED to 1, ItemStatus.NOT_NEEDED to 1), v.counts)
        assertEquals(1, ItemRules.view(items, ItemRules.ALL, "Video Promo", "").baseCount)
        assertEquals(1, ItemRules.view(items, ItemRules.ALL, ItemRules.ALL, "podc").shown.size)
        assertEquals("Podcast", ItemRules.types(items, listOf("Extra")).let { it[it.size - 1] })
        assertTrue("Extra" in ItemRules.types(items, listOf("Extra")))
        assertEquals(null, ItemRules.safeUrl("javascript:alert(1)"))
        assertEquals("https://youtu.be/x", ItemRules.safeUrl(" https://youtu.be/x "))
    }
}

class WeeklyPlanTest {
    @Test
    fun tuesdayHasItsPieces() {
        val tue = LocalDate.of(2026, 9, 29)
        assertEquals(2, WeeklyPlan.dow(tue))
        assertEquals(
            listOf("Reel from Sermon", "Promo with Picture & Video", "Live Worship Video / Practice Time"),
            WeeklyPlan.chips(tue).map { it.title },
        )
        assertEquals(listOf("Promo Creative"), WeeklyPlan.spans(2).map { it.title })
        assertEquals(emptyList<PlanChip>(), WeeklyPlan.spans(6))
        assertEquals(LocalDate.of(2026, 9, 27), WeeklyPlan.weekStart(tue))
    }

    @Test
    fun theEventDay() {
        val event = WeeklyPlan.EVENT
        val chips = WeeklyPlan.chips(event.date)
        assertTrue(chips.first().isEvent)
        assertEquals(23, WeeklyPlan.daysUntil(LocalDate.of(2026, 9, 27)))
        val weeks = WeeklyPlan.weeksToEvent(LocalDate.of(2026, 9, 27))
        assertEquals(listOf("This week", "4 Oct", "11 Oct", "Meet week"), weeks.map { it.label })
        assertTrue(weeks.first().isNow)
        assertTrue(weeks.last().isEvent)
        assertEquals(emptyList<WeekBar>(), WeeklyPlan.weeksToEvent(event.date.plusDays(1)))
    }
}
