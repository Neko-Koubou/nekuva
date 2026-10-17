package coil3.network.okhttp

import coil3.ImageLoader
import coil3.Uri
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.network.httpHeaders
import coil3.request.Options
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.Buffer
import okio.FileSystem

fun OkHttpNetworkFetcherFactory(
    callFactory: () -> OkHttpClient,
): Fetcher.Factory<Uri> = IosOkHttpNetworkFetcherFactory(callFactory)

private class IosOkHttpNetworkFetcherFactory(
    private val callFactory: () -> OkHttpClient,
) : Fetcher.Factory<Uri> {
    override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
        val scheme = data.scheme?.lowercase()
        if (scheme != "http" && scheme != "https") return null
        return IosOkHttpFetcher(data.toString(), options, imageLoader, callFactory)
    }
}

private class IosOkHttpFetcher(
    private val url: String,
    private val options: Options,
    private val imageLoader: ImageLoader,
    private val callFactory: () -> OkHttpClient,
) : Fetcher {
    override suspend fun fetch(): FetchResult? = withContext(Dispatchers.IO) {
        val diskCache = imageLoader.diskCache
        val cacheKey = options.diskCacheKey ?: url

        if (options.diskCachePolicy.readEnabled && diskCache != null) {
            diskCache.openSnapshot(cacheKey)?.let { snapshot ->
                return@withContext SourceFetchResult(
                    source = ImageSource(
                        file = snapshot.data,
                        fileSystem = diskCache.fileSystem,
                        diskCacheKey = cacheKey,
                        closeable = snapshot,
                    ),
                    mimeType = null,
                    dataSource = DataSource.DISK,
                )
            }
        }

        val reqBuilder = Request.Builder().url(url)
        for ((name, values) in options.httpHeaders.asMap()) {
            for (value in values) {
                reqBuilder.addHeader(name, value)
            }
        }

        val response = callFactory().newCall(reqBuilder.build()).execute()
        if (!response.isSuccessful) {
            response.close()
            throw okio.IOException("HTTP ${response.code}: ${response.message}")
        }

        val body = response.body
        val mimeType = body.contentType()?.let { "${it.type}/${it.subtype}" }
        val bytes = try {
            body.bytes()
        } finally {
            response.close()
        }

        if (options.diskCachePolicy.writeEnabled && diskCache != null && bytes.isNotEmpty()) {
            val editor = diskCache.openEditor(cacheKey)
            if (editor != null) {
                try {
                    diskCache.fileSystem.write(editor.data) {
                        write(bytes)
                    }
                    val snapshot = editor.commitAndOpenSnapshot()
                    if (snapshot != null) {
                        return@withContext SourceFetchResult(
                            source = ImageSource(
                                file = snapshot.data,
                                fileSystem = diskCache.fileSystem,
                                diskCacheKey = cacheKey,
                                closeable = snapshot,
                            ),
                            mimeType = mimeType,
                            dataSource = DataSource.NETWORK,
                        )
                    }
                } catch (e: Throwable) {
                    runCatching { editor.abort() }
                }
            }
        }

        val buffer = Buffer().write(bytes)
        SourceFetchResult(
            source = ImageSource(source = buffer, fileSystem = FileSystem.SYSTEM),
            mimeType = mimeType,
            dataSource = DataSource.NETWORK,
        )
    }
}
