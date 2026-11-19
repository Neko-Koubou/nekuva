package org.nekosukuriputo.nekuva.core.parser

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect as SkiaRect
import org.nekosukuriputo.nekuva.parsers.bitmap.Bitmap
import org.nekosukuriputo.nekuva.parsers.bitmap.Rect
import platform.JavaScriptCore.JSContext

private const val IOS_DEFAULT_UA =
    "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1"

actual fun platformDefaultUserAgent(): String? = IOS_DEFAULT_UA

actual suspend fun evaluateJsPlatform(baseUrl: String, script: String): String? =
    withContext(Dispatchers.Default) {
        runCatching {
            val context = JSContext() ?: return@runCatching null
            if (baseUrl.isNotBlank()) {
                val escapedUrl = baseUrl.replace("\\", "\\\\").replace("\"", "\\\"")
                context.evaluateScript(
                    """
                    var window = this;
                    var self = this;
                    var globalThis = this;
                    var location = { href: "$escapedUrl", origin: "$escapedUrl" };
                    var document = { URL: "$escapedUrl", location: location, cookie: "" };
                    var navigator = { userAgent: "$IOS_DEFAULT_UA", platform: "iPhone" };
                    """.trimIndent()
                )
            }
            val result = context.evaluateScript(script) ?: return@runCatching null
            if (result.isUndefined() || result.isNull()) {
                null
            } else {
                result.toString()
            }
        }.getOrNull()
    }

actual fun createBitmapPlatform(width: Int, height: Int): Bitmap {
    val skiaBitmap = org.jetbrains.skia.Bitmap().apply {
        allocN32Pixels(width, height)
    }
    return IosSkiaBitmap(skiaBitmap)
}

actual fun redrawImageResponsePlatform(response: Response, redraw: (image: Bitmap) -> Bitmap): Response {
    val bytes = response.body.bytes()
    val skiaImage = Image.makeFromEncoded(bytes)
    val srcBitmap = org.jetbrains.skia.Bitmap.makeFromImage(skiaImage)
    val result = redraw(IosSkiaBitmap(srcBitmap)) as IosSkiaBitmap
    val outImage = Image.makeFromBitmap(result.skiaBitmap)
    val encoded = outImage.encodeToData(EncodedImageFormat.PNG)
        ?: throw IllegalStateException("Failed to encode redrawn bitmap to PNG")
    return response.newBuilder()
        .body(encoded.bytes.toResponseBody("image/png".toMediaType()))
        .build()
}

internal class IosSkiaBitmap(val skiaBitmap: org.jetbrains.skia.Bitmap) : Bitmap {

    private val canvas by lazy { Canvas(skiaBitmap) }
    private val paint = Paint()

    override val width: Int get() = skiaBitmap.width
    override val height: Int get() = skiaBitmap.height

    override fun drawBitmap(sourceBitmap: Bitmap, src: Rect, dst: Rect) {
        val srcSkia = (sourceBitmap as IosSkiaBitmap).skiaBitmap
        val image = Image.makeFromBitmap(srcSkia)
        canvas.drawImageRect(
            image,
            SkiaRect.makeLTRB(src.left.toFloat(), src.top.toFloat(), src.right.toFloat(), src.bottom.toFloat()),
            SkiaRect.makeLTRB(dst.left.toFloat(), dst.top.toFloat(), dst.right.toFloat(), dst.bottom.toFloat()),
            paint,
        )
    }
}
