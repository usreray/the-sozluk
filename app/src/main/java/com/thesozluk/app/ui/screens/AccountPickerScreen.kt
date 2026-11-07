package com.thesozluk.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.thesozluk.app.network.EksiSession
import com.thesozluk.app.ui.components.AppAlertDialog
import com.thesozluk.app.ui.components.AuthorAvatar
import com.thesozluk.app.ui.components.BrandIcons
import com.thesozluk.app.ui.components.FloatingTopBar
import com.thesozluk.app.ui.components.motionReduced
import com.thesozluk.app.ui.components.rememberAvatarUrl
import com.thesozluk.app.ui.navigation.Screen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Choosing the account to read with, like Nuvio's profile picker: every account signed in on
 * this device as a big avatar in a two-column grid, the current one marked, and a tile to add
 * one more. "hesapları yönet" turns on removing: a cross on the others, tapping one asks to
 * forget it here. Opened from the profile tab's "hesap değiştir".
 */
@Composable
fun AccountPickerScreen(navController: NavController) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val current = EksiSession.nick.value
    val accounts = EksiSession.accounts.value.ifEmpty {
        // Signed in before the device kept a list: at least the current one
        listOfNotNull(current?.let { EksiSession.SavedAccount(it, EksiSession.cookies, EksiSession.userAgent) })
    }
    var editing by remember { mutableStateOf(false) }
    var forgetting by remember { mutableStateOf<EksiSession.SavedAccount?>(null) }

    forgetting?.let { account ->
        AppAlertDialog(
            onDismissRequest = { forgetting = null },
            title = { Text(account.nick) },
            text = { Text("bu hesap bu cihazdan kaldırılsın mı? tekrar eklemek için giriş yapman gerekir.") },
            confirmButton = {
                TextButton(onClick = {
                    EksiSession.forget(account)
                    forgetting = null
                }) { Text("kaldır", color = colors.error) }
            },
            dismissButton = { TextButton(onClick = { forgetting = null }) { Text("vazgeç") } }
        )
    }

    // Title and tiles come in one after another
    val still = motionReduced()
    val header = remember { Animatable(if (still) 1f else 0f) }
    LaunchedEffect(Unit) { header.animateTo(1f, tween(600, easing = FastOutSlowInEasing)) }

    Scaffold(
        topBar = { FloatingTopBar(title = null, onBack = { navController.popBackStack() }) }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                // A soft glow of the theme's color from the top, behind everything
                .background(
                    Brush.verticalGradient(
                        0f to colors.primaryContainer.copy(alpha = 0.35f),
                        0.55f to colors.surface
                    )
                )
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(top = padding.calculateTopPadding(), start = 24.dp, end = 24.dp, bottom = 32.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.graphicsLayer {
                        alpha = header.value
                        translationY = (1f - header.value) * 20.dp.toPx()
                    }
                ) {
                    Spacer(Modifier.height(24.dp))
                    val logoColor = colors.primary
                    val logo = remember(logoColor) { BrandIcons.logo(logoColor, logoColor) }
                    Image(logo, contentDescription = null, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(18.dp))
                    Text(
                        "kim okuyor?",
                        style = MaterialTheme.typography.headlineLarge.copy(fontSize = 30.sp, letterSpacing = 0.sp),
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface
                    )
                }
                Spacer(Modifier.height(40.dp))
                // Two to a row; the add tile last
                val tiles = accounts.size + 1
                for (row in 0 until (tiles + 1) / 2) {
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.padding(bottom = 20.dp)) {
                        for (col in 0..1) {
                            val index = row * 2 + col
                            when {
                                index < accounts.size -> {
                                    val account = accounts[index]
                                    val isCurrent = account.nick.equals(current, ignoreCase = true)
                                    AccountTile(
                                        index = index,
                                        nick = account.nick,
                                        isCurrent = isCurrent,
                                        removable = editing && !isCurrent,
                                        onClick = {
                                            when {
                                                editing && !isCurrent -> forgetting = account
                                                isCurrent -> navController.popBackStack()
                                                else -> {
                                                    EksiSession.switchTo(account)
                                                    restartApp(context)
                                                }
                                            }
                                        }
                                    )
                                }
                                index == accounts.size -> AddTile(index) {
                                    EksiSession.prepareToAddAccount()
                                    navController.navigate(Screen.Login.route)
                                }
                                else -> Spacer(Modifier.width(150.dp))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                // Manage: on, the others get a cross and a tap removes them
                if (accounts.size > 1) {
                    val shape = RoundedCornerShape(24.dp)
                    Text(
                        if (editing) "tamam" else "hesapları yönet",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = if (editing) colors.primary else colors.onSurfaceVariant,
                        modifier = Modifier
                            .clip(shape)
                            .background(if (editing) colors.primary.copy(alpha = 0.15f) else androidx.compose.ui.graphics.Color.Transparent)
                            .border(
                                1.dp,
                                if (editing) colors.primary.copy(alpha = 0.4f) else colors.onSurfaceVariant.copy(alpha = 0.3f),
                                shape
                            )
                            .clickable { editing = !editing }
                            .padding(horizontal = 24.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}

/** Fades, rises and grows in after the ones before it; presses in a little while held. */
@Composable
private fun Modifier.tileMotion(index: Int, interaction: MutableInteractionSource): Modifier {
    val still = motionReduced()
    val appear = remember { Animatable(if (still) 1f else 0f) }
    LaunchedEffect(Unit) {
        delay(index * 80L + 150L)
        appear.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
    }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed && !still) 0.95f else 1f, label = "tilePress")
    return graphicsLayer {
        val t = appear.value
        alpha = t
        val s = (0.85f + 0.15f * t) * press
        scaleX = s
        scaleY = s
        translationY = (1f - t) * 30.dp.toPx()
    }
}

@Composable
private fun AccountTile(index: Int, nick: String, isCurrent: Boolean, removable: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(150.dp)
            .tileMotion(index, interaction)
            .clip(RoundedCornerShape(20.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(8.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(110.dp)) {
            // A halo of the theme's color, stronger around the account in use
            Box(
                Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(colors.primary.copy(alpha = if (isCurrent) 0.3f else 0.15f))
            )
            AuthorAvatar(nick, size = 100, avatarUrl = rememberAvatarUrl(nick), modifier = Modifier.clip(CircleShape))
            when {
                removable -> Badge(Icons.Rounded.Close, colors.error, colors.onError, "kaldır")
                isCurrent -> Badge(Icons.Rounded.Check, colors.primary, colors.onPrimary, "şu anki hesap")
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            nick,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = colors.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.Badge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    container: androidx.compose.ui.graphics.Color,
    content: androidx.compose.ui.graphics.Color,
    description: String
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .offset(x = (-2).dp, y = (-2).dp)
            .size(34.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .padding(2.dp)
            .clip(CircleShape)
            .background(container)
    ) {
        Icon(icon, contentDescription = description, tint = content, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun AddTile(index: Int, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(150.dp)
            .tileMotion(index, interaction)
            .clip(RoundedCornerShape(20.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(8.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(110.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(colors.surfaceVariant.copy(alpha = 0.5f))
                    .border(2.dp, colors.onSurfaceVariant.copy(alpha = 0.2f), CircleShape)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, tint = colors.onSurfaceVariant.copy(alpha = 0.7f), modifier = Modifier.size(40.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "hesap ekle",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
