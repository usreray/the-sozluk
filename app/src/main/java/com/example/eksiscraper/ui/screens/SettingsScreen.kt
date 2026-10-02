package com.example.eksiscraper.ui.screens

import android.os.Build
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
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("ayarlar") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Geri")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
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
