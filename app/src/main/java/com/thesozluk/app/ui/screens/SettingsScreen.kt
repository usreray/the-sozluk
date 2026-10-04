package com.thesozluk.app.ui.screens

import android.content.Intent
import android.content.pm.verify.domain.DomainVerificationManager
import android.content.pm.verify.domain.DomainVerificationUserState
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.core.tween
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import coil3.SingletonImageLoader
import com.thesozluk.app.data.offline.EntryBackup
import com.thesozluk.app.data.offline.OfflineStore
import com.thesozluk.app.network.EksiSession
import com.thesozluk.app.notify.Notifier
import com.thesozluk.app.settings.AppSettings
import com.thesozluk.app.settings.ContrastChoice
import com.thesozluk.app.settings.PaletteChoice
import com.thesozluk.app.settings.ReadingFont
import com.thesozluk.app.settings.ReadingHistory
import com.thesozluk.app.settings.SeedColor
import com.thesozluk.app.settings.ThemeMode
import com.thesozluk.app.ui.components.FloatingTopBar
import com.thesozluk.app.ui.components.LargeTitle
import com.thesozluk.app.ui.components.TextPromptDialog
import com.thesozluk.app.ui.components.entryBodyStyle
import com.thesozluk.app.ui.components.segmentedShape
import com.thesozluk.app.ui.navigation.Screen
import com.thesozluk.app.ui.theme.isAppInDarkTheme
import com.thesozluk.app.ui.theme.rememberSeedScheme
import com.thesozluk.app.viewmodel.HomeCategory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/** Settings pages, like the categories of the system settings app. */
enum class SettingsSection(val key: String, val label: String, val icon: ImageVector) {
    Appearance("gorunum", "görünüm", Icons.Rounded.Palette),
    Reading("okuma", "okuma", Icons.AutoMirrored.Rounded.MenuBook),
    Home("anasayfa", "ana sayfa", Icons.Rounded.Home),
    Notifications("bildirimler", "bildirimler", Icons.Rounded.Notifications),
    Content("icerik", "içerik ve gizlilik", Icons.Rounded.Block),
    Data("veri", "indirme ve arşiv", Icons.Rounded.Download),
    Links("baglantilar", "bağlantılar", Icons.Rounded.Link),
    About("hakkinda", "hakkında", Icons.Rounded.Info)
}

