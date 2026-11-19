package org.nekosukuriputo.nekuva.core.diagnostics

import java.io.File
import org.nekosukuriputo.nekuva.core.os.iosAppDataDir

actual fun crashLogDir(): File =
    File(iosAppDataDir(), "crash")
