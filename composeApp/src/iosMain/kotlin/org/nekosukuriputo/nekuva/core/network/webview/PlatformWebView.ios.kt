@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.nekosukuriputo.nekuva.core.network.webview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.cinterop.readValue
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSURL
import platform.WebKit.WKNavigation
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.darwin.NSObject

@Composable
actual fun PlatformWebView(
    url: String,
    state: WebViewState,
    modifier: Modifier,
    userAgent: String?,
) {
    val delegate = remember(state) {
        object : NSObject(), WKNavigationDelegateProtocol {
            @ObjCSignatureOverride
            override fun webView(
                webView: WKWebView,
                didStartProvisionalNavigation: WKNavigation?,
            ) {
                state.isLoading = true
                state.progress = 0.2f
                webView.URL?.absoluteString?.let { state.currentUrl = it }
            }

            @ObjCSignatureOverride
            override fun webView(
                webView: WKWebView,
                didFinishNavigation: WKNavigation?,
            ) {
                state.isLoading = false
                state.progress = 1.0f
                webView.URL?.absoluteString?.let { state.currentUrl = it }
                webView.title?.takeIf { it.isNotBlank() }?.let { state.title = it }
                state.canGoBack = webView.canGoBack
            }
        }
    }

    UIKitView(
        factory = {
            val config = WKWebViewConfiguration()
            val webView = WKWebView(frame = CGRectZero.readValue(), configuration = config)
            webView.navigationDelegate = delegate
            userAgent?.takeIf { it.isNotBlank() }?.let { ua ->
                webView.customUserAgent = ua
            }

            state.goBackAction = { if (webView.canGoBack) webView.goBack() }
            state.reloadAction = { webView.reload() }
            state.evaluateJsAction = { script, onResult ->
                webView.evaluateJavaScript(script) { result, _ ->
                    onResult(result?.toString())
                }
            }

            NSURL.URLWithString(url)?.let { nsUrl ->
                val req = NSMutableURLRequest.requestWithURL(nsUrl)
                webView.loadRequest(req)
            }
            webView
        },
        modifier = modifier,
    )

    DisposableEffect(state) {
        onDispose {
            state.goBackAction = null
            state.reloadAction = null
            state.evaluateJsAction = null
        }
    }
}
