package org.anmoljeevan.office.ui.admin

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.anmoljeevan.office.core.Csv
import org.anmoljeevan.office.core.PrintHtml
import org.anmoljeevan.office.core.Text
import org.anmoljeevan.office.core.admin.RegFilter
import org.anmoljeevan.office.core.admin.Registration
import org.anmoljeevan.office.core.admin.RegistrationRules
import org.anmoljeevan.office.core.admin.SORT_OPTIONS
import org.anmoljeevan.office.core.admin.Sources
import org.anmoljeevan.office.core.admin.Statuses
import org.anmoljeevan.office.ui.components.AjmCard
import org.anmoljeevan.office.ui.components.Avatar
import org.anmoljeevan.office.ui.components.CelebrationArt
import org.anmoljeevan.office.ui.components.CountChip
import org.anmoljeevan.office.ui.components.CountUpText
import org.anmoljeevan.office.ui.components.EmptyState
import org.anmoljeevan.office.ui.components.EyebrowText
import org.anmoljeevan.office.ui.components.GradientButton
import org.anmoljeevan.office.ui.components.HeroCard
import org.anmoljeevan.office.ui.components.RollingNumber
import org.anmoljeevan.office.ui.components.SearchField
import org.anmoljeevan.office.ui.components.TintBadge
import org.anmoljeevan.office.ui.components.bouncyClick
import org.anmoljeevan.office.ui.components.isScrollingUp
import org.anmoljeevan.office.ui.components.staggeredEntrance
import org.anmoljeevan.office.ui.theme.AjmTheme
import org.anmoljeevan.office.ui.theme.Brand
import org.anmoljeevan.office.ui.theme.Figure
import org.anmoljeevan.office.ui.theme.Motion
import org.anmoljeevan.office.util.Launch
import java.time.LocalDate

