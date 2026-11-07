package com.thesozluk.app.ui.components

import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

/** How much of the surface color a glass dialog keeps; enough to stay readable. */
private const val GLASS_DIALOG_ALPHA = 0.84f

/**
 * [AlertDialog] under "sıvı cam": it floats over the app like the bars, so it takes the same
 * glass. The page behind is frosted (SheetBlur), the dialog lets it show through a little and
 * its top edge catches the light. Without liquid glass it is the plain Material dialog.
 */
@Composable
fun AppAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    properties: DialogProperties = DialogProperties()
) {
    val glass = liquidGlass()
    if (glass) SheetBlur.BlurBehind()
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        modifier = if (glass) modifier.glassEdge(AlertDialogDefaults.shape) else modifier,
        dismissButton = dismissButton,
        icon = icon,
        title = title,
        text = text,
        containerColor = AlertDialogDefaults.containerColor.let { if (glass) it.copy(alpha = GLASS_DIALOG_ALPHA) else it },
        // Over glass a shadow shows as a dark rim around the see-through panel
        tonalElevation = if (glass) 0.dp else AlertDialogDefaults.TonalElevation,
        properties = properties
    )
}

/** The container color of a bottom sheet: see-through like the dialogs under liquid glass. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun glassSheetColor(): Color =
    BottomSheetDefaults.ContainerColor.let { if (liquidGlass()) it.copy(alpha = GLASS_DIALOG_ALPHA) else it }

/**
 * The sheet's drag handle, with the sheet's top edge catching the light under liquid glass.
 * The line is drawn here, along the full width of the sheet's top, because a modifier on the
 * sheet itself lands on the whole screen behind it rather than on the sheet.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun GlassSheetHandle() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (liquidGlass()) Modifier.glassEdge(BottomSheetDefaults.ExpandedShape) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        BottomSheetDefaults.DragHandle()
    }
}

/**
 * The app's bottom sheet: a card floating a little above the screen's edges, rounded on every
 * corner with a fine border, like the floating bars (and Nuvio's sheets). The page behind is
 * frosted by the sheet's content (SheetBlur); under liquid glass the card is see-through with
 * its lit top edge. Same states and gestures as [ModalBottomSheet], whose own surface is left
 * transparent around the card.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AppBottomSheet(
    onDismissRequest: () -> Unit,
    sheetState: androidx.compose.material3.SheetState,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp)
    val colors = androidx.compose.material3.MaterialTheme.colorScheme
    // The sheet's window colors its bar icons from the sheet's content color, which is left
    // at its default (dark) as the sheet's own surface is transparent; say it from the theme
    val dark = com.thesozluk.app.ui.theme.isAppInDarkTheme()
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = androidx.compose.ui.graphics.RectangleShape,
        containerColor = Color.Transparent,
        tonalElevation = 0.dp,
        dragHandle = null,
        properties = androidx.compose.material3.ModalBottomSheetProperties(
            isAppearanceLightStatusBars = !dark,
            isAppearanceLightNavigationBars = !dark
        )
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .padding(horizontal = 10.dp)
                .padding(bottom = 10.dp)
                .clip(shape)
                .background(glassSheetColor())
                .border(1.dp, colors.outlineVariant.copy(alpha = 0.5f), shape)
                .then(if (liquidGlass()) Modifier.glassEdge(shape) else Modifier)
        ) {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.material3.LocalContentColor provides colors.onSurface
            ) {
                // A small handle, quieter than Material's
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier
                            .padding(top = 10.dp, bottom = 14.dp)
                            .size(width = 36.dp, height = 4.dp)
                            .clip(CircleShape)
                            .background(colors.onSurfaceVariant.copy(alpha = 0.4f))
                    )
                }
                content()
            }
        }
    }
}
