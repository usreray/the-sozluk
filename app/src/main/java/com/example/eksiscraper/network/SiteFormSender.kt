package com.example.eksiscraper.network

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import kotlin.coroutines.resume

/**
 * Submits a form exactly as the site's own page would, in an invisible WebView that carries the
 * login. Used where a plain HTTP post is accepted but silently dropped by ekşi (sending messages).
 */
object SiteFormSender {

    /**
     * Opens [pagePath], fills the fields of the form [formSelector] with [values] and submits it.
     * Returns null on success or an error message.
     */
    @SuppressLint("SetJavaScriptEnabled")
    suspend fun submit(
        context: Context,
        pagePath: String,
        formSelector: String,
        values: Map<String, String>
    ): String? = withContext(Dispatchers.Main) {
        try {
            withTimeout(30_000) {
                suspendCancellableCoroutine<String?> { continuation ->
                    val webView = WebView(context.applicationContext)
                    webView.settings.javaScriptEnabled = true
                    webView.settings.domStorageEnabled = true
                    EksiSession.userAgent?.let { webView.settings.userAgentString = it }
                    var submitted = false

                    fun finish(result: String?) {
                        if (!continuation.isActive) return
                        webView.stopLoading()
                        webView.destroy()
                        continuation.resume(result)
                    }

                    // Only the HTML matters for filling and posting a form
                    webView.settings.blockNetworkImage = true
                    webView.settings.loadsImagesAutomatically = false
                    webView.webViewClient = object : WebViewClient() {
                        // Ads, scripts, styles and images make the page slow; skip everything but
                        // the page itself
                        override fun shouldInterceptRequest(
                            view: WebView,
                            request: android.webkit.WebResourceRequest
                        ): android.webkit.WebResourceResponse? =
                            if (request.isForMainFrame) null
                            else android.webkit.WebResourceResponse("text/plain", "utf-8", java.io.ByteArrayInputStream(ByteArray(0)))

                        override fun onPageFinished(view: WebView, url: String) {
                            if (submitted) {
                                if (url.contains("/giris")) {
                                    finish("Oturum süresi dolmuş; tekrar giriş yap")
                                    return
                                }
                                // A sent message redirects to its conversation; anything else is a
                                // rejection whose reason the site shows on the page
                                val landed = android.net.Uri.parse(url).path.orEmpty()
                                if (Regex("^/mesaj/\\d+$").matches(landed)) {
                                    finish(null)
                                    return
                                }
                                view.evaluateJavascript(READ_NOTICES) { raw ->
                                    val notice = runCatching { org.json.JSONArray("[$raw]").getString(0) }.getOrNull()
                                        ?.trim().orEmpty()
                                    android.util.Log.d("EksiForm", "site form landed on ${android.net.Uri.parse(url).path} notice=${notice.ifEmpty { "-" }}")
                                    finish(notice.ifEmpty { null })
                                }
                                return
                            }
                            val fill = values.entries.joinToString("") { (name, value) ->
                                "var el=f.querySelector('[name=\"$name\"]');if(!el)return 'missing:$name';el.value=${JSONObject.quote(value)};"
                            }
                            val script = "(function(){var f=document.querySelector(${JSONObject.quote(formSelector)});" +
                                "if(!f)return 'noform';$fill f.submit();return 'ok';})()"
                            view.evaluateJavascript(script) { result ->
                                when (result?.trim('"')) {
                                    "ok" -> submitted = true
                                    "noform" -> finish("Form bulunamadı; sayfa değişmiş olabilir")
                                    else -> finish("Form doldurulamadı ($result)")
                                }
                            }
                        }
                    }
                    continuation.invokeOnCancellation { webView.post { webView.destroy() } }
                    webView.loadUrl(EksiSession.BASE_URL + pagePath)
                }
            }
        } catch (e: Exception) {
            "Gönderilemedi: ${e.message ?: "zaman aşımı"}"
        }
    }

    // Visible validation / notice boxes the site uses for errors and warnings (its own texts)
    private const val READ_NOTICES = """
(function() {
  var sel = '.field-validation-error,.validation-summary-errors,#message-validation-result,' +
            '.error,.errors,.warning,.notice,.alert,#notice,.toast,.message-error,.info-message,#flash';
  var out = [];
  document.querySelectorAll(sel).forEach(function(el) {
    var t = (el.innerText || '').trim();
    if (t && el.offsetParent !== null && t.length < 300 && out.indexOf(t) < 0) out.push(t);
  });
  return out.join(' · ');
})();
"""
}
