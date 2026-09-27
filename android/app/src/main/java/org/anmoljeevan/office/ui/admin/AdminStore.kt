package org.anmoljeevan.office.ui.admin

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.anmoljeevan.office.core.ApiException
import org.anmoljeevan.office.core.Text
import org.anmoljeevan.office.core.admin.AdminApi
import org.anmoljeevan.office.core.admin.BulkOutcome
import org.anmoljeevan.office.core.admin.Meeting
import org.anmoljeevan.office.core.admin.MeetingMark
import org.anmoljeevan.office.core.admin.MeetingRules
import org.anmoljeevan.office.core.admin.RegFilter
import org.anmoljeevan.office.core.admin.Registration
import org.anmoljeevan.office.core.admin.RegistrationList
import org.anmoljeevan.office.core.admin.RegistrationRules
import org.anmoljeevan.office.core.admin.SORT_OPTIONS
import org.anmoljeevan.office.core.admin.SentSet
import org.anmoljeevan.office.core.admin.SortOption
import java.time.Instant
import java.time.LocalDate

enum class HistoryState { UNKNOWN, OK, UNSUPPORTED, ERROR }

enum class AdminOverlay { NONE, HISTORY, ZOOM }

sealed interface AdminDialog {
    val rows: List<Registration>

    data class Archive(override val rows: List<Registration>) : AdminDialog
    data class Delete(override val rows: List<Registration>) : AdminDialog
    data class Restore(override val rows: List<Registration>) : AdminDialog
}

/** A stable key for a row, even for old rows saved before registrations had IDs. */
val Registration.key: String get() = id.ifEmpty { "row:$timestamp|$name|$phone" }

/**
 * Keys that are unique even if a row was copy-pasted in the sheet (so its ID appears twice):
 * the second copy gets "#2", and so on. Lists crash on repeated keys, so every list uses these.
 */
fun <T> uniqueKeys(list: List<T>, base: (T) -> String): List<String> {
    val seen = HashMap<String, Int>()
    return list.map { item ->
        val k = base(item)
        val n = (seen[k] ?: 0) + 1
        seen[k] = n
        if (n == 1) k else "$k#$n"
    }
}

/**
 * Everything the admin screens show and do — admin.html's logic, as Compose state.
 * Lives as long as the login; [scope] is cancelled on log out.
 */
class AdminStore(private val api: AdminApi, initial: RegistrationList, val scope: CoroutineScope) {
    val snackbar = SnackbarHostState()

    var rows by mutableStateOf(initial.rows)
        private set
    var archiveSupported by mutableStateOf(initial.archiveSupported)
        private set
    var refreshing by mutableStateOf(false)
        private set

    var filter by mutableStateOf(RegFilter.ALL)
        private set
    var query by mutableStateOf("")
        private set
    var sort by mutableStateOf<SortOption>(SORT_OPTIONS.first())

    var selection by mutableStateOf<Set<String>>(emptySet())
        private set
    var bulkBusy by mutableStateOf<String?>(null)
        private set

    var detailKey by mutableStateOf<String?>(null)
    var overlay by mutableStateOf(AdminOverlay.NONE)
    var dialog by mutableStateOf<AdminDialog?>(null)
    var dialogProgress by mutableStateOf<String?>(null)
        private set

    var meetings by mutableStateOf<List<Meeting>>(emptyList())
        private set
    var historyState by mutableStateOf(HistoryState.UNKNOWN)
        private set
    var historyTab by mutableIntStateOf(0)
    var pastQuery by mutableStateOf("")

    var zoom by mutableStateOf<ZoomSend?>(null)
        private set

    /** The Zoom message is kept between sends, like the text box on the web page. */
    var zoomMessage = MeetingRules.DEFAULT_MESSAGE

    /** Bumped on every full reload, so the list can replay its entrance animation. */
    var loadGeneration by mutableIntStateOf(0)
        private set

    val visible by derivedStateOf { RegistrationRules.sort(RegistrationRules.filter(rows, filter, query), sort) }

    /** Each row object's unique key, worked out once over the whole sheet so it never changes with filters. */
    private val rowKeys by derivedStateOf {
        val keys = uniqueKeys(rows) { it.key }
        java.util.IdentityHashMap<Registration, String>().also { m -> rows.forEachIndexed { i, r -> m[r] = keys[i] } }
    }

    fun keyFor(r: Registration): String = rowKeys[r] ?: r.key

