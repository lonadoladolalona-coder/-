package org.anmoljeevan.office.core.admin

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.anmoljeevan.office.core.Csv
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class RegistrationRulesTest {
    private fun reg(
        id: String,
        phone: String = "",
        name: String = id,
        source: String = Sources.WOMENS,
        status: String = "Pending",
        ts: String = "2026-09-01T10:00:00.000Z",
        archive: String = "",
    ) = Registration(id, ts, source, name, phone, "", "", status, archive, "")

    @Test
    fun parsesTheSheetIncludingNumbersAndOldRows() {
        val json = Json.parseToJsonElement(
            """{"ok":true,"data":[{"Timestamp":"2026-09-20T10:15:00.000Z","Source":"Women's Meet","Name":"Asha","Phone":919876543210,"Email":"","Details":"","ID":"a1","Status":"Confirmed","Archive":"","ArchivedAt":""}]}""",
        ) as JsonObject
        val list = parseRegistrations(json)
        assertEquals("919876543210", list.rows.single().phone)
        assertTrue(list.archiveSupported)

        val old = parseRegistrations(Json.parseToJsonElement("""{"ok":true,"data":[{"Name":"B","ID":"b"}]}""") as JsonObject)
        assertFalse(old.archiveSupported)
        assertTrue(old.rows.single().isActive)
        assertEquals("Pending", old.rows.single().statusOrPending)
    }

    @Test
    fun phoneKeyUsesTheLastTenDigits() {
        assertEquals("9876543210", Phones.key("+91 98765 43210"))
        assertEquals("9876543210", Phones.key("098765-43210"))
        assertEquals("", Phones.key("12345"))
        assertEquals("https://wa.me/919876543210?text=Hi%20Asha%21", Phones.whatsAppUrl("+91 98765 43210", "Hi Asha!"))
        assertEquals(null, Phones.whatsAppUrl("no phone"))
    }

    @Test
    fun duplicatesIgnorePeopleInHistory() {
        val all = listOf(
            reg("1", "+91 98765 43210"),
            reg("2", "9876543210"),
            reg("3", "98765 43210", archive = "Past event"),
            reg("4", "1234567890"),
        )
        val counts = RegistrationRules.duplicateCounts(all)
        assertEquals(2, RegistrationRules.dupCount(all[0], counts))
        assertEquals(1, RegistrationRules.dupCount(all[2], counts))
        assertEquals(listOf("1", "2"), RegistrationRules.filter(all, RegFilter.DUPLICATES, "").map { it.id })
        assertEquals(2, RegistrationRules.stats(all).duplicates)
        assertEquals(1, RegistrationRules.stats(all).inHistory)
    }

    @Test
    fun dedupeKeepsTheNewestPerPhoneAndEveryBlank() {
        val rows = listOf(
            reg("old", "9876543210", ts = "2026-09-01T10:00:00.000Z"),
            reg("new", "+91 9876543210", ts = "2026-09-05T10:00:00.000Z"),
            reg("blank1"),
            reg("blank2"),
        )
        assertEquals(listOf("new", "blank1", "blank2"), RegistrationRules.dedupeByPhone(rows).map { it.id })
    }

    @Test
    fun filterBySourceAndSearch() {
        val all = listOf(
            reg("1", name = "Asha Devi", source = Sources.WOMENS),
            reg("2", name = "Ravi", source = Sources.EVENT),
            reg("3", name = "asha k", source = Sources.EVENT, archive = "Old"),
        )
        assertEquals(listOf("2"), RegistrationRules.filter(all, RegFilter.EVENT, "").map { it.id })
        assertEquals(listOf("1"), RegistrationRules.filter(all, RegFilter.ALL, " ASHA ").map { it.id })
    }

    @Test
    fun sortingFollowsTheWebPage() {
        val rows = listOf(reg("b", name = "bina"), reg("a", name = "Asha"), reg("c", name = "Chitra 10"), reg("d", name = "Chitra 9"))
        val byName = RegistrationRules.sort(rows, SORT_OPTIONS.first { it.label == "Name (A–Z)" }).map { it.id }
        assertEquals(listOf("a", "b", "d", "c"), byName)
        val newest = RegistrationRules.sort(
            listOf(reg("x", ts = "2026-09-01T00:00:00.000Z"), reg("y", ts = "2026-09-03T00:00:00.000Z")),
            SORT_OPTIONS.first(),
        ).map { it.id }
        assertEquals(listOf("y", "x"), newest)
    }

    @Test
    fun statsCountStatusesOfTheMainListOnly() {
        val s = RegistrationRules.stats(
            listOf(reg("1", status = "Confirmed"), reg("2", status = ""), reg("3", status = "Contacted"), reg("4", status = "Confirmed", archive = "X")),
        )
        assertEquals(3, s.total)
        assertEquals(1, s.confirmed)
        assertEquals(1, s.contacted)
        assertEquals(1, s.pending)
        assertEquals(3, s.bySource[Sources.WOMENS])
    }

    @Test
    fun zoomSendHelpers() {
        val meeting = Meeting(
            "m", "Women's Meet — 28 Sep", "", "2026-09-28T00:00:00Z", "",
            listOf(
                MeetingRecipient("1", "A", "9876543210", "sent", ""),
                MeetingRecipient("", "B", "+91 1111111111", "sent", ""),
                MeetingRecipient("3", "C", "", "skipped", ""),
            ),
            2, 1,
        )
        val all = listOf(
            reg("1", "000", status = "Confirmed"),
            reg("2", "1111111111", status = "Confirmed"),
            reg("3", status = "Confirmed"),
            reg("4", status = "Pending"),
        )
        val sent = SentSet.of(meeting)
        assertTrue(sent.contains(all[0]))
        assertTrue(sent.contains(all[1]))
        assertFalse(sent.contains(all[2]))
        assertEquals(listOf("3"), MeetingRules.rest(all, meeting).map { it.id })
        assertEquals(meeting, MeetingRules.find(listOf(meeting), "  women's meet —  28 sep "))
        assertEquals("Zoom meeting — 27 Sep 2026", MeetingRules.defaultName("All", LocalDate.of(2026, 9, 27)))
        assertEquals("Hi there!", MeetingRules.personalise("Hi {name}!", ""))
    }

    @Test
    fun pastGroupsAndArchiveName() {
        val all = listOf(
            reg("1", archive = "Women's Meet — Aug", status = "Confirmed").copy(archivedAt = "2026-08-30"),
            reg("2", archive = "Women's Meet — Aug", name = "Ravi").copy(archivedAt = "2026-08-31"),
            reg("3", archive = "Retreat").copy(archivedAt = "2026-09-10"),
            reg("4"),
        )
        val groups = RegistrationRules.pastGroups(all, "")
        assertEquals(listOf("Retreat", "Women's Meet — Aug"), groups.map { it.name })
        assertEquals(1, groups[1].confirmed)
        val searched = RegistrationRules.pastGroups(all, "ravi")
        assertEquals(listOf("2"), searched.single().shown.map { it.id })
        assertEquals(2, searched.single().all.size)
        assertEquals(2, RegistrationRules.pastEventCount(all))
        assertEquals("Women's Meet — September 2026", RegistrationRules.defaultArchiveName(all.take(2), LocalDate.of(2026, 9, 27)))
        assertEquals("Past registrations — September 2026", RegistrationRules.defaultArchiveName(listOf(all[0], reg("x", source = Sources.EVENT)), LocalDate.of(2026, 9, 27)))
    }

    @Test
    fun csvGuardsFormulasButNotPhoneNumbers() {
        assertEquals("\"'=SUM(A1)\"", Csv.cell("Name", "=SUM(A1)"))
        assertEquals("\"+91 98765 43210\"", Csv.cell("Phone", "+91 98765 43210"))
        assertEquals("\"'=HYPERLINK(1)\"", Csv.cell("Phone", "=HYPERLINK(1)"))
        assertEquals("\"say \"\"hi\"\"\"", Csv.cell("Details", "say \"hi\""))
        val csv = Csv.registrations(listOf(reg("1", "+91 1", name = "@x")))
        assertTrue(csv.startsWith("﻿\"Timestamp\""))
        assertTrue(csv.contains("\"'@x\""))
        assertEquals("Women-s-Meet-28-Sep", Csv.slug("Women's Meet — 28 Sep"))
    }
}
