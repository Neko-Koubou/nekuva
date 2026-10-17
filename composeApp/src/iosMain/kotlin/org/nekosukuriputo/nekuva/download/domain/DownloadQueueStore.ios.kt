package org.nekosukuriputo.nekuva.download.domain

import java.io.File
import org.nekosukuriputo.nekuva.core.os.iosAppDataDir

actual fun downloadQueueFile(): File =
    File(iosAppDataDir(), "downloads_queue.json")
