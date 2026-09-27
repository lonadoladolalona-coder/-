package org.anmoljeevan.office.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.anmoljeevan.office.ui.theme.AjmTheme
import org.anmoljeevan.office.ui.theme.Eyebrow

/** The white, softly shadowed card every screen is built from. */
@Composable
fun AjmCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(24.dp),
    color: Color = AjmTheme.colors.card,
    border: BorderStroke? = BorderStroke(1.dp, AjmTheme.colors.cardBorder),
    elevation: Dp = 6.dp,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val shadow = AjmTheme.colors.shadow.copy(alpha = if (AjmTheme.colors.isDark) 0.6f else 0.35f)
    val layer = if (onClick != null) {
        modifier.bouncyClick(shape = shape, elevation = elevation, shadowColor = shadow, onLongClick = onLongClick, onClick = onClick)
    } else {
        modifier.softShadow(shape, elevation, shadow)
    }
    Column(
        layer
            .background(color)
            .then(if (border != null) Modifier.border(border, shape) else Modifier)
            .padding(contentPadding),
        content = content,
    )
}

/** A gradient card (navy by default) with a soft gold glow in the corner, like the web hero tiles. */
@Composable
fun HeroCard(
    modifier: Modifier = Modifier,
    brush: Brush = AjmTheme.colors.heroBrush,
    glow: Color = AjmTheme.colors.gold,
    shape: Shape = RoundedCornerShape(28.dp),
    contentPadding: PaddingValues = PaddingValues(22.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shadow = AjmTheme.colors.shadow.copy(alpha = 0.55f)
    val layer = if (onClick != null) modifier.bouncyClick(shape = shape, elevation = 14.dp, shadowColor = shadow, onClick = onClick)
    else modifier.softShadow(shape, 14.dp, shadow)
    Column(
        layer
            .background(brush)
            .drawBehind {
                drawCircle(
                    Brush.radialGradient(
                        listOf(glow.copy(alpha = 0.38f), Color.Transparent),
                        center = Offset(size.width - 40.dp.toPx(), -20.dp.toPx()),
                        radius = 170.dp.toPx(),
                    ),
                    radius = 170.dp.toPx(),
                    center = Offset(size.width - 40.dp.toPx(), -20.dp.toPx()),
                )
            }
            .padding(contentPadding),
        content = content,
    )
}

@Composable
fun EyebrowText(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(text.uppercase(), modifier = modifier, style = Eyebrow, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Composable
fun SectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        trailing()
    }
}

/** Round initials, tinted by [color]. */
@Composable
fun Avatar(text: String, color: Color, modifier: Modifier = Modifier, size: Dp = 44.dp) {
    val c = AjmTheme.colors
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(c.soft(color)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = c.ink(color),
            fontWeight = FontWeight.ExtraBold,
            fontSize = (size.value * 0.34f).sp,
            maxLines = 1,
        )
    }
}

/** A small tinted pill: source, status, "sent before"… */
@Composable
fun TintBadge(text: String, color: Color, modifier: Modifier = Modifier, icon: ImageVector? = null, strong: Boolean = false) {
    val c = AjmTheme.colors
    Row(
        modifier
            .clip(RoundedCornerShape(100))
            .background(if (strong) color else c.soft(color))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = if (strong) Color.White else c.ink(color), modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(
            text,
            color = if (strong) Color.White else c.ink(color),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** The main call to action: a navy gradient pill that bounces when pressed and morphs into a spinner. */
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    brush: Brush = AjmTheme.colors.heroBrush,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null,
    height: Dp = 54.dp,
) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier
            .heightIn(min = height)
            .alpha(if (enabled) 1f else 0.5f)
            .bouncyClick(
                enabled = enabled && !loading,
                shape = shape,
                elevation = if (enabled) 10.dp else 0.dp,
                shadowColor = AjmTheme.colors.shadow.copy(alpha = 0.6f),
                onClick = onClick,
            )
            .background(brush)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = loading,
            transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.7f)) togetherWith (fadeOut() + scaleOut(targetScale = 0.7f)) },
            label = "buttonLoading",
        ) { busy ->
            if (busy) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.5.dp, modifier = Modifier.size(22.dp))
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (icon != null) {
                        Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                    }
                    Text(text, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        }
    }
}

