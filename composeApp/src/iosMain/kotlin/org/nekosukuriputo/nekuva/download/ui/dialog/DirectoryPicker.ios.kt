package org.nekosukuriputo.nekuva.download.ui.dialog

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.nekosukuriputo.nekuva.core.ui.getTopViewController
import platform.Foundation.NSURL
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UniformTypeIdentifiers.UTTypeFolder
import platform.darwin.NSObject

actual val supportsDirectoryPicker: Boolean = true

private val activeDelegates = mutableSetOf<Any>()

actual suspend fun pickMangaDirectory(): String? = withContext(Dispatchers.Main) {
    val topVc = getTopViewController() ?: return@withContext null
    val deferred = CompletableDeferred<String?>()
    val picker = UIDocumentPickerViewController(forOpeningContentTypes = listOf(UTTypeFolder), asCopy = false)
    picker.allowsMultipleSelection = false

    val delegate = object : NSObject(), UIDocumentPickerDelegateProtocol {
        override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
            val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL
            if (url != null) {
                url.startAccessingSecurityScopedResource()
                deferred.complete(url.path)
            } else {
                deferred.complete(null)
            }
        }

        override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
            deferred.complete(null)
        }
    }
    activeDelegates.add(delegate)
    picker.delegate = delegate
    deferred.invokeOnCompletion {
        activeDelegates.remove(delegate)
    }
    topVc.presentViewController(picker, animated = true, completion = null)
    deferred.await()
}
