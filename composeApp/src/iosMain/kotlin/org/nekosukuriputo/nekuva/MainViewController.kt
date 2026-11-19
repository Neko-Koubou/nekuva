package org.nekosukuriputo.nekuva

import androidx.compose.ui.window.ComposeUIViewController
import org.nekosukuriputo.nekuva.core.diagnostics.CrashReporter
import org.nekosukuriputo.nekuva.di.initKoin
import platform.UIKit.UIViewController

private var koinInitialized = false

fun initIosDependencies() {
    if (!koinInitialized) {
        koinInitialized = true
        CrashReporter.install()
        initKoin()
    }
}

fun MainViewController(): UIViewController {
    initIosDependencies()
    return ComposeUIViewController {
        App()
    }
}
