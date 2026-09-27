package org.anmoljeevan.office.ui.components

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.anmoljeevan.office.ui.theme.AjmTheme
import org.anmoljeevan.office.ui.theme.Motion

data class PillOption(val label: String, val icon: ImageVector? = null, val count: Int? = null)

/**
 * A segmented switch whose highlight slides (with a little spring) to the chosen option.
 * [brushFor] can give each option its own highlight (e.g. navy for Made, green for Uploaded).
 */
@Composable
fun PillSwitcher(
    options: List<PillOption>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 46.dp,
    brushFor: @Composable (Int) -> Brush = { AjmTheme.colors.heroBrush },
    container: Color = AjmTheme.colors.chip,
) {
    val haptic = LocalHapticFeedback.current
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(100))
            .background(container)
            .padding(4.dp),
    ) {
        val segment = maxWidth / options.size.coerceAtLeast(1)
        val x by animateDpAsState(
            targetValue = segment * selected,
            animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
            label = "pillX",
        )
        Box(
            Modifier
                .offset(x = x)
                .width(segment)
                .fillMaxHeight()
                .softShadow(RoundedCornerShape(100), 6.dp, AjmTheme.colors.shadow.copy(alpha = 0.5f))
                .background(brushFor(selected)),
        )
        Row(Modifier.fillMaxSize()) {
            options.forEachIndexed { i, opt ->
                val active = i == selected
                val fg by animateColorAsState(
                    if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    tween(250),
                    label = "pillFg",
                )
                Row(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(100))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Tab,
                        ) {
                            if (i != selected) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelect(i)
                            }
                        },
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (opt.icon != null) {
                        Icon(opt.icon, null, tint = fg, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(7.dp))
                    }
                    Text(opt.label, color = fg, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (opt.count != null) {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(100))
                                .background(if (active) Color.White.copy(alpha = 0.22f) else AjmTheme.colors.card)
                                .padding(horizontal = 7.dp, vertical = 1.dp),
                        ) {
                            RollingNumber(opt.count, MaterialTheme.typography.labelMedium, color = if (active) Color.White else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Wraps a screen so the Android 14+ predictive back gesture shrinks and rounds it while the
 * finger moves; letting go runs [onBack], cancelling springs it back.
 */
@Composable
fun PredictiveBackScreen(enabled: Boolean = true, onBack: () -> Unit, content: @Composable () -> Unit) {
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    PredictiveBackHandler(enabled = enabled) { events ->
        try {
            events.collect { e -> progress.snapTo(e.progress) }
            onBack()
            scope.launch { progress.animateTo(0f, tween(300)) }
        } catch (e: CancellationException) {
            scope.launch { progress.animateTo(0f, spring()) }
        }
    }
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                val p = Motion.EmphasizedDecelerate.transform(progress.value)
                val s = 1f - 0.09f * p
                scaleX = s
                scaleY = s
                translationX = 18.dp.toPx() * p
                if (progress.value > 0f) {
                    shape = RoundedCornerShape(28.dp * p)
                    clip = true
                }
            },
    ) { content() }
}

/** A label that slides left or right when it changes (months, weeks). */
@Composable
fun <T : Comparable<T>> SlidingLabel(value: T, modifier: Modifier = Modifier, content: @Composable (T) -> Unit) {
    AnimatedContent(
        targetState = value,
        transitionSpec = {
            val forward = targetState > initialState
            (androidx.compose.animation.slideInHorizontally(tween(320, easing = Motion.Emphasized)) { w -> if (forward) w / 2 else -w / 2 } + fadeIn(tween(220))) togetherWith
                (androidx.compose.animation.slideOutHorizontally(tween(320, easing = Motion.Emphasized)) { w -> if (forward) -w / 2 else w / 2 } + fadeOut(tween(160)))
        },
        modifier = modifier,
        label = "slidingLabel",
    ) { v -> content(v) }
}
