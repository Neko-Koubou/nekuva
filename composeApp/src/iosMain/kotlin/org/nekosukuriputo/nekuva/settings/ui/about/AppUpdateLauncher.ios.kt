package org.nekosukuriputo.nekuva.settings.ui.about

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.nekosukuriputo.nekuva.core.github.AppVersion
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

@Composable
actual fun rememberAppUpdateLauncher(): AppUpdateLauncher = remember { IosAppUpdateLauncher }

private object IosAppUpdateLauncher : AppUpdateLauncher {

    override val canInstallInApp: Boolean = false

    override fun startUpdate(version: AppVersion) {
        val url = version.url.ifEmpty { version.apkUrl }
        if (url.isEmpty()) return
        runCatching {
            val nsUrl = NSURL.URLWithString(url) ?: return
            UIApplication.sharedApplication.openURL(nsUrl, options = emptyMap<Any?, Any>(), completionHandler = null)
        }
    }
}
