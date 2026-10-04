package com.thesozluk.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.thesozluk.app.R

@Composable
fun CreatorLinks(modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedButton(
            onClick = { uriHandler.openUri("https://github.com/usreray") },
            contentPadding = PaddingValues(horizontal = 12.dp)
        ) {
            Icon(painterResource(R.drawable.ic_github), contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Text("usreray")
        }
        OutlinedButton(
            onClick = { uriHandler.openUri("https://buymeacoffee.com/usreray") },
            contentPadding = PaddingValues(horizontal = 12.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.buymeacoffee_logo),
                contentDescription = null,
                modifier = Modifier.height(22.dp).width(15.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text("buy me a coffee")
        }
    }
}
