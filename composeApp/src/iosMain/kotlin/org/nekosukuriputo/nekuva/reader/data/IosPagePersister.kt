package org.nekosukuriputo.nekuva.reader.data

import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import org.nekosukuriputo.nekuva.core.os.iosDocumentsDir
import org.nekosukuriputo.nekuva.core.prefs.AppSettings
import org.nekosukuriputo.nekuva.reader.domain.PagePersister
import platform.Foundation.NSURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIWindow
import platform.UIKit.popoverPresentationController

class IosPagePersister(
    private val settings: AppSettings,
) : PagePersister {

    private val defaultDir: File
        get() = settings.getPagesSaveDirUri()?.let { File(it) }?.takeIf { it.isDirectory() || it.mkdirs() }
            ?: File(iosDocumentsDir(), "Pictures/Nekuva").apply { mkdirs() }

    override suspend fun savePage(bytes: ByteArray, fileName: String, mimeType: String): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val target = File(defaultDir, fileName)
                target.writeBytes(bytes)
                target.absolutePath
            }.getOrNull()
        }

    override suspend fun sharePage(bytes: ByteArray, fileName: String, mimeType: String): String? {
        val path = savePage(bytes, fileName, mimeType) ?: return null
        withContext(Dispatchers.Main) {
            runCatching {
                val window = UIApplication.sharedApplication.keyWindow
                    ?: (UIApplication.sharedApplication.windows.firstOrNull() as? UIWindow)
                val rootVc = window?.rootViewController ?: return@runCatching
                val fileUrl = NSURL.fileURLWithPath(path)
                val activityVc = UIActivityViewController(
                    activityItems = listOf(fileUrl),
                    applicationActivities = null,
                )
                activityVc.popoverPresentationController?.sourceView = rootVc.view
                rootVc.presentViewController(activityVc, animated = true, completion = null)
            }
        }
        return path
    }
}
