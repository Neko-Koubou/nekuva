package org.nekosukuriputo.nekuva.core.network.cookies

import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import platform.Foundation.NSHTTPCookie
import platform.Foundation.NSHTTPCookieStorage
import platform.WebKit.WKWebsiteDataStore

actual fun createCookieJar(): MutableCookieJar = MemoryCookieJar()

actual suspend fun syncBrowserCookies(url: String, cookieJar: MutableCookieJar) {
    val httpUrl = url.toHttpUrlOrNull() ?: return
    runCatching {
        val sharedCookies = NSHTTPCookieStorage.sharedHTTPCookieStorage.cookies?.mapNotNull { it as? NSHTTPCookie }.orEmpty()
        val converted = sharedCookies.mapNotNull { nsCookie ->
            val domain = nsCookie.domain.removePrefix(".")
            if (domain.isEmpty()) return@mapNotNull null
            Cookie.Builder()
                .name(nsCookie.name)
                .value(nsCookie.value)
                .domain(domain)
                .path(nsCookie.path.ifEmpty { "/" })
                .apply {
                    if (nsCookie.isSecure()) secure()
                    if (nsCookie.isHTTPOnly()) httpOnly()
                }
                .build()
        }
        if (converted.isNotEmpty()) {
            cookieJar.saveFromResponse(httpUrl, converted)
        }
    }
    runCatching {
        val wkCookies = suspendCancellableCoroutine<List<NSHTTPCookie>> { cont ->
            WKWebsiteDataStore.defaultDataStore().httpCookieStore.getAllCookies { list ->
                val cookies = list?.mapNotNull { it as? NSHTTPCookie }.orEmpty()
                if (cont.isActive) cont.resume(cookies)
            }
        }
        val converted = wkCookies.mapNotNull { nsCookie ->
            val domain = nsCookie.domain.removePrefix(".")
            if (domain.isEmpty()) return@mapNotNull null
            Cookie.Builder()
                .name(nsCookie.name)
                .value(nsCookie.value)
                .domain(domain)
                .path(nsCookie.path.ifEmpty { "/" })
                .apply {
                    if (nsCookie.isSecure()) secure()
                    if (nsCookie.isHTTPOnly()) httpOnly()
                }
                .build()
        }
        if (converted.isNotEmpty()) {
            cookieJar.saveFromResponse(httpUrl, converted)
        }
    }
}
