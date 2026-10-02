package com.example.eksiscraper.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

private val Base = Typography()

private fun TextStyle.scaled(scale: Float) =
    copy(fontSize = fontSize * scale, lineHeight = lineHeight * scale)

/**
 * Entries are long reads: slightly larger body text with roomier lines. [scale] is the reading
 * size from settings and only touches body text, so buttons and bars keep their layout.
 */
fun scaledTypography(scale: Float) = Base.copy(
    bodyLarge = Base.bodyLarge.copy(fontSize = 16.sp, lineHeight = 26.sp, letterSpacing = 0.2.sp).scaled(scale),
    bodyMedium = Base.bodyMedium.copy(lineHeight = 22.sp).scaled(scale)
)
