package org.anmoljeevan.office.core.admin

import kotlinx.serialization.json.JsonObject
import org.anmoljeevan.office.core.Text
import org.anmoljeevan.office.core.int
import org.anmoljeevan.office.core.str
import java.time.LocalDate

/** One person in a Zoom-link send. */
data class MeetingRecipient(
    val id: String,
    val name: String,
    val phone: String,
    /** "sent" or "skipped" */
    val result: String,
    val at: String,
) {
    val sent: Boolean get() = result == "sent"
}

/** One Zoom-link send, filed under a meeting name ("Meetings" tab of the sheet). */
data class Meeting(
    val id: String,
    val name: String,
    val createdAt: String,
    val updatedAt: String,
    val message: String,
    val recipients: List<MeetingRecipient>,
    val sent: Int,
    val skipped: Int,
)

fun parseMeeting(o: JsonObject): Meeting {
    val people = (o["recipients"] as? kotlinx.serialization.json.JsonArray).orEmpty().mapNotNull { it as? JsonObject }.map { r ->
        MeetingRecipient(
            id = r["id"].str(),
            name = r["name"].str(),
            phone = r["phone"].str(),
            result = if (r["result"].str() == "sent") "sent" else "skipped",
            at = r["at"].str(),
        )
    }
    return Meeting(
        id = o["id"].str(),
        name = o["name"].str(),
        createdAt = o["createdAt"].str(),
        updatedAt = o["updatedAt"].str(),
        message = o["message"].str(),
        recipients = people,
        sent = o["sent"].int(people.count { it.sent }),
        skipped = o["skipped"].int(people.count { !it.sent }),
    )
}

/** Who a meeting was already sent to — matched by registration ID or by phone number. */
class SentSet(private val ids: Set<String>, private val phones: Set<String>) {
    fun contains(r: Registration): Boolean {
        if (r.id.isNotEmpty() && r.id in ids) return true
        val k = Phones.key(r.phone)
        return k.isNotEmpty() && k in phones
    }

    companion object {
        val EMPTY = SentSet(emptySet(), emptySet())

        fun of(meeting: Meeting?): SentSet {
            if (meeting == null) return EMPTY
            val ids = HashSet<String>()
            val phones = HashSet<String>()
            meeting.recipients.filter { it.sent }.forEach { r ->
                if (r.id.isNotEmpty()) ids += r.id
                Phones.key(r.phone).takeIf { it.isNotEmpty() }?.let { phones += it }
            }
            return SentSet(ids, phones)
        }
    }
}

object MeetingRules {
    fun find(meetings: List<Meeting>, name: String): Meeting? {
        val n = Text.norm(name)
        return meetings.firstOrNull { Text.norm(it.name) == n }
    }

    /** Replaces the meeting with the same ID (or adds it), newest first. */
    fun upsert(meetings: List<Meeting>, m: Meeting): List<Meeting> =
        (meetings.filter { it.id != m.id } + m).sortedByDescending { it.updatedAt }

    /** Confirmed people (still in the main list) who haven't been sent this meeting yet. */
    fun rest(all: List<Registration>, meeting: Meeting): List<Registration> {
        val sent = SentSet.of(meeting)
        return RegistrationRules.dedupeByPhone(all.filter { it.isActive && it.status == Statuses.CONFIRMED && !sent.contains(it) })
    }

    /** "Zoom meeting — 27 Sep 2026", or "<tab name> — …" when a source tab is open. */
    fun defaultName(label: String, today: LocalDate): String =
        (if (label == "All") "Zoom meeting" else label) + " — " + Text.dayMonthYear(today)

    const val DEFAULT_MESSAGE = "Hi {name}! 🙏 Thank you for confirming for Women's Meet. Here's your Zoom link: [PASTE ZOOM LINK HERE]\n\nSee you there!"

    fun personalise(template: String, name: String): String = template.replace("{name}", name.ifEmpty { "there" })
}
