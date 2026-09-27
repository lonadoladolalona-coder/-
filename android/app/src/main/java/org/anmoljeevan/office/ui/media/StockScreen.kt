package org.anmoljeevan.office.ui.media

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.anmoljeevan.office.core.Text
import org.anmoljeevan.office.core.media.ContentItem
import org.anmoljeevan.office.core.media.ItemRules
import org.anmoljeevan.office.core.media.ItemStatus
import org.anmoljeevan.office.core.media.PLATFORMS
import org.anmoljeevan.office.ui.components.AjmCard
import org.anmoljeevan.office.ui.components.CountChip
import org.anmoljeevan.office.ui.components.EmptyState
import org.anmoljeevan.office.ui.components.ErrorState
import org.anmoljeevan.office.ui.components.GradientButton
import org.anmoljeevan.office.ui.components.SearchField
import org.anmoljeevan.office.ui.components.SheetHeader
import org.anmoljeevan.office.ui.components.SkeletonCards
import org.anmoljeevan.office.ui.components.TintBadge
import org.anmoljeevan.office.ui.components.bouncyClick
import org.anmoljeevan.office.ui.theme.AjmTheme
import org.anmoljeevan.office.ui.theme.Brand
import org.anmoljeevan.office.util.Launch

@Composable
private fun statusTint(status: String): Color = when (status) {
    ItemStatus.UPLOADED -> AjmTheme.colors.success
    ItemStatus.NOT_NEEDED -> Brand.Slate
    else -> AjmTheme.colors.warning
}

/** Every piece the team has made — and exactly which ones are still waiting to be uploaded. */
@Composable
fun StockScreen(store: MediaStore) {
    val view = store.itemView
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item("howto") { HowToCard(store) }
        item("status") {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ItemStatus.ALL.forEach { s ->
                    CountChip(s, view.counts[s] ?: 0, store.statusFilter == s, { store.statusFilter = s }, accent = statusTint(s))
                }
                CountChip("All", view.baseCount, store.statusFilter == ItemRules.ALL, { store.statusFilter = ItemRules.ALL }, selectedBrush = AjmTheme.colors.heroBrush)
            }
        }
        item("search") { SearchField(store.itemQuery, { store.itemQuery = it }, "Search titles…", Modifier.fillMaxWidth()) }
        item("types") {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CountChip("All types", null, store.typeFilter == ItemRules.ALL, { store.typeFilter = ItemRules.ALL }, accent = MaterialTheme.colorScheme.onSurface)
                store.itemTypes.forEach { t ->
                    CountChip(t, null, store.typeFilter == t, { store.typeFilter = t }, accent = typeColor(t), dot = typeColor(t))
                }
            }
        }
        when {
            store.itemsLoading -> item("loading") { SkeletonCards(4) }
            store.itemsError != null -> item("error") { ErrorState(store.itemsError.orEmpty(), store::reloadItems) }
            view.shown.isEmpty() -> item("empty") {
                when {
                    store.items.isEmpty() -> EmptyState("📦", "Nothing in the list yet", "Add the pieces you have made with the + button.")
                    store.statusFilter == ItemStatus.READY && view.baseCount == 0 -> EmptyState("✅", "Nothing here", "Nothing for this filter.")
                    store.statusFilter == ItemStatus.READY -> EmptyState("🎉", "All posted!", "Everything here has been uploaded or marked not needed.")
                    else -> EmptyState("🔎", "No matches", "No pieces match this filter.")
                }
            }
            else -> {
                item("hint") {
                    Text(
                        "Swipe right when a piece is uploaded · swipe left if it isn’t needed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
                items(view.shown, key = { it.id }) { item ->
                    SwipeItem(store, item, Modifier.animateItem())
                }
            }
        }
    }
}

