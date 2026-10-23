package com.thesozluk.app.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/**
 * The sheet's handle, which also grows the sheet: dragging it up opens the sheet over the whole
 * screen, dragging it down from there brings it back (and from the normal size, closes it).
 * The sheet reads [expanded] to fill the screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpandingSheetHandle(expanded: Boolean, onExpandedChange: (Boolean) -> Unit, onDismiss: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .pointerInput(expanded) {
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onVerticalDrag = { change, amount ->
                        change.consume()
                        total += amount
                    },
                    onDragEnd = {
                        val threshold = 40.dp.toPx()
                        when {
                            total < -threshold -> onExpandedChange(true)
                            total > threshold && expanded -> onExpandedChange(false)
                            total > threshold -> onDismiss()
                        }
                    }
                )
            }
    ) { BottomSheetDefaults.DragHandle() }
}
