package org.anmoljeevan.office.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.anmoljeevan.office.ui.theme.AjmTheme
import org.anmoljeevan.office.ui.theme.Brand
import org.anmoljeevan.office.ui.theme.Fraunces
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Slowly drifting colour blobs behind the login screen. */
@Composable
fun AuroraBackground(modifier: Modifier = Modifier) {
    val c = AjmTheme.colors
    val base = MaterialTheme.colorScheme.background
    val blobs = if (c.isDark) {
        listOf(Color(0xFF1D3B80).copy(alpha = 0.75f), Color(0xFF3A2F9E).copy(alpha = 0.55f), Brand.Gold.copy(alpha = 0.22f))
    } else {
        listOf(Color(0xFFCFDDFF), Color(0xFFE4DAFF), Color(0xFFFFE3B8))
    }
    val t = rememberInfiniteTransition(label = "aurora")
    val phase by t.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(22_000, easing = LinearEasing)),
        label = "phase",
    )
    Canvas(modifier.fillMaxSize()) {
        drawRect(base)
        val w = size.width
        val h = size.height
        val r = maxOf(w, h) * 0.55f
        val centers = listOf(
            Offset(w * (0.85f + 0.10f * cos(phase)), h * (0.08f + 0.06f * sin(phase))),
            Offset(w * (0.10f + 0.12f * sin(phase + 1.3f)), h * (0.22f + 0.08f * cos(phase + 0.7f))),
            Offset(w * (0.55f + 0.18f * cos(phase + 2.4f)), h * (0.92f + 0.05f * sin(phase * 2f))),
        )
        blobs.forEachIndexed { i, color ->
            drawCircle(Brush.radialGradient(listOf(color, color.copy(alpha = 0f)), center = centers[i], radius = r), radius = r, center = centers[i])
        }
    }
}

/** The app's mark: the cross in front of a rising sun, on a navy tile. Draws itself in on first show. */
@Composable
fun BrandMark(modifier: Modifier = Modifier, size: Dp = 76.dp, animate: Boolean = true) {
    val draw = remember { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(Unit) { draw.animateTo(1f, tween(1100, easing = FastOutSlowInEasing)) }
    val t = rememberInfiniteTransition(label = "glow")
    val glow by t.animateFloat(0.35f, 0.7f, infiniteRepeatable(tween(2200), RepeatMode.Reverse), label = "glow")
    val c = AjmTheme.colors
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().graphicsLayer { alpha = glow }) {
            drawCircle(Brush.radialGradient(listOf(Brand.Gold.copy(alpha = 0.55f), Color.Transparent)), radius = this.size.minDimension * 0.75f)
        }
        Box(
            Modifier
                .size(size * 0.86f)
                .softShadow(RoundedCornerShape(size * 0.3f), 16.dp, c.shadow.copy(alpha = 0.6f))
                .background(Brush.linearGradient(listOf(Brand.Navy, Brand.NavyMid, Brand.Blue))),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) { drawMark(draw.value) }
        }
    }
}

/** The mark on a 108-unit grid, like the launcher icon. [p] 0..1 draws it in. */
fun DrawScope.drawMark(p: Float, sun: Color = Brand.Gold, cross: Color = Color.White) {
    val s = size.minDimension / 108f
    fun o(x: Float, y: Float) = Offset(x * s, y * s)
    val sunPath = Path().apply {
        moveTo(29f * s, 63f * s)
        arcTo(androidx.compose.ui.geometry.Rect(o(29f, 38f), Size(50f * s, 50f * s)), 180f, 180f, false)
    }
    val sunP = (p / 0.55f).coerceIn(0f, 1f)
    val upP = ((p - 0.3f) / 0.4f).coerceIn(0f, 1f)
    val beamP = ((p - 0.55f) / 0.45f).coerceIn(0f, 1f)
    drawPathPortion(sunPath, sunP, sun, 4.5f * s)
    if (upP > 0f) drawLine(cross, o(54f, 79f), o(54f, 79f - 48f * upP), 8f * s, StrokeCap.Round)
    if (beamP > 0f) drawLine(cross, o(40f, 45f), o(40f + 28f * beamP, 45f), 8f * s, StrokeCap.Round)
}

