package com.example.eksiscraper.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** Jump to any page of a long topic with a slider, plus shortcuts to the ends. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PagePickerDialog(
    currentPage: Int,
    totalPages: Int,
    onPageSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var value by remember { mutableFloatStateOf(currentPage.toFloat()) }
    val page = value.roundToInt().coerceIn(1, totalPages)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sayfaya git") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "$page / $totalPages",
                    style = MaterialTheme.typography.displaySmallEmphasized,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Slider(
                    value = value,
                    onValueChange = { value = it },
                    valueRange = 1f..totalPages.toFloat()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = { value = 1f }, modifier = Modifier.weight(1f)) {
                        Text("ilk sayfa")
                    }
                    OutlinedButton(
                        onClick = { value = totalPages.toFloat() },
                        modifier = Modifier.weight(1f)
                    ) { Text("son sayfa") }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onPageSelected(page) }) { Text("Git") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } }
    )
}

@Composable
fun LoginRequiredDialog(onLogin: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.WaterDrop, contentDescription = null) },
        title = { Text("Giriş yapın") },
        text = { Text("Entry'leri favorilemek için ekşi sözlük hesabınızla giriş yapmanız gerekiyor.") },
        confirmButton = { TextButton(onClick = onLogin) { Text("Giriş yap") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } }
    )
}
