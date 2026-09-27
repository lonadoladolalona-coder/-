package org.anmoljeevan.office.ui.media

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.anmoljeevan.office.core.Text
import org.anmoljeevan.office.core.media.LedgerRow
import org.anmoljeevan.office.ui.components.AjmCard
import org.anmoljeevan.office.ui.components.CountChip
import org.anmoljeevan.office.ui.components.CountUpText
import org.anmoljeevan.office.ui.components.Donut
import org.anmoljeevan.office.ui.components.EmptyState
import org.anmoljeevan.office.ui.components.ErrorState
import org.anmoljeevan.office.ui.components.EyebrowText
import org.anmoljeevan.office.ui.components.GrowBar
import org.anmoljeevan.office.ui.components.HeroCard
import org.anmoljeevan.office.ui.components.SectionTitle
import org.anmoljeevan.office.ui.components.SkeletonCards
import org.anmoljeevan.office.ui.components.TintBadge
import org.anmoljeevan.office.ui.components.bouncyClick
import org.anmoljeevan.office.ui.components.staggeredEntrance
import org.anmoljeevan.office.ui.theme.AjmTheme
import org.anmoljeevan.office.ui.theme.Brand
import org.anmoljeevan.office.ui.theme.Figure

/** Of everything the media team has made: how much is uploaded, and how much is still left. */
@Composable
fun SummaryScreen(store: MediaStore) {
    val l = store.ledger
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        when {
            store.totalsLoading -> item("loading") { SkeletonCards(4, height = 120.dp) }
            store.totalsError != null -> item("error") { ErrorState(store.totalsError.orEmpty(), store::reloadTotals) }
            l.isEmpty -> item("empty") { EmptyState("📭", "Nothing yet", "Nothing has been filled in yet.") }
            else -> {
                item("hero") { Hero(store) }
                item("tiles") { Tiles(store) }
                item("ledgerHead") {
                    SectionTitle(
                        "Stock by type",
                        subtitle = if (store.hasStock) "Left = Opening + Made − Uploaded − Not needed. Tap Opening or Not needed to change it." else null,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                itemsIndexed(l.rows, key = { _, r -> "type:" + r.type }) { i, r ->
                    LedgerCard(store, r, Modifier.staggeredEntrance(i))
                }
                item("everything") { EverythingCard(store) }
                item("monthHead") {
                    SectionTitle("By month", subtitle = "Tap a number to open the log it was added up from", modifier = Modifier.padding(top = 6.dp))
                }
                item("years") {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CountChip("All time", null, store.year == "all", { store.year = "all" }, selectedBrush = AjmTheme.colors.heroBrush)
                        store.years.forEach { y -> CountChip(y, null, store.year == y, { store.year = y }, selectedBrush = AjmTheme.colors.heroBrush) }
                    }
                }
                item("months") { MonthTableCard(store) }
            }
        }
    }
}

@Composable
private fun Hero(store: MediaStore) {
    val l = store.ledger
    val c = AjmTheme.colors
    HeroCard(Modifier.fillMaxWidth().staggeredEntrance(0)) {
        if (store.hasUploads) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Donut(l.percent)
                Spacer(Modifier.width(20.dp))
                Column {
                    EyebrowText("Left to upload", color = c.onHeroMuted)
                    CountUpText(l.left, Figure.copy(fontSize = 50.sp, lineHeight = 54.sp), color = Color.White)
                    Text("${Text.number(l.uploaded)} of ${Text.number(l.pipeline)} uploaded", style = MaterialTheme.typography.bodySmall, color = c.onHeroMuted)
                }
            }
        } else {
            EyebrowText("Total content", color = c.onHeroMuted)
            CountUpText(l.made, Figure.copy(fontSize = 50.sp, lineHeight = 54.sp), color = Color.White)
            Text("made so far, added up from all the monthly charts", style = MaterialTheme.typography.bodySmall, color = c.onHeroMuted)
            Spacer(Modifier.height(8.dp))
            Text(
                "Tracking uploads needs the Google Sheet script to be updated — see ADMIN-SETUP.md.",
                style = MaterialTheme.typography.bodySmall,
                color = c.onHeroMuted,
            )
        }
    }
}