private fun DrawScope.drawPathPortion(path: Path, portion: Float, color: Color, width: Float) {
    if (portion <= 0f) return
    val m = PathMeasure()
    m.setPath(path, false)
    val out = Path()
    m.getSegment(0f, m.length * portion, out, true)
    drawPath(out, color, style = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

/** A donut that sweeps round to [percent] with a spring. */
@Composable
fun Donut(
    percent: Int,
    modifier: Modifier = Modifier,
    size: Dp = 104.dp,
    stroke: Dp = 11.dp,
    track: Color = Color.White.copy(alpha = 0.18f),
    progress: Brush = Brush.sweepGradient(listOf(Brand.Gold, Color(0xFFFFE2A6), Brand.Gold)),
    label: @Composable () -> Unit = {
        Text("$percent%", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
    },
) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(percent) { anim.animateTo(percent / 100f, spring(dampingRatio = 0.8f, stiffness = 60f)) }
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = stroke.toPx()
            val d = this.size.minDimension - w
            val tl = Offset((this.size.width - d) / 2, (this.size.height - d) / 2)
            drawArc(track, 0f, 360f, false, tl, Size(d, d), style = Stroke(w))
            if (anim.value > 0f) {
                drawArc(progress, -90f, 360f * anim.value, false, tl, Size(d, d), style = Stroke(w, cap = StrokeCap.Round))
            }
        }
        label()
    }
}

/** A tick in a ring that draws itself, with a few sparkles — "all caught up", "all done". */
@Composable
fun CelebrationArt(modifier: Modifier = Modifier, size: Dp = 132.dp) {
    val ring = remember { Animatable(0f) }
    val tick = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        ring.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
        tick.animateTo(1f, tween(520, easing = FastOutSlowInEasing))
    }
    val t = rememberInfiniteTransition(label = "sparkles")
    val sp by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2600, easing = LinearEasing)), label = "sp")
    val green = AjmTheme.colors.success
    val fill = AjmTheme.colors.soft(MaterialTheme.colorScheme.primary)
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension / 132f
        val center = Offset(66f * s, 66f * s)
        drawCircle(fill, 56f * s, center)
        drawArc(
            Brush.sweepGradient(listOf(Brand.Blue, Color(0xFF27A06A), Brand.Blue), center),
            -90f, 360f * ring.value, false,
            Offset(10f * s, 10f * s), Size(112f * s, 112f * s),
            style = Stroke(7f * s, cap = StrokeCap.Round),
        )
        val tickPath = Path().apply {
            moveTo(42f * s, 68f * s)
            lineTo(59f * s, 85f * s)
            lineTo(90f * s, 48f * s)
        }
        drawPathPortion(tickPath, tick.value, green, 8f * s)
        if (tick.value >= 1f) {
            listOf(Offset(114f, 18f) to 0f, Offset(16f, 32f) to 0.3f, Offset(122f, 98f) to 0.55f, Offset(26f, 112f) to 0.8f).forEach { (o, delay) ->
                val k = ((sp + delay) % 1f)
                val a = sin(k * PI).toFloat()
                drawCircle(Brand.Gold.copy(alpha = a), (2f + 2.5f * a) * s, Offset(o.x * s, o.y * s))
            }
        }
    }
}

/** A 0..1 progress ring with an animated end, for the Zoom-link queue. */
@Composable
fun ProgressRing(progress: Float, modifier: Modifier = Modifier, size: Dp = 56.dp, color: Color = MaterialTheme.colorScheme.primary) {
    val p by animateFloatAsState(progress, spring(stiffness = 120f), label = "ring")
    val track = AjmTheme.colors.chip
    Canvas(modifier.size(size)) {
        val w = 6.dp.toPx()
        val d = min(this.size.width, this.size.height) - w
        val tl = Offset(w / 2, w / 2)
        drawArc(track, 0f, 360f, false, tl, Size(d, d), style = Stroke(w))
        drawArc(color, -90f, 360f * p, false, tl, Size(d, d), style = Stroke(w, cap = StrokeCap.Round))
    }
}

/** A big faded letter used as a watermark on the event card. */
@Composable
fun Watermark(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier,
        style = MaterialTheme.typography.displayLarge.copy(fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 220.sp, lineHeight = 220.sp),
        color = Color.White.copy(alpha = 0.07f),
        maxLines = 1,
    )
}

/** A thin pill-shaped progress bar that grows with a spring. */
@Composable
fun GrowBar(fraction: Float, modifier: Modifier = Modifier, brush: Brush, track: Color) {
    val f by animateFloatAsState(fraction.coerceIn(0f, 1f), spring(dampingRatio = 0.85f, stiffness = 90f), label = "bar")
    Box(modifier.clip(RoundedCornerShape(100)).background(track)) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(f)
                .clip(RoundedCornerShape(100))
                .background(brush),
        )
    }
}
