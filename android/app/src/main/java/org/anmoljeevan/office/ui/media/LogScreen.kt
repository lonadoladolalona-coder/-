package org.anmoljeevan.office.ui.media

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarViewMonth
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.ViewDay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.anmoljeevan.office.core.Text
import org.anmoljeevan.office.core.media.Cells
import org.anmoljeevan.office.core.media.WeeklyPlan
import org.anmoljeevan.office.core.media.WorkTypes
import org.anmoljeevan.office.ui.components.AjmCard
import org.anmoljeevan.office.ui.components.EyebrowText
import org.anmoljeevan.office.ui.components.GradientButton
import org.anmoljeevan.office.ui.components.HeroCard
import org.anmoljeevan.office.ui.components.PillOption
import org.anmoljeevan.office.ui.components.PillSwitcher
import org.anmoljeevan.office.ui.components.PulsingDot
import org.anmoljeevan.office.ui.components.RollingNumber
import org.anmoljeevan.office.ui.components.SheetHeader
import org.anmoljeevan.office.ui.components.SlidingLabel
import org.anmoljeevan.office.ui.components.bouncyClick
import org.anmoljeevan.office.ui.components.staggeredEntrance
import org.anmoljeevan.office.ui.theme.AjmTheme
import org.anmoljeevan.office.ui.theme.Brand
import org.anmoljeevan.office.ui.theme.Fraunces
import java.time.LocalDate
import java.time.YearMonth

fun typeColor(type: String): Color = Color(WorkTypes.color(type))

/** Colour for a cell's value: ✓ green, P amber, R violet, O grey, numbers ink. */
@Composable
fun cellColor(v: String): Color {
    val c = AjmTheme.colors
    return when (v) {
        "c" -> c.success
        "p" -> c.warning
        "r" -> c.violet
        "o" -> Brand.Slate
        else -> MaterialTheme.colorScheme.onSurface
    }
}

