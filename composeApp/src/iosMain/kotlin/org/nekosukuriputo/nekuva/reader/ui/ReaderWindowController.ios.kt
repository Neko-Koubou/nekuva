package org.nekosukuriputo.nekuva.reader.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UIApplication

@Composable
actual fun rememberReaderWindowController(): ReaderWindowController = remember { IosReaderWindowController }

private object IosReaderWindowController : ReaderWindowController {
    override val supportsOrientation: Boolean = false

    override fun apply(keepScreenOn: Boolean, fullscreen: Boolean, orientationIndex: Int) {
        runCatching {
            UIApplication.sharedApplication.idleTimerDisabled = keepScreenOn
        }
    }

    override fun toggleOrientationLock() {}

    override fun reset() {
        runCatching {
            UIApplication.sharedApplication.idleTimerDisabled = false
        }
    }
}
