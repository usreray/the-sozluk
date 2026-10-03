package com.example.eksiscraper.ui.screens

import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import android.os.Build
import androidx.compose.ui.platform.LocalContext
import com.example.eksiscraper.network.EksiSession
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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.eksiscraper.notify.Notifier
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.material3.InputChip
import androidx.compose.material3.AssistChip
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Add
import com.example.eksiscraper.ui.components.TextPromptDialog
import com.example.eksiscraper.viewmodel.HomeCategory
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.eksiscraper.settings.AppSettings
import com.example.eksiscraper.settings.ThemeMode
import com.example.eksiscraper.ui.components.FloatingTopBar
import com.example.eksiscraper.ui.components.LargeTitle
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
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
    val showEntryNumbers by AppSettings.showEntryNumbers
    val showAvatars by AppSettings.showAvatars
    val keepScreenOn by AppSettings.keepScreenOn
    val prefetchNextPage by AppSettings.prefetchNextPage
    val marqueeTitles by AppSettings.marqueeTitles
    val hideBarsOnScroll by AppSettings.hideBarsOnScroll
    val hiddenTabs by AppSettings.hiddenTabs
    val blockedWords by AppSettings.blockedWords
    var addingWord by remember { mutableStateOf(false) }
    val notifications by AppSettings.notifications
    val isLoggedIn by EksiSession.isLoggedIn
    val context = LocalContext.current
    // Android 13+ asks before the first notification; turn on only if allowed
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        AppSettings.setNotifications(granted)
        Notifier.schedule(context, granted)
    }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val scrolled by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
    Scaffold(
        // A bare back button over a big "ayarlar"; the small title appears after scrolling
        topBar = {
            FloatingTopBar(
                title = if (scrolled) "ayarlar" else null,
                onBack = { navController.popBackStack() },
                // Tapping the title goes back to the top
                onTitleClick = { scope.launch { listState.animateScrollToItem(0) } }
            )
        }
    ) { padding ->
        // Edge to edge: the list starts below the bars but scrolls under them and the status bar
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding(),
                bottom = 32.dp + padding.calculateBottomPadding()
            ),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            item { LargeTitle("ayarlar") }
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
                                    // Icon and label fit a third of the width without wrapping
                                    contentPadding = PaddingValues(horizontal = 8.dp),
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
                                    Text(mode.label, maxLines = 1, softWrap = false)
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
                    else "android 12 ve üstünde",
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
                Surface(shape = segmentedShape(0, 7), color = MaterialTheme.colorScheme.surfaceContainerLow) {
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
            item {
                SwitchRow(
                    title = "entry sıra numarası",
                    subtitle = "her entry'nin başlıktaki sırası (#12)",
                    checked = showEntryNumbers,
                    enabled = true,
                    shape = segmentedShape(1, 7),
                    onCheckedChange = AppSettings::setShowEntryNumbers
                )
            }
            item {
                SwitchRow(
                    title = "profil resimleri",
                    subtitle = "entry'lerde yazarın resmini göster",
                    checked = showAvatars,
                    enabled = true,
                    shape = segmentedShape(2, 7),
                    onCheckedChange = AppSettings::setShowAvatars
                )
            }
            item {
                SwitchRow(
                    title = "ekranı açık tut",
                    subtitle = "başlık okurken ekran kararmasın",
                    checked = keepScreenOn,
                    enabled = true,
                    shape = segmentedShape(3, 7),
                    onCheckedChange = AppSettings::setKeepScreenOn
                )
            }
            item {
                SwitchRow(
                    title = "sonraki sayfayı önceden yükle",
                    subtitle = "kaydırırken beklememek için",
                    checked = prefetchNextPage,
                    enabled = true,
                    shape = segmentedShape(4, 7),
                    onCheckedChange = AppSettings::setPrefetchNextPage
                )
            }
            item {
                SwitchRow(
                    title = "uzun başlıkları kaydır",
                    subtitle = "üst bara sığmayan başlık sağdan sola aksın; basılı tutunca tamamı görünür",
                    checked = marqueeTitles,
                    enabled = true,
                    shape = segmentedShape(5, 7),
                    onCheckedChange = AppSettings::setMarqueeTitles
                )
            }
            item {
                SwitchRow(
                    title = "kaydırınca barları gizle",
                    subtitle = "aşağı kaydırırken üst bar, sayfa düğmeleri ve başlık alanları kaybolsun",
                    checked = hideBarsOnScroll,
                    enabled = true,
                    shape = segmentedShape(6, 7),
                    onCheckedChange = AppSettings::setHideBarsOnScroll
                )
            }

            item { SectionTitle("bildirimler") }
            item {
                SwitchRow(
                    title = "mesaj ve olay bildirimleri",
                    subtitle = if (isLoggedIn) "yeni mesaj ya da takip ettiğin başlıkta yeni entry olunca (15 dakikada bir bakar)"
                    else "giriş yapınca kullanılabilir",
                    checked = notifications,
                    enabled = isLoggedIn,
                    shape = RoundedCornerShape(24.dp),
                    onCheckedChange = { enabled ->
                        if (enabled && !Notifier.canNotify(context) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            AppSettings.setNotifications(enabled)
                            Notifier.schedule(context, enabled)
                        }
                    }
                )
            }

            item { SectionTitle("ana sayfa listeleri") }
            item {
                Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "olay, takip, son, kenar ve çaylaklar giriş yapınca görünür",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            HomeCategory.entries.forEach { category ->
                                val visible = category.name !in hiddenTabs
                                FilterChip(
                                    selected = visible,
                                    onClick = { AppSettings.setTabVisible(category.name, !visible) },
                                    label = { Text(category.label) },
                                    leadingIcon = if (visible) ({ Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }) else null
                                )
                            }
                        }
                    }
                }
            }

            item { SectionTitle("engellenen kelimeler") }
            item {
                Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "bu kelimeleri içeren başlıklar listelerde gösterilmez",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            blockedWords.sorted().forEach { word ->
                                InputChip(
                                    selected = false,
                                    onClick = { AppSettings.removeBlockedWord(word) },
                                    label = { Text(word) },
                                    trailingIcon = { Icon(Icons.Rounded.Close, contentDescription = "kaldır", modifier = Modifier.size(18.dp)) }
                                )
                            }
                            AssistChip(
                                onClick = { addingWord = true },
                                label = { Text("kelime ekle") },
                                leadingIcon = { Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            )
                        }
                    }
                }
            }
        }
    }
    if (addingWord) {
        TextPromptDialog(
            title = "kelime engelle",
            placeholder = "örn. burç",
            confirmLabel = "ekle",
            onConfirm = {
                AppSettings.addBlockedWord(it)
                addingWord = false
            },
            onDismiss = { addingWord = false }
        )
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
