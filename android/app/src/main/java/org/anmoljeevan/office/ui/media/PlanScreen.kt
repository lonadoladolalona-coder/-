package org.anmoljeevan.office.ui.media

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.OndemandVideo
import androidx.compose.material.icons.rounded.Slideshow
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.anmoljeevan.office.core.Text
import org.anmoljeevan.office.core.media.PlanChip
import org.anmoljeevan.office.core.media.PlanRow
import org.anmoljeevan.office.core.media.WeeklyPlan
import org.anmoljeevan.office.ui.components.AjmCard
import org.anmoljeevan.office.ui.components.CountUpText
import org.anmoljeevan.office.ui.components.EyebrowText
import org.anmoljeevan.office.ui.components.HeroCard
import org.anmoljeevan.office.ui.components.PulsingDot
import org.anmoljeevan.office.ui.components.SectionTitle
import org.anmoljeevan.office.ui.components.SlidingLabel
import org.anmoljeevan.office.ui.components.Watermark
import org.anmoljeevan.office.ui.components.staggeredEntrance
import org.anmoljeevan.office.ui.theme.AjmTheme
import org.anmoljeevan.office.ui.theme.Brand
import org.anmoljeevan.office.ui.theme.Fraunces
import java.time.LocalDate

fun planIcon(key: String): ImageVector = when (key) {
    "reel" -> Icons.Rounded.Slideshow
    "live" -> Icons.Rounded.LiveTv
    "long" -> Icons.Rounded.OndemandVideo
    "story" -> Icons.Rounded.MenuBook
    "promo" -> Icons.Rounded.Campaign
    "promovid" -> Icons.Rounded.Image
    "song" -> Icons.Rounded.MusicNote
    "star" -> Icons.Rounded.Star
    else -> Icons.Rounded.Groups
}

/** What the media team makes on which day. The same plan repeats every week. */
@Composable
fun PlanScreen(store: MediaStore) {
    val today = store.planDay
    var offset by rememberSaveable { mutableIntStateOf(0) }
    val weekStart = WeeklyPlan.weekStart(today).plusWeeks(offset.toLong())
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item("now") { NowCard(today, Modifier.staggeredEntrance(0)) }
        if (WeeklyPlan.daysUntil(today) >= 0) item("event") { EventCard(today, Modifier.staggeredEntrance(1)) }
        item("weekHead") {
            SectionTitle(
                "The week",
                subtitle = "Sun ${Text.dayMonth(weekStart)} – Sat ${Text.dayMonth(weekStart.plusDays(6))} ${weekStart.plusDays(6).year}" + if (offset == 0) " · this week" else "",
                modifier = Modifier.padding(top = 6.dp),
            ) {
                IconButton(onClick = { offset-- }, enabled = offset > WeeklyPlan.OFFSETS.first) { Icon(Icons.Rounded.ChevronLeft, "Previous week") }
                if (offset != 0) TextButton(onClick = { offset = 0 }) { Text("This week") }
                IconButton(onClick = { offset++ }, enabled = offset < WeeklyPlan.OFFSETS.last) { Icon(Icons.Rounded.ChevronRight, "Next week") }
            }
        }
        item("week") {
            SlidingLabel(offset) { o ->
                val start = WeeklyPlan.weekStart(today).plusWeeks(o.toLong())
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    WeeklyPlan.ribbons().forEach { Ribbon(it.chip) }
                    (0..6).forEach { i -> DayCard(start.plusDays(i.toLong()), isToday = start.plusDays(i.toLong()) == today) }
                }
            }
        }
        item("planHead") {
            SectionTitle("The plan, type by type", subtitle = "Coloured boxes are the days it is made", modifier = Modifier.padding(top = 6.dp))
        }
        items(WeeklyPlan.ROWS, key = { "row" + it.n }) { r -> PlanRowCard(r, WeeklyPlan.dow(today)) }
        item("people") { PeopleCard() }
    }
}

@Composable
private fun NowCard(today: LocalDate, modifier: Modifier = Modifier) {
    val c = AjmTheme.colors
    HeroCard(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PulsingDot(Brand.Gold)
            EyebrowText("Today", color = c.onHeroMuted)
        }
        Text(WeeklyPlan.FULL_DAYS[WeeklyPlan.dow(today)], style = MaterialTheme.typography.displaySmall, color = Color.White)
        Text(Text.dayMonth(today) + " " + today.year, style = MaterialTheme.typography.bodyMedium, color = c.onHeroMuted)
        Spacer(Modifier.height(14.dp))
        DayPills(WeeklyPlan.dayItems(today))
        Spacer(Modifier.height(18.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.16f)))
        Spacer(Modifier.height(14.dp))
        val tomorrow = today.plusDays(1)
        EyebrowText("Tomorrow", color = c.onHeroMuted)
        Text(WeeklyPlan.FULL_DAYS[WeeklyPlan.dow(tomorrow)], style = MaterialTheme.typography.headlineSmall, color = Color.White)
        Spacer(Modifier.height(10.dp))
        DayPills(WeeklyPlan.dayItems(tomorrow))
    }
}

