package org.anmoljeevan.office.core

import org.anmoljeevan.office.core.admin.Meeting
import org.anmoljeevan.office.core.admin.Registration
import java.time.LocalDate
import java.time.ZoneId

/**
 * CSV files for the exports. A cell that starts with = + - @ can be run as a formula when the file
 * is opened in a spreadsheet, so those get a leading apostrophe — except real phone numbers
 * ("+91 98…"), which are left as they are.
 */
object Csv {
    private const val BOM = "﻿"
    private val phoneLike = Regex("^\\+?[0-9 ().\\-]+$")

    fun cell(column: String, value: String): String {
        var s = value
        val risky = s.isNotEmpty() && s[0] in "=+-@"
        if (risky && !(column == "Phone" && phoneLike.matches(s))) s = "'$s"
        return "\"" + s.replace("\"", "\"\"") + "\""
    }

    fun table(columns: List<String>, rows: List<List<String>>): String =
        BOM + (listOf(columns.joinToString(",") { cell("", it) }) +
            rows.map { r -> r.mapIndexed { i, v -> cell(columns.getOrElse(i) { "" }, v) }.joinToString(",") }).joinToString("\n")

    fun registrations(rows: List<Registration>): String = table(
        listOf("Timestamp", "Source", "Name", "Phone", "Email", "Details", "Status"),
        rows.map { listOf(it.timestamp, it.source, it.name, it.phone, it.email, it.details, it.status) },
    )

    fun meeting(m: Meeting): String = table(
        listOf("Meeting", "Name", "Phone", "Result", "Time"),
        m.recipients.map { listOf(m.name, it.name, it.phone, it.result, it.at) },
    )

    fun pastEvent(name: String, rows: List<Registration>): String = table(
        listOf("Event", "Timestamp", "Source", "Name", "Phone", "Email", "Details", "Status"),
        rows.map { listOf(name, it.timestamp, it.source, it.name, it.phone, it.email, it.details, it.status) },
    )

    /** A safe file-name piece from a meeting / event name. */
    fun slug(s: String): String =
        s.replace(Regex("[^A-Za-z0-9_\\-]+"), "-").trim('-').take(40).ifEmpty { "export" }

    fun registrationsFileName(today: LocalDate) = "ajm-registrations-$today.csv"
}

/** The printable attendee list — the same sheet admin.html's "Print List" makes. */
object PrintHtml {
    fun attendeeList(label: String, rows: List<Registration>, zone: ZoneId = ZoneId.systemDefault()): String {
        val sorted = rows.sortedWith(compareBy(Text.natural) { it.name })
        val now = java.time.ZonedDateTime.now(zone)
        val generated = java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", java.util.Locale.ENGLISH).format(now)
        val body = sorted.mapIndexed { i, r ->
            "<tr><td>${i + 1}</td><td><span class=\"box\"></span></td><td>${Text.html(r.name)}</td><td>${Text.html(r.phone)}</td></tr>"
        }.joinToString("")
        val title = Text.html(label)
        return """
            <!DOCTYPE html><html><head><meta charset="utf-8"><title>$title — Attendee List</title>
            <style>
              body{font-family:Arial,sans-serif;padding:32px;color:#111}
              h1{font-size:1.4rem;margin-bottom:2px}
              .sub{color:#666;font-size:.85rem;margin-bottom:20px}
              table{width:100%;border-collapse:collapse}
              th,td{text-align:left;padding:10px 8px;border-bottom:1px solid #ccc;font-size:.95rem}
              th{font-size:.75rem;text-transform:uppercase;letter-spacing:.05em;color:#666}
              .box{width:18px;height:18px;border:1.5px solid #333;display:inline-block}
            </style></head><body>
            <h1>Anmol Jeevan Ministries — $title</h1>
            <div class="sub">Attendee list · ${sorted.size} total · generated ${Text.html(generated)}</div>
            <table><thead><tr><th style="width:36px">#</th><th style="width:36px"></th><th>Name</th><th>Phone</th></tr></thead><tbody>
            $body
            </tbody></table>
            </body></html>
        """.trimIndent()
    }
}
