package com.example.eksiscraper.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.example.eksiscraper.network.EksiNetworkDataSource
import com.example.eksiscraper.network.EksiSession
import kotlinx.coroutines.launch

/**
 * Shows eksisozluk.com's own login page. The password never passes through the app:
 * once the login is confirmed we copy the WebView's cookies and go back to the app.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LoginScreen(navController: NavController) {
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    Scaffold { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        CookieManager.getInstance().setAcceptCookie(true)
                        webViewClient = object : WebViewClient() {
                            // onPageFinished can fire again after we leave; pop only once
                            private var loggedIn = false

                            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                                isLoading = true
                            }

                            override fun onPageFinished(view: WebView, url: String) {
                                isLoading = false
                                CookieManager.getInstance().flush()
                                // Still on the login form (or its error page): wait for the user
                                if (loggedIn || url.contains("/giris")) return
                                val cookies = EksiSession.webViewCookies()
                                val userAgent = view.settings.userAgentString
                                scope.launch {
                                    // Landing on the login-only page proves it; anywhere else, ask the site
                                    val confirmed =
                                            Uri.parse(url).path?.startsWith(EksiSession.LOGGED_IN_PATH) == true ||
                                                    EksiNetworkDataSource.isLoggedIn(cookies, userAgent)
                                    if (confirmed && !loggedIn) {
                                        loggedIn = true
                                        EksiSession.saveLogin(cookies, userAgent)
                                        navController.popBackStack()
                                    } else if (!confirmed) {
                                        // Wandered off without logging in: back to the form
                                        view.loadUrl(EksiSession.LOGIN_URL)
                                    }
                                }
                            }
                        }
                        loadUrl(EksiSession.LOGIN_URL)
                    }
                }
            )

            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter)
                )
            }
        }
    }
}
