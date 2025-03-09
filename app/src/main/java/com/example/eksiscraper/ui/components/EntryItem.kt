package com.example.eksiscraper.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.eksiscraper.model.Entry
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.text.TextLayoutResult

@Composable
fun EntryItem(
    entry: Entry, 
    index: Int,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit
) {
    // Track whether text is actually visually truncated
    var isTextTruncated by remember { mutableStateOf(false) }
    
    // Count explicit line breaks for rough estimation (used as a fallback)
    val explicitLineBreaks = entry.content.count { it == '\n' } + 1
    val mightBeLongEntry = explicitLineBreaks > 5  // Lower threshold for estimation
    
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 12.dp),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 2.dp
        ),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Entry number and date
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "#$index",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                if (entry.date.isNotEmpty()) {
                    Text(
                        text = entry.date,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Content with proper spacing
            Spacer(modifier = Modifier.height(12.dp))
            
            // Display the content with proper paragraph spacing
            Text(
                text = entry.content,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = MaterialTheme.typography.bodyLarge.fontSize * 1.5f,
                letterSpacing = MaterialTheme.typography.bodyLarge.letterSpacing,
                softWrap = true,
                // If not expanded, limit to 7 lines
                maxLines = if (!isExpanded) 7 else Int.MAX_VALUE,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { textLayoutResult ->
                    // Check if the text is actually truncated (more lines exist but aren't shown)
                    isTextTruncated = textLayoutResult.hasVisualOverflow || 
                                     textLayoutResult.lineCount > 7
                }
            )
            
            // Show expand/collapse button only if text is actually truncated or already expanded
            if (isTextTruncated || (mightBeLongEntry && isExpanded)) {
                TextButton(
                    onClick = { onExpandToggle() },
                    modifier = Modifier.padding(top = 4.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = if (isExpanded) "collapse" else "expand",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            // Author with icon
            if (entry.author.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Author",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text(
                        text = entry.author,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
} 