    fun rowFor(key: String): Registration? = rows.firstOrNull { keyFor(it) == key }
    val stats by derivedStateOf { RegistrationRules.stats(rows) }
    val dupCounts by derivedStateOf { RegistrationRules.duplicateCounts(rows) }
    val filterCounts by derivedStateOf { RegFilter.entries.associateWith { RegistrationRules.countFor(rows, it) } }
    val activeCount by derivedStateOf { rows.count { it.isActive } }
    val pastGroups by derivedStateOf { RegistrationRules.pastGroups(rows, pastQuery) }
    val pastEventCount by derivedStateOf { RegistrationRules.pastEventCount(rows) }
    val archiveNames by derivedStateOf { rows.filter { !it.isActive }.map { it.archive }.distinct() }

    val selecting: Boolean get() = selection.isNotEmpty()
    val selectedRows: List<Registration> get() = rows.filter { it.id.isNotEmpty() && it.id in selection }
    private val visibleIds: List<String> get() = visible.mapNotNull { r -> r.id.ifEmpty { null } }
    val allVisibleSelected: Boolean get() = visibleIds.isNotEmpty() && visibleIds.all { it in selection }

    init {
        scope.launch { loadMeetings() }
    }

    fun toast(message: String) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(message)
        }
    }

    private fun errorText(e: ApiException): String = when (e) {
        is ApiException.Unauthorized -> "session expired — log in again"
        is ApiException.Network -> "no connection to the sheet"
        is ApiException.BadResponse -> "the sheet gave an unexpected answer"
        is ApiException.Server -> e.code
    }

    // ---------- loading ----------

    fun refresh() {
        if (refreshing) return
        refreshing = true
        scope.launch {
            try {
                val list = api.list()
                rows = list.rows
                archiveSupported = list.archiveSupported
                pruneSelection()
                loadGeneration++
            } catch (e: ApiException) {
                toast("Couldn’t refresh — ${errorText(e)}")
            } finally {
                refreshing = false
            }
            loadMeetings()
        }
    }

    suspend fun loadMeetings() {
        try {
            val list = api.meetings()
            if (list == null) {
                meetings = emptyList()
                historyState = HistoryState.UNSUPPORTED // an older script answers with the plain list
            } else {
                meetings = list
                historyState = HistoryState.OK
            }
        } catch (e: ApiException) {
            historyState = HistoryState.ERROR
        }
    }

    // ---------- the list ----------

    fun updateFilter(f: RegFilter) {
        filter = f
        selection = emptySet()
    }

    fun updateQuery(q: String) {
        query = q
        selection = emptySet()
    }

    fun toggle(r: Registration) {
        if (r.id.isEmpty()) {
            toast("This row has no ID — change it directly in the sheet.")
            return
        }
        selection = if (r.id in selection) selection - r.id else selection + r.id
    }

    fun toggleAllVisible() {
        val ids = visibleIds
        selection = if (allVisibleSelected) selection - ids.toSet() else selection + ids
    }

    fun clearSelection() {
        selection = emptySet()
    }

    private fun pruneSelection() {
        val known = rows.filter { it.isActive }.map { it.id }.toSet()
        selection = selection.filter { it in known }.toSet()
    }

    /** Changes one status straight away, and puts it back if the sheet can't be updated. */
    fun setStatus(r: Registration, status: String) {
        if (r.id.isEmpty()) {
            toast("This row has no ID — update it directly in the sheet.")
            return
        }
        if (r.status == status) return
        val before = r.status
        rows = rows.map { if (it.id == r.id) it.copy(status = status) else it }
        scope.launch {
            try {
                api.setStatus(r.id, status)
            } catch (e: ApiException) {
                rows = rows.map { if (it.id == r.id) it.copy(status = before) else it }
                toast("Couldn’t update the status — ${errorText(e)}")
            }
        }
    }

    // ---------- bulk actions ----------

    fun bulkStatus(status: String) {
        val ids = selection.toList()
        if (ids.isEmpty() || bulkBusy != null) return
        bulkBusy = "Marking ${ids.size} $status…"
        scope.launch {
            val res = api.runBulk("bulkStatus", ids, listOf("status" to status)) { d, t -> bulkBusy = "Marking $d of $t…" }
            applyBulk("bulkStatus", res, status = status)
            toast(bulkMessage("bulkStatus", res, ids.size, status = status))
            bulkBusy = null
        }
    }

    fun askArchive(rows: List<Registration>) {
        val withIds = rows.filter { it.id.isNotEmpty() }
        if (withIds.isEmpty()) return
        if (!archiveSupported) {
            toast("Moving people to History needs the Google Sheet script update — see ADMIN-SETUP.md.")
            return
        }
        dialog = AdminDialog.Archive(withIds)
    }

    fun askDelete(rows: List<Registration>) {
        val withIds = rows.filter { it.id.isNotEmpty() }
        if (withIds.isEmpty()) {
            toast("This row has no ID (added before the delete feature) — delete it directly in the sheet.")
            return
        }
        dialog = AdminDialog.Delete(withIds)
    }

    fun askRestore(rows: List<Registration>) {
        val withIds = rows.filter { it.id.isNotEmpty() }
        if (withIds.isNotEmpty()) dialog = AdminDialog.Restore(withIds)
    }

    fun archive(rows: List<Registration>, name: String) =
        runDialog("archive", rows.map { it.id }, listOf("name" to name), "Moving", name = name)

    fun delete(rows: List<Registration>) = runDialog("bulkDelete", rows.map { it.id }, emptyList(), "Deleting")

    fun restore(rows: List<Registration>) = runDialog("unarchive", rows.map { it.id }, emptyList(), "Restoring")

    private fun runDialog(action: String, ids: List<String>, extra: List<Pair<String, String>>, verb: String, name: String? = null) {
        if (dialogProgress != null) return
        dialogProgress = "Working…"
        scope.launch {
            val res = api.runBulk(action, ids, extra) { d, t -> dialogProgress = "$verb $d of $t…" }
            applyBulk(action, res, name = name)
            toast(bulkMessage(action, res, ids.size, name = name))
            dialogProgress = null
            dialog = null
        }
    }

    private fun applyBulk(action: String, res: BulkOutcome, status: String? = null, name: String? = null) {
        val done = res.doneIds.toSet()
        if (done.isEmpty()) return
        rows = when (action) {
            "bulkStatus" -> rows.map { if (it.id in done) it.copy(status = status.orEmpty()) else it }
            "archive" -> rows.map {
                if (it.id in done) it.copy(archive = name.orEmpty(), archivedAt = res.at.ifEmpty { Instant.now().toString() }) else it
            }
            "unarchive" -> rows.map { if (it.id in done) it.copy(archive = "", archivedAt = "") else it }
            "bulkDelete" -> rows.filter { it.id !in done }
            else -> rows
        }
        selection = selection - done
        val open = detailKey
        if (open != null && rowFor(open)?.isActive != true) detailKey = null
    }

    private fun bulkMessage(action: String, res: BulkOutcome, total: Int, status: String? = null, name: String? = null): String {
        val n = res.doneIds.size
        if (res.error == BulkOutcome.NEEDS_UPDATE) return "Moving people to History needs the Google Sheet script update — see ADMIN-SETUP.md."
        if (res.error != null) return "$n of $total done, then it stopped (${res.error}). The rest are still selected."
        return when (action) {
            "bulkStatus" -> "$n marked $status"
            "archive" -> "$n moved to History — “$name”"
            "unarchive" -> "$n restored to the main list"
            else -> "$n deleted"
        }
    }

    // ---------- labels for exports ----------

    val filterLabel: String
        get() = when (filter) {
            RegFilter.ALL -> "All Registrations"
            RegFilter.DUPLICATES -> "Duplicate Entries"
            else -> filter.label
        }

    // ---------- history ----------

    fun openHistory(tab: Int? = null) {
        if (tab != null) historyTab = tab
        overlay = AdminOverlay.HISTORY
        scope.launch { loadMeetings() }
    }

    fun closeOverlay() {
        overlay = AdminOverlay.NONE
    }

    fun reloadMeetings() {
        scope.launch { loadMeetings() }
    }

    // ---------- Send Zoom Link ----------

    /** [all] = every Confirmed person, not just the open tab (used by "Send to the rest"). */
    fun openZoom(all: Boolean = false, meeting: String? = null, message: String? = null) {
        scope.launch {
            if (historyState != HistoryState.OK && historyState != HistoryState.UNSUPPORTED) loadMeetings()
            val base = if (all) rows.filter { it.isActive } else RegistrationRules.filter(rows, filter, query)
            val label = when {
                all || filter == RegFilter.ALL -> "All"
                filter == RegFilter.DUPLICATES -> "Duplicates"
                else -> filter.label
            }
            if (!message.isNullOrEmpty()) zoomMessage = message
            zoom = ZoomSend(
                store = this@AdminStore,
                pool = RegistrationRules.confirmedPool(base),
                label = label,
                initialMeeting = meeting ?: MeetingRules.defaultName(label, LocalDate.now()),
                initialMessage = zoomMessage,
            )
            overlay = AdminOverlay.ZOOM
        }
    }

    internal suspend fun saveMark(mark: MeetingMark) {
        val m = api.meetingMark(mark)
        meetings = MeetingRules.upsert(meetings, m)
    }
}