@Composable
private fun DayPills(items: List<PlanChip>) {
    val c = AjmTheme.colors
    if (items.isEmpty()) {
        Text("Nothing planned", style = MaterialTheme.typography.bodyMedium, color = c.onHeroMuted)
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { chip ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (chip.isEvent) Brand.Gold.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.11f))
                    .border(1.dp, if (chip.isEvent) Brand.Gold.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.14f), RoundedCornerShape(16.dp))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(chip.color)),
                    contentAlignment = Alignment.Center,
                ) { Icon(planIcon(chip.icon), null, tint = Color.White, modifier = Modifier.size(18.dp)) }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(chip.title, color = Color.White, style = MaterialTheme.typography.titleSmall)
                    Text(chip.sub, color = c.onHeroMuted, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun EventCard(today: LocalDate, modifier: Modifier = Modifier) {
    val e = WeeklyPlan.EVENT
    val left = WeeklyPlan.daysUntil(today).toInt()
    val weeks = WeeklyPlan.weeksToEvent(today)
    HeroCard(modifier.fillMaxWidth(), brush = AjmTheme.colors.eventBrush, glow = Brand.Gold) {
        Box(Modifier.fillMaxWidth()) {
            Watermark("Z", Modifier.align(Alignment.TopEnd).offset(x = 10.dp, y = (-70).dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(
                        Modifier
                            .width(66.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.White)
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(e.date.month.name.take(3), color = Color(0xFF5B4BF0), style = MaterialTheme.typography.labelSmall)
                        Text("${e.date.dayOfMonth}", color = Brand.Navy, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
                        Text(WeeklyPlan.SHORT_DAYS[WeeklyPlan.dow(e.date)].uppercase(), color = Color(0xFF5B4BF0), style = MaterialTheme.typography.labelSmall)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        EyebrowText("Coming up", color = Color(0xFFC9CDF5))
                        Text(e.name, style = MaterialTheme.typography.headlineSmall, color = Color.White)
                        Text("Theme · ${e.theme}", style = MaterialTheme.typography.bodySmall, color = Color(0xFFD3D6FF))
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    GoldText("“${e.quote}”", Modifier.weight(1f))
                    Column(
                        Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.12f))
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        when (left) {
                            0 -> Text("Today", style = MaterialTheme.typography.headlineSmall, color = Brand.Gold)
                            1 -> Text("Tomorrow", style = MaterialTheme.typography.headlineSmall, color = Brand.Gold)
                            else -> CountUpText(left, MaterialTheme.typography.headlineLarge.copy(fontFamily = Fraunces), color = Brand.Gold)
                        }
                        Text(if (left <= 1) "is the day" else "days to go", style = MaterialTheme.typography.labelSmall, color = Color(0xFFD3D6FF))
                    }
                }
                if (weeks.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        weeks.forEachIndexed { i, w ->
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                val fill = remember { Animatable(0f) }
                                LaunchedEffect(Unit) { fill.animateTo(1f, tween(500, delayMillis = 200 + i * 120, easing = FastOutSlowInEasing)) }
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(100))
                                        .background(Color.White.copy(alpha = 0.2f)),
                                ) {
                                    Box(
                                        Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(if (w.isNow || w.isEvent) fill.value else 0f)
                                            .background(if (w.isEvent) Color.White else Brand.Gold),
                                    )
                                }
                                Spacer(Modifier.height(5.dp))
                                Text(
                                    w.label,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (w.isEvent) Color.White else Color(0xFFC9CDF5),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Italic Fraunces with a gold gradient, like the web page's quote. */
@Composable
private fun GoldText(text: String, modifier: Modifier = Modifier) {
    val brush = Brush.linearGradient(listOf(Color(0xFFFFE2A6), Brand.Gold))
    Text(
        text,
        modifier = modifier
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithCache {
                onDrawWithContent {
                    drawContent()
                    drawRect(brush, blendMode = BlendMode.SrcAtop)
                }
            },
        style = MaterialTheme.typography.headlineSmall.copy(fontFamily = Fraunces, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Medium),
        color = Color.White,
    )
}

@Composable
private fun Ribbon(chip: PlanChip) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp, topEnd = 40.dp, bottomEnd = 40.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFFF7CD7A), Brand.Gold)))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center,
        ) { Icon(planIcon(chip.icon), null, tint = Color(0xFF3B2A00), modifier = Modifier.size(17.dp)) }
        Spacer(Modifier.width(10.dp))
        Text(chip.title, style = MaterialTheme.typography.titleSmall, color = Color(0xFF3B2A00))
        Spacer(Modifier.width(10.dp))
        Text(chip.sub, style = MaterialTheme.typography.bodySmall, color = Color(0xFF3B2A00).copy(alpha = 0.8f))
    }
}