/**
 * Settings: the first page lists the categories with a short summary of each, and every
 * category opens its own page ([section] set).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(navController: NavController, section: String? = null) {
    val page = SettingsSection.entries.firstOrNull { it.key == section }
    val title = page?.label ?: "ayarlar"
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val scrolled by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }

    Scaffold(
        topBar = {
            FloatingTopBar(
                title = if (scrolled) title else null,
                onBack = { navController.popBackStack() },
                onTitleClick = { scope.launch { listState.animateScrollToItem(0) } }
            )
        }
    ) { padding ->
        // Edge to edge: the list starts below the bar but scrolls under it and the status bar
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
            item { LargeTitle(title) }
            when (page) {
                null -> categories(navController)
                SettingsSection.Appearance -> appearance()
                SettingsSection.Reading -> reading()
                SettingsSection.Home -> homeLists()
                SettingsSection.Notifications -> notifications()
                SettingsSection.Content -> content()
                SettingsSection.Data -> data(navController)
                SettingsSection.Links -> links()
                SettingsSection.About -> about()
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// The first page

private fun LazyListScope.categories(navController: NavController) {
    val sections = SettingsSection.entries
    sections.forEachIndexed { index, section ->
        item(key = section.key) {
            CategoryRow(
                section = section,
                summary = summaryOf(section),
                shape = segmentedShape(index, sections.size),
                onClick = { navController.navigate(Screen.Settings.createRoute(section.key)) }
            )
        }
    }
}

@Composable
private fun summaryOf(section: SettingsSection): String = when (section) {
    SettingsSection.Appearance -> "tema ${AppSettings.themeMode.value.label}, " +
        if (AppSettings.dynamicColor.value) "duvar kâğıdı renkleri" else "${AppSettings.seed.value.label} paleti"
    SettingsSection.Reading -> "yazı tipi ${AppSettings.readingFont.value.label}, boyut %${(AppSettings.textScale.value * 100).roundToInt()}"
    SettingsSection.Home -> "${HomeCategory.entries.count { it.name !in AppSettings.hiddenTabs.value }} liste açık"
    SettingsSection.Notifications -> if (AppSettings.notifications.value) "mesaj ve olay bildirimleri açık" else "kapalı"
    SettingsSection.Content -> "${AppSettings.blockedWords.value.size} engellenen kelime, okuma geçmişi " +
        if (AppSettings.historyEnabled.value) "açık" else "kapalı"
    SettingsSection.Data -> "${OfflineStore.topics.value.size} çevrimdışı başlık, entry yedeği"
    SettingsSection.Links -> "ekşi linklerini uygulamada aç"
    SettingsSection.About -> "sürüm ${appVersion()}"
}

@Composable
private fun CategoryRow(section: SettingsSection, summary: String, shape: Shape, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = shape, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        ListItem(
            leadingContent = {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                ) {
                    Icon(section.icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            },
            headlineContent = { Text(section.label) },
            supportingContent = { Text(summary, maxLines = 1) },
            trailingContent = { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null) },
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        )
    }
}

// ---------------------------------------------------------------------------------------------
// görünüm

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun LazyListScope.appearance() {
    item { SectionTitle(
        "tema",
        changed = AppSettings.themeMode.value != ThemeMode.System || AppSettings.pureBlack.value,
        onReset = { AppSettings.setThemeMode(ThemeMode.System); AppSettings.setPureBlack(false) }
    ) }
    item {
        val themeMode by AppSettings.themeMode
        Surface(shape = segmentedShape(0, 3), color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
                    ThemeMode.entries.forEachIndexed { index, mode ->
                        ToggleButton(
                            checked = themeMode == mode,
                            onCheckedChange = { AppSettings.setThemeMode(mode) },
                            modifier = Modifier.weight(1f),
                            // Icon and label fit a third of the width without wrapping
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            shapes = connectedShapes(index, ThemeMode.entries.size)
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
            subtitle = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) "renkleri duvar kâğıdından al" else "android 12 ve üstünde",
            checked = AppSettings.dynamicColor.value,
            enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
            shape = segmentedShape(1, 3),
            onCheckedChange = AppSettings::setDynamicColor
        )
    }
    item {
        SwitchRow(
            title = "simsiyah arka plan",
            subtitle = "koyu temada OLED ekranlar için",
            checked = AppSettings.pureBlack.value,
            enabled = isAppInDarkTheme(),
            shape = segmentedShape(2, 3),
            onCheckedChange = AppSettings::setPureBlack
        )
    }
    item { SectionTitle(
        "renkler",
        changed = !AppSettings.dynamicColor.value || AppSettings.seed.value != SeedColor.Eksi ||
            AppSettings.palette.value != PaletteChoice.TonalSpot || AppSettings.contrast.value != ContrastChoice.Standard,
        onReset = {
            AppSettings.setDynamicColor(true); AppSettings.setSeed(SeedColor.Eksi)
            AppSettings.setPalette(PaletteChoice.TonalSpot); AppSettings.setContrast(ContrastChoice.Standard)
        }
    ) }
    item {
        val dynamicColor by AppSettings.dynamicColor
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
                            shapes = connectedShapes(index, ContrastChoice.entries.size)
                        ) { Text(choice.label) }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// okuma

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun LazyListScope.reading() {
    // Live preview of an entry with the choices below
    item {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Text(
                "bilgi bir kitaptır, kafa kâğıttır, ekşi ise sözlüktür. (bkz: önizleme)\n\naşağıdaki ayarlar entry'lerin nasıl görüneceğini değiştirir.",
                style = entryBodyStyle(),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth().padding(start = AppSettings.entryPadding.value.dp, end = 20.dp, top = 16.dp, bottom = 16.dp)
            )
        }
    }
    item { SectionTitle(
        "metin",
        changed = AppSettings.textScale.value != 1f || AppSettings.readingFont.value != ReadingFont.Default ||
            AppSettings.lineSpacing.value != 1f || AppSettings.entryPadding.value != 20f,
        onReset = {
            AppSettings.setTextScale(1f); AppSettings.setReadingFont(ReadingFont.Default)
            AppSettings.setLineSpacing(1f); AppSettings.setEntryPadding(20f)
        }
    ) }
    item {
        SliderRow(
            title = "yazı boyutu · %${(AppSettings.textScale.value * 100).roundToInt()}",
            value = AppSettings.textScale.value,
            range = 0.85f..1.3f,
            steps = 8,
            shape = segmentedShape(0, 4),
            onChange = AppSettings::setTextScale
        )
    }
    item {
        Surface(shape = segmentedShape(1, 4), color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("yazı tipi", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
                    ReadingFont.entries.forEachIndexed { index, font ->
                        ToggleButton(
                            checked = AppSettings.readingFont.value == font,
                            onCheckedChange = { AppSettings.setReadingFont(font) },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp),
                            shapes = connectedShapes(index, ReadingFont.entries.size)
                        ) { Text(font.label, fontFamily = font.family, maxLines = 1, softWrap = false) }
                    }
                }
            }
        }
    }
    item {
        SliderRow(
            title = "satır aralığı · ${"%.1f".format(AppSettings.lineSpacing.value)}x",
            value = AppSettings.lineSpacing.value,
            range = 0.9f..1.8f,
            steps = 8,
            shape = segmentedShape(2, 4),
            onChange = AppSettings::setLineSpacing
        )
    }
    item {
        SliderRow(
            title = "kenar boşluğu · ${AppSettings.entryPadding.value.roundToInt()} dp",
            value = AppSettings.entryPadding.value,
            range = 8f..36f,
            steps = 6,
            shape = segmentedShape(3, 4),
            onChange = AppSettings::setEntryPadding
        )
    }
    item { SectionTitle(
        "entry'ler",
        changed = AppSettings.showEntryNumbers.value || !AppSettings.showAvatars.value,
        onReset = { AppSettings.setShowEntryNumbers(false); AppSettings.setShowAvatars(true) }
    ) }
    item {
        SwitchRow("entry sıra numarası", "her entry'nin başlıktaki sırası (#12)", AppSettings.showEntryNumbers.value, true, segmentedShape(0, 2), AppSettings::setShowEntryNumbers)
    }
    item {
        SwitchRow("profil resimleri", "entry'lerde yazarın resmini göster", AppSettings.showAvatars.value, true, segmentedShape(1, 2), AppSettings::setShowAvatars)
    }
    item { SectionTitle(
        "ekran",
        changed = !AppSettings.marqueeTitles.value || !AppSettings.hideBarsOnScroll.value || AppSettings.keepScreenOn.value,
        onReset = {
            AppSettings.setMarqueeTitles(true); AppSettings.setHideBarsOnScroll(true); AppSettings.setKeepScreenOn(false)
        }
    ) }
    item {
        SwitchRow("uzun başlıkları kaydır", "üst bara sığmayan başlık sağdan sola aksın; basılı tutunca tamamı görünür", AppSettings.marqueeTitles.value, true, segmentedShape(0, 3), AppSettings::setMarqueeTitles)
    }
    item {
        SwitchRow("kaydırınca barları gizle", "aşağı kaydırırken üst bar, sayfa düğmeleri ve başlık alanları kaybolsun", AppSettings.hideBarsOnScroll.value, true, segmentedShape(1, 3), AppSettings::setHideBarsOnScroll)
    }
    item {
        SwitchRow("ekranı açık tut", "başlık okurken ekran kararmasın", AppSettings.keepScreenOn.value, true, segmentedShape(2, 3), AppSettings::setKeepScreenOn)
    }
}

// ---------------------------------------------------------------------------------------------
// ana sayfa

private fun LazyListScope.homeLists() {
    item { SectionTitle(
        "listeler",
        changed = AppSettings.homeTabsChanged(),
        onReset = AppSettings::resetHomeTabs
    ) }
    item {
        val hiddenTabs by AppSettings.hiddenTabs
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "olay, takip, son, kenar ve çaylaklar giriş yapınca görünür",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
}

// ---------------------------------------------------------------------------------------------
// bildirimler

private fun LazyListScope.notifications() {
    item {
        val context = LocalContext.current
        val enabledByDefault = EksiSession.isLoggedIn.value && Notifier.canNotify(context)
        SectionTitle(
            "bildirimler",
            changed = AppSettings.notifications.value != enabledByDefault,
            onReset = {
                AppSettings.setNotifications(enabledByDefault)
                Notifier.schedule(context, enabledByDefault)
            }
        )
    }
    item {
        val context = LocalContext.current
        val isLoggedIn by EksiSession.isLoggedIn
        // Android 13+ asks before the first notification; turn on only if allowed
        val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            AppSettings.setNotifications(granted)
            Notifier.schedule(context, granted)
        }
        SwitchRow(
            title = "mesaj ve olay bildirimleri",
            subtitle = if (isLoggedIn) "yeni mesaj ya da takip ettiğin başlıkta yeni entry olunca (15 dakikada bir bakar)"
            else "giriş yapınca kullanılabilir",
            checked = AppSettings.notifications.value,
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
    item {
        val context = LocalContext.current
        Hint("bildirim sesi ve önemi telefonun bildirim ayarlarından değiştirilebilir")
        TextButton(onClick = {
            context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
        }) { Text("sistem bildirim ayarlarını aç") }
    }
}

// ---------------------------------------------------------------------------------------------
// içerik ve gizlilik

private fun LazyListScope.content() {
    item { SectionTitle(
        "engellenen kelimeler",
        changed = AppSettings.blockedWords.value.isNotEmpty(),
        onReset = { AppSettings.blockedWords.value.forEach(AppSettings::removeBlockedWord) }
    ) }
    item {
        val blockedWords by AppSettings.blockedWords
        var adding by remember { mutableStateOf(false) }
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "bu kelimeleri içeren başlıklar listelerde gösterilmez",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    blockedWords.sorted().forEach { word ->
                        InputChip(
                            selected = false,
                            onClick = { AppSettings.removeBlockedWord(word) },
                            label = { Text(word) },
                            trailingIcon = { Icon(Icons.Rounded.Close, contentDescription = "kaldır", modifier = Modifier.size(18.dp)) }
                        )
                    }
                    AssistChip(
                        onClick = { adding = true },
                        label = { Text("kelime ekle") },
                        leadingIcon = { Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }
            }
        }
        if (adding) {
            TextPromptDialog(
                title = "kelime engelle",
                placeholder = "örn. burç",
                confirmLabel = "ekle",
                onConfirm = {
                    AppSettings.addBlockedWord(it)
                    adding = false
                },
                onDismiss = { adding = false }
            )
        }
    }
    item { SectionTitle(
        "okuma geçmişi",
        changed = !AppSettings.historyEnabled.value,
        onReset = { AppSettings.setHistoryEnabled(true) }
    ) }
    item {
        SwitchRow(
            "okuma geçmişini tut", "açtığın başlıklar profildeki geçmişte listelenir; yalnızca bu cihazda durur",
            AppSettings.historyEnabled.value, true, segmentedShape(0, 2), AppSettings::setHistoryEnabled
        )
    }
    item {
        val count = ReadingHistory.items.value.size
        ActionRow(
            title = "geçmişi temizle",
            subtitle = if (count == 0) "geçmiş boş" else "$count başlık",
            button = "temizle",
            enabled = count > 0,
            shape = segmentedShape(1, 2),
            onClick = ReadingHistory::clear
        )
    }
}

// ---------------------------------------------------------------------------------------------
// indirme ve arşiv

private fun LazyListScope.data(navController: NavController) {
    item { SectionTitle(
        "çevrimdışı okuma",
        changed = !AppSettings.prefetchNextPage.value,
        onReset = { AppSettings.setPrefetchNextPage(true) }
    ) }
    item {
        val topics by OfflineStore.topics
        val downloading = OfflineStore.progress.values.count { !it.finished }
        NavRow(
            title = "çevrimdışı başlıklar",
            subtitle = buildString {
                append(if (topics.isEmpty()) "henüz yok" else "${topics.size} başlık · ${formatBytes(OfflineStore.totalBytes())}")
                if (downloading > 0) append(" · $downloading indiriliyor")
            },
            shape = segmentedShape(0, 2),
            onClick = { navController.navigate(Screen.Library.createRoute(LibraryKind.Offline)) }
        )
    }
    item {
        SwitchRow(
            "sonraki sayfayı önceden yükle", "kaydırırken beklememek için",
            AppSettings.prefetchNextPage.value, true, segmentedShape(1, 2), AppSettings::setPrefetchNextPage
        )
    }
    item { Hint("bir başlığı indirmek için başlıktaki ⋮ menüsünden \"çevrimdışı kaydet\"i seç") }

    item { SectionTitle("entry yedeği") }
    item {
        val context = LocalContext.current
        val nick by EksiSession.nick
        val progress by EntryBackup.progress
        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/markdown")) { uri ->
            val who = nick
            if (uri != null && who != null) EntryBackup.start(context, who, uri)
        }
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("kendi entry'lerini yedekle", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (nick == null) "giriş yapınca kullanılabilir"
                    else "$nick hesabındaki tüm entry'ler başlıklarıyla birlikte tek bir markdown dosyasına yazılır",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val p = progress
                when {
                    p != null && !p.finished -> {
                        Text("${p.done} entry okundu…", style = MaterialTheme.typography.labelLarge)
                        LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth())
                        OutlinedButton(onClick = EntryBackup::cancel) { Text("durdur") }
                    }
                    else -> {
                        if (p?.error != null) Text("yedek alınamadı: ${p.error}", color = MaterialTheme.colorScheme.error)
                        else if (p?.finished == true) Text("${p.done} entry yedeklendi", color = MaterialTheme.colorScheme.primary)
                        FilledTonalButton(
                            enabled = nick != null,
                            onClick = {
                                val day = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                                launcher.launch("eksi-yedek-${nick.orEmpty()}-$day.md")
                            }
                        ) { Text("yedeği al") }
                    }
                }
            }
        }
    }

    item { SectionTitle("depolama") }
    item {
        val context = LocalContext.current
        var cleared by remember { mutableStateOf(false) }
        ActionRow(
            title = "görsel önbelleği",
            subtitle = if (cleared) "temizlendi" else "profil resimleri ve görseller tekrar indirilir",
            button = "temizle",
            enabled = !cleared,
            shape = segmentedShape(0, 2),
            onClick = {
                val loader = SingletonImageLoader.get(context)
                loader.memoryCache?.clear()
                loader.diskCache?.clear()
                cleared = true
            }
        )
    }
    item {
        val topics by OfflineStore.topics
        var confirm by remember { mutableStateOf(false) }
        ActionRow(
            title = "çevrimdışı başlıkları sil",
            subtitle = if (topics.isEmpty()) "silinecek bir şey yok" else formatBytes(OfflineStore.totalBytes()),
            button = if (confirm) "emin misin?" else "sil",
            enabled = topics.isNotEmpty(),
            shape = segmentedShape(1, 2),
            onClick = {
                if (confirm) {
                    OfflineStore.clearAll()
                    confirm = false
                } else confirm = true
            }
        )
    }
}

// ---------------------------------------------------------------------------------------------
// bağlantılar

private fun LazyListScope.links() {
    item {
        val context = LocalContext.current
        // Re-read when coming back from the system screen
        var refresh by remember { mutableIntStateOf(0) }
        val owner = LocalLifecycleOwner.current
        DisposableEffect(owner) {
            val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) refresh++ }
            owner.lifecycle.addObserver(observer)
            onDispose { owner.lifecycle.removeObserver(observer) }
        }
        val enabled = remember(refresh) { linksEnabled(context) }
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("ekşi linklerini uygulamada aç", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (enabled) "açık: eksisozluk.com linkleri bu uygulamada açılıyor"
                    else "kapalı: android, doğrulanmamış uygulamalarda bunu telefon ayarlarından açmanı istiyor. " +
                        "açılan sayfada \"desteklenen bağlantıları aç\"ı etkinleştirip eksisozluk.com adreslerini seç.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                FilledTonalButton(onClick = {
                    val intent = Intent(Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS, Uri.parse("package:${context.packageName}"))
                    runCatching { context.startActivity(intent) }
                }) { Text(if (enabled) "ayarları aç" else "aç") }
            }
        }
    }
    item { Hint("uygulama simgesine basılı tutunca \"ara\", \"mesajlar\" ve \"olay\" kısayolları da çıkar") }
}

private fun linksEnabled(context: android.content.Context): Boolean {
    val manager = context.getSystemService(DomainVerificationManager::class.java) ?: return false
    val state = runCatching { manager.getDomainVerificationUserState(context.packageName) }.getOrNull() ?: return false
    return state.hostToStateMap.any { (host, value) ->
        host.endsWith("eksisozluk.com") && value != DomainVerificationUserState.DOMAIN_STATE_NONE
    }
}

// ---------------------------------------------------------------------------------------------
// hakkında

private fun LazyListScope.about() {
    item {
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("the sözlük", style = MaterialTheme.typography.headlineSmallEmphasized, color = MaterialTheme.colorScheme.primary)
                Text("sürüm ${appVersion()}", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "ekşi sözlük'ün resmi uygulaması değildir; içerik eksisozluk.com'dan okunur, hesabınla yapılan her işlem site üzerinden gider.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun appVersion(): String {
    val context = LocalContext.current
    return remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "?"
    }
}

// ---------------------------------------------------------------------------------------------
// Building blocks

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun connectedShapes(index: Int, count: Int) = when (index) {
    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
    count - 1 -> ButtonGroupDefaults.connectedTrailingButtonShapes()
    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
}

@Composable
private fun SectionTitle(text: String, changed: Boolean = false, onReset: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp)
        )
        AnimatedVisibility(
            visible = changed && onReset != null,
            enter = fadeIn(tween(180)) + expandHorizontally(tween(220), expandFrom = Alignment.End),
            exit = fadeOut(tween(120)) + shrinkHorizontally(tween(180), shrinkTowards = Alignment.End)
        ) {
            onReset?.let { reset ->
                Text(
                    "varsayılana dön",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.clickable(role = Role.Button, onClick = reset).padding(horizontal = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
    )
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    shape: Shape,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        onClick = { if (enabled) onCheckedChange(!checked) },
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        ListItem(
            headlineContent = { Text(title) },
            supportingContent = { Text(subtitle) },
            trailingContent = { Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled) },
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        )
    }
}

@Composable
private fun NavRow(title: String, subtitle: String, shape: Shape, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = shape, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        ListItem(
            headlineContent = { Text(title) },
            supportingContent = { Text(subtitle) },
            trailingContent = { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null) },
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        )
    }
}

@Composable
private fun ActionRow(title: String, subtitle: String, button: String, enabled: Boolean, shape: Shape, onClick: () -> Unit) {
    Surface(shape = shape, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        ListItem(
            headlineContent = { Text(title) },
            supportingContent = { Text(subtitle) },
            trailingContent = { TextButton(onClick = onClick, enabled = enabled) { Text(button) } },
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        )
    }
}

@Composable
private fun SliderRow(title: String, value: Float, range: ClosedFloatingPointRange<Float>, steps: Int, shape: Shape, onChange: (Float) -> Unit) {
    Surface(shape = shape, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Slider(value = value, onValueChange = onChange, valueRange = range, steps = steps)
        }
    }
}

fun formatBytes(bytes: Long): String = when {
    bytes >= 1024 * 1024 -> "%.1f MB".format(bytes / 1024f / 1024f)
    bytes >= 1024 -> "${bytes / 1024} KB"
    else -> "$bytes B"
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
