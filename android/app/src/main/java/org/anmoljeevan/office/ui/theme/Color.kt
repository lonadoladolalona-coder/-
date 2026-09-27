package org.anmoljeevan.office.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/** The website's palette (admin.html / media.html). */
object Brand {
    val Navy = Color(0xFF16305A)
    val NavyMid = Color(0xFF26468A)
    val Blue = Color(0xFF3B6FD6)
    val BrightBlue = Color(0xFF2F6DF6)
    val Indigo = Color(0xFF4B5CF0)
    val Gold = Color(0xFFE8A33D)
    val Pink = Color(0xFFD9558A)
    val Amber = Color(0xFFD98A0B)
    val Green = Color(0xFF1F9D63)
    val Red = Color(0xFFD64545)
    val Violet = Color(0xFF6B3FA0)
    val Slate = Color(0xFF5B6A84)
}

internal val LightScheme = lightColorScheme(
    primary = Brand.BrightBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE3ECFF),
    onPrimaryContainer = Brand.Navy,
    secondary = Brand.Gold,
    onSecondary = Color(0xFF3B2A00),
    secondaryContainer = Color(0xFFFFF1D6),
    onSecondaryContainer = Color(0xFF5C3D00),
    tertiary = Brand.Indigo,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE6E8FF),
    onTertiaryContainer = Color(0xFF1D1B4D),
    background = Color(0xFFF3F5FA),
    onBackground = Color(0xFF141A2E),
    surface = Color(0xFFF3F5FA),
    onSurface = Color(0xFF141A2E),
    surfaceVariant = Color(0xFFE8EDF6),
    onSurfaceVariant = Color(0xFF5F6B82),
    surfaceTint = Brand.BrightBlue,
    inverseSurface = Color(0xFF1B2340),
    inverseOnSurface = Color(0xFFEEF2FA),
    inversePrimary = Color(0xFF8DB3FF),
    error = Brand.Red,
    onError = Color.White,
    errorContainer = Color(0xFFFDE8E8),
    onErrorContainer = Color(0xFF7A1C1C),
    outline = Color(0xFFC3CEE2),
    outlineVariant = Color(0xFFE3E8F1),
    scrim = Color(0xFF0E162D),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFDDE3EE),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF8FAFD),
    surfaceContainer = Color(0xFFEEF2FA),
    surfaceContainerHigh = Color(0xFFE8EDF6),
    surfaceContainerHighest = Color(0xFFE1E7F2),
)

internal val DarkScheme = darkColorScheme(
    primary = Color(0xFF8DB3FF),
    onPrimary = Color(0xFF0A2463),
    primaryContainer = Color(0xFF1D3B80),
    onPrimaryContainer = Color(0xFFDCE6FF),
    secondary = Color(0xFFF2C06B),
    onSecondary = Color(0xFF3B2A00),
    secondaryContainer = Color(0xFF4A3510),
    onSecondaryContainer = Color(0xFFFFE2A6),
    tertiary = Color(0xFFAEB6FF),
    onTertiary = Color(0xFF1B1F6B),
    tertiaryContainer = Color(0xFF2F3596),
    onTertiaryContainer = Color(0xFFE1E3FF),
    background = Color(0xFF0A1020),
    onBackground = Color(0xFFE7ECF7),
    surface = Color(0xFF0A1020),
    onSurface = Color(0xFFE7ECF7),
    surfaceVariant = Color(0xFF1C2943),
    onSurfaceVariant = Color(0xFF9AA7C2),
    surfaceTint = Color(0xFF8DB3FF),
    inverseSurface = Color(0xFFE7ECF7),
    inverseOnSurface = Color(0xFF141A2E),
    inversePrimary = Brand.BrightBlue,
    error = Color(0xFFFF8A80),
    onError = Color(0xFF5C0B0B),
    errorContainer = Color(0xFF5C1A1A),
    onErrorContainer = Color(0xFFFFD8D4),
    outline = Color(0xFF3B4A6B),
    outlineVariant = Color(0xFF25324D),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF26324D),
    surfaceDim = Color(0xFF0A1020),
    surfaceContainerLowest = Color(0xFF111A2E),
    surfaceContainerLow = Color(0xFF131D33),
    surfaceContainer = Color(0xFF172239),
    surfaceContainerHigh = Color(0xFF1C2943),
    surfaceContainerHighest = Color(0xFF23324F),
)

/** Colours Material 3 has no slot for: gradients, statuses, sources, soft tints. */
@Immutable
data class AjmColors(
    val isDark: Boolean,
    val hero: List<Color>,
    val upload: List<Color>,
    val event: List<Color>,
    val onHero: Color,
    val onHeroMuted: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val violet: Color,
    val gold: Color,
    val card: Color,
    val cardBorder: Color,
    val subtle: Color,
    val chip: Color,
    val shadow: Color,
) {
    val heroBrush: Brush get() = Brush.linearGradient(hero)
    val uploadBrush: Brush get() = Brush.linearGradient(upload)
    val eventBrush: Brush get() = Brush.linearGradient(event)

    /** A soft background tint of [c] that works on light and dark surfaces. */
    fun soft(c: Color): Color = c.copy(alpha = if (isDark) 0.20f else 0.12f)

    /** [c] made readable as text on its own [soft] tint. */
    fun ink(c: Color): Color = if (isDark) lerp(c, Color.White, 0.35f) else lerp(c, Color.Black, 0.12f)
}

internal val LightAjm = AjmColors(
    isDark = false,
    hero = listOf(Brand.Navy, Brand.NavyMid, Brand.Blue),
    upload = listOf(Color(0xFF0F4D36), Color(0xFF1C7A53), Color(0xFF27A06A)),
    event = listOf(Color(0xFF1D1B4D), Color(0xFF38359F), Color(0xFF5B4BF0)),
    onHero = Color.White,
    onHeroMuted = Color(0xFFC6D6F5),
    success = Brand.Green,
    warning = Brand.Amber,
    danger = Brand.Red,
    violet = Brand.Violet,
    gold = Brand.Gold,
    card = Color.White,
    cardBorder = Color(0xFFEDF1F8),
    subtle = Color(0xFFF7F9FD),
    chip = Color(0xFFEEF2FA),
    shadow = Color(0xFF14285A),
)

internal val DarkAjm = AjmColors(
    isDark = true,
    hero = listOf(Color(0xFF0F2247), Color(0xFF1D3A78), Color(0xFF2F5BC4)),
    upload = listOf(Color(0xFF0B3827), Color(0xFF166443), Color(0xFF1F8A5A)),
    event = listOf(Color(0xFF15133A), Color(0xFF2C2A82), Color(0xFF4A3CD0)),
    onHero = Color.White,
    onHeroMuted = Color(0xFFB5C6EA),
    success = Color(0xFF4ADE9A),
    warning = Color(0xFFF2B34B),
    danger = Color(0xFFFF8A80),
    violet = Color(0xFFC3A4F0),
    gold = Color(0xFFF2C06B),
    card = Color(0xFF111A2E),
    cardBorder = Color(0xFF1E2A44),
    subtle = Color(0xFF131D33),
    chip = Color(0xFF1C2943),
    shadow = Color(0xFF000000),
)

val LocalAjmColors = staticCompositionLocalOf { LightAjm }
