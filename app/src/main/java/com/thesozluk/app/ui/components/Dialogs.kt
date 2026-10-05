package com.thesozluk.app.ui.components

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Jump to any page of a long topic: type the page, or step to the ends / by 10 (100 in very long
 * topics). Kept compact; a slider can't hit an exact page among hundreds anyway.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PagePickerDialog(
    currentPage: Int,
    totalPages: Int,
    onPageSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var page by remember { mutableIntStateOf(currentPage.coerceIn(1, totalPages)) }
    // The number starts selected, so typing replaces it straight away
    var typed by remember { mutableStateOf(page.toString().let { TextFieldValue(it, TextRange(0, it.length)) }) }
    fun set(value: Int) {
        page = value.coerceIn(1, totalPages)
        typed = page.toString().let { TextFieldValue(it, TextRange(0, it.length)) }
    }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val bigStep = if (totalPages > 200) 100 else 10

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("sayfaya git") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = typed,
                        onValueChange = { value ->
                            val digits = value.text.filter(Char::isDigit).take(6)
                            typed = value.copy(text = digits, selection = TextRange(minOf(value.selection.end, digits.length)))
                            digits.toIntOrNull()?.let { page = it.coerceIn(1, totalPages) }
                        },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.titleLarge.copy(textAlign = TextAlign.Center),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = { onPageSelected(page) }),
                        modifier = Modifier
                            .width(96.dp)
                            .focusRequester(focus)
                            // Tapping the box again selects the whole number too
                            .onFocusChanged { state ->
                                if (state.isFocused) typed = typed.copy(selection = TextRange(0, typed.text.length))
                            }
                    )
                    Text(
                        "/ $totalPages",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 10.dp)
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StepButton("ilk", Modifier.weight(1f)) { set(1) }
                    StepButton("-$bigStep", Modifier.weight(1f)) { set(page - bigStep) }
                    StepButton("+$bigStep", Modifier.weight(1f)) { set(page + bigStep) }
                    StepButton("son", Modifier.weight(1f)) { set(totalPages) }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onPageSelected(page) }) { Text("git") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("vazgeç") } }
    )
}

@Composable
private fun StepButton(label: String, modifier: Modifier, onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick, modifier = modifier, contentPadding = PaddingValues(horizontal = 4.dp)) {
        Text(label, maxLines = 1)
    }
}

@Composable
fun LoginRequiredDialog(onLogin: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(FavoriteIcons.DropFilled, contentDescription = null) },
        title = { Text("giriş yapın") },
        text = { Text("entry'leri favorilemek için ekşi sözlük hesabınızla giriş yapmanız gerekiyor.") },
        confirmButton = { TextButton(onClick = onLogin) { Text("giriş yap") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("vazgeç") } }
    )
}