@Composable
private fun SwipeItem(store: MediaStore, item: ContentItem, modifier: Modifier = Modifier) {
    val haptic = LocalHapticFeedback.current
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> if (item.status != ItemStatus.UPLOADED) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    store.setItemStatus(item, ItemStatus.UPLOADED)
                }
                SwipeToDismissBoxValue.EndToStart -> if (item.status != ItemStatus.NOT_NEEDED) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    store.setItemStatus(item, ItemStatus.NOT_NEEDED)
                }
                SwipeToDismissBoxValue.Settled -> Unit
            }
            false // always spring back; the card updates (or moves to another tab) by itself
        },
    )
    SwipeToDismissBox(
        state = state,
        modifier = modifier,
        enableDismissFromStartToEnd = item.status != ItemStatus.UPLOADED,
        enableDismissFromEndToStart = item.status != ItemStatus.NOT_NEEDED,
        backgroundContent = {
            val dir = state.dismissDirection
            val c = AjmTheme.colors
            val color = when (dir) {
                SwipeToDismissBoxValue.StartToEnd -> c.success
                SwipeToDismissBoxValue.EndToStart -> Brand.Slate
                else -> Color.Transparent
            }
            val bg by animateColorAsState(color, label = "swipeBg")
            val scale by animateFloatAsState(if (state.targetValue != SwipeToDismissBoxValue.Settled) 1.25f else 0.9f, spring(dampingRatio = 0.5f), label = "swipeIcon")
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(bg)
                    .padding(horizontal = 22.dp),
                contentAlignment = if (dir == SwipeToDismissBoxValue.EndToStart) Alignment.CenterEnd else Alignment.CenterStart,
            ) {
                if (dir != SwipeToDismissBoxValue.Settled) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (dir == SwipeToDismissBoxValue.StartToEnd) Icons.Rounded.CloudDone else Icons.Rounded.DoNotDisturbOn,
                            null,
                            tint = Color.White,
                            modifier = Modifier.graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            },
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(if (dir == SwipeToDismissBoxValue.StartToEnd) "Uploaded" else "Not needed", color = Color.White, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        },
    ) {
        ItemCard(store, item)
    }
}

