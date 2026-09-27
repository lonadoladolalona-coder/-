package org.anmoljeevan.office.ui.admin

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.VideoCall
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.anmoljeevan.office.core.Csv
import org.anmoljeevan.office.core.Text
import org.anmoljeevan.office.core.admin.Meeting
import org.anmoljeevan.office.core.admin.MeetingRules
import org.anmoljeevan.office.core.admin.PastGroup
import org.anmoljeevan.office.core.admin.Statuses
import org.anmoljeevan.office.ui.components.AjmCard
import org.anmoljeevan.office.ui.components.EmptyState
import org.anmoljeevan.office.ui.components.Notice
import org.anmoljeevan.office.ui.components.NoticeKind
import org.anmoljeevan.office.ui.components.PillOption
import org.anmoljeevan.office.ui.components.PredictiveBackScreen
import org.anmoljeevan.office.ui.components.PillSwitcher
import org.anmoljeevan.office.ui.components.SearchField
import org.anmoljeevan.office.ui.components.SoftButton
import org.anmoljeevan.office.ui.components.TintBadge
import org.anmoljeevan.office.ui.components.bouncyClick
import org.anmoljeevan.office.ui.components.staggeredEntrance
import org.anmoljeevan.office.ui.theme.AjmTheme
import org.anmoljeevan.office.ui.theme.Brand
import org.anmoljeevan.office.util.Launch

/** Past events moved out of the main list, and every Zoom-link send. */
@Composable
fun HistoryScreen(store: AdminStore) {
    PredictiveBackScreen(onBack = store::closeOverlay) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(store.snackbar) },
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("History")
                            Text(
                                "Past events and every Zoom-link send",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    navigationIcon = { IconButton(onClick = store::closeOverlay) { Icon(Icons.Rounded.Close, "Close") } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                )
            },
        ) { padding ->
            Column(
                Modifier
                    .padding(padding)
                    .fillMaxSize(),
            ) {
                PillSwitcher(
                    options = listOf(
                        PillOption("Zoom sends", count = store.meetings.size),
                        PillOption("Past events", count = store.pastEventCount),
                    ),
                    selected = store.historyTab,
                    onSelect = { store.historyTab = it },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                AnimatedContent(
                    targetState = store.historyTab,
                    transitionSpec = { fadeIn(tween(260)) togetherWith fadeOut(tween(160)) },
                    label = "historyTab",
                ) { tab ->
                    if (tab == 0) MeetingsTab(store) else PastTab(store)
                }
            }
        }
    }
}