@Composable
private fun DayCard(date: LocalDate, isToday: Boolean) {
    val c = AjmTheme.colors
    val items = WeeklyPlan.chips(date)
    val sunday = WeeklyPlan.dow(date) == 0
    AjmCard(
        Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(0.dp),
        border = if (isToday) androidx.compose.foundation.BorderStroke(2.dp, Brand.Gold) else androidx.compose.foundation.BorderStroke(1.dp, c.cardBorder),
        elevation = if (isToday) 10.dp else 4.dp,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(if (isToday) Brand.Gold else if (sunday) Color(0xFF284A7C) else Brand.Navy)
                .padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val fg = if (isToday) Color(0xFF3B2A00) else Color.White
            Text(WeeklyPlan.SHORT_DAYS[WeeklyPlan.dow(date)].uppercase(), style = MaterialTheme.typography.labelLarge, color = fg)
            Spacer(Modifier.width(8.dp))
            Text(Text.dayMonth(date), style = MaterialTheme.typography.labelMedium, color = fg.copy(alpha = 0.75f), modifier = Modifier.weight(1f))
            if (isToday) {
                Text(
                    "TODAY",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFFFE2A6),
                    modifier = Modifier
                        .clip(RoundedCornerShape(100))
                        .background(Color(0xFF3B2A00))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
        Column(
            Modifier
                .fillMaxWidth()
                .background(if (isToday) Color(0x14E8A33D) else Color.Transparent)
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (items.isEmpty()) {
                Text("Nothing else planned", style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items.forEach { chip ->
                val col = Color(chip.color)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (chip.isEvent) Brand.Gold.copy(alpha = 0.22f) else c.soft(col))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (chip.isEvent) Brand.Gold else col),
                        contentAlignment = Alignment.Center,
                    ) { Icon(planIcon(chip.icon), null, tint = Color.White, modifier = Modifier.size(16.dp)) }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(chip.title, style = MaterialTheme.typography.titleSmall)
                        Text(chip.sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanRowCard(r: PlanRow, todayDow: Int) {
    val c = AjmTheme.colors
    val col = Color(r.color)
    AjmCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
        Box(Modifier.fillMaxWidth().height(5.dp).background(col))
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(28.dp).clip(CircleShape).background(c.chip), contentAlignment = Alignment.Center) {
                    Text("${r.n}", style = MaterialTheme.typography.labelMedium)
                }
                Spacer(Modifier.width(10.dp))
                Box(Modifier.size(30.dp).clip(RoundedCornerShape(10.dp)).background(c.soft(col)), contentAlignment = Alignment.Center) {
                    Icon(planIcon(r.icon), null, tint = c.ink(col), modifier = Modifier.size(17.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text(r.type, style = MaterialTheme.typography.titleMedium)
            }
            r.parts.forEach { p ->
                Spacer(Modifier.height(12.dp))
                Text(p.qty, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    WeeklyPlan.DAY_LETTERS.forEachIndexed { i, letter ->
                        val on = i in p.allDays
                        Box(
                            Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (on) col else c.chip)
                                .then(if (i == todayDow) Modifier.border(2.dp, Brand.Gold, RoundedCornerShape(8.dp)) else Modifier),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(letter, color = if (on) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PeopleCard() {
    AjmCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(28.dp).clip(CircleShape).background(AjmTheme.colors.chip), contentAlignment = Alignment.Center) {
                Text("${WeeklyPlan.ROWS.size + 1}", style = MaterialTheme.typography.labelMedium)
            }
            Spacer(Modifier.width(10.dp))
            Text("Content Selection", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            WeeklyPlan.PEOPLE.forEach { n ->
                Row(
                    Modifier
                        .clip(RoundedCornerShape(100))
                        .background(AjmTheme.colors.chip)
                        .padding(start = 4.dp, end = 14.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(AjmTheme.colors.heroBrush),
                        contentAlignment = Alignment.Center,
                    ) { Text(n.take(1).uppercase(), color = Color.White, style = MaterialTheme.typography.labelLarge) }
                    Spacer(Modifier.width(9.dp))
                    Text(n, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