@Composable
private fun ItemCard(store: MediaStore, item: ContentItem) {
    val context = LocalContext.current
    val c = AjmTheme.colors
    val color = typeColor(item.type)
    val url = ItemRules.safeUrl(item.link)
    val busy = item.id in store.busyItems
    val done = item.status != ItemStatus.READY
    AjmCard(
        Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = if (done) 0.86f else 1f },
        shape = RoundedCornerShape(20.dp),
        contentPadding = PaddingValues(0.dp),
        elevation = 4.dp,
    ) {
        Row(Modifier.drawBehind { drawRect(color, size = Size(5.dp.toPx(), size.height)) }) {
            Column(Modifier.padding(start = 19.dp, end = 10.dp, top = 12.dp, bottom = 12.dp).weight(1f)) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(item.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    IconButton(onClick = { store.itemSheet = ItemDraft(item.id, item.title, item.type, item.link) }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Rounded.Edit, "Edit", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = { store.confirmDelete = item }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Rounded.DeleteOutline, "Remove", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(4.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    TintBadge(item.type, color)
                    Text("Added ${Text.shortDate(item.addedAt)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (done) Text("· ${item.status} ${Text.shortDate(item.updatedAt)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (item.platform.isNotEmpty()) TintBadge(item.platform, c.success, strong = true)
                }
                if (item.link.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.then(if (url != null) Modifier.bouncyClick(shape = RoundedCornerShape(100)) { Launch.openUrl(context, url) } else Modifier),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(if (url != null) Icons.Rounded.OpenInNew else Icons.Rounded.Link, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(
                            if (url != null) "Open link" else item.link,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                StatusSegments(item.status, enabled = !busy) { store.setItemStatus(item, it) }
                AnimatedVisibility(item.status == ItemStatus.UPLOADED, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                    Column {
                        Spacer(Modifier.height(10.dp))
                        Text("Uploaded to", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(6.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            PLATFORMS.forEach { p ->
                                val on = item.platform == p
                                val bg by animateColorAsState(if (on) c.success else c.chip, label = "platform")
                                Text(
                                    p,
                                    color = if (on) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier
                                        .bouncyClick(enabled = !busy, shape = RoundedCornerShape(100)) { store.togglePlatform(item, p) }
                                        .background(bg)
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Ready · Uploaded · Not needed, with a sliding highlight. */
@Composable
private fun StatusSegments(status: String, enabled: Boolean, onSelect: (String) -> Unit) {
    val c = AjmTheme.colors
    val index = ItemStatus.ALL.indexOf(status).coerceAtLeast(0)
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(38.dp)
            .clip(RoundedCornerShape(100))
            .background(c.chip)
            .padding(3.dp),
    ) {
        val w = maxWidth / 3
        val x by animateDpAsState(w * index, spring(dampingRatio = 0.72f, stiffness = 420f), label = "segX")
        val tint = statusTint(status)
        val bg by animateColorAsState(if (c.isDark) tint.copy(alpha = 0.35f) else lighten(tint), label = "segBg")
        Box(
            Modifier
                .offset(x = x)
                .width(w)
                .fillMaxHeight()
                .clip(RoundedCornerShape(100))
                .background(bg),
        )
        Row(Modifier.fillMaxSize()) {
            ItemStatus.ALL.forEach { s ->
                val on = s == status
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(100))
                        .bouncyClick(enabled = enabled && !on) { onSelect(s) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        s,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (on) c.ink(statusTint(s)) else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

private fun lighten(c: Color): Color = androidx.compose.ui.graphics.lerp(c, Color.White, 0.8f)

/** Add or edit one piece of content. */
@Composable
fun ItemSheet(store: MediaStore) {
    val draft = store.itemSheet ?: return
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember(draft) { mutableStateOf(draft.title) }
    var type by remember(draft) { mutableStateOf(draft.type) }
    var link by remember(draft) { mutableStateOf(draft.link) }
    ModalBottomSheet(onDismissRequest = { store.itemSheet = null }, sheetState = state, containerColor = MaterialTheme.colorScheme.surfaceContainerLowest) {
        Column(
            Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        ) {
            SheetHeader(if (draft.id == null) "Add a piece" else "Edit piece", if (draft.id == null) "It starts as Ready — waiting to be uploaded" else null)
            Column(Modifier.padding(horizontal = 22.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 120) title = it },
                    label = { Text("Title") },
                    placeholder = { Text("e.g. Easter morning reel") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(14.dp))
                Text("Type", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    store.itemTypes.forEach { t ->
                        CountChip(t, null, type == t, { type = t }, accent = typeColor(t), dot = typeColor(t))
                    }
                }
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = link,
                    onValueChange = { if (it.length <= 300) link = it },
                    label = { Text("Link (optional)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(18.dp))
                GradientButton(
                    if (draft.id == null) "Add to stock" else "Save",
                    onClick = { store.saveDraft(draft.copy(title = title, type = type, link = link)) },
                    enabled = title.isNotBlank(),
                    loading = store.itemSaving,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
fun DeleteItemDialog(store: MediaStore) {
    val item = store.confirmDelete ?: return
    AlertDialog(
        onDismissRequest = { store.confirmDelete = null },
        icon = { Icon(Icons.Rounded.DeleteOutline, null) },
        title = { Text("Remove from the list?") },
        text = { Text("“${item.title}” will be removed from Content Stock.") },
        confirmButton = {
            Button(
                onClick = { store.deleteItem(item) },
                colors = ButtonDefaults.buttonColors(containerColor = AjmTheme.colors.danger, contentColor = MaterialTheme.colorScheme.onError),
            ) { Text("Remove") }
        },
        dismissButton = { TextButton(onClick = { store.confirmDelete = null }) { Text("Cancel") } },
    )
}

