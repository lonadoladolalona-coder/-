package org.anmoljeevan.office.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.anmoljeevan.office.R

/** The website's two typefaces: Fraunces for headings, Inter for everything else. */
val Fraunces = FontFamily(
    Font(R.font.fraunces_medium, FontWeight.Medium),
    Font(R.font.fraunces_semibold, FontWeight.SemiBold),
    Font(R.font.fraunces_bold, FontWeight.Bold),
    Font(R.font.fraunces_italic, FontWeight.Medium, FontStyle.Italic),
)

val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
    Font(R.font.inter_extrabold, FontWeight.ExtraBold),
)

private fun display(size: Int, line: Int, weight: FontWeight = FontWeight.SemiBold, tracking: Float = -0.4f) =
    TextStyle(fontFamily = Fraunces, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp, letterSpacing = tracking.sp)

private fun inter(size: Int, line: Int, weight: FontWeight, tracking: Float = 0f) =
    TextStyle(fontFamily = Inter, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp, letterSpacing = tracking.sp)

val AjmTypography = Typography(
    displayLarge = display(54, 58),
    displayMedium = display(44, 50),
    displaySmall = display(36, 42),
    headlineLarge = display(32, 38),
    headlineMedium = display(28, 34),
    headlineSmall = display(24, 30, tracking = -0.2f),
    titleLarge = display(22, 28, tracking = -0.1f),
    titleMedium = inter(16, 22, FontWeight.SemiBold, 0.1f),
    titleSmall = inter(14, 20, FontWeight.SemiBold, 0.1f),
    bodyLarge = inter(16, 24, FontWeight.Normal),
    bodyMedium = inter(14, 20, FontWeight.Normal),
    bodySmall = inter(12, 16, FontWeight.Normal),
    labelLarge = inter(14, 20, FontWeight.SemiBold),
    labelMedium = inter(12, 16, FontWeight.SemiBold, 0.2f),
    labelSmall = inter(11, 16, FontWeight.SemiBold, 0.3f),
)

/** Small caps-style label above a number or a section ("TOTAL REGISTRATIONS"). */
val Eyebrow = TextStyle(fontFamily = Inter, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 1.1.sp)

/** Big figures on the tiles. */
val Figure = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.SemiBold, fontSize = 40.sp, lineHeight = 44.sp, letterSpacing = (-0.6).sp)
