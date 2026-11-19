package org.nekosukuriputo.nekuva.local.data

import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.runInterruptible
import okhttp3.Cache
import org.nekosukuriputo.nekuva.core.os.iosAppDataDir
import org.nekosukuriputo.nekuva.core.os.iosCachesDir
import org.nekosukuriputo.nekuva.core.prefs.AppSettings
import org.nekosukuriputo.nekuva.core.util.ext.isReadable

private const val DIR_NAME = "NekuvaManga"

class IosLocalStorageManager(
    private val settings: AppSettings,
) : LocalStorageManager {

    private val appDataDir: File
        get() = iosAppDataDir()

    private val cachesDir: File
        get() = iosCachesDir()

    override suspend fun getReadableDirs(): List<File> = runInterruptible(Dispatchers.IO) {
        getConfiguredStorageDirs().filter { it.isReadable() }
    }

    override suspend fun getWriteableDirs(): List<File> = runInterruptible(Dispatchers.IO) {
        getConfiguredStorageDirs().filter { it.canWrite() }
    }

    override suspend fun getDefaultWriteableDir(): File? = runInterruptible(Dispatchers.IO) {
        val preferredDir = settings.mangaStorageDir?.let { File(it) }?.takeIf { it.canWrite() }
        preferredDir ?: getFallbackStorageDir()?.takeIf { it.canWrite() }
    }

    override suspend fun getApplicationStorageDirs(): Set<File> = runInterruptible(Dispatchers.IO) {
        getAvailableStorageDirs()
    }

    override fun isOnExternalStorage(file: File): Boolean = false

    override fun hasExternalStoragePermission(isReadOnly: Boolean): Boolean = true

    override suspend fun getDirectoryDisplayName(dir: File, isFullPath: Boolean): String =
        runInterruptible(Dispatchers.IO) {
            if (isFullPath) dir.path else dir.name
        }

    override fun createHttpCache(): Cache =
        Cache(File(cachesDir, "http_cache").apply { mkdirs() }, HTTP_CACHE_SIZE_BYTES)

    override suspend fun computeCacheSize(cache: CacheDir): Long = runInterruptible(Dispatchers.IO) {
        File(cachesDir, cache.dir).dirSizeBytes()
    }

    override suspend fun clearCache(cache: CacheDir): Unit = runInterruptible(Dispatchers.IO) {
        File(cachesDir, cache.dir).deleteContentsRecursively()
    }

    override suspend fun computeCacheSize(): Long = runInterruptible(Dispatchers.IO) {
        listOf("image_cache", "favicons", CacheDir.PAGES.dir, "http_cache", "adblock")
            .sumOf { File(cachesDir, it).dirSizeBytes() }
    }

    override suspend fun computeStorageSize(): Long = runInterruptible(Dispatchers.IO) {
        getConfiguredStorageDirs().sumOf { it.dirSizeBytes() }
    }

    override suspend fun computeAvailableSize(): Long = runInterruptible(Dispatchers.IO) {
        runCatching { appDataDir.usableSpace }.getOrDefault(0L)
    }

    override fun adblockListFile(): File =
        File(File(cachesDir, "adblock").apply { mkdirs() }, "easylist.txt")

    private fun getConfiguredStorageDirs(): MutableSet<File> {
        val set = getAvailableStorageDirs()
        settings.userSpecifiedMangaDirectories.forEach { set += File(it) }
        return set
    }

    private fun getAvailableStorageDirs(): MutableSet<File> {
        val result = LinkedHashSet<File>()
        result += File(appDataDir, DIR_NAME)
        result.retainAll { it.exists() || it.mkdirs() }
        return result
    }

    private fun getFallbackStorageDir(): File? {
        return File(appDataDir, DIR_NAME).takeIf {
            it.exists() || it.mkdirs()
        }
    }
}
