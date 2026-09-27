package org.anmoljeevan.office.ui.admin

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.anmoljeevan.office.core.admin.Registration
import org.anmoljeevan.office.core.admin.Sources
import org.anmoljeevan.office.core.admin.Statuses
import org.anmoljeevan.office.ui.components.Dot
import org.anmoljeevan.office.ui.components.bouncyClick
import org.anmoljeevan.office.ui.theme.AjmTheme
import org.anmoljeevan.office.ui.theme.Brand

data class SourceStyle(val color: Color, val icon: ImageVector)

fun sourceStyle(source: String): SourceStyle = when (source) {
    Sources.WOMENS -> SourceStyle(Brand.Pink, Icons.Rounded.Favorite)
    Sources.EVENT -> SourceStyle(Brand.BrightBlue, Icons.Rounded.Event)
    Sources.INVITE -> SourceStyle(Brand.Amber, Icons.Rounded.Mic)
    else -> SourceStyle(Brand.Slate, Icons.Rounded.Person)
}

@Composable
fun statusColor(status: String): Color = when (status) {
    Statuses.CONFIRMED -> AjmTheme.colors.success
    Statuses.CONTACTED -> Brand.BrightBlue
    else -> Brand.Slate
}

/** The coloured status pill; tap it to choose another status. */
@Composable
fun StatusPill(status: String, onChange: (String) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val c = AjmTheme.colors
    val haptic = LocalHapticFeedback.current
    var open by remember { mutableStateOf(false) }
    val color = statusColor(status)
    val bg by animateColorAsState(c.soft(color), tween(300), label = "statusBg")
    val fg by animateColorAsState(c.ink(color), tween(300), label = "statusFg")
    Box(modifier) {
        Row(
            Modifier
                .bouncyClick(enabled = enabled, shape = RoundedCornerShape(100)) { open = true }
                .background(bg)
                .padding(start = 10.dp, end = if (enabled) 6.dp else 10.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Dot(fg, size = 7.dp)
            Spacer(Modifier.width(6.dp))
            AnimatedContent(status, transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(120)) }, label = "statusText") { s ->
                Text(s, color = fg, style = MaterialTheme.typography.labelMedium, maxLines = 1)
            }
            if (enabled) Icon(Icons.Rounded.ExpandMore, null, tint = fg, modifier = Modifier.size(16.dp))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            Statuses.ALL.forEach { s ->
                DropdownMenuItem(
                    text = { Text(s) },
                    leadingIcon = { Dot(statusColor(s), size = 10.dp) },
                    trailingIcon = { if (s == status) Icon(Icons.Rounded.Check, null) },
                    onClick = {
                        open = false
                        if (s != status) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onChange(s)
                        }
                    },
                )
            }
        }
    }
}

/** A tappable phone / email chip. */
@Composable
fun ContactChip(icon: ImageVector, text: String, color: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = AjmTheme.colors
    Row(
        modifier
            .bouncyClick(shape = RoundedCornerShape(100), onClick = onClick)
            .background(c.soft(color))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = c.ink(color), modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, color = c.ink(color), style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 220.dp))
    }
}

/** "Asha, Ravi, … and 12 more" as a short bullet list, for confirm dialogs. */
@Composable
fun NamesPreview(rows: List<Registration>) {
    val c = AjmTheme.colors
    Column(
        Modifier
            .padding(top = 12.dp)
            .background(c.subtle, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        rows.take(6).forEach { r ->
            Text("• " + r.name.ifBlank { "(no name)" }, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (rows.size > 6) {
            Text("…and ${rows.size - 6} more", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