/** The admin side: registrations list ⇄ one registration (a container transform), plus History and Zoom sends. */
@Composable
fun AdminRoot(store: AdminStore, onLogout: () -> Unit) {
    val listState = rememberLazyListState()
    val barState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(barState)

    Box(Modifier.fillMaxSize()) {
        SharedTransitionLayout {
            AnimatedContent(
                targetState = store.detailKey,
                transitionSpec = { fadeIn(tween(380)) togetherWith fadeOut(tween(380)) },
                label = "detail",
            ) { key ->
                if (key == null) {
                    RegistrationsScreen(store, listState, scrollBehavior, this@SharedTransitionLayout, this@AnimatedContent, onLogout)
                } else {
                    val r = store.rowFor(key)
                    if (r != null) {
                        RegistrationDetail(r, store, this@SharedTransitionLayout, this@AnimatedContent)
                    } else {
                        LaunchedEffect(key) { store.detailKey = null }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = store.overlay == AdminOverlay.HISTORY,
            enter = slideInVertically(tween(460, easing = Motion.EmphasizedDecelerate)) { it } + fadeIn(tween(200)),
            exit = slideOutVertically(tween(320, easing = Motion.EmphasizedAccelerate)) { it } + fadeOut(tween(300)),
        ) { HistoryScreen(store) }

        AnimatedVisibility(
            visible = store.overlay == AdminOverlay.ZOOM,
            enter = slideInVertically(tween(460, easing = Motion.EmphasizedDecelerate)) { it } + fadeIn(tween(200)),
            exit = slideOutVertically(tween(320, easing = Motion.EmphasizedAccelerate)) { it } + fadeOut(tween(300)),
        ) {
            store.zoom?.let { ZoomSendScreen(store, it) }
        }

        AdminDialogs(store)
    }
}

@Composable
private fun RegistrationsScreen(
    store: AdminStore,
    listState: LazyListState,
    scrollBehavior: TopAppBarScrollBehavior,
    shared: SharedTransitionScope,
    anim: AnimatedVisibilityScope,
    onLogout: () -> Unit,
) {
    val context = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    BackHandler(enabled = store.selecting) { store.clearSelection() }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(store.snackbar) },
        topBar = {
            AnimatedContent(
                targetState = store.selecting,
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
                label = "topBar",
            ) { selecting ->
                if (selecting) {
                    TopAppBar(
                        navigationIcon = {
                            IconButton(onClick = store::clearSelection) { Icon(Icons.Rounded.Close, "Clear selection") }
                        },
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RollingNumber(store.selection.size, MaterialTheme.typography.titleLarge)
                                Text(" selected", style = MaterialTheme.typography.titleLarge)
                            }
                        },
                        actions = {
                            TextButton(onClick = store::toggleAllVisible) {
                                Text(if (store.allVisibleSelected) "Unselect all" else "Select all")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    )
                } else {
                    LargeTopAppBar(
                        title = { Text("Registrations") },
                        actions = {
                            IconButton(onClick = { store.openHistory() }) { Icon(Icons.Rounded.History, "History") }
                            IconButton(onClick = store::refresh) { Icon(Icons.Rounded.Refresh, "Refresh") }
                            Box {
                                IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, "More") }
                                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                    DropdownMenuItem(
                                        text = { Text("Print list") },
                                        leadingIcon = { Icon(Icons.Rounded.Print, null) },
                                        onClick = {
                                            menu = false
                                            val rows = RegistrationRules.filter(store.rows, store.filter, store.query)
                                            Launch.printHtml(context, "AJM attendee list", PrintHtml.attendeeList(store.filterLabel, rows))
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Export CSV") },
                                        leadingIcon = { Icon(Icons.Rounded.FileDownload, null) },
                                        onClick = {
                                            menu = false
                                            val rows = RegistrationRules.filter(store.rows, store.filter, store.query)
                                            Launch.shareCsv(context, Csv.registrationsFileName(LocalDate.now()), Csv.registrations(rows))
                                        },
                                    )
                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        text = { Text("Log out") },
                                        leadingIcon = { Icon(Icons.Rounded.Logout, null) },
                                        onClick = {
                                            menu = false
                                            onLogout()
                                        },
                                    )
                                }
                            }
                        },
                        scrollBehavior = scrollBehavior,
                        colors = TopAppBarDefaults.largeTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        ),
                    )
                }
            }
        },
        floatingActionButton = {
            AnimatedVisibility(visible = !store.selecting, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
                ExtendedFloatingActionButton(
                    text = { Text("Send Zoom Link", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.AutoMirrored.Rounded.Send, null) },
                    onClick = { store.openZoom() },
                    expanded = listState.isScrollingUp(),
                    containerColor = Brand.Navy,
                    contentColor = Color.White,
                )
            }
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = store.refreshing,
            onRefresh = store::refresh,
            modifier = Modifier
                .padding(top = padding.calculateTopPadding())
                .fillMaxSize(),
        ) {
            val visible = store.visible
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 140.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "hero") { StatsHero(store) }
                item(key = "tiles") { SourceTiles(store) }
                stickyHeader(key = "filters") { FilterBar(store) }
                item(key = "count") { CountLine(store) }
                if (visible.isEmpty()) {
                    item(key = "empty") { EmptyList(store) }
                } else {
                    items(visible, key = { store.keyFor(it) }, contentType = { "registration" }) { r ->
                        RegistrationCard(
                            r = r,
                            store = store,
                            shared = shared,
                            anim = anim,
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
            BulkBar(store, Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun StatsHero(store: AdminStore) {
    val s = store.stats
    val c = AjmTheme.colors
    HeroCard(Modifier.fillMaxWidth().staggeredEntrance(0)) {
        EyebrowText("Total registrations", color = c.onHeroMuted)
        Spacer(Modifier.height(4.dp))
        CountUpText(s.total, Figure.copy(fontSize = 52.sp, lineHeight = 56.sp), color = Color.White)
        Spacer(Modifier.height(14.dp))
        StackBar(s.confirmed, s.contacted, s.pending)
        Spacer(Modifier.height(12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            LegendDot(Color(0xFF4ADE9A), s.confirmed, "confirmed")
            LegendDot(Color(0xFF8FB5FF), s.contacted, "contacted")
            LegendDot(Color.White.copy(alpha = 0.5f), s.pending, "pending")
        }
        if (s.inHistory > 0) {
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier
                    .bouncyClick(shape = RoundedCornerShape(100)) { store.openHistory(1) }
                    .background(Color.White.copy(alpha = 0.14f))
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Archive, null, tint = Color.White, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text("${s.inHistory} in History →", color = Color.White, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun StackBar(confirmed: Int, contacted: Int, pending: Int) {
    val total = (confirmed + contacted + pending).coerceAtLeast(1).toFloat()
    val a by animateFloatAsState(confirmed / total, spring(dampingRatio = 0.85f, stiffness = 60f), label = "a")
    val b by animateFloatAsState(contacted / total, spring(dampingRatio = 0.85f, stiffness = 60f), label = "b")
    Box(
        Modifier
            .fillMaxWidth()
            .height(9.dp)
            .clip(RoundedCornerShape(100))
            .background(Color.White.copy(alpha = 0.18f)),
    ) {
        Row(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(a.coerceIn(0f, 1f)).background(Color(0xFF4ADE9A)))
            val rest = (1f - a).coerceAtLeast(0.0001f)
            Box(Modifier.fillMaxHeight().fillMaxWidth((b / rest).coerceIn(0f, 1f)).background(Color(0xFF8FB5FF)))
        }
    }
}

@Composable
private fun LegendDot(color: Color, n: Int, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        RollingNumber(n, MaterialTheme.typography.labelLarge, color = Color.White)
        Text(" $label", style = MaterialTheme.typography.bodySmall, color = AjmTheme.colors.onHeroMuted)
    }
}

@Composable
private fun SourceTiles(store: AdminStore) {
    val s = store.stats
    val tiles = listOf(
        Triple(RegFilter.WOMENS, s.bySource[Sources.WOMENS] ?: 0, sourceStyle(Sources.WOMENS)),
        Triple(RegFilter.EVENT, s.bySource[Sources.EVENT] ?: 0, sourceStyle(Sources.EVENT)),
        Triple(RegFilter.INVITE, s.bySource[Sources.INVITE] ?: 0, sourceStyle(Sources.INVITE)),
        Triple(RegFilter.DUPLICATES, s.duplicates, SourceStyle(AjmTheme.colors.warning, Icons.Rounded.ContentCopy)),
    )
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        tiles.forEachIndexed { i, (f, n, style) ->
            val active = store.filter == f
            val c = AjmTheme.colors
            val border by animateColorAsState(if (active) style.color else c.cardBorder, label = "tileBorder")
            AjmCard(
                Modifier
                    .width(150.dp)
                    .staggeredEntrance(i + 1),
                onClick = { store.updateFilter(if (active) RegFilter.ALL else f) },
                border = androidx.compose.foundation.BorderStroke(if (active) 2.dp else 1.dp, border),
                contentPadding = PaddingValues(16.dp),
            ) {
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(c.soft(style.color)),
                    contentAlignment = Alignment.Center,
                ) { Icon(style.icon, null, tint = c.ink(style.color), modifier = Modifier.size(19.dp)) }
                Spacer(Modifier.height(10.dp))
                EyebrowText(if (f == RegFilter.DUPLICATES) "Duplicates" else f.label)
                CountUpText(n, Figure.copy(fontSize = 32.sp, lineHeight = 36.sp), color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun FilterBar(store: AdminStore) {
    var sortMenu by remember { mutableStateOf(false) }
    val c = AjmTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SearchField(store.query, store::updateQuery, "Search name, phone, email…", Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            Box {
                Box(
                    Modifier
                        .size(48.dp)
                        .bouncyClick(shape = CircleShape) { sortMenu = true }
                        .background(c.card)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Sort, "Sort") }
                DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                    SORT_OPTIONS.forEach { o ->
                        DropdownMenuItem(
                            text = { Text(o.label) },
                            trailingIcon = { if (o == store.sort) Icon(Icons.Rounded.Check, null) },
                            onClick = {
                                store.sort = o
                                sortMenu = false
                            },
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RegFilter.entries.forEach { f ->
                val dup = f == RegFilter.DUPLICATES
                CountChip(
                    label = if (dup) "⚠ Duplicates" else f.label,
                    count = store.filterCounts[f] ?: 0,
                    selected = store.filter == f,
                    onClick = { store.updateFilter(f) },
                    accent = if (dup) c.warning else MaterialTheme.colorScheme.primary,
                    selectedBrush = if (dup) null else c.heroBrush,
                )
            }
        }
    }
}

@Composable
private fun CountLine(store: AdminStore) {
    val shown = store.visible.size
    val active = store.activeCount
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            if (active > 0) "Showing $shown of $active" else "",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        if (shown > 0) {
            Text(
                "Long-press to select",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun EmptyList(store: AdminStore) {
    when {
        store.activeCount > 0 -> EmptyState("🔎", "No matches", "No registrations match this view.")
        store.rows.isNotEmpty() -> Column(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            CelebrationArt()
            Spacer(Modifier.height(16.dp))
            Text("All caught up!", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                "Everyone has been moved to History. New registrations will appear here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Spacer(Modifier.height(18.dp))
            GradientButton("Open History", onClick = { store.openHistory(1) }, icon = Icons.Rounded.History, height = 48.dp)
        }
        else -> EmptyState("📭", "No registrations yet", "New sign-ups from the website will appear here.")
    }
}

@Composable
private fun RegistrationCard(
    r: Registration,
    store: AdminStore,
    shared: SharedTransitionScope,
    anim: AnimatedVisibilityScope,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val c = AjmTheme.colors
    val key = store.keyFor(r)
    val selected = r.id.isNotEmpty() && r.id in store.selection
    val selecting = store.selecting
    val style = sourceStyle(r.source)
    val dup = RegistrationRules.dupCount(r, store.dupCounts)
    val shape = RoundedCornerShape(24.dp)
    val primary = MaterialTheme.colorScheme.primary
    val border by animateColorAsState(if (selected) primary else if (dup > 1) c.warning.copy(alpha = 0.45f) else c.cardBorder, label = "cardBorder")
    val bg by animateColorAsState(if (selected) (if (c.isDark) Color(0xFF16284F) else Color(0xFFEEF4FF)) else c.card, label = "cardBg")

    with(shared) {
        Column(
            modifier
                .fillMaxWidth()
                .sharedBounds(
                    rememberSharedContentState("card-$key"),
                    animatedVisibilityScope = anim,
                    resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                    clipInOverlayDuringTransition = OverlayClip(shape),
                )
                .bouncyClick(
                    shape = shape,
                    elevation = if (selected) 10.dp else 4.dp,
                    shadowColor = c.shadow.copy(alpha = 0.35f),
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        store.toggle(r)
                    },
                    onClick = { if (selecting) store.toggle(r) else store.detailKey = key },
                )
                .background(bg)
                .border(if (selected) 2.dp else 1.dp, border, shape)
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.sharedElement(rememberSharedContentState("avatar-$key"), anim)) {
                    AnimatedContent(
                        targetState = selected,
                        transitionSpec = { (scaleIn(spring(dampingRatio = 0.5f)) + fadeIn()) togetherWith (scaleOut() + fadeOut()) },
                        label = "avatar",
                    ) { sel ->
                        if (sel) {
                            Box(Modifier.size(44.dp).clip(CircleShape).background(primary), contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.Check, null, tint = Color.White)
                            }
                        } else {
                            Avatar(Text.initials(r.name), style.color)
                        }
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            r.name.ifBlank { "(no name)" },
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .sharedBounds(rememberSharedContentState("name-$key"), anim),
                        )
                        if (dup > 1) {
                            Spacer(Modifier.width(6.dp))
                            TintBadge("×$dup", c.warning, icon = Icons.Rounded.Warning)
                        }
                    }
                    Spacer(Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TintBadge(r.source.ifBlank { "Other" }, style.color)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            Text.dateTime(r.timestamp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                StatusPill(r.statusOrPending, onChange = { store.setStatus(r, it) }, enabled = r.id.isNotEmpty() && !selecting)
            }
            if (r.phone.isNotBlank() || r.email.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (r.phone.isNotBlank()) ContactChip(Icons.Rounded.Chat, r.phone, c.success, onClick = { Launch.whatsApp(context, r.phone) })
                    if (r.email.isNotBlank()) ContactChip(Icons.Rounded.Email, r.email, primary, onClick = { Launch.email(context, r.email) })
                }
            }
            if (r.details.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    r.details,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** The dark floating toolbar that rises when rows are selected. */
@Composable
private fun BulkBar(store: AdminStore, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = store.selecting,
        modifier = modifier
            .navigationBarsPadding()
            .padding(16.dp),
        enter = slideInVertically(spring(dampingRatio = 0.72f, stiffness = 380f)) { it * 2 } + fadeIn(),
        exit = slideOutVertically(tween(220)) { it * 2 } + fadeOut(),
    ) {
        Surface(shape = RoundedCornerShape(28.dp), color = Color(0xFF101A33), shadowElevation = 18.dp) {
            Column(Modifier.padding(horizontal = 10.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                val busy = store.bulkBusy
                AnimatedVisibility(visible = busy != null) {
                    Row(Modifier.padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(busy.orEmpty(), color = Color.White, style = MaterialTheme.typography.labelMedium)
                    }
                }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    BarAction("Confirm", Icons.Rounded.CheckCircle, Color(0xFF4ADE9A), busy == null) { store.bulkStatus(Statuses.CONFIRMED) }
                    BarAction("Contacted", Icons.Rounded.Phone, Color(0xFF8FB5FF), busy == null) { store.bulkStatus(Statuses.CONTACTED) }
                    BarAction("Pending", Icons.Rounded.Schedule, Color(0xFFC6D0E4), busy == null) { store.bulkStatus(Statuses.PENDING) }
                    BarAction("History", Icons.Rounded.Archive, Color(0xFFFFD48A), busy == null) { store.askArchive(store.selectedRows) }
                    BarAction("Delete", Icons.Rounded.Delete, Color(0xFFFFA3A3), busy == null) { store.askDelete(store.selectedRows) }
                }
            }
        }
    }
}

@Composable
private fun BarAction(label: String, icon: ImageVector, tint: Color, enabled: Boolean, onClick: () -> Unit) {
    Column(
        Modifier
            .width(68.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.4f }
            .bouncyClick(enabled = enabled, shape = RoundedCornerShape(18.dp), onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, color = Color.White, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}
