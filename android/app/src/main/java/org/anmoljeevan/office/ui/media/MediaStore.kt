package org.anmoljeevan.office.ui.media

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.anmoljeevan.office.core.ApiException
import org.anmoljeevan.office.core.ServerClock
import org.anmoljeevan.office.core.media.CellGrid
import org.anmoljeevan.office.core.media.Cells
import org.anmoljeevan.office.core.media.ContentItem
import org.anmoljeevan.office.core.media.ItemRules
import org.anmoljeevan.office.core.media.ItemStatus
import org.anmoljeevan.office.core.media.MediaApi
import org.anmoljeevan.office.core.media.MediaChart
import org.anmoljeevan.office.core.media.MediaLogin
import org.anmoljeevan.office.core.media.StockAdjustment
import org.anmoljeevan.office.core.media.StockRules
import org.anmoljeevan.office.core.media.WorkTypes
import org.anmoljeevan.office.data.Settings
import java.time.LocalDate
import java.time.YearMonth
import kotlin.coroutines.coroutineContext

enum class MediaTab(val title: String) { LOG("Content Log"), PLAN("Weekly Plan"), STOCK("Content Stock"), SUMMARY("Stock Summary") }

enum class ChartView { MADE, UPLOADED }

enum class LogMode { DAY, MONTH }

enum class SaveKind { NEUTRAL, OK, BUSY, ERROR }

data class SaveState(val text: String, val kind: SaveKind)

data class CellRef(val type: String, val day: Int)

/** The add / edit form for one piece of content. */
data class ItemDraft(val id: String?, val title: String, val type: String, val link: String)

/**
 * media.html's logic as Compose state. One shared chart per month for the whole team: a save
 * only sends the cells this phone changed, and they are merged into whatever is stored, so
 * several people can fill it in at once. Lives as long as the login.
 */
