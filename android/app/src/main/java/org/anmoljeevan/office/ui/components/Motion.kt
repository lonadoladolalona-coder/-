package org.anmoljeevan.office.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt
import org.anmoljeevan.office.core.Text as CoreText

/**
 * Shrinks a little while pressed and springs back — every tappable card uses it. With a [shape],
 * the ripple and the content are clipped to it; with an [elevation], a soft shadow that scales
 * with the press.
 */
fun Modifier.bouncyClick(
    enabled: Boolean = true,
    shape: Shape? = null,
    elevation: Dp = 0.dp,
    shadowColor: Color = Color.Black,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.965f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "press",
    )
    val base = this.graphicsLayer {
        scaleX = scale
        scaleY = scale
        if (shape != null) {
            this.shape = shape
            clip = true
        }
        if (elevation > 0.dp) {
            shadowElevation = elevation.toPx()
            ambientShadowColor = shadowColor
            spotShadowColor = shadowColor
        }
    }
    if (onLongClick != null) {
        base.combinedClickable(
            interactionSource = source,
            indication = LocalIndication.current,
            enabled = enabled,
            onLongClick = onLongClick,
            onClick = onClick,
        )
    } else {
        base.clickable(interactionSource = source, indication = LocalIndication.current, enabled = enabled, onClick = onClick)
    }
}

/** A rounded, softly shadowed layer for cards that aren't tappable. */
fun Modifier.softShadow(shape: Shape, elevation: Dp, color: Color): Modifier = this.graphicsLayer {
    this.shape = shape
    clip = true
    shadowElevation = elevation.toPx()
    ambientShadowColor = color
    spotShadowColor = color
}

/** Fades and slides a list item in once, a little after the one above it. */
fun Modifier.staggeredEntrance(index: Int, enabled: Boolean = true): Modifier = composed {
    if (!enabled) return@composed this
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(durationMillis = 420, delayMillis = index.coerceIn(0, 10) * 45, easing = FastOutSlowInEasing))
    }
    this.graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 36f * density
    }
}

/** Counts up to [target] the first time, then animates between values. */
@Composable
fun CountUpText(
    target: Int,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    format: (Int) -> String = { CoreText.number(it) },
) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(target) {
        val duration = (380 + abs(target - anim.value.roundToInt()) * 6).coerceAtMost(950)
        anim.animateTo(target.toFloat(), tween(duration, easing = FastOutSlowInEasing))
    }
    Text(format(anim.value.roundToInt()), modifier = modifier, style = style, color = color, maxLines = 1)
}

/** A number that rolls up or down when it changes (badges, counters). */
@Composable
fun RollingNumber(
    value: Int,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    format: (Int) -> String = { it.toString() },
) {
    AnimatedContent(
        targetState = value,
        transitionSpec = {
            val up = targetState > initialState
            (slideInVertically(spring(stiffness = Spring.StiffnessMediumLow)) { h -> if (up) h else -h } + fadeIn()) togetherWith
                (slideOutVertically(spring(stiffness = Spring.StiffnessMediumLow)) { h -> if (up) -h else h } + fadeOut())
        },
        modifier = modifier,
        label = "rolling",
    ) { v -> Text(format(v), style = style, color = color, maxLines = 1) }
}

/** Horizontal shake, for a wrong key. Call [shake] to play it. */
class Shaker {
    internal val offset = Animatable(0f)

    suspend fun shake() {
        offset.snapTo(0f)
        for (x in listOf(-18f, 16f, -12f, 9f, -5f, 2f, 0f)) offset.animateTo(x, tween(55, easing = LinearEasing))
    }
}

fun Modifier.shake(shaker: Shaker): Modifier = this.graphicsLayer { translationX = shaker.offset.value * density }

/** A soft light sweeping across placeholder blocks while something loads. */
fun Modifier.shimmer(base: Color, highlight: Color): Modifier = composed {
    val t = rememberInfiniteTransition(label = "shimmer")
    val x by t.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing)),
        label = "shimmerX",
    )
    this.drawBehind {
        drawRect(
            Brush.linearGradient(
                colors = listOf(base, highlight, base),
                start = Offset(size.width * x - size.width * 0.5f, 0f),
                end = Offset(size.width * x + size.width * 0.5f, size.height),
            ),
        )
    }
}

/** Is the list being scrolled towards its top (or resting)? Used to expand the FAB. */
@Composable
fun LazyListState.isScrollingUp(): Boolean {
    var previousIndex by remember(this) { mutableIntStateOf(firstVisibleItemIndex) }
    var previousOffset by remember(this) { mutableIntStateOf(firstVisibleItemScrollOffset) }
    var up by remember(this) { mutableStateOf(true) }
    val v by remember(this) {
        derivedStateOf {
            if (previousIndex != firstVisibleItemIndex) {
                up = previousIndex > firstVisibleItemIndex
            } else if (previousOffset != firstVisibleItemScrollOffset) {
                up = previousOffset >= firstVisibleItemScrollOffset
            }
            previousIndex = firstVisibleItemIndex
            previousOffset = firstVisibleItemScrollOffset
            up
        }
    }
    return v || firstVisibleItemIndex == 0
}