@Composable
fun LogScreen(store: MediaStore) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item("howto") { HowToCard(store) }
        item("period") { PeriodCard(store) }
        item("mode") {
            PillSwitcher(
                options = listOf(PillOption("Day by day", Icons.Rounded.ViewDay), PillOption("Whole month", Icons.Rounded.CalendarViewMonth)),
                selected = store.mode.ordinal,
                onSelect = { store.mode = LogMode.entries[it] },
                height = 44.dp,
            )
        }
        item("body") {
            AnimatedContent(
                targetState = store.mode,
                transitionSpec = { fadeIn(tween(260)) togetherWith fadeOut(tween(160)) },
                label = "logMode",
            ) { mode ->
                if (mode == LogMode.DAY) DayView(store) else MonthGrid(store)
            }
        }
        item("legend") { Legend() }
        item("notes") { NotesCard(store) }
        item("foot") {
            Text(
                "Anmol Jeevan Ministries · one shared team log, no individual records",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun PeriodCard(store: MediaStore) {
    val c = AjmTheme.colors
    val up = store.view == ChartView.UPLOADED
    val stops = if (up) c.upload else c.hero
    val a by animateColorAsState(stops[0], tween(500), label = "g0")
    val b by animateColorAsState(stops[1], tween(500), label = "g1")
    val d by animateColorAsState(stops[2], tween(500), label = "g2")
    var picker by remember { mutableStateOf(false) }
    val thisMonth = YearMonth.from(store.today())

    HeroCard(Modifier.fillMaxWidth(), brush = Brush.linearGradient(listOf(a, b, d))) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RoundIcon(Icons.Rounded.ChevronLeft, "Previous month") { store.switchMonth(store.month.minusMonths(1)) }
            SlidingLabel(store.month, Modifier.weight(1f)) { m ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .bouncyClick(shape = RoundedCornerShape(16.dp)) { picker = true }
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(m.month.name.lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.headlineMedium, color = Color.White)
                    Text("${m.year}", style = MaterialTheme.typography.labelLarge, color = c.onHeroMuted)
                }
            }
            RoundIcon(Icons.Rounded.ChevronRight, "Next month") { store.switchMonth(store.month.plusMonths(1)) }
        }
        AnimatedVisibility(store.month != thisMonth) {
            Box(Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
                Row(
                    Modifier
                        .bouncyClick(shape = RoundedCornerShape(100)) { store.switchMonth(thisMonth) }
                        .background(Color.White.copy(alpha = 0.16f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Today, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Back to this month", color = Color.White, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        if (store.hasUploads) {
            PillSwitcher(
                options = listOf(PillOption("Made", count = store.madeTotal), PillOption("Uploaded", count = store.uploadedTotal)),
                selected = store.view.ordinal,
                onSelect = { store.view = ChartView.entries[it] },
                brushFor = { SolidColor(Color.White) },
                container = Color.White.copy(alpha = 0.14f),
                activeText = if (up) Color(0xFF166B45) else Brand.Navy,
                inactiveText = Color.White.copy(alpha = 0.85f),
            )
            Spacer(Modifier.height(10.dp))
            AnimatedContent(up, label = "viewHelp") { u ->
                Text(
                    if (u) "How many of each type were uploaded (posted) on each day." else "How many pieces of each type were made on each day.",
                    color = c.onHeroMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EyebrowText("Made this month", color = c.onHeroMuted)
                Spacer(Modifier.width(8.dp))
                RollingNumber(store.madeTotal, MaterialTheme.typography.titleMedium, color = Color.White)
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            SaveChip(store, Modifier.weight(1f, fill = false))
            Spacer(Modifier.weight(1f))
            if (store.updatedAt.isNotEmpty()) {
                Text(
                    "Updated " + Text.whenShort(store.updatedAt, store.today()),
                    color = c.onHeroMuted,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                )
            }
        }
    }

    if (picker) {
        MonthPicker(
            current = store.month,
            thisMonth = thisMonth,
            onPick = {
                picker = false
                store.switchMonth(it)
            },
            onDismiss = { picker = false },
        )
    }
}

@Composable
private fun RoundIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White.copy(alpha = 0.14f), contentColor = Color.White),
    ) { Icon(icon, label) }
}

@Composable
private fun SaveChip(store: MediaStore, modifier: Modifier = Modifier) {
    val s = store.save
    val dot = when (s.kind) {
        SaveKind.OK -> Color(0xFF4ADE9A)
        SaveKind.BUSY -> Brand.Gold
        SaveKind.ERROR -> Color(0xFFFF8A80)
        SaveKind.NEUTRAL -> Color.White.copy(alpha = 0.6f)
    }
    Row(
        modifier
            .bouncyClick(enabled = s.kind == SaveKind.ERROR, shape = RoundedCornerShape(100), onClick = store::retrySave)
            .background(Color.White.copy(alpha = if (s.kind == SaveKind.ERROR) 0.2f else 0.12f))
            .padding(start = 4.dp, end = 12.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PulsingDot(dot, pulsing = s.kind == SaveKind.BUSY)
        AnimatedContent(
            targetState = s.text,
            transitionSpec = { (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut()) },
            label = "saveText",
        ) { t ->
            Text(t, color = Color.White, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

// ---------------------------------------------------------------- day by day

@Composable
private fun DayView(store: MediaStore) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        DayStrip(store)
        val date = store.month.atDay(store.selectedDay.coerceIn(1, store.days))
        Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                WeeklyPlan.FULL_DAYS[WeeklyPlan.dow(date)] + ", " + Text.dayMonth(date),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            if (date == store.today()) {
                Text(
                    "TODAY",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF3B2A00),
                    modifier = Modifier
                        .clip(RoundedCornerShape(100))
                        .background(Brand.Gold)
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                )
            }
        }
        store.types.forEachIndexed { i, type ->
            TypeDayCard(store, type, store.selectedDay, Modifier.staggeredEntrance(i))
        }
    }
}

@Composable
private fun DayStrip(store: MediaStore) {
    val listState = rememberLazyListState()
    val today = store.todayDay
    LaunchedEffect(store.month) {
        listState.scrollToItem((store.selectedDay - 3).coerceAtLeast(0))
    }
    LazyRow(state = listState, horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 2.dp)) {
        items(store.days) { i ->
            val d = i + 1
            DayChip(
                date = store.month.atDay(d),
                selected = d == store.selectedDay,
                isToday = d == today,
                hasEntry = store.current.hasEntry(d),
                onClick = { store.selectedDay = d },
            )
        }
    }
}

@Composable
private fun DayChip(date: LocalDate, selected: Boolean, isToday: Boolean, hasEntry: Boolean, onClick: () -> Unit) {
    val c = AjmTheme.colors
    val haptic = LocalHapticFeedback.current
    val sunday = WeeklyPlan.dow(date) == 0
    val scale by animateFloatAsState(if (selected) 1.06f else 1f, spring(dampingRatio = 0.55f, stiffness = 400f), label = "dayScale")
    val fg by animateColorAsState(if (selected) Color.White else MaterialTheme.colorScheme.onSurface, label = "dayFg")
    val shape = RoundedCornerShape(18.dp)
    Column(
        Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .width(50.dp)
            .bouncyClick(shape = shape) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .then(
                when {
                    selected -> Modifier.background(c.heroBrush)
                    sunday -> Modifier.background(c.soft(Brand.BrightBlue))
                    else -> Modifier.background(c.card)
                },
            )
            .border(if (isToday) 2.dp else 1.dp, if (isToday) Brand.Gold else c.cardBorder, shape)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(WeeklyPlan.DAY_LETTERS[WeeklyPlan.dow(date)], style = MaterialTheme.typography.labelSmall, color = fg.copy(alpha = 0.7f))
        Text("${date.dayOfMonth}", style = MaterialTheme.typography.titleLarge.copy(fontFamily = Fraunces), color = fg)
        Box(
            Modifier
                .padding(top = 3.dp)
                .size(5.dp)
                .clip(CircleShape)
                .background(if (hasEntry) (if (selected) Color.White else Brand.BrightBlue) else Color.Transparent),
        )
    }
}

@Composable
private fun TypeDayCard(store: MediaStore, type: String, day: Int, modifier: Modifier = Modifier) {
    val v = store.cell(type, day)
    val color = typeColor(type)
    AjmCard(modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(8.dp))
            Text(type, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("Month ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            RollingNumber(store.current.rowTotal(type, store.days), MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.height(8.dp))
        CellControls(v, onChange = { store.edit(type, day, it) }, enabled = store.ready)
    }
}

/** − value + and the four status marks — shared by the day cards and the cell editor. */
@Composable
private fun CellControls(v: String, onChange: (String) -> Unit, enabled: Boolean, big: Boolean = false) {
    val haptic = LocalHapticFeedback.current
    val c = AjmTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        StepButton(Icons.Rounded.Remove, "Less", enabled && v.isNotEmpty(), big) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onChange(Cells.decrement(v))
        }
        AnimatedContent(
            targetState = v,
            transitionSpec = {
                val up = Cells.value(targetState) >= Cells.value(initialState)
                (slideInVertically(spring(dampingRatio = 0.7f, stiffness = 500f)) { h -> if (up) h else -h } + fadeIn()) togetherWith
                    (slideOutVertically(spring(dampingRatio = 0.7f, stiffness = 500f)) { h -> if (up) -h else h } + fadeOut())
            },
            modifier = Modifier.width(if (big) 96.dp else 56.dp),
            contentAlignment = Alignment.Center,
            label = "cellValue",
        ) { value ->
            Text(
                if (value.isEmpty()) "–" else Cells.display(value),
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = if (big) 44.sp else 28.sp),
                color = if (value.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else cellColor(value),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        StepButton(Icons.Rounded.Add, "More", enabled, big) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onChange(Cells.increment(v))
        }
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(if (big) 8.dp else 4.dp)) {
            Cells.STATUS_LETTERS.forEach { s ->
                val on = v == s
                val col = cellColor(s)
                val bg by animateColorAsState(if (on) col else c.soft(col), label = "mark")
                Box(
                    Modifier
                        .size(if (big) 44.dp else 32.dp)
                        .bouncyClick(enabled = enabled, shape = CircleShape) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onChange(if (on) "" else s)
                        }
                        .background(bg),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(Cells.display(s), color = if (on) Color.White else c.ink(col), fontWeight = FontWeight.ExtraBold, fontSize = if (big) 17.sp else 13.sp)
                }
            }
        }
    }
}

