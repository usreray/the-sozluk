package com.example.eksiscraper.ui.screens

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import com.example.eksiscraper.settings.ContrastChoice
import com.example.eksiscraper.settings.PaletteChoice
import com.example.eksiscraper.settings.SeedColor
import com.example.eksiscraper.ui.theme.rememberSeedScheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.eksiscraper.settings.AppSettings
import com.example.eksiscraper.settings.ThemeMode
import com.example.eksiscraper.ui.components.FloatingTopBar
import com.example.eksiscraper.ui.components.segmentedShape
import com.example.eksiscraper.ui.theme.isAppInDarkTheme
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val themeMode by AppSettings.themeMode
    val dynamicColor by AppSettings.dynamicColor
    val pureBlack by AppSettings.pureBlack
    val textScale by AppSettings.textScale

    Scaffold(
        topBar = { FloatingTopBar(title = "ayarlar", onBack = { navController.popBackStack() }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            item { SectionTitle("görünüm") }
            item {
                Surface(shape = segmentedShape(0, 3), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("tema", style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
                            ThemeMode.entries.forEachIndexed { index, mode ->
                                ToggleButton(
                                    checked = themeMode == mode,
                                    onCheckedChange = { AppSettings.setThemeMode(mode) },
                                    modifier = Modifier.weight(1f),
                                    shapes = when (index) {
                                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                        ThemeMode.entries.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                    }
                                ) {
                                    Icon(
                                        when (mode) {
                                            ThemeMode.System -> Icons.Rounded.BrightnessAuto
                                            ThemeMode.Light -> Icons.Rounded.LightMode
                                            ThemeMode.Dark -> Icons.Rounded.DarkMode
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.padding(end = ToggleButtonDefaults.IconSpacing)
                                    )
                                    Text(mode.label)
                                }
                            }
                        }
                    }
                }
            }
            item {
                SwitchRow(
                    title = "dinamik renk",
                    subtitle = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) "renkleri duvar kâğıdından al"
                    else "Android 12 ve üstünde",
                    checked = dynamicColor,
                    enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                    shape = segmentedShape(1, 3),
                    onCheckedChange = AppSettings::setDynamicColor
                )
            }
            item {
                SwitchRow(
                    title = "simsiyah arka plan",
                    subtitle = "koyu temada OLED ekranlar için",
                    checked = pureBlack,
                    enabled = isAppInDarkTheme(),
                    shape = segmentedShape(2, 3),
                    onCheckedChange = AppSettings::setPureBlack
                )
            }

            item { SectionTitle("renkler") }
            item {
                Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            if (dynamicColor) "bir renk seçince duvar kâğıdı renkleri kapanır" else "palet",
                            style = MaterialTheme.typography.titleMedium
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            SeedColor.entries.forEach { seed ->
                                SeedSwatch(
                                    seed = seed,
                                    selected = !dynamicColor && seed == AppSettings.seed.value,
                                    onClick = {
                                        AppSettings.setSeed(seed)
                                        AppSettings.setDynamicColor(false)
                                    }
                                )
                            }
                        }
                        Text("stil", style = MaterialTheme.typography.titleMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PaletteChoice.entries.forEach { choice ->
                                FilterChip(
                                    selected = choice == AppSettings.palette.value,
                                    onClick = {
                                        AppSettings.setPalette(choice)
                                        AppSettings.setDynamicColor(false)
                                    },
                                    label = { Text(choice.label) }
                                )
                            }
                        }
                        Text("kontrast", style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
                            ContrastChoice.entries.forEachIndexed { index, choice ->
                                ToggleButton(
                                    checked = choice == AppSettings.contrast.value,
                                    onCheckedChange = { AppSettings.setContrast(choice) },
                                    modifier = Modifier.weight(1f),
                                    enabled = !dynamicColor,
                                    shapes = when (index) {
                                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                        ContrastChoice.entries.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                    }
                                ) { Text(choice.label) }
                            }
                        }
                    }
                }
            }

            item { SectionTitle("okuma") }
            item {
                Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "yazı boyutu · %${(textScale * 100).roundToInt()}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Slider(
                            value = textScale,
                            onValueChange = AppSettings::setTextScale,
                            valueRange = 0.85f..1.3f,
                            steps = 8
                        )
                        // Live preview in the entry text style
                        Text(
                            "bilgi bir kitaptır, kafa kâğıttır, ekşi ise sözlüktür.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 8.dp, top = 20.dp, bottom = 8.dp)
    )
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    shape: androidx.compose.ui.graphics.Shape,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(shape = shape, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        ListItem(
            headlineContent = { Text(title) },
            supportingContent = { Text(subtitle) },
            trailingContent = { Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled) },
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        )
    }
}

/**
 * A seed color shown as the palette it produces (primary on top, secondary and tertiary below),
 * like the wallpaper color picker; the chosen one morphs into a cookie shape.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SeedSwatch(seed: SeedColor, selected: Boolean, onClick: () -> Unit) {
    val scheme = rememberSeedScheme(
        seed = seed,
        palette = AppSettings.palette.value,
        darkTheme = isAppInDarkTheme(),
        pureBlack = false,
        contrastLevel = 0.0
    )
    val shape = if (selected) MaterialShapes.Cookie9Sided.toShape() else CircleShape
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(60.dp)
                .clip(shape)
                .clickable(onClick = onClick)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxWidth().weight(1f).background(scheme.primary))
                Row(Modifier.fillMaxWidth().weight(1f)) {
                    Box(Modifier.fillMaxHeight().weight(1f).background(scheme.secondaryContainer))
                    Box(Modifier.fillMaxHeight().weight(1f).background(scheme.tertiary))
                }
            }
            if (selected) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = "seçili",
                    tint = scheme.onPrimary,
                    modifier = Modifier.background(scheme.primary, CircleShape).padding(4.dp).size(18.dp)
                )
            }
        }
        Text(
            seed.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
