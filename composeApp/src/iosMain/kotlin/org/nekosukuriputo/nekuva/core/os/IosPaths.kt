package org.nekosukuriputo.nekuva.core.os

import java.io.File
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSHomeDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

internal fun iosDocumentsDir(): File {
    val paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
    val docPath = (paths.firstOrNull() as? String) ?: "${NSHomeDirectory()}/Documents"
    return File(docPath).apply { mkdirs() }
}

internal fun iosCachesDir(): File {
    val paths = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true)
    val cachePath = (paths.firstOrNull() as? String) ?: "${NSHomeDirectory()}/Library/Caches"
    return File(cachePath).apply { mkdirs() }
}

internal fun iosAppDataDir(): File =
    File(iosDocumentsDir(), ".nekuva").apply { mkdirs() }