@Composable
private fun Tiles(store: MediaStore) {
    if (!store.hasUploads) return
    val l = store.ledger
    val c = AjmTheme.colors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Tile("Uploaded", l.uploaded, "posted so far", Icons.Rounded.Check, c.success, Modifier.weight(1f).staggeredEntrance(1))
        Tile("Made", l.made, if (l.opening > 0) "+ ${Text.number(l.opening)} opening" else "from the charts", Icons.Rounded.Layers, Brand.BrightBlue, Modifier.weight(1f).staggeredEntrance(2))
        if (store.hasStock) {
            Tile(
                "Not listed",
                l.unlisted,
                if (l.unlisted > 0) "left but not in the list" else "all listed",
                Icons.Rounded.PlaylistAdd,
                c.warning,
                Modifier.weight(1f).staggeredEntrance(3),
            )
        }
    }
}

@Composable
private fun Tile(label: String, value: Int, sub: String, icon: ImageVector, color: Color, modifier: Modifier) {
    val c = AjmTheme.colors
    AjmCard(modifier, contentPadding = PaddingValues(14.dp)) {
        Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(c.soft(color)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = c.ink(color), modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.height(8.dp))
        EyebrowText(label)
        CountUpText(value, Figure.copy(fontSize = 28.sp, lineHeight = 32.sp), color = MaterialTheme.colorScheme.onSurface)
        Text(sub, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun LedgerCard(store: MediaStore, r: LedgerRow, modifier: Modifier = Modifier) {
    val c = AjmTheme.colors
    val color = typeColor(r.type)
    AjmCard(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(8.dp))
            Text(r.type, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (store.hasUploads) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(Text.number(r.left), style = MaterialTheme.typography.headlineSmall)
                    Text("left to upload", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (store.hasUploads) {
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                GrowBar(
                    r.percent / 100f,
                    Modifier
                        .weight(1f)
                        .height(8.dp),
                    brush = Brush.horizontalGradient(listOf(Brand.Blue, Color(0xFF27A06A))),
                    track = c.chip,
                )
                Spacer(Modifier.width(10.dp))
                Text(if (r.pipeline > 0) "${r.percent}%" else "–", style = MaterialTheme.typography.labelLarge)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (store.hasStock) Fact("Opening", r.opening, Modifier.weight(1f), editable = true) { store.stockEdit = r.type to "opening" }
            Fact("Made", r.made, Modifier.weight(1f))
            if (store.hasUploads) Fact("Uploaded", r.uploaded, Modifier.weight(1f))
            if (store.hasStock) Fact("Not needed", r.notNeeded, Modifier.weight(1f), editable = true) { store.stockEdit = r.type to "notNeeded" }
        }
        if (r.over || store.hasStock) {
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (r.over) TintBadge("⚠ more uploaded than made", c.danger)
                if (store.hasStock) {
                    when {
                        r.toList == 0 && r.left > 0 -> TintBadge("✓ all listed", c.success)
                        r.toList > 0 -> TintBadge("${r.toList} to add to the list", c.warning)
                        r.toList < 0 -> TintBadge("${-r.toList} extra in the list", Brand.BrightBlue)
                    }
                }
            }
        }
    }
}

@Composable
private fun Fact(label: String, value: Int, modifier: Modifier, editable: Boolean = false, onEdit: () -> Unit = {}) {
    val c = AjmTheme.colors
    Column(
        modifier
            .then(if (editable) Modifier.bouncyClick(shape = RoundedCornerShape(14.dp), onClick = onEdit) else Modifier)
            .background(if (editable) c.soft(MaterialTheme.colorScheme.primary) else c.subtle, RoundedCornerShape(14.dp))
            .padding(vertical = 8.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(Text.numberOrDash(value), style = MaterialTheme.typography.titleMedium)
            if (editable) {
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Rounded.Edit, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

@Composable
private fun EverythingCard(store: MediaStore) {
    val l = store.ledger
    AjmCard(Modifier.fillMaxWidth(), color = AjmTheme.colors.subtle) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Inventory2, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            Text("Everything", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (store.hasUploads) Text("${l.percent}% uploaded", style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (store.hasStock) Fact("Opening", l.opening, Modifier.weight(1f))
            Fact("Made", l.made, Modifier.weight(1f))
            if (store.hasUploads) Fact("Uploaded", l.uploaded, Modifier.weight(1f))
            if (store.hasStock) Fact("Not needed", l.notNeeded, Modifier.weight(1f))
            if (store.hasUploads) Fact("Left", l.left, Modifier.weight(1f))
        }
    }
}

@Composable
private fun MonthTableCard(store: MediaStore) {
    val t = store.monthTable
    val c = AjmTheme.colors
    if (t.rows.isEmpty()) {
        EmptyState("🗓️", "No months", "Nothing was filled in for this year.")
        return
    }
    val first = 118.dp
    val col = 78.dp
    AjmCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            Column {
                // header
                Row(Modifier.background(c.subtle)) {
                    HeadCell("Month", first)
                    t.types.forEach { HeadCell(it, col) }
                    HeadCell("Made", col)
                    if (store.hasUploads) HeadCell("Uploaded", col)
                }
                t.rows.forEachIndexed { i, r ->
                    Row(Modifier.background(if (i % 2 == 1) c.subtle.copy(alpha = 0.6f) else Color.Transparent)) {
                        LinkCell(r.label, first, bold = true) { store.openMonth(r.month, ChartView.MADE) }
                        r.perType.forEach { n -> LinkCell(Text.numberOrDash(n), col, enabled = n > 0) { store.openMonth(r.month, ChartView.MADE) } }
                        LinkCell(Text.number(r.made), col, bold = true, enabled = r.made > 0) { store.openMonth(r.month, ChartView.MADE) }
                        if (store.hasUploads) LinkCell(Text.numberOrDash(r.uploaded), col, enabled = r.uploaded > 0) { store.openMonth(r.month, ChartView.UPLOADED) }
                    }
                }
                Row(Modifier.background(c.soft(MaterialTheme.colorScheme.primary))) {
                    PlainCell(if (store.year == "all") "All months" else store.year, first, bold = true)
                    t.sums.forEach { PlainCell(Text.number(it), col, bold = true) }
                    PlainCell(Text.number(t.made), col, bold = true)
                    if (store.hasUploads) PlainCell(Text.number(t.uploaded), col, bold = true)
                }
            }
        }
    }
}

@Composable
private fun HeadCell(text: String, width: androidx.compose.ui.unit.Dp) {
    Box(Modifier.width(width).height(44.dp).padding(horizontal = 10.dp), contentAlignment = Alignment.CenterStart) {
        Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun LinkCell(text: String, width: androidx.compose.ui.unit.Dp, bold: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier
            .width(width)
            .height(46.dp)
            .then(if (enabled) Modifier.bouncyClick(onClick = onClick) else Modifier)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text,
            style = if (bold) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyMedium,
            color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Composable
private fun PlainCell(text: String, width: androidx.compose.ui.unit.Dp, bold: Boolean = false) {
    Box(Modifier.width(width).height(46.dp).padding(horizontal = 10.dp), contentAlignment = Alignment.CenterStart) {
        Text(text, style = if (bold) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyMedium, maxLines = 1)
    }
}

/** Changing Opening stock or Not needed for one work type. */
@Composable
fun StockEditDialog(store: MediaStore) {
    val (type, field) = store.stockEdit ?: return
    val currentValue = store.stock[type]?.let { if (field == "opening") it.opening else it.notNeeded } ?: 0
    var text by remember(type, field) { mutableStateOf(currentValue.toString()) }
    val valid = Regex("^\\d{1,5}$").matches(text.trim())
    AlertDialog(
        onDismissRequest = { store.stockEdit = null },
        title = { Text(if (field == "opening") "Opening stock" else "Not needed") },
        text = {
            Column {
                Text(
                    if (field == "opening") "How many $type pieces were already in stock before this log was used."
                    else "How many $type pieces will never be uploaded.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { v -> if (v.length <= 5 && v.all { it.isDigit() }) text = v },
                    singleLine = true,
                    isError = !valid,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { store.setStock(type, field, text.trim().toInt()) }, enabled = valid) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = { store.stockEdit = null }) { Text("Cancel") } },
    )
}
