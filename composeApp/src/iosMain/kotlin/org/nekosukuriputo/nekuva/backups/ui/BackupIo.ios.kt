package org.nekosukuriputo.nekuva.backups.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import org.nekosukuriputo.nekuva.core.ui.getTopViewController
import org.nekosukuriputo.nekuva.ioscompat.inputStream
import org.nekosukuriputo.nekuva.ioscompat.outputStream
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UniformTypeIdentifiers.UTTypeData
import platform.UniformTypeIdentifiers.UTTypeZIP
import platform.darwin.NSObject

@Composable
actual fun rememberBackupIo(): BackupIo = remember { IosBackupIo }

private val activeDelegates = mutableSetOf<Any>()

private object IosBackupIo : BackupIo {

    override suspend fun pickAndWrite(
        defaultName: String,
        writer: suspend (OutputStream) -> Unit,
    ): Boolean {
        val tempFile = File(NSTemporaryDirectory(), defaultName)
        try {
            withContext(Dispatchers.IO) {
                tempFile.outputStream().use { writer(it) }
            }
            return withContext(Dispatchers.Main) {
                val topVc = getTopViewController() ?: return@withContext false
                val deferred = CompletableDeferred<Boolean>()
                val fileUrl = NSURL.fileURLWithPath(tempFile.absolutePath)
                val picker = UIDocumentPickerViewController(forExportingURLs = listOf(fileUrl), asCopy = true)

                val delegate = object : NSObject(), UIDocumentPickerDelegateProtocol {
                    override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
                        deferred.complete(true)
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
        } finally {
            tempFile.delete()
        }
    }

    override suspend fun pickAndRead(
        reader: suspend (InputStream) -> Unit,
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
                    val file = File(path)
                    if (file.exists()) {
                        file.inputStream().use {
                            // Using a blocking reader in a dispatcher
                            kotlinx.coroutines.runBlocking(Dispatchers.IO) {
                                reader(it)
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
                    if (accessed) {
                        url.stopAccessingSecurityScopedResource()
                    }
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
