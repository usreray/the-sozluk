package com.example.eksiscraper.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.eksiscraper.model.Entry

@Composable
fun EntryItem(entry: Entry, index: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "#${index}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            
            Text(
                text = entry.content,
                style = MaterialTheme.typography.bodyMedium
            )
            
            if (entry.author.isNotEmpty() || entry.date.isNotEmpty()) {
                Text(
                    text = buildString {
                        if (entry.author.isNotEmpty()) {
                            append(entry.author)
                        }
                        if (entry.author.isNotEmpty() && entry.date.isNotEmpty()) {
                            append(" - ")
                        }
                        if (entry.date.isNotEmpty()) {
                            append(entry.date)
                        }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
} 