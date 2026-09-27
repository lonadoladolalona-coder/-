package org.anmoljeevan.office.ui.media

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.ViewWeek
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import org.anmoljeevan.office.core.media.ItemRules
import org.anmoljeevan.office.core.media.StockRules
import org.anmoljeevan.office.ui.components.AjmCard
import org.anmoljeevan.office.ui.theme.AjmTheme
import org.anmoljeevan.office.ui.theme.Brand
import org.anmoljeevan.office.util.Launch

private data class TabSpec(val tab: MediaTab, val label: String, val icon: ImageVector)

@Composable
fun MediaRoot(store: MediaStore, onLogout: () -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var menu by remember { mutableStateOf(false) }

    // look for the team's changes every 15 seconds, only while the app is on screen
    LaunchedEffect(store) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                delay(15_000)
                store.tick()
            }
        }
    }
    // going to the background saves anything typed
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_STOP) store.saveInBackground() }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(store.sessionExpired) {
        if (store.sessionExpired) {
            delay(1200)
            onLogout()
        }
    }

    val tabs = buildList {
        add(TabSpec(MediaTab.LOG, "Log", Icons.Rounded.CalendarMonth))
        add(TabSpec(MediaTab.PLAN, "Plan", Icons.Rounded.ViewWeek))
        if (store.hasStock) add(TabSpec(MediaTab.STOCK, "Stock", Icons.Rounded.Inventory2))
        add(TabSpec(MediaTab.SUMMARY, "Summary", Icons.Rounded.BarChart))
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(store.snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Media Office")
                        AnimatedContent(store.tab, transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(120)) }, label = "tabTitle") { t ->
                            Text(t.title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { store.setHowto(!store.howtoVisible) }) { Icon(Icons.Rounded.HelpOutline, "How to use") }
                    if (store.tab == MediaTab.SUMMARY) {
                        IconButton(onClick = store::reloadTotals) { Icon(Icons.Rounded.Refresh, "Refresh") }
                    }
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, "More") }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            if (store.tab == MediaTab.STOCK) {
                                DropdownMenuItem(
                                    text = { Text("Export CSV") },
                                    leadingIcon = { Icon(Icons.Rounded.FileDownload, null) },
                                    onClick = {
                                        menu = false
                                        if (store.items.isEmpty()) store.toast("Nothing to export")
                                        else Launch.shareCsv(context, "media-content-stock.csv", ItemRules.csv(store.items))
                                    },
                                )
                            }
                            if (store.tab == MediaTab.SUMMARY) {
                                DropdownMenuItem(
                                    text = { Text("Export CSV") },
                                    leadingIcon = { Icon(Icons.Rounded.FileDownload, null) },
                                    onClick = {
                                        menu = false
                                        val csv = StockRules.totalsCsv(store.charts, store.year, store.hasUploads, store.stock, store.items)
                                        if (csv == null) store.toast("Nothing to export") else Launch.shareCsv(context, "media-total-work.csv", csv)
                                    },
                                )
                            }
                            if (store.tab == MediaTab.STOCK || store.tab == MediaTab.SUMMARY) HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Log out") },
                                leadingIcon = { Icon(Icons.Rounded.Logout, null) },
                                onClick = {
                                    menu = false
                                    store.logout(onLogout)
                                },
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            NavigationBar(containerColor = AjmTheme.colors.card, tonalElevation = 0.dp) {
                tabs.forEach { spec ->
                    NavigationBarItem(
                        selected = store.tab == spec.tab,
                        onClick = { store.selectTab(spec.tab) },
                        icon = { Icon(spec.icon, null) },
                        label = { Text(spec.label, fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            indicatorColor = if (AjmTheme.colors.isDark) Brand.Blue else Brand.NavyMid,
                        ),
                    )
                }
            }
        },
        floatingActionButton = {
            AnimatedVisibility(visible = store.tab == MediaTab.STOCK && store.hasStock, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
                ExtendedFloatingActionButton(
                    text = { Text("Add piece", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Rounded.Add, null) },
                    onClick = store::newItemDraft,
                    containerColor = Brand.Navy,
                    contentColor = Color.White,
                )
            }
        },
    ) { padding ->
        AnimatedContent(
            targetState = store.tab,
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            transitionSpec = {
                // Material "fade through"
                (fadeIn(tween(240, delayMillis = 80)) + scaleIn(tween(240, delayMillis = 80), initialScale = 0.96f)) togetherWith fadeOut(tween(90))
            },
            label = "mediaTab",
        ) { t ->
            when (t) {
                MediaTab.LOG -> LogScreen(store)
                MediaTab.PLAN -> PlanScreen(store)
                MediaTab.STOCK -> StockScreen(store)
                MediaTab.SUMMARY -> SummaryScreen(store)
            }
        }
    }

    store.pendingDiscard?.let {
        AlertDialog(
            onDismissRequest = { store.pendingDiscard = null },
            title = { Text("Changes not saved") },
            text = { Text("Your latest changes could not be saved. Discard them and continue?") },
            confirmButton = { TextButton(onClick = store::confirmDiscard) { Text("Discard") } },
            dismissButton = { TextButton(onClick = { store.pendingDiscard = null }) { Text("Keep editing") } },
        )
    }
    ItemSheet(store)
    StockEditDialog(store)
    CellEditorSheet(store)
    DeleteItemDialog(store)
}

/** The "New here?" strip — shown until it is dismissed on this phone. */
@Composable
fun HowToCard(store: MediaStore) {
    AnimatedVisibility(visible = store.howtoVisible) {
        AjmCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("New here?", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                TextButton(onClick = { store.setHowto(false) }) { Text("Got it") }
            }
            Spacer(Modifier.height(6.dp))
            Step(1, "Content Log — tap + for each piece made that day")
            if (store.hasStock) Step(2, "Content Stock — list each piece, swipe right once it is posted")
            Step(if (store.hasStock) 3 else 2, "Stock Summary — see how much is left")
            Spacer(Modifier.height(4.dp))
            Text("Everything saves by itself.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Step(n: Int, text: String) {
    Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) { Text("$n", color = Color.White, style = MaterialTheme.typography.labelMedium) }
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

internal val PillShape = RoundedCornerShape(100)