class MediaStore(
    private val api: MediaApi,
    val login: MediaLogin,
    private val settings: Settings,
    val scope: CoroutineScope,
) {
    val snackbar = SnackbarHostState()
    val clock = ServerClock.fromServerTime(login.serverNow)
    val hasUploads = login.hasUploads
    val hasStock = login.hasStock

    var tab by mutableStateOf(MediaTab.LOG)
        private set
    var howtoVisible by mutableStateOf(!settings.mediaHowtoHidden)
        private set
    var sessionExpired by mutableStateOf(false)
        private set

    fun today(): LocalDate = clock.today()

    fun toast(message: String) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(message)
        }
    }

    fun setHowto(visible: Boolean) {
        howtoVisible = visible
        settings.mediaHowtoHidden = !visible
    }

    private fun errorText(e: ApiException): String = when (e) {
        is ApiException.Unauthorized -> "session expired"
        is ApiException.Network -> "no connection"
        is ApiException.BadResponse -> "unexpected answer from the sheet"
        is ApiException.Server -> e.code
    }

    private fun expired(e: ApiException): Boolean {
        if (e !is ApiException.Unauthorized) return false
        toast("Session expired — please log in again")
        sessionExpired = true
        return true
    }

    // ======================= Content Log =======================

    var month by mutableStateOf(YearMonth.from(today()))
        private set
    var view by mutableStateOf(ChartView.MADE)
    var mode by mutableStateOf(LogMode.DAY)
    var selectedDay by mutableIntStateOf(today().dayOfMonth)
    var editing by mutableStateOf<CellRef?>(null)

    var made by mutableStateOf(CellGrid())
        private set
    var uploaded by mutableStateOf(CellGrid())
        private set
    var extraTypes by mutableStateOf<List<String>>(emptyList())
        private set
    var note by mutableStateOf("")
        private set
    var noteDirty by mutableStateOf(false)
        private set
    var updatedAt by mutableStateOf("")
        private set
    var ready by mutableStateOf(false)
        private set
    var save by mutableStateOf(SaveState("Loading…", SaveKind.BUSY))
        private set

    /** A month / logout switch waiting for "discard your unsaved changes?" */
    var pendingDiscard by mutableStateOf<(() -> Unit)?>(null)

    private var saving = false
    private val saveLock = Mutex()
    private var autosave: Job? = null

    val monthKey: String get() = Cells.monthKey(month)
    val days: Int get() = month.lengthOfMonth()
    val types: List<String> get() = WorkTypes.withExtras(extraTypes)
    val todayDay: Int get() = if (YearMonth.from(today()) == month) today().dayOfMonth else 0
    val current: CellGrid get() = if (view == ChartView.MADE) made else uploaded
    val hasUnsaved: Boolean get() = made.hasPending || uploaded.hasPending || noteDirty
    val madeTotal: Int get() = made.grandTotal(types, days)
    val uploadedTotal: Int get() = uploaded.grandTotal(types, days)

    init {
        scope.launch { loadChart() }
    }

    private fun reset() {
        made = CellGrid()
        uploaded = CellGrid()
        extraTypes = emptyList()
        note = ""
        noteDirty = false
        updatedAt = ""
    }

    suspend fun loadChart() {
        autosave?.cancel()
        ready = false
        reset()
        save = SaveState("Loading…", SaveKind.BUSY)
        try {
            val chart = api.chart(monthKey)
            applyChart(chart)
            ready = true
            save = if (chart != null) SaveState("All changes saved", SaveKind.OK) else SaveState("New month — nothing filled in yet", SaveKind.NEUTRAL)
        } catch (e: ApiException) {
            if (!expired(e)) save = SaveState("Couldn’t load this month — tap to try again", SaveKind.ERROR)
        }
    }

    private fun applyChart(c: MediaChart?) {
        made = CellGrid.fromRemote(c?.data.orEmpty())
        uploaded = CellGrid.fromRemote(c?.uploaded.orEmpty())
        extraTypes = c?.extraTypes().orEmpty()
        note = c?.note.orEmpty()
        noteDirty = false
        updatedAt = c?.updatedAt.orEmpty()
    }

    /** Bring in what the rest of the team saved, without touching cells changed here but not saved yet. */
    private fun mergeRemote(c: MediaChart) {
        made = made.mergedWith(c.data)
        uploaded = uploaded.mergedWith(c.uploaded)
        val remoteExtra = c.extraTypes()
        extraTypes = remoteExtra + extraTypes.filter { it !in remoteExtra && (made.pending[it] != null || uploaded.pending[it] != null) }
        if (!noteDirty) note = c.note
        updatedAt = c.updatedAt
    }

    fun cell(type: String, day: Int): String = current.get(type, day)

    fun edit(type: String, day: Int, value: String) {
        if (!ready) {
            toast("Please wait — the chart is still loading")
            return
        }
        if (current.get(type, day) == value) return
        if (view == ChartView.MADE) made = made.edit(type, day, value) else uploaded = uploaded.edit(type, day, value)
        markDirty()
    }

    fun editNote(v: String) {
        if (!ready) return
        note = v.take(300)
        noteDirty = true
        markDirty()
    }

    private fun markDirty() {
        save = SaveState("Unsaved changes…", SaveKind.BUSY)
        scheduleSave(1500)
    }

    private fun scheduleSave(delayMs: Long, except: Job? = null) {
        autosave?.let { if (it != except) it.cancel() }
        autosave = scope.launch {
            delay(delayMs)
            saveNow()
        }
    }

    /** Saves whatever is pending. Returns false when it couldn't be saved. */
    suspend fun saveNow(): Boolean = saveLock.withLock { doSave() }

    fun retrySave() {
        scope.launch { saveNow() }
    }

    private suspend fun doSave(): Boolean {
        if (!ready) {
            loadChart() // "Save" after a failed load = try again
            return ready
        }
        val madeSnap = made
        val upSnap = uploaded
        val sentNote = if (noteDirty) note else null
        if (!madeSnap.hasPending && !upSnap.hasPending && sentNote == null) {
            save = SaveState("All changes saved", SaveKind.OK)
            return true
        }
        saving = true
        save = SaveState("Saving…", SaveKind.BUSY)
        val key = monthKey
        return try {
            val chart = api.save(key, madeSnap.pendingJson(), if (hasUploads) upSnap.pendingJson() else null, sentNote)
            made = made.pruned(madeSnap.pending)
            uploaded = uploaded.pruned(upSnap.pending)
            if (sentNote != null && note == sentNote) noteDirty = false
            mergeRemote(chart)
            if (hasUnsaved) {
                save = SaveState("Unsaved changes…", SaveKind.BUSY)
                scheduleSave(500, coroutineContext[Job])
            } else {
                save = SaveState("All changes saved", SaveKind.OK)
            }
            true
        } catch (e: ApiException) {
            if (!expired(e)) save = SaveState("Not saved — check your connection and tap to retry", SaveKind.ERROR)
            false
        } finally {
            saving = false
        }
    }

    /** Called when the app goes to the background: nothing typed should be lost. */
    fun saveInBackground() {
        if (hasUnsaved) scope.launch { saveNow() }
    }

    fun switchMonth(target: YearMonth) {
        if (target == month) return
        scope.launch {
            if (hasUnsaved && !saveNow()) {
                pendingDiscard = { scope.launch { goToMonth(target) } }
                return@launch
            }
            goToMonth(target)
        }
    }

    private suspend fun goToMonth(target: YearMonth) {
        month = target
        selectedDay = if (YearMonth.from(today()) == target) today().dayOfMonth else 1
        editing = null
        loadChart()
    }

    fun confirmDiscard() {
        val go = pendingDiscard
        pendingDiscard = null
        made = CellGrid(made.cells)
        uploaded = CellGrid(uploaded.cells)
        noteDirty = false
        go?.invoke()
    }

    fun logout(onLogout: () -> Unit) {
        scope.launch {
            if (hasUnsaved && !saveNow()) {
                pendingDiscard = onLogout
                return@launch
            }
            onLogout()
        }
    }

    private suspend fun pollChart() {
        if (saving || !ready || hasUnsaved) return
        val key = monthKey
        try {
            val chart = api.chart(key) ?: return
            if (key != monthKey || hasUnsaved || saving) return
            if (chart.updatedAt == updatedAt) return
            mergeRemote(chart)
            toast("Updated with your team’s latest changes")
        } catch (e: ApiException) {
            // try again next time
        }
    }

    // ======================= tabs & polling =======================

    /** Bumped by the poll so the weekly plan redraws itself after midnight. */
    var planDay by mutableStateOf(today())
        private set

    fun selectTab(t: MediaTab) {
        if (t == tab) return
        val leaving = tab
        tab = t
        scope.launch {
            if (leaving == MediaTab.LOG && hasUnsaved) saveNow()
            when (t) {
                MediaTab.SUMMARY -> loadTotals()
                MediaTab.STOCK -> loadItems()
                MediaTab.LOG -> pollChart()
                MediaTab.PLAN -> planDay = today()
            }
        }
    }

    /** Every 15 s while the app is on screen: look for changes made by the rest of the team. */
    suspend fun tick() {
        when (tab) {
            MediaTab.LOG -> pollChart()
            MediaTab.PLAN -> if (planDay != today()) planDay = today()
            MediaTab.STOCK -> if (hasStock && itemSheet == null && busyItems.isEmpty()) pollItems()
            MediaTab.SUMMARY -> Unit
        }
    }

    /** A number in "By month" opens the chart it was added up from, so any total can be traced. */
    fun openMonth(key: String, v: ChartView) {
        val ym = Cells.parseMonthKey(key) ?: return
        view = v
        mode = LogMode.MONTH
        selectTab(MediaTab.LOG)
        switchMonth(ym)
    }

    // ======================= Content Stock =======================

    var items by mutableStateOf<List<ContentItem>>(emptyList())
        private set
    var itemsLoading by mutableStateOf(false)
        private set
    var itemsError by mutableStateOf<String?>(null)
        private set
    private var itemsLoaded = false

    var statusFilter by mutableStateOf(ItemStatus.READY)
    var typeFilter by mutableStateOf(ItemRules.ALL)
    var itemQuery by mutableStateOf("")
    var itemSheet by mutableStateOf<ItemDraft?>(null)
    var confirmDelete by mutableStateOf<ContentItem?>(null)
    var busyItems by mutableStateOf<Set<String>>(emptySet())
        private set
    var itemSaving by mutableStateOf(false)
        private set

    val itemView by derivedStateOf { ItemRules.view(items, statusFilter, typeFilter, itemQuery) }
    val itemTypes by derivedStateOf { ItemRules.types(items, extraTypes) }

    fun reloadItems() {
        scope.launch { loadItems() }
    }

    suspend fun loadItems() {
        if (!hasStock) return
        itemsLoading = !itemsLoaded
        itemsError = null
        try {
            items = api.items()
            itemsLoaded = true
        } catch (e: ApiException) {
            if (!expired(e)) itemsError = "Could not reach the sheet — wait a few seconds and try again."
        } finally {
            itemsLoading = false
        }
    }

    private suspend fun pollItems() {
        try {
            val fresh = api.items()
            if (itemSheet == null && tab == MediaTab.STOCK && busyItems.isEmpty() && fresh != items) items = fresh
        } catch (e: ApiException) {
            // next time
        }
    }

    private fun put(item: ContentItem) {
        val at = items.indexOfFirst { it.id == item.id }
        items = if (at >= 0) items.toMutableList().also { it[at] = item } else listOf(item) + items
    }

    fun newItemDraft() {
        itemSheet = ItemDraft(null, "", if (typeFilter != ItemRules.ALL) typeFilter else WorkTypes.CATEGORIES.first(), "")
    }

    fun saveDraft(d: ItemDraft) {
        if (d.title.isBlank() || itemSaving) return
        itemSaving = true
        scope.launch {
            try {
                val saved = api.saveItem(id = d.id, title = d.title.trim(), type = d.type, link = d.link.trim())
                put(saved)
                if (d.id == null) {
                    // show what was just added even if a different filter was open
                    if (statusFilter != ItemStatus.READY && statusFilter != ItemRules.ALL) statusFilter = ItemStatus.READY
                    if (typeFilter != ItemRules.ALL && typeFilter != saved.type) typeFilter = ItemRules.ALL
                    itemQuery = ""
                    toast("Added to stock")
                } else {
                    toast("Saved")
                }
                itemSheet = null
            } catch (e: ApiException) {
                if (!expired(e)) toast(if (d.id == null) "Could not add — try again" else "Could not save — try again")
            } finally {
                itemSaving = false
            }
        }
    }

    /** Status and platform change at once on screen, and go back if the sheet can't be updated. */
    private fun update(item: ContentItem, optimistic: ContentItem, call: suspend () -> ContentItem) {
        if (item.id in busyItems) return
        val before = item
        put(optimistic)
        busyItems = busyItems + item.id
        scope.launch {
            try {
                put(call())
            } catch (e: ApiException) {
                put(before)
                if (!expired(e)) toast("Could not update — ${errorText(e)}")
            } finally {
                busyItems = busyItems - item.id
            }
        }
    }

    fun setItemStatus(item: ContentItem, status: String) {
        if (item.status == status) return
        val now = java.time.Instant.now().toString()
        update(item, item.copy(status = status, platform = if (status == ItemStatus.UPLOADED) item.platform else "", updatedAt = now)) {
            api.saveItem(id = item.id, status = status)
        }
    }

    fun togglePlatform(item: ContentItem, platform: String) {
        val p = if (item.platform == platform) "" else platform
        update(item, item.copy(platform = p)) { api.saveItem(id = item.id, platform = p) }
    }

    fun deleteItem(item: ContentItem) {
        confirmDelete = null
        busyItems = busyItems + item.id
        scope.launch {
            try {
                api.deleteItem(item.id)
                items = items.filter { it.id != item.id }
                toast("Removed")
            } catch (e: ApiException) {
                if (!expired(e)) toast("Could not remove — try again")
            } finally {
                busyItems = busyItems - item.id
            }
        }
    }

    // ======================= Stock Summary =======================

    var charts by mutableStateOf<List<MediaChart>>(emptyList())
        private set
    var stock by mutableStateOf<Map<String, StockAdjustment>>(emptyMap())
        private set
    var totalsLoading by mutableStateOf(false)
        private set
    var totalsError by mutableStateOf<String?>(null)
        private set
    private var totalsLoaded = false
    var year by mutableStateOf("all")
    var stockEdit by mutableStateOf<Pair<String, String>?>(null) // type to "opening" | "notNeeded"

    val ledger by derivedStateOf { StockRules.ledger(charts, stock, items, hasStock) }
    val monthTable by derivedStateOf { StockRules.monthTable(charts, year, stock, items) }
    val years by derivedStateOf { StockRules.years(charts) }

    fun reloadTotals() {
        scope.launch { loadTotals() }
    }

    suspend fun loadTotals() {
        totalsLoading = !totalsLoaded
        totalsError = null
        try {
            val (listing, list) = coroutineScope {
                val a = async { api.list() }
                val b = async { if (hasStock) api.items() else items }
                a.await() to b.await()
            }
            charts = listing.charts
            stock = listing.stock
            items = list
            totalsLoaded = true
            if (year != "all" && year !in years) year = "all"
        } catch (e: ApiException) {
            if (!expired(e)) totalsError = "Could not reach the sheet — wait a few seconds and try Refresh."
        } finally {
            totalsLoading = false
        }
    }

    fun setStock(type: String, field: String, value: Int) {
        stockEdit = null
        scope.launch {
            try {
                stock = api.setStock(type, field, value)
                toast("Saved")
            } catch (e: ApiException) {
                if (!expired(e)) toast("Could not save — try again")
            }
        }
    }
}
