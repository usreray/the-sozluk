package com.thesozluk.app.ui.components

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.thesozluk.app.settings.AppSettings
import com.thesozluk.app.settings.SeedColor
import com.thesozluk.app.settings.ThemeMode
import com.thesozluk.app.ui.theme.isAppInDarkTheme
import com.thesozluk.app.ui.theme.rememberSeedScheme
import kotlinx.coroutines.launch

/**
 * The first-run guide: shown once, on a fresh install only (an update goes straight to the
 * app, with "neler yeni"). Three steps: what the app is, how it looks (theme, palette, liquid
 * glass, applied as they are picked), and a send-off.
 */
object Onboarding {
    private const val PREFS = "onboarding"

    private val _needed = mutableStateOf(false)
    val needed: State<Boolean> = _needed

    private var prefs: android.content.SharedPreferences? = null

    fun init(context: Context) {
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = p
        if (p.getBoolean("done", false)) return
        val info = runCatching { context.packageManager.getPackageInfo(context.packageName, 0) }.getOrNull()
        // Updated from an older version: the app is already set up
        val fresh = info == null || info.firstInstallTime == info.lastUpdateTime
        if (fresh) _needed.value = true else finish()
    }

    fun finish() {
        _needed.value = false
        prefs?.edit()?.putBoolean("done", true)?.apply()
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen() {
    val pages = 3
    val pager = rememberPagerState { pages }
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme
    BackHandler(enabled = pager.currentPage > 0) { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } }

    Surface(modifier = Modifier.fillMaxSize(), color = colors.surface) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = Onboarding::finish) { Text("geç") }
            }
            HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { page ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 28.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterVertically)
                ) {
                    when (page) {
                        0 -> WelcomeStep()
                        1 -> LookStep()
                        else -> ReadyStep()
                    }
                }
            }
            // Where you are, and the way on
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                    repeat(pages) { i ->
                        val selected = pager.currentPage == i
                        val width by animateDpAsState(if (selected) 24.dp else 8.dp, label = "dot")
                        val color by animateColorAsState(if (selected) colors.primary else colors.outlineVariant, label = "dotColor")
                        Box(Modifier.height(8.dp).width(width).clip(CircleShape).background(color))
                    }
                }
                if (pager.currentPage < pages - 1) {
                    Button(onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } }) { Text("devam") }
                } else {
                    Button(onClick = Onboarding::finish) { Text("başla") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun WelcomeStep() {
    val colors = MaterialTheme.colorScheme
    Surface(shape = MaterialShapes.Sunny.toShape(), color = colors.primary, modifier = Modifier.size(120.dp)) {
        Box(contentAlignment = Alignment.Center) {
            val color = colors.onPrimary
            val logo = remember(color) { BrandIcons.logo(color, color) }
            Image(logo, contentDescription = null, modifier = Modifier.size(64.dp))
        }
    }
    Text("the sözlük", style = MaterialTheme.typography.displaySmall, color = colors.primary)
    Text(
        "ekşi sözlük'ü rahatça okumak için tasarlanmış modern bir uygulama.",
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center
    )
    Text(
        "ekşi sözlük'ün resmi uygulaması değildir.",
        style = MaterialTheme.typography.bodyMedium,
        color = colors.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun LookStep() {
    val colors = MaterialTheme.colorScheme
    Text("görünüm", style = MaterialTheme.typography.headlineMedium)
    Text(
        "dilediğin gibi seç, hemen değişir. hepsi sonra ayarlar → görünüm'den de değiştirilebilir.",
        style = MaterialTheme.typography.bodyMedium,
        color = colors.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
    // Theme
    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.material3.LocalMinimumInteractiveComponentSize provides 0.dp
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween), modifier = Modifier.fillMaxWidth()) {
            ThemeMode.entries.forEachIndexed { index, mode ->
                val shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    ThemeMode.entries.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                }
                val checked = AppSettings.themeMode.value == mode
                ToggleButton(
                    checked = checked,
                    onCheckedChange = { AppSettings.setThemeMode(mode) },
                    shapes = shapes,
                    modifier = Modifier.weight(1f).cardEdge(if (checked) shapes.checkedShape else shapes.shape)
                ) {
                    Icon(
                        when (mode) {
                            ThemeMode.System -> Icons.Rounded.BrightnessAuto
                            ThemeMode.Light -> Icons.Rounded.LightMode
                            ThemeMode.Dark -> Icons.Rounded.DarkMode
                        },
                        contentDescription = null,
                        modifier = Modifier.padding(end = 6.dp).size(18.dp)
                    )
                    Text(mode.label, maxLines = 1)
                }
            }
        }
    }
    // Palette
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        SeedColor.ordered.forEach { seed ->
            val selected = !AppSettings.dynamicColor.value && AppSettings.seed.value == seed
            val scheme = rememberSeedScheme(
                seed = seed,
                palette = AppSettings.palette.value,
                darkTheme = isAppInDarkTheme(),
                pureBlack = false,
                contrastLevel = 0.0
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(if (selected) MaterialShapes.Cookie9Sided.toShape() else CircleShape)
                        .clickable {
                            AppSettings.setSeed(seed)
                            AppSettings.setDynamicColor(false)
                        }
                ) {
                    Column(Modifier.fillMaxSize()) {
                        Box(Modifier.fillMaxWidth().weight(1f).background(scheme.primary))
                        Row(Modifier.fillMaxWidth().weight(1f)) {
                            Box(Modifier.fillMaxSize().weight(1f).background(scheme.secondaryContainer))
                            Box(Modifier.fillMaxSize().weight(1f).background(scheme.tertiary))
                        }
                    }
                    if (selected) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = "seçili",
                            tint = scheme.onPrimary,
                            modifier = Modifier.background(scheme.primary, CircleShape).padding(3.dp).size(16.dp)
                        )
                    }
                }
                Text(seed.label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
    // Liquid glass, with the bars' preview so it's clear what it does
    GlassPreview()
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth().cardEdge(RoundedCornerShape(20.dp))
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Column(Modifier.weight(1f)) {
                Text("sıvı cam", style = MaterialTheme.typography.titleMedium)
                Text(
                    "çubuklar ve pencereler cam gibi olur",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }
            Switch(
                checked = AppSettings.liquidGlass.value,
                enabled = AppSettings.blurEffects.value,
                onCheckedChange = AppSettings::setLiquidGlass
            )
        }
    }
}

/** The last step: a small drawn scene and a send-off, then "başla". */
@Composable
private fun ReadyStep() {
    val colors = MaterialTheme.colorScheme
    StateIllustration(icon = Icons.Rounded.AutoStories, modifier = Modifier.size(220.dp))
    Text("her şey hazır", style = MaterialTheme.typography.headlineMedium, color = colors.onSurface)
}
