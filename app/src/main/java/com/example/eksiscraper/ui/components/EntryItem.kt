package com.example.eksiscraper.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.eksiscraper.network.EksiService
import kotlinx.coroutines.launch
import androidx.compose.ui.res.painterResource
import com.example.eksiscraper.R

@Composable
fun EntryItem(
    entry: Entry, 
    index: Int,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    isFavorite: Boolean = false,  // This parameter will be ignored now
    onFavoriteToggle: (String) -> Unit = {}
) {
    // Track whether text is actually visually truncated
    var isTextTruncated by remember { mutableStateOf(false) }
    
    // Count explicit line breaks for rough estimation (used as a fallback)
    val explicitLineBreaks = entry.content.count { it == '\n' } + 1
    val mightBeLongEntry = explicitLineBreaks > 5  // Lower threshold for estimation
    
    // Create a coroutine scope for making network requests
    val coroutineScope = rememberCoroutineScope()
    
    // Use rememberSaveable with a key based on entryId to persist state across scrolling
    // Local UI state for favorite status - initialize with server state
    var isLocallyFavorited by rememberSaveable(key = "fav_${entry.entryId}") { 
        mutableStateOf(entry.isFavorited) 
    }
    
    // Local UI state for favorite count - initialize with server count
    var localFavoriteCount by rememberSaveable(key = "count_${entry.entryId}") { 
        mutableStateOf(entry.favoriteCount) 
    }
    
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 12.dp),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 3.dp
        ),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
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
            
            // Bottom section starting with favorite count (left-aligned)
            Spacer(modifier = Modifier.height(16.dp))
            
            // Favorite count - left aligned, always visible
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Start
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Use different icon and color based on favorite state
                    if (isLocallyFavorited) {
                        IconButton(
                            onClick = { 
                                // Store current state before toggling
                                val currentlyFavorited = isLocallyFavorited
                                
                                // Toggle local UI state for immediate feedback
                                isLocallyFavorited = !isLocallyFavorited
                                
                                // Decrease favorite count by 1 when unfavoriting
                                localFavoriteCount = (localFavoriteCount - 1).coerceAtLeast(0)
                                
                                // Call the toggle function with the current favorite state before toggling
                                onFavoriteToggle(entry.entryId)
                                
                                // Make request to unfavorite using the favlama endpoint
                                coroutineScope.launch {
                                    try {
                                        // We were favorited, so we need to unfavorite
                                        EksiService.unfavoriteEntry(entry.entryId)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.thumb_up_24px_filled),
                                contentDescription = "Unlike entry",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        IconButton(
                            onClick = { 
                                // Store current state before toggling
                                val currentlyFavorited = isLocallyFavorited
                                
                                // Toggle local UI state for immediate feedback
                                isLocallyFavorited = !isLocallyFavorited
                                
                                // Increase favorite count by 1 when favoriting
                                localFavoriteCount += 1
                                
                                // Call the toggle function with the current favorite state before toggling
                                onFavoriteToggle(entry.entryId)
                                
                                // Make request to favorite using the favla endpoint
                                coroutineScope.launch {
                                    try {
                                        // We were not favorited, so we need to favorite
                                        EksiService.favoriteEntry(entry.entryId)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.thumb_up_24px),
                                contentDescription = "Like entry",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    // Only show the number if it's greater than 0
                    if (localFavoriteCount > 0) {
                        Text(
                            text = localFavoriteCount.toString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Spacer between favorite and author info
            Spacer(modifier = Modifier.height(8.dp))
            
            // Author info right-aligned
            if (entry.author.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Username and date in a column
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        // Username - adjusted for long usernames
                        Text(
                            text = entry.author,
                            style = MaterialTheme.typography.labelLarge, // Smaller but still prominent
                            color = MaterialTheme.colorScheme.primary,
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                            softWrap = true
                        )
                        
                        // Date row (if date exists)
                        if (entry.date.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = entry.date,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    // User icon next to both username and timestamp
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Author",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }
    }
} 