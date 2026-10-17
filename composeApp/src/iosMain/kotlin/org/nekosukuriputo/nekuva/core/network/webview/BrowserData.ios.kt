package org.nekosukuriputo.nekuva.core.network.webview

import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koin.core.context.GlobalContext
import org.nekosukuriputo.nekuva.core.network.cookies.MutableCookieJar
import platform.Foundation.NSDate
import platform.Foundation.NSHTTPCookie
import platform.Foundation.NSHTTPCookieStorage
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.WebKit.WKWebsiteDataStore

actual suspend fun clearBrowserData() {
    runCatching {
        GlobalContext.get().get<MutableCookieJar>().clear()
    }
    runCatching {
        val storage = NSHTTPCookieStorage.sharedHTTPCookieStorage
        storage.cookies?.forEach { cookie ->
            (cookie as? NSHTTPCookie)?.let { storage.deleteCookie(it) }
        }
    }
    runCatching {
        suspendCancellableCoroutine<Unit> { cont ->
            val dataStore = WKWebsiteDataStore.defaultDataStore()
            val types = WKWebsiteDataStore.allWebsiteDataTypes()
            dataStore.removeDataOfTypes(
                dataTypes = types,
                modifiedSince = NSDate.dateWithTimeIntervalSince1970(0.0),
            ) {
                if (cont.isActive) cont.resume(Unit)
            }
        }
    }
}
