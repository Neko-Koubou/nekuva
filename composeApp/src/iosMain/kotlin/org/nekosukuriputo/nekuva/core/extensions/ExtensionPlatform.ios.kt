package org.nekosukuriputo.nekuva.core.extensions

import java.io.File
import org.nekosukuriputo.nekuva.core.os.iosAppDataDir

actual fun extensionsDir(): File =
    File(iosAppDataDir(), "extensions").apply { mkdirs() }

actual val extensionPlatformKey: String = "ios"

actual val supportsExtensionImport: Boolean = false

actual suspend fun pickExtensionJar(): String? = null
