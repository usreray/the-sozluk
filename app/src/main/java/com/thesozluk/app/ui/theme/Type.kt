package com.thesozluk.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Base = Typography()

private fun TextStyle.scaled(scale: Float) =
    copy(fontSize = fontSize * scale, lineHeight = lineHeight * scale)

/** One step heavier, as the system's bold text setting does. */
private fun TextStyle.bolder(): TextStyle {
    val weight = (fontWeight ?: FontWeight.Normal).weight
    return copy(fontWeight = FontWeight((weight + 200).coerceAtMost(900)))
}

/** Applies [change] to every style, the emphasized ones included. */
private fun Typography.map(change: (TextStyle) -> TextStyle) = copy(
    displayLarge = change(displayLarge), displayMedium = change(displayMedium), displaySmall = change(displaySmall),
    headlineLarge = change(headlineLarge), headlineMedium = change(headlineMedium), headlineSmall = change(headlineSmall),
    titleLarge = change(titleLarge), titleMedium = change(titleMedium), titleSmall = change(titleSmall),
    bodyLarge = change(bodyLarge), bodyMedium = change(bodyMedium), bodySmall = change(bodySmall),
    labelLarge = change(labelLarge), labelMedium = change(labelMedium), labelSmall = change(labelSmall),
    displayLargeEmphasized = change(displayLargeEmphasized), displayMediumEmphasized = change(displayMediumEmphasized),
    displaySmallEmphasized = change(displaySmallEmphasized), headlineLargeEmphasized = change(headlineLargeEmphasized),
    headlineMediumEmphasized = change(headlineMediumEmphasized), headlineSmallEmphasized = change(headlineSmallEmphasized),
    titleLargeEmphasized = change(titleLargeEmphasized), titleMediumEmphasized = change(titleMediumEmphasized),
    titleSmallEmphasized = change(titleSmallEmphasized), bodyLargeEmphasized = change(bodyLargeEmphasized),
    bodyMediumEmphasized = change(bodyMediumEmphasized), bodySmallEmphasized = change(bodySmallEmphasized),
    labelLargeEmphasized = change(labelLargeEmphasized), labelMediumEmphasized = change(labelMediumEmphasized),
    labelSmallEmphasized = change(labelSmallEmphasized)
)

/**
 * Entries are long reads: slightly larger body text with roomier lines. [scale] is the reading
 * size from settings and only touches body text, so buttons and bars keep their layout.
 * [family] (the reading font, when chosen for the whole app) and [bold] apply everywhere.
 */
fun scaledTypography(scale: Float, family: FontFamily? = null, bold: Boolean = false): Typography {
    var type = Base.copy(
        bodyLarge = Base.bodyLarge.copy(fontSize = 16.sp, lineHeight = 26.sp, letterSpacing = 0.2.sp).scaled(scale),
        bodyMedium = Base.bodyMedium.copy(lineHeight = 22.sp).scaled(scale)
    )
    if (family != null) type = type.map { it.copy(fontFamily = family) }
    if (bold) type = type.map { it.bolder() }
    return type
}