@Composable
private fun MeetingsTab(store: AdminStore) {
    val context = LocalContext.current
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when (store.historyState) {
            HistoryState.UNSUPPORTED -> item {
                Notice("Meeting history isn’t switched on yet — the Google Sheet script needs one update. See ADMIN-SETUP.md (section “Meeting history”).")
            }
            HistoryState.ERROR, HistoryState.UNKNOWN -> item {
                Notice(
                    "Couldn’t reach the sheet right now — wait a few seconds and try again.",
                    kind = NoticeKind.ERROR,
                    action = { SoftButton("Try again", store::reloadMeetings, compact = true) },
                )
            }
            HistoryState.OK -> if (store.meetings.isEmpty()) {
                item {
                    EmptyState("🗂️", "No Zoom sends yet", "Use Send Zoom Link — each send is saved here under its meeting name.")
                }
            } else {
                val list = store.meetings
                val keys = uniqueKeys(list) { it.id }
                items(list.size, key = { keys[it] }) { i ->
                    val m = list[i]
                    MeetingCard(
                        m = m,
                        rest = MeetingRules.rest(store.rows, m).size,
                        onRest = {
                            store.closeOverlay()
                            store.openZoom(all = true, meeting = m.name, message = m.message)
                        },
                        onExport = { Launch.shareCsv(context, "meeting-" + Csv.slug(m.name) + ".csv", Csv.meeting(m)) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }
}

@Composable
private fun MeetingCard(m: Meeting, rest: Int, onRest: () -> Unit, onExport: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val c = AjmTheme.colors
    var open by rememberSaveable(m.id) { mutableStateOf(false) }
    val turn by animateFloatAsState(if (open) 180f else 0f, label = "chevron")
    AjmCard(modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                IconBadge(Icons.Rounded.VideoCall, Brand.BrightBlue)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(m.name, style = MaterialTheme.typography.titleMedium)
                    Text("Last sent " + Text.dateTime(m.updatedAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TintBadge("✓ ${m.sent} sent", c.success)
                        TintBadge("${m.skipped} skipped", Brand.Slate)
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (rest > 0) SoftButton("Send to the rest ($rest)", onRest, icon = Icons.AutoMirrored.Rounded.Send, compact = true)
                SoftButton("Export CSV", onExport, color = Brand.Slate, icon = Icons.Rounded.FileDownload, compact = true)
            }
        }
        HorizontalDivider(color = c.cardBorder)
        Row(
            Modifier
                .fillMaxWidth()
                .bouncyClick { open = !open }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("People & message", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
            Icon(Icons.Rounded.ExpandMore, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.rotate(turn))
        }
        AnimatedVisibility(visible = open, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                if (m.message.isNotBlank()) {
                    Text(
                        m.message,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(c.subtle)
                            .padding(12.dp),
                    )
                    Spacer(Modifier.height(10.dp))
                }
                m.recipients.forEach { r ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(r.name.ifBlank { "(no name)" }, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                listOf(r.phone, Text.dateTime(r.at)).filter { it.isNotBlank() }.joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = if (r.phone.isNotBlank()) Modifier.bouncyClick { Launch.whatsApp(context, r.phone) } else Modifier,
                            )
                        }
                        TintBadge(if (r.sent) "✓ Sent" else "Skipped", if (r.sent) c.success else Brand.Slate)
                    }
                }
            }
        }
    }
}

@Composable
private fun PastTab(store: AdminStore) {
    val context = LocalContext.current
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!store.archiveSupported) {
            item {
                Notice("Moving people to History isn’t switched on yet — the Google Sheet script needs one update. See ADMIN-SETUP.md (section “History & bulk actions”).")
            }
            return@LazyColumn
        }
        item { SearchField(store.pastQuery, { store.pastQuery = it }, "Search past events or people…", Modifier.fillMaxWidth()) }
        val groups = store.pastGroups
        if (groups.isEmpty()) {
            item {
                if (store.pastQuery.isNotBlank()) {
                    EmptyState("🔎", "No matches", "No past events or people match.")
                } else {
                    EmptyState("🗂️", "Nothing here yet", "Long-press people in the list and choose History — for example once an event has happened.")
                }
            }
        } else {
            items(groups, key = { "past:" + it.name }) { g ->
                PastCard(
                    g,
                    onRestoreAll = { store.askRestore(g.all) },
                    onRestoreOne = { r -> store.askRestore(listOf(r)) },
                    onExport = { Launch.shareCsv(context, "event-" + Csv.slug(g.name) + ".csv", Csv.pastEvent(g.name, g.all)) },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}

@Composable
private fun PastCard(
    g: PastGroup,
    onRestoreAll: () -> Unit,
    onRestoreOne: (org.anmoljeevan.office.core.admin.Registration) -> Unit,
    onExport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = AjmTheme.colors
    var open by rememberSaveable(g.name) { mutableStateOf(false) }
    val turn by animateFloatAsState(if (open) 180f else 0f, label = "chevron")
    AjmCard(modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                IconBadge(Icons.Rounded.Groups, Brand.Indigo)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(g.name, style = MaterialTheme.typography.titleMedium)
                    Text("Moved to history " + Text.dateTime(g.at), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TintBadge("${g.all.size} ${if (g.all.size == 1) "person" else "people"}", Brand.Indigo)
                        TintBadge("✓ ${g.confirmed} confirmed", c.success)
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SoftButton("Restore all", onRestoreAll, icon = Icons.Rounded.Restore, compact = true)
                SoftButton("Export CSV", onExport, color = Brand.Slate, icon = Icons.Rounded.FileDownload, compact = true)
            }
        }
        HorizontalDivider(color = c.cardBorder)
        Row(
            Modifier
                .fillMaxWidth()
                .bouncyClick { open = !open }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Show people", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
            Icon(Icons.Rounded.ExpandMore, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.rotate(turn))
        }
        AnimatedVisibility(visible = open, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)) {
                g.shown.forEachIndexed { i, r ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .staggeredEntrance(i)
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(r.name.ifBlank { "(no name)" }, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                listOf(r.phone, r.source).filter { it.isNotBlank() }.joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        TintBadge(r.statusOrPending, if (r.status == Statuses.CONFIRMED) c.success else Brand.Slate)
                        Spacer(Modifier.width(6.dp))
                        if (r.id.isNotEmpty()) {
                            IconButton(onClick = { onRestoreOne(r) }) { Icon(Icons.Rounded.Restore, "Restore") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IconBadge(icon: androidx.compose.ui.graphics.vector.ImageVector, color: androidx.compose.ui.graphics.Color) {
    val c = AjmTheme.colors
    androidx.compose.foundation.layout.Box(
        Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(c.soft(color)),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = c.ink(color), modifier = Modifier.size(20.dp)) }
}