@Composable
private fun StepButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, enabled: Boolean, big: Boolean, onClick: () -> Unit) {
    val c = AjmTheme.colors
    Box(
        Modifier
            .size(if (big) 56.dp else 40.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.4f }
            .bouncyClick(enabled = enabled, shape = CircleShape, onClick = onClick)
            .background(c.chip),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, label, tint = MaterialTheme.colorScheme.onSurface) }
}

// ---------------------------------------------------------------- the whole month

private val LabelWidth = 112.dp
private val CellWidth = 42.dp
private val CellHeight = 40.dp

@Composable
private fun MonthGrid(store: MediaStore) {
    val c = AjmTheme.colors
    val scroll = rememberScrollState()
    val density = LocalDensity.current
    val up = store.view == ChartView.UPLOADED
    val headBg = if (up) Color(0xFF0F4D36) else Brand.Navy
    val types = store.types
    LaunchedEffect(store.month) {
        val start = (store.todayDay - 3).coerceAtLeast(0)
        scroll.scrollTo(with(density) { (CellWidth * start).roundToPx() })
    }
    AjmCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
        Row {
            Column(Modifier.width(LabelWidth)) {
                HeaderCell(Modifier.width(LabelWidth), headBg) { Text("Type", color = Color.White, style = MaterialTheme.typography.labelMedium) }
                types.forEachIndexed { i, t ->
                    Row(
                        Modifier
                            .width(LabelWidth)
                            .height(CellHeight)
                            .background(if (i % 2 == 0) c.subtle else c.card)
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(typeColor(t)))
                        Spacer(Modifier.width(6.dp))
                        Text(t, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            Row(Modifier.horizontalScroll(scroll)) {
                for (d in 1..store.days) {
                    val date = store.month.atDay(d)
                    val isToday = d == store.todayDay
                    val sunday = WeeklyPlan.dow(date) == 0
                    Column(Modifier.width(CellWidth)) {
                        HeaderCell(Modifier.width(CellWidth), if (isToday) Brand.Gold else if (sunday) headBg.copy(alpha = 0.85f) else headBg) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$d", color = if (isToday) Color(0xFF3B2A00) else Color.White, style = MaterialTheme.typography.labelMedium)
                                Text(WeeklyPlan.DAY_LETTERS[WeeklyPlan.dow(date)], color = (if (isToday) Color(0xFF3B2A00) else Color.White).copy(alpha = 0.7f), fontSize = 9.sp)
                            }
                        }
                        types.forEachIndexed { i, t ->
                            GridCell(
                                v = store.cell(t, d),
                                striped = i % 2 == 0,
                                sunday = sunday,
                                today = isToday,
                                onClick = { store.editing = CellRef(t, d) },
                            )
                        }
                    }
                }
                Column(Modifier.width(52.dp)) {
                    HeaderCell(Modifier.width(52.dp), if (up) c.success else Brand.BrightBlue) {
                        Text("Total", color = Color.White, style = MaterialTheme.typography.labelMedium)
                    }
                    types.forEach { t ->
                        Box(
                            Modifier
                                .width(52.dp)
                                .height(CellHeight)
                                .background(c.soft(if (up) c.success else Brand.BrightBlue)),
                            contentAlignment = Alignment.Center,
                        ) {
                            RollingNumber(store.current.rowTotal(t, store.days), MaterialTheme.typography.labelLarge, color = c.ink(if (up) c.success else Brand.BrightBlue))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderCell(modifier: Modifier, color: Color, content: @Composable () -> Unit) {
    Box(modifier.height(44.dp).background(color), contentAlignment = Alignment.Center) { content() }
}

@Composable
private fun GridCell(v: String, striped: Boolean, sunday: Boolean, today: Boolean, onClick: () -> Unit) {
    val c = AjmTheme.colors
    val base = when {
        today -> Brand.Gold.copy(alpha = 0.18f)
        sunday -> c.soft(Brand.BrightBlue).copy(alpha = 0.08f)
        striped -> c.subtle
        else -> c.card
    }
    val target = when (v) {
        "c", "p", "r", "o" -> c.soft(cellColor(v))
        else -> base
    }
    val bg by animateColorAsState(target, tween(250), label = "cell")
    Box(
        Modifier
            .width(CellWidth)
            .height(CellHeight)
            .background(bg)
            .border(0.5.dp, c.cardBorder)
            .bouncyClick(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(v, transitionSpec = { (scaleIn(initialScale = 0.6f) + fadeIn()) togetherWith (scaleOut(targetScale = 0.6f) + fadeOut()) }, label = "gridCell") { value ->
            Text(
                Cells.display(value),
                color = if (value == "c" || value == "p" || value == "r" || value == "o") c.ink(cellColor(value)) else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = if (value == "c") 16.sp else 13.sp,
            )
        }
    }
}

/** Editing one cell of the month grid, with ‹ › to walk through the days. */
@Composable
fun CellEditorSheet(store: MediaStore) {
    val ref = store.editing ?: return
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val date = store.month.atDay(ref.day.coerceIn(1, store.days))
    ModalBottomSheet(onDismissRequest = { store.editing = null }, sheetState = state, containerColor = MaterialTheme.colorScheme.surfaceContainerLowest) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 16.dp)) {
            SheetHeader(
                ref.type,
                (if (store.view == ChartView.UPLOADED) "Uploaded · " else "Made · ") + WeeklyPlan.FULL_DAYS[WeeklyPlan.dow(date)] + " " + Text.dayMonth(date),
            )
            Column(Modifier.padding(horizontal = 22.dp)) {
                Spacer(Modifier.height(10.dp))
                CellControls(store.cell(ref.type, ref.day), onChange = { store.edit(ref.type, ref.day, it) }, enabled = store.ready, big = true)
                Spacer(Modifier.height(12.dp))
                Text(Cells.label(store.cell(ref.type, ref.day)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { store.editing = ref.copy(day = ref.day - 1) }, enabled = ref.day > 1) {
                        Icon(Icons.Rounded.ChevronLeft, null)
                        Text("Previous day")
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { store.editing = ref.copy(day = ref.day + 1) }, enabled = ref.day < store.days) {
                        Text("Next day")
                        Icon(Icons.Rounded.ChevronRight, null)
                    }
                }
                Spacer(Modifier.height(8.dp))
                GradientButton(
                    "Done",
                    onClick = { scope.launch { state.hide() }.invokeOnCompletion { store.editing = null } },
                    modifier = Modifier.fillMaxWidth(),
                    height = 50.dp,
                )
            }
        }
    }
}

@Composable
private fun Legend() {
    AjmCard(Modifier.fillMaxWidth(), color = AjmTheme.colors.subtle, elevation = 0.dp) {
        EyebrowText("How to fill in")
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            LegendItem("1, 2…", "how many pieces", MaterialTheme.colorScheme.onSurface)
            LegendItem("✓", "one finished piece", AjmTheme.colors.success)
            LegendItem("P", "in progress", AjmTheme.colors.warning)
            LegendItem("R", "in review", AjmTheme.colors.violet)
            LegendItem("O", "nothing that day", Brand.Slate)
        }
        Spacer(Modifier.height(8.dp))
        Text("Total = the numbers plus 1 for every ✓.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun LegendItem(mark: String, text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(mark, color = color, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.width(5.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun NotesCard(store: MediaStore) {
    AjmCard(Modifier.fillMaxWidth()) {
        EyebrowText("Notes (optional)")
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = store.note,
            onValueChange = store::editNote,
            enabled = store.ready,
            minLines = 3,
            placeholder = { Text("e.g. what “Other Work” covered this month") },
            supportingText = { Text("${store.note.length}/300") },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Month and year, as a grid of months with ‹ year › arrows. */
@Composable
private fun MonthPicker(current: YearMonth, thisMonth: YearMonth, onPick: (YearMonth) -> Unit, onDismiss: () -> Unit) {
    var year by remember { mutableIntStateOf(current.year) }
    val c = AjmTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { year-- }, enabled = year > thisMonth.year - 5) { Icon(Icons.Rounded.ChevronLeft, "Previous year") }
                SlidingLabel(year, Modifier.weight(1f)) { y -> Text("$y", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
                IconButton(onClick = { year++ }, enabled = year < thisMonth.year + 1) { Icon(Icons.Rounded.ChevronRight, "Next year") }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..12).chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { m ->
                            val ym = YearMonth.of(year, m)
                            val selected = ym == current
                            val isNow = ym == thisMonth
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .bouncyClick(shape = RoundedCornerShape(14.dp)) { onPick(ym) }
                                    .then(if (selected) Modifier.background(c.heroBrush) else Modifier.background(c.chip))
                                    .then(if (isNow && !selected) Modifier.border(2.dp, Brand.Gold, RoundedCornerShape(14.dp)) else Modifier),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    ym.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() },
                                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

