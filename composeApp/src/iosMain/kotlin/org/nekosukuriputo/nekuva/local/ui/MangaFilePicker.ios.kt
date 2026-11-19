package org.nekosukuriputo.nekuva.local.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.io.File
import java.io.InputStream
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.nekosukuriputo.nekuva.core.ui.getTopViewController
import org.nekosukuriputo.nekuva.ioscompat.inputStream
import platform.Foundation.NSURL
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UniformTypeIdentifiers.UTTypeData
import platform.UniformTypeIdentifiers.UTTypeFolder
import platform.UniformTypeIdentifiers.UTTypeZIP
import platform.darwin.NSObject

@Composable
actual fun rememberMangaFilePicker(): MangaFilePicker = remember { IosMangaFilePicker }

private val activeDelegates = mutableSetOf<Any>()

private object IosMangaFilePicker : MangaFilePicker {

    override suspend fun pickCbz(
        onPicked: suspend (name: String, input: InputStream) -> Unit,
    ): Boolean = withContext(Dispatchers.Main) {
        val topVc = getTopViewController() ?: return@withContext false
        val deferred = CompletableDeferred<Boolean>()
        val picker = UIDocumentPickerViewController(
            forOpeningContentTypes = listOf(UTTypeZIP, UTTypeData),
            asCopy = true,
        )

        val delegate = object : NSObject(), UIDocumentPickerDelegateProtocol {
            override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
                val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL
                if (url == null) {
                    deferred.complete(false)
                    return
                }
                val accessed = url.startAccessingSecurityScopedResource()
                try {
                    val path = url.path ?: ""
                    val name = url.lastPathComponent ?: "imported.cbz"
                    val file = File(path)
                    if (file.exists()) {
                        file.inputStream().use { stream ->
                            runBlocking(Dispatchers.IO) {
                                onPicked(name, stream)
                            }
                        }
                        deferred.complete(true)
                    } else {
                        deferred.complete(false)
                    }
                } catch (e: Throwable) {
                    e.printStackTrace()
                    deferred.complete(false)
                } finally {
                    if (accessed) url.stopAccessingSecurityScopedResource()
                }
            }

            override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
                deferred.complete(false)
            }
        }
        activeDelegates.add(delegate)
        picker.delegate = delegate
        deferred.invokeOnCompletion { activeDelegates.remove(delegate) }
        topVc.presentViewController(picker, animated = true, completion = null)
        deferred.await()
    }

    override suspend fun pickDirectory(
        onPicked: suspend (name: String, copyInto: suspend (destDir: File) -> Unit) -> Unit,
    ): Boolean = withContext(Dispatchers.Main) {
        val topVc = getTopViewController() ?: return@withContext false
        val deferred = CompletableDeferred<Boolean>()
        val picker = UIDocumentPickerViewController(
            forOpeningContentTypes = listOf(UTTypeFolder),
            asCopy = false,
        )

        val delegate = object : NSObject(), UIDocumentPickerDelegateProtocol {
            override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
                val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL
                if (url == null) {
                    deferred.complete(false)
                    return
                }
                val accessed = url.startAccessingSecurityScopedResource()
                try {
                    val path = url.path ?: ""
                    val name = url.lastPathComponent ?: "imported"
                    val srcDir = File(path)
                    if (srcDir.exists() && srcDir.isDirectory()) {
                        runBlocking(Dispatchers.IO) {
                            onPicked(name) { destDir ->
                                srcDir.copyRecursively(destDir, overwrite = true)
                            }
                        }
                        deferred.complete(true)
                    } else {
                        deferred.complete(false)
                    }
                } catch (e: Throwable) {
                    e.printStackTrace()
                    deferred.complete(false)
                } finally {
                    if (accessed) url.stopAccessingSecurityScopedResource()
                }
            }

            override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
                deferred.complete(false)
            }
        }
        activeDelegates.add(delegate)
        picker.delegate = delegate
        deferred.invokeOnCompletion { activeDelegates.remove(delegate) }
        topVc.presentViewController(picker, animated = true, completion = null)
        deferred.await()
    }
}