/**
 * One "Send Zoom Link" run. Opening WhatsApp does NOT count as sent by itself: the admin
 * confirms afterwards that they hit send, so the tally is real. Each result is saved to the
 * meeting's history as it happens, one small request at a time, so nothing is lost if the app
 * is closed half-way.
 */
class ZoomSend(
    private val store: AdminStore,
    val pool: List<Registration>,
    val label: String,
    initialMeeting: String,
    initialMessage: String,
) {
    enum class Step { SETUP, QUEUE, DONE }

    var step by mutableStateOf(Step.SETUP)
        private set
    var meetingName by mutableStateOf(initialMeeting)
        private set
    var message by mutableStateOf(initialMessage)
        private set
    var checked by mutableStateOf<Set<Int>>(emptySet())
        private set
    private var signature = ""

    var queue by mutableStateOf<List<Registration>>(emptyList())
        private set
    var index by mutableIntStateOf(0)
        private set
    var opened by mutableStateOf(false)
        private set
    var sentCount by mutableIntStateOf(0)
        private set

    /** The meeting name this run is filed under ("" = not being saved). */
    var savingAs by mutableStateOf("")
        private set
    var inFlight by mutableIntStateOf(0)
        private set
    var failed by mutableStateOf<List<MeetingMark>>(emptyList())
        private set
    private var messageSaved = false
    private val order = Mutex() // fair: results are saved in the order they happened

    val historyOn: Boolean get() = store.historyState == HistoryState.OK
    val current: Registration? get() = queue.getOrNull(index)
    val stoppedEarly: Boolean get() = index < queue.size

    init {
        resetChecks()
    }

    fun sentBefore(): SentSet =
        if (historyOn && meetingName.isNotBlank()) SentSet.of(MeetingRules.find(store.meetings, meetingName)) else SentSet.EMPTY

    private fun signatureOf(s: SentSet) = pool.joinToString("") { if (s.contains(it)) "1" else "0" }

    /** Everyone who wasn't already sent this meeting is ticked. */
    private fun resetChecks() {
        val s = sentBefore()
        checked = pool.indices.filter { !s.contains(pool[it]) }.toSet()
        signature = signatureOf(s)
    }

    /** Choosing a meeting that already exists marks who was already sent it, and unticks them. */
    fun updateMeetingName(v: String) {
        meetingName = v
        if (signatureOf(sentBefore()) != signature) resetChecks()
    }

    fun updateMessage(v: String) {
        message = v
        store.zoomMessage = v
    }

    fun toggle(i: Int) {
        checked = if (i in checked) checked - i else checked + i
    }

    fun selectAll() {
        checked = pool.indices.toSet()
    }

    fun selectNone() {
        checked = emptySet()
    }

    /** Returns a problem to show, or null when the queue started. */
    fun start(): String? {
        val q = pool.filterIndexed { i, _ -> i in checked }
        if (q.isEmpty()) return "Select at least one recipient."
        queue = q
        index = 0
        sentCount = 0
        opened = false
        messageSaved = false
        failed = emptyList()
        savingAs = if (historyOn) Text.clean(meetingName).ifEmpty { MeetingRules.defaultName(label, LocalDate.now()) } else ""
        step = Step.QUEUE
        return null
    }

    fun personalMessage(r: Registration): String = MeetingRules.personalise(message, r.name)

    fun markOpened() {
        opened = true
    }

    fun skip() {
        current?.let { record(it, "skipped") }
        advance()
    }

    fun confirmSent() {
        current?.let {
            sentCount++
            record(it, "sent")
        }
        advance()
    }

    fun stop() {
        step = Step.DONE
    }

    private fun advance() {
        index++
        opened = false
        if (index >= queue.size) step = Step.DONE
    }

    private fun record(r: Registration, result: String) {
        if (savingAs.isEmpty() || !historyOn) return
        val msg = if (!messageSaved) {
            messageSaved = true
            message.take(700)
        } else {
            null
        }
        enqueue(MeetingMark(savingAs, result, r.id, r.name, r.phone, msg))
    }

    private fun enqueue(mark: MeetingMark) {
        inFlight++
        store.scope.launch {
            order.withLock {
                try {
                    store.saveMark(mark)
                } catch (e: ApiException) {
                    failed = failed + mark
                } finally {
                    inFlight--
                }
            }
        }
    }

    fun retryFailed() {
        val list = failed
        failed = emptyList()
        list.forEach(::enqueue)
    }
}
