package org.nekosukuriputo.nekuva.core.image

import java.io.File
import okio.Path
import org.nekosukuriputo.nekuva.core.os.iosCachesDir

actual fun faviconCacheDir(): Path =
    File(iosCachesDir(), "favicons").apply { mkdirs() }.toOkioPath()
