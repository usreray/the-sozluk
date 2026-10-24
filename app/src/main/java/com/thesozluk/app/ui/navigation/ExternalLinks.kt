package com.thesozluk.app.ui.navigation

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.ui.platform.UriHandler

/**
 * Opens outside links the way people expect: in the app that owns the address when one is
 * installed (YouTube, X, Instagram, ...), otherwise in a browser tab over the app
 * ([openInAppTab]), so reading isn't left behind for a separate browser.
 */
class ExternalLinkHandler(private val context: Context, private val toolbarColor: Int) : UriHandler {
    override fun openUri(uri: String) {
        val parsed = Uri.parse(uri)
        if (parsed.scheme != "http" && parsed.scheme != "https") {
            // mailto:, tel:, market: and the like go to whatever handles them
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, parsed).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            return
        }
        // Only an app that isn't a browser takes it here; browsers are left to the tab below
        val native = Intent(Intent.ACTION_VIEW, parsed)
            .addCategory(Intent.CATEGORY_BROWSABLE)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REQUIRE_NON_BROWSER)
        try {
            context.startActivity(native)
        } catch (e: ActivityNotFoundException) {
            openInAppTab(context, uri, toolbarColor)
        }
    }
}

/** A browser tab on top of the app (Custom Tabs), colored like the app's bars. */
fun openInAppTab(context: Context, url: String, toolbarColor: Int) {
    val colors = CustomTabColorSchemeParams.Builder().setToolbarColor(toolbarColor).build()
    val intent = CustomTabsIntent.Builder()
        .setDefaultColorSchemeParams(colors)
        .setShowTitle(true)
        .setShareState(CustomTabsIntent.SHARE_STATE_ON)
        .build()
    intent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        intent.launchUrl(context, Uri.parse(url))
    } catch (e: ActivityNotFoundException) {
        // No browser that supports tabs: hand it to anything that can show it
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
}
