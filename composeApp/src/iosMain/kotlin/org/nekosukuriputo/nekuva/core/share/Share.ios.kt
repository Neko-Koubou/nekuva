package org.nekosukuriputo.nekuva.core.share

import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIPasteboard
import platform.UIKit.UIWindow
import platform.UIKit.popoverPresentationController

actual fun shareText(text: String) {
    runCatching {
        UIPasteboard.generalPasteboard.string = text
        val window = UIApplication.sharedApplication.keyWindow
            ?: (UIApplication.sharedApplication.windows.firstOrNull() as? UIWindow)
        val rootVc = window?.rootViewController ?: return
        val activityVc = UIActivityViewController(
            activityItems = listOf(text),
            applicationActivities = null,
        )
        activityVc.popoverPresentationController?.sourceView = rootVc.view
        rootVc.presentViewController(activityVc, animated = true, completion = null)
    }
}
