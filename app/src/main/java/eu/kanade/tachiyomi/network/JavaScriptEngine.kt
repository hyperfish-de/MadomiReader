package eu.kanade.tachiyomi.network

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Chạy JavaScript bằng một WebView ẩn — một vài extension dùng để giải mã tham số
 * hoặc lấy token từ trang web.
 *
 * @since tachiyomix 1.4
 */
class JavaScriptEngine(private val context: Context) {

    @SuppressLint("SetJavaScriptEnabled")
    suspend fun <T> evaluate(script: String): T = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            val webView = WebView(context)
            try {
                webView.settings.javaScriptEnabled = true
                webView.webViewClient = WebViewClient()
                webView.evaluateJavascript(script) { raw ->
                    val decoded = raw
                        ?.takeIf { it != "null" }
                        ?.removeSurrounding("\"")
                        ?.replace("\\u003d", "=")
                        ?.replace("\\u0026", "&")
                        ?.replace("\\\"", "\"")
                    @Suppress("UNCHECKED_CAST")
                    continuation.resume(decoded as T)
                    runCatching { webView.destroy() }
                }
            } catch (error: Throwable) {
                runCatching { webView.destroy() }
                continuation.resumeWithException(error)
            }
        }
    }
}
