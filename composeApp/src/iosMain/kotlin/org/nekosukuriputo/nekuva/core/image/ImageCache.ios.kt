package org.nekosukuriputo.nekuva.core.image

import coil3.PlatformContext
import java.io.File
import okio.Path
import org.nekosukuriputo.nekuva.core.os.iosCachesDir

actual fun imageDiskCacheDir(context: PlatformContext): Path =
    File(iosCachesDir(), "image_cache").apply { mkdirs() }.toOkioPath()
