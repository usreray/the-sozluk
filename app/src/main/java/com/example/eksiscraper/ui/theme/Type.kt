package com.example.eksiscraper.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.unit.sp

private val Base = Typography()

// Entries are long reads: slightly larger body text with roomier lines
val Typography = Base.copy(
    bodyLarge = Base.bodyLarge.copy(fontSize = 16.sp, lineHeight = 26.sp, letterSpacing = 0.2.sp),
    bodyMedium = Base.bodyMedium.copy(lineHeight = 22.sp)
)
