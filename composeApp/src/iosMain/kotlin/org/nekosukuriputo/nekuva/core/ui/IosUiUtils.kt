package org.nekosukuriputo.nekuva.core.ui

import platform.UIKit.UIApplication
import platform.UIKit.UISceneActivationStateForegroundActive
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene

fun getTopViewController(): UIViewController? {
    val scene = UIApplication.sharedApplication.connectedScenes
        .filterIsInstance<UIWindowScene>()
        .firstOrNull { it.activationState == UISceneActivationStateForegroundActive }
        ?: UIApplication.sharedApplication.connectedScenes.filterIsInstance<UIWindowScene>().firstOrNull()
    val window = scene?.windows?.filterIsInstance<UIWindow>()?.firstOrNull { it.isKeyWindow() }
        ?: scene?.windows?.filterIsInstance<UIWindow>()?.firstOrNull()
        ?: UIApplication.sharedApplication.keyWindow
        ?: (UIApplication.sharedApplication.windows.firstOrNull() as? UIWindow)

    var top = window?.rootViewController
    while (top?.presentedViewController != null && !top.presentedViewController!!.isBeingDismissed()) {
        top = top.presentedViewController
    }
    return top
}
