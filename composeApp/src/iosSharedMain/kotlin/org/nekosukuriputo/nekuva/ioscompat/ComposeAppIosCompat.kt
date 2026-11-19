package org.nekosukuriputo.nekuva.ioscompat

import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.Closeable
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.Reader
import java.net.URI
import java.util.concurrent.TimeUnit

typealias Runnable = kotlinx.coroutines.Runnable

fun interface UncaughtExceptionHandler {
    fun uncaughtException(t: Thread, e: Throwable)
}

private var defaultUncaughtHandler: UncaughtExceptionHandler? = null

fun Thread.Companion.getDefaultUncaughtExceptionHandler(): UncaughtExceptionHandler? =
    defaultUncaughtHandler

fun Thread.Companion.setDefaultUncaughtExceptionHandler(eh: UncaughtExceptionHandler?) {
    defaultUncaughtHandler = eh
}

fun String.byteInputStream(): ByteArrayInputStream = ByteArrayInputStream(encodeToByteArray())

fun Closeable.closeSafe() {
    try {
        close()
    } catch (_: Throwable) {
    }
}

fun File.outputStream(): FileOutputStream = FileOutputStream(this)

fun File.inputStream(): FileInputStream = FileInputStream(this)

fun File.setLastModified(time: Long): Boolean = true

val File.isHidden: Boolean
    get() = name.startsWith(".")

val File.totalSpace: Long
    get() = usableSpace

fun File.walkTopDown(): Sequence<File> = sequence {
    if (!exists()) return@sequence
    yield(this@walkTopDown)
    if (isDirectory()) {
        listFiles()?.forEach { child ->
            yieldAll(child.walkTopDown())
        }
    }
}

fun File.walk(): Sequence<File> = walkTopDown()

fun InputStream.copyTo(out: OutputStream, bufferSize: Int = 8192): Long {
    var bytesCopied: Long = 0
    val buffer = ByteArray(bufferSize)
    var bytes = read(buffer)
    while (bytes >= 0) {
        out.write(buffer, 0, bytes)
        bytesCopied += bytes
        bytes = read(buffer)
    }
    return bytesCopied
}

fun InputStream.bufferedReader(): BufferedReader = BufferedReader(InputStreamReader(this))

fun InputStream.reader(): InputStreamReader = InputStreamReader(this)

fun Reader.readText(): String = when (this) {
    is BufferedReader -> this.readText()
    is InputStreamReader -> this.readText()
    else -> {
        val sb = StringBuilder()
        val buf = CharArray(4096)
        while (true) {
            val n = read(buf, 0, buf.size)
            if (n < 0) break
            sb.appendRange(buf, 0, n)
        }
        sb.toString()
    }
}

val URI.userInfo: String?
    get() = null

fun buildMultiArgUri(
    scheme: String?,
    userInfo: String?,
    host: String?,
    port: Int,
    path: String?,
    query: String?,
    fragment: String?,
): URI {
    val sb = StringBuilder()
    if (!scheme.isNullOrEmpty()) {
        sb.append(scheme).append(':')
    }
    if (!host.isNullOrEmpty()) {
        sb.append("//")
        if (!userInfo.isNullOrEmpty()) {
            sb.append(userInfo).append('@')
        }
        sb.append(host)
        if (port > 0) {
            sb.append(':').append(port)
        }
    }
    if (!path.isNullOrEmpty()) {
        sb.append(path)
    }
    if (!query.isNullOrEmpty()) {
        sb.append('?').append(query)
    }
    if (!fragment.isNullOrEmpty()) {
        sb.append('#').append(fragment)
    }
    return URI(sb.toString())
}

fun okhttp3.CacheControl.Companion.parse(headers: okhttp3.Headers): okhttp3.CacheControl {
    val raw = headers["Cache-Control"] ?: return okhttp3.CacheControl.Builder().build()
    val b = okhttp3.CacheControl.Builder()
    for (part in raw.split(',')) {
        val token = part.trim().lowercase()
        when {
            token == "no-store" -> b.noStore()
            token == "no-cache" -> b.noCache()
            token == "only-if-cached" -> b.onlyIfCached()
            token.startsWith("max-age=") -> token.removePrefix("max-age=").toIntOrNull()?.let {
                b.maxAge(it, TimeUnit.SECONDS)
            }
        }
    }
    return b.build()
}