/** A quieter pill button: tinted background, coloured label. */
@Composable
fun SoftButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    compact: Boolean = false,
) {
    val c = AjmTheme.colors
    val shape = RoundedCornerShape(100)
    Row(
        modifier
            .alpha(if (enabled) 1f else 0.45f)
            .bouncyClick(enabled = enabled, shape = shape, onClick = onClick)
            .background(c.soft(color))
            .padding(horizontal = if (compact) 12.dp else 16.dp, vertical = if (compact) 7.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = c.ink(color), modifier = Modifier.size(if (compact) 16.dp else 18.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, color = c.ink(color), style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

/** Rounded search box. */
@Composable
fun SearchField(value: String, onValueChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    val c = AjmTheme.colors
    Row(
        modifier
            .clip(RoundedCornerShape(100))
            .background(c.card)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(100))
            .padding(start = 14.dp, end = 4.dp)
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f), maxLines = 1)
            }
            androidx.compose.foundation.text.BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        AnimatedVisibility(visible = value.isNotEmpty(), enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) {
            IconButton(onClick = { onValueChange("") }) {
                Icon(Icons.Rounded.Close, "Clear search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (value.isEmpty()) Spacer(Modifier.width(10.dp))
    }
}

/** A filter chip with a count that rolls when it changes. */
@Composable
fun CountChip(
    label: String,
    count: Int?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    dot: Color? = null,
    selectedBrush: Brush? = null,
) {
    val c = AjmTheme.colors
    val shape = RoundedCornerShape(100)
    val fg by animateColorAsState(if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, label = "chipFg")
    val bg by animateColorAsState(if (selected) accent else c.card, label = "chipBg")
    Row(
        modifier
            .bouncyClick(shape = shape, onClick = onClick)
            .then(if (selected && selectedBrush != null) Modifier.background(selectedBrush) else Modifier.background(bg))
            .border(1.dp, if (selected) Color.Transparent else MaterialTheme.colorScheme.outlineVariant, shape)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dot != null) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(if (selected) Color.White else dot))
            Spacer(Modifier.width(7.dp))
        }
        Text(label, color = fg, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        if (count != null) {
            Spacer(Modifier.width(7.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(100))
                    .background(if (selected) Color.White.copy(alpha = 0.22f) else c.chip)
                    .padding(horizontal = 7.dp, vertical = 1.dp),
            ) {
                RollingNumber(count, MaterialTheme.typography.labelMedium, color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

enum class NoticeKind { INFO, WARN, ERROR }

@Composable
fun Notice(text: String, modifier: Modifier = Modifier, kind: NoticeKind = NoticeKind.WARN, action: (@Composable () -> Unit)? = null) {
    val c = AjmTheme.colors
    val color = when (kind) {
        NoticeKind.INFO -> MaterialTheme.colorScheme.primary
        NoticeKind.WARN -> c.warning
        NoticeKind.ERROR -> c.danger
    }
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(c.soft(color))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            when (kind) {
                NoticeKind.INFO -> Icons.Rounded.Info
                NoticeKind.WARN -> Icons.Rounded.WarningAmber
                NoticeKind.ERROR -> Icons.Rounded.ErrorOutline
            },
            null,
            tint = c.ink(color),
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = c.ink(color), modifier = Modifier.weight(1f))
        if (action != null) {
            Spacer(Modifier.width(6.dp))
            action()
        }
    }
}

/** An empty screen with a big friendly emoji. */
@Composable
fun EmptyState(emoji: String, title: String, text: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Column(modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        val t = rememberInfiniteTransition(label = "emoji")
        val bob by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1600), RepeatMode.Reverse), label = "bob")
        Text(emoji, fontSize = 48.sp, modifier = Modifier.graphicsLayer { translationY = -6f * bob * density })
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        if (action != null) {
            Spacer(Modifier.height(18.dp))
            action()
        }
    }
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    EmptyState("📡", "Couldn’t reach the sheet", message, modifier) {
        SoftButton("Try again", onRetry)
    }
}

/** Grey placeholder cards with a shimmer, while the first load is on its way. */
@Composable
fun SkeletonCards(count: Int = 5, modifier: Modifier = Modifier, height: Dp = 92.dp) {
    val c = AjmTheme.colors
    val base = if (c.isDark) Color(0xFF16213A) else Color(0xFFE9EEF7)
    val hi = if (c.isDark) Color(0xFF22304F) else Color(0xFFF7F9FD)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(count) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(height)
                    .clip(RoundedCornerShape(22.dp))
                    .shimmer(base, hi),
            )
        }
    }
}

/** A dot that breathes — "live", "saving…". */
@Composable
fun PulsingDot(color: Color, modifier: Modifier = Modifier, size: Dp = 8.dp, pulsing: Boolean = true) {
    val t = rememberInfiniteTransition(label = "pulse")
    val p by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1400), RepeatMode.Restart), label = "p")
    Box(modifier.size(size * 2.4f), contentAlignment = Alignment.Center) {
        if (pulsing) {
            Box(
                Modifier
                    .size(size)
                    .graphicsLayer {
                        val s = 1f + p * 1.4f
                        scaleX = s
                        scaleY = s
                        alpha = (1f - p) * 0.55f
                    }
                    .clip(CircleShape)
                    .background(color),
            )
        }
        Box(Modifier.size(size).clip(CircleShape).background(color))
    }
}

/** A white surface sheet header used inside bottom sheets. */
@Composable
fun SheetHeader(title: String, subtitle: String? = null, onClose: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(start = 22.dp, end = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (onClose != null) {
            IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, "Close") }
        }
    }
}

@Composable
fun LinkText(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(onClick = onClick, modifier = modifier) { Text(text, style = MaterialTheme.typography.labelLarge) }
}

@Composable
fun Dot(color: Color, modifier: Modifier = Modifier, size: Dp = 8.dp) {
    Box(modifier.size(size).clip(CircleShape).background(color))
}